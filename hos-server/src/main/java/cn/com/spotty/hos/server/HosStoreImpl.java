package cn.com.spotty.hos.server;

import cn.com.spotty.hos.common.HosObject;
import cn.com.spotty.hos.common.HosObjectSummary;
import cn.com.spotty.hos.common.ObjectListResult;
import cn.com.spotty.hos.common.ObjectMetaData;
import cn.com.spotty.hos.common.util.JsonUtil;
import cn.com.spotty.hos.core.ErrorCodes;
import com.google.common.base.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.io.ByteBufferInputStream;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.util.HostUtil;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class HosStoreImpl implements IHosStore {
    private static Logger logger = Logger.getLogger(HosStoreImpl.class);
    private Connection connection = null;
    private IHdfsService fileStore;
    private String zkUrls;
    private CuratorFramework zkClient;

    public HosStoreImpl(Connection connection, IHdfsService fileStore, String zkUrls) throws IOException {
        this.connection = connection;
        this.fileStore = fileStore;
        this.zkUrls = zkUrls;
        this.zkClient = CuratorFrameworkFactory.newClient(zkUrls, new ExponentialBackoffRetry(20, 5));
        this.zkClient.start();
    }

    @Override
    public void createBucketStore(String bucket) throws IOException {
        // 1. 创建目录表
        HBaseServiceImpl.createTable(connection, HosUtil.getDirTableName(bucket), HosUtil.getDirColumnFamily(), null);
        // 2. 创建文件表
        HBaseServiceImpl.createTable(connection, HosUtil.getObjTableName(bucket), HosUtil.getObjColumnFamily(), HosUtil.OBJ_REGIONS);
        // 3. 将其添加到seq表
        Put put = new Put(bucket.getBytes());
        put.addColumn(HosUtil.BUCKET_DIR_SEQ_CF_BYTES, HosUtil.BUCKET_DIR_SEQ_QUALIFIER, Bytes.toBytes(0));
        HBaseServiceImpl.putRow(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, put);
        // 4. 创建hdfs目录
        fileStore.mkDir(HosUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void deleteBucketStore(String bucket) throws IOException {
        // 删除目录表和文件表
        HBaseServiceImpl.deleteTable(connection, HosUtil.getDirTableName(bucket));
        HBaseServiceImpl.deleteTable(connection, HosUtil.getObjTableName(bucket));
        // 删除seq表中的记录
        HBaseServiceImpl.deleteRow(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, bucket);
        // 删除hdfs目录
        fileStore.deleteDir(HosUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void createSeqTable() throws IOException {
        HBaseServiceImpl.createTable(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, new String[]{HosUtil.BUCKET_DIR_SEQ_CF});
    }

    @Override
    public void put(String bucket, String key, ByteBuffer content, long length, String mediaType, Map<String, String> properties) throws Exception {
        InterProcessMutex lock = null;
        try {
            // 判断是否创建目录
            if (key.endsWith("/")) {
                putDir(bucket, key);
                return;
            }
            // 获取seqid
            String dir = key.substring(0, key.lastIndexOf("/") + 1);
            String hash = null;
            while (hash == null) {
                if (!dirExist(bucket, dir)) {
                    hash = putDir(bucket, dir);
                } else {
                    hash = getDirSeqId(bucket, dir);
                }
            }

            // 上传文件到文件表
            //   获取锁
            String lockKey = key.replaceAll("/", "_");
            lock = new InterProcessMutex(this.zkClient, "/hos/" + bucket + "/" + lockKey);
            lock.acquire();

            // 上传文件
            String fileKey = hash + "_" + key.substring(key.lastIndexOf("/") + 1);
            Put contentPut = new Put(fileKey.getBytes());
            if (!Strings.isNullOrEmpty(mediaType)) {
                contentPut.addColumn(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_MEDIATYPE_QUALIFIER, mediaType.getBytes());
            }
            if (properties != null) {
                String props = JsonUtil.toJson(properties);
                contentPut.addColumn(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_PROPS_QUALIFIER, props.getBytes());
            }
            contentPut.addColumn(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER, Bytes.toBytes(length));

            // 判断文件大小，小于20m存储到hbase,否则存储到hdfs
            if (length <= HosUtil.FILE_STORE_THRESHOLD) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(HosUtil.OBJ_CONT_QUALIFIER);
                contentPut.addColumn(HosUtil.OBJ_CONT_CF_BYTES, byteBuffer, System.currentTimeMillis(), content);
                byteBuffer.clear();
            } else {
                String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + hash;
                String name = key.substring(key.lastIndexOf("/") + 1);
                InputStream inputStream = new ByteBufferInputStream(content);
                this.fileStore.saveFile(fileDir, name, inputStream, length, getBucketReplication(bucket));
            }
            HBaseServiceImpl.putRow(connection, HosUtil.getObjTableName(bucket), contentPut);
        } finally {
            if (lock != null) {
                lock.release();
            }
        }
    }

    @Override
    public HosObjectSummary getSummary(String bucket, String key) throws IOException {
        // 判断是否为文件夹
        if (key.endsWith("/")) {
            Result result = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
            if (!result.isEmpty()) {
                // 读取文件夹的基础属性 转换为 HosObjectSummary
                return this.dirObjectToSummary(result, bucket, key);
            }
            return null;
        }
        // 获取文件的基本属性
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String seq = getDirSeqId(bucket, dir);
        if (seq == null) {
            return null;
        }
        String objKey = seq + "_" + key.substring(key.lastIndexOf("/") + 1);
        Result result = HBaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), objKey);
        if (result.isEmpty()) {
            return null;
        }
        return this.resultToObjectSummary(result, bucket, dir);
    }

    @Override
    public List<HosObjectSummary> list(String bucket, String startKey, String endKey) throws IOException {
        String dir1 = startKey.substring(0, startKey.lastIndexOf("/") + 1).trim();
        if (dir1.length() == 0) {
            dir1 = "/";
        }
        String dir2 = endKey.substring(0, endKey.lastIndexOf("/") + 1).trim();
        if (dir2.length() == 0) {
            dir2 = "/";
        }
        String name1 = startKey.substring(startKey.lastIndexOf("/") + 1);
        String name2 = endKey.substring(endKey.lastIndexOf("/") + 1);
        String seqId = this.getDirSeqId(bucket, dir1);
        // 查询dir1中大于name1的全部文件
        List<HosObjectSummary> keys = new ArrayList<>();
        if (seqId != null && name1.length() > 0) {
            byte[] max = Bytes.createMaxByteArray(100);
            byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
            if (dir1.equals(dir2)) {
                tail = (seqId + "_" + name2).getBytes();
            }
            byte[] start = (seqId + "_" + name1).getBytes();
            ResultScanner scanner1 = HBaseServiceImpl.getScanner(connection, HosUtil.getObjTableName(bucket), start, tail);
            Result result = null;
            while ((result = scanner1.next()) != null) {
                HosObjectSummary summary = this.resultToObjectSummary(result, bucket, dir1);
                keys.add(summary);
            }
            if (scanner1 != null) {
                scanner1.close();
            }
        }
        // startKey~endKey之间的全部目录
        ResultScanner scanner2 = HBaseServiceImpl.getScanner(connection, HosUtil.getDirTableName(bucket), startKey, endKey);
        Result result = null;
        while ((result = scanner2.next()) != null) {
            String seqId2 = Bytes.toString(result.getValue(HosUtil.DIR_META_CF_BYTES, HosUtil.DIR_SEQID_QUALIFIER));
            if (seqId2 == null) {
                continue;
            }
            String dir = Bytes.toString(result.getRow());
            keys.add(dirObjectToSummary(result, bucket, dir));
            this.getDirAllFiles(bucket, dir, seqId2, keys, endKey);
        }
        if (scanner2 != null) {
            scanner2.close();
        }
        Collections.sort(keys);
        return keys;
    }

    @Override
    public ObjectListResult listDir(String bucket, String dir, String start, int maxCount) throws IOException {
        // 查询目录表
        // 混合两者查询结果返回给用户
        start = Strings.nullToEmpty(start);
        Get get = new Get(Bytes.toBytes(dir));
        get.addFamily(HosUtil.DIR_SUBDIR_CF_BYTES);
        if (start.length() > 0) {
            get.setFilter(new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(Bytes.toBytes(start))));
        }
        Result dirResult = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), get);
        List<HosObjectSummary> subDirs = null;
        if (!dirResult.isEmpty()) {
            subDirs = new ArrayList<>();
            for (Cell cell : dirResult.rawCells()) {
                HosObjectSummary summary = new HosObjectSummary();
                byte[] qualifierBytes = new byte[cell.getQualifierLength()];
                CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
                String name = Bytes.toString(qualifierBytes);
                summary.setKey(dir + name + "/");
                summary.setName(name);
                summary.setLastModifyTime(cell.getTimestamp());
                summary.setMediaType("");
                summary.setBucket(bucket);
                subDirs.add(summary);
                if (subDirs.size() >= maxCount + 1) {
                    break;
                }
            }
        }

        // 查询文件表
        String dirSeq = this.getDirSeqId(bucket, dir);
        byte[] objStart = Bytes.toBytes(dirSeq + "_" + start);
        Scan objScan = new Scan();
        objScan.setStartRow(objStart);
        objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_"));
        objScan.setMaxResultsPerColumnFamily(maxCount + 1);
        // 这里只需要获取文件的基本属性，限定只获取meta cf列簇
        objScan.addFamily(HosUtil.OBJ_META_CF_BYTES);
        ResultScanner objScanner = HBaseServiceImpl.getScanner(connection, HosUtil.getObjTableName(bucket), objScan);
        List<HosObjectSummary> objectSummaryList = new ArrayList<>();
        Result result = null;
        while (objectSummaryList.size() < maxCount + 2 && (result = objScanner.next()) != null) {
            HosObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
            objectSummaryList.add(summary);
        }
        if (objScanner != null) {
            objScanner.close();
        }
        logger.info("scan complete: " + Bytes.toString(objStart) + "-");
        if (subDirs != null && subDirs.size() > 0) {
            objectSummaryList.addAll(subDirs);
        }
        Collections.sort(objectSummaryList);
        if (objectSummaryList.size() > maxCount) {
            objectSummaryList = objectSummaryList.subList(0, maxCount);
        }

        ObjectListResult listResult = new ObjectListResult();
        HosObjectSummary nextMarkerObj = objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size() - 1) : null;
        if (nextMarkerObj != null) {
            listResult.setNextMarker(nextMarkerObj.getKey());
        }
        listResult.setMaxKeyNumber(maxCount);
        if (objectSummaryList.size() > 0) {
            listResult.setMinKey(objectSummaryList.get(0).getKey());
            listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
        }
        listResult.setObjectCount(objectSummaryList.size());
        listResult.setObjectSummaryList(objectSummaryList);
        listResult.setBucket(bucket);
        return listResult;
    }

    @Override
    public ObjectListResult listByPrefix(String bucket, String dir, String keyPrefix, String start, int maxCount) throws IOException {
        if (start == null) {
            start = "";
        }
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filterList.addFilter(new ColumnPrefixFilter(keyPrefix.getBytes()));
        if (start.length() > 0) {
            filterList.addFilter(new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(Bytes.toBytes(start))));
        }
        int maxCount1 = maxCount + 2;
        Result dirResult = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), dir, filterList);
        List<HosObjectSummary> subDirs = null;
        if (!dirResult.isEmpty()) {
            subDirs = new ArrayList<>();
            for (Cell cell : dirResult.rawCells()) {
                HosObjectSummary summary = new HosObjectSummary();
                byte[] qualifierBytes = new byte[cell.getQualifierLength()];
                CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
                String name = Bytes.toString(qualifierBytes);
                summary.setKey(dir + name + "/");
                summary.setName(name);
                summary.setLastModifyTime(cell.getTimestamp());
                summary.setMediaType("");
                summary.setBucket(bucket);
                subDirs.add(summary);
                if (subDirs.size() >= maxCount1) {
                    break;
                }
            }
        }
        String dirSeq = this.getDirSeqId(bucket, dir);
        byte[] objStart = Bytes.toBytes(dirSeq + "_" + start);
        Scan objScan = new Scan();
        objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_" + keyPrefix));
        objScan.setFilter(new PageFilter(maxCount1 + 1));
        objScan.setStartRow(objStart);
        objScan.setMaxResultsPerColumnFamily(maxCount1);
        objScan.addFamily(HosUtil.OBJ_META_CF_BYTES);
        logger.info("scan start:" + Bytes.toString(objStart) + "-");
        ResultScanner objectScanner = HBaseServiceImpl.getScanner(connection, HosUtil.getObjTableName(bucket), objScan);
        List<HosObjectSummary> objectSummaryList = new ArrayList<>();
        Result result = null;
        while ((result = objectScanner.next()) != null) {
            HosObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
            objectSummaryList.add(summary);
        }
        if (objectScanner != null) {
            objectScanner.close();
        }
        logger.info("scan complete:" + Bytes.toString(objStart) + "-");
        if (subDirs != null && subDirs.size() > 0) {
            objectSummaryList.addAll(subDirs);
        }
        Collections.sort(objectSummaryList);
        ObjectListResult listResult = new ObjectListResult();
        HosObjectSummary nextMarkerObj = objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size() - 1) : null;
        if (nextMarkerObj != null) {
            listResult.setNextMarker(nextMarkerObj.getKey());
        }
        if (objectSummaryList.size() > maxCount) {
            objectSummaryList = objectSummaryList.subList(0, maxCount);
        }
        listResult.setMaxKeyNumber(maxCount);
        if (objectSummaryList.size() > 0) {
            listResult.setMinKey(objectSummaryList.get(0).getKey());
            listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
        }
        listResult.setObjectCount(objectSummaryList.size());
        listResult.setObjectSummaryList(objectSummaryList);
        listResult.setBucket(bucket);

        return listResult;
    }

    @Override
    public HosObject getObject(String bucket, String key) throws IOException {
        // 判断是否为目录
        if (key.endsWith("/")) {
            // 读取目录表
            Result result = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
            if (result.isEmpty()) {
                return null;
            }
            ObjectMetaData metaData = new ObjectMetaData();
            metaData.setBucket(bucket);
            metaData.setKey(key);
            metaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
            metaData.setLength(0);

            HosObject object = new HosObject();
            object.setMetaData(metaData);
            return object;
        }

        // 读取文件表
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String name = key.substring(key.lastIndexOf("/") + 1);
        String seq = getDirSeqId(bucket, dir);
        String objKey = seq + "_" + name;
        Result result = HBaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), objKey);
        if (result.isEmpty()) {
            return null;
        }
        HosObject object = new HosObject();
        long len = Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER));
        ObjectMetaData metaData = new ObjectMetaData();
        metaData.setBucket(bucket);
        metaData.setKey(key);
        metaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
        metaData.setLength(len);
        metaData.setMediaType(Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_MEDIATYPE_QUALIFIER)));
        object.setMetaData(metaData);
        byte[] b = result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_PROPS_QUALIFIER);
        if (b != null) {
            metaData.setAttrs(JsonUtil.fromJson(Map.class, Bytes.toString(b)));
        }

        if (result.containsNonEmptyColumn(HosUtil.OBJ_CONT_CF_BYTES, HosUtil.OBJ_CONT_QUALIFIER)) {
            ByteArrayInputStream bas = new ByteArrayInputStream(result.getValue(HosUtil.OBJ_CONT_CF_BYTES, HosUtil.OBJ_CONT_QUALIFIER));
            object.setContent(bas);
        } else {
            String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seq;
            InputStream inputStream = fileStore.openFile(fileDir, name);
            object.setContent(inputStream);
        }
        return object;
    }


    @Override
    public void deleteObject(String bucket, String key) throws Exception {
        // 判断当前key是否为目录
        if (key.endsWith("/")) {
            // 删除目录
            // 判断目录是否为空
            if (!isDirEmpty(bucket, key)) {
                throw new HosServerException(ErrorCodes.ERROR_PERMISSION_DENIED, "dir is not empty");
            }
            // 获取锁
            InterProcessMutex lock = null;
            String lockKey = key.replaceAll("/", "_");
            lock = new InterProcessMutex(this.zkClient, "/hos/" + bucket + "/" + lockKey);
            lock.acquire();
            // 从父目录删除数据
            String dir1 = key.substring(0, key.lastIndexOf("/"));
            String name = dir1.substring(dir1.lastIndexOf("/") + 1);
            if (name.length() > 0) {
                String parent = key.substring(0, key.lastIndexOf("/"));
                // 从父目录sub列簇中删除
                HBaseServiceImpl.deleteColumnQualifier(connection, HosUtil.getDirTableName(bucket), parent, HosUtil.DIR_SUBDIR_CF, name);
            }
            // 从目录表删除数据
            HBaseServiceImpl.deleteRow(connection, HosUtil.getDirTableName(bucket), key);
            // 释放锁
            lock.release();
            return;
        }

        // 删除文件
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String name = key.substring(key.lastIndexOf("/") + 1);
        String seqId = this.getDirSeqId(bucket, dir);
        String objKey = seqId + "_" + name;
        Result result = HBaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), objKey, HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER);
        if (result.isEmpty()) {
            return;
        }
        // 从文件表获取文件的length,通过length判断资源存储在hdfs还是在hbase
        long len = Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER));
        if (len > HosUtil.FILE_STORE_THRESHOLD) {
            String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seqId;
            this.fileStore.deleteFile(fileDir, name);
        }
        // 无论资源资源存放在hdfs还是hbase, hbase中rowKey的都是要删除的
        HBaseServiceImpl.deleteRow(connection, HosUtil.getObjTableName(bucket), objKey);
    }

    private boolean dirExist(String bucket, String dir) {
        return HBaseServiceImpl.existsRow(connection, HosUtil.getDirTableName(bucket), dir);
    }

    private String getDirSeqId(String bucket, String dir) {
        Result result = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), dir);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return Bytes.toString(result.getValue(HosUtil.DIR_META_CF_BYTES, HosUtil.DIR_SEQID_QUALIFIER));
    }

    private String putDir(String bucket, String dir) throws Exception {
        if (dirExist(bucket, dir)) {
            return null;
        }
        // 从zk获取锁
        InterProcessMutex lock = null;
        try {
            String locKey = dir.replaceAll("/", "_");
            lock = new InterProcessMutex(this.zkClient, "/hos/" + bucket + "/" + locKey);
            lock.acquire();
            // 假设 dir=/aa/bb/cc/dd
            //     dir1=/aa/bb/cc
            //     name=cc
            //     parent=/aa/bb/
            // 创建目录
            String dir1 = dir.substring(0, dir.lastIndexOf("/"));
            String name = dir1.substring(dir1.lastIndexOf("/") + 1);
            if (name.length() > 0) {
                String parent = dir1.substring(0, dir1.lastIndexOf("/") + 1);
                if (!this.dirExist(bucket, parent)) {
                    this.putDir(bucket, parent);
                }
                // 在父目录的sub列簇内添加子项
                Put put = new Put(Bytes.toBytes(parent));
                put.addColumn(HosUtil.DIR_SUBDIR_CF_BYTES, Bytes.toBytes(name), Bytes.toBytes("1"));
                HBaseServiceImpl.putRow(connection, HosUtil.getDirTableName(bucket), put);
            }
            // 添加到目录表
            String seqId = this.getDirSeqId(bucket, dir);
            String hash = seqId == null ? makeDirSeqId(bucket) : seqId;
            Put dirPut = new Put(dir.getBytes());
            dirPut.addColumn(HosUtil.DIR_META_CF_BYTES, HosUtil.DIR_SEQID_QUALIFIER, Bytes.toBytes(hash));
            HBaseServiceImpl.putRow(connection, HosUtil.getDirTableName(bucket), dirPut);
            return hash;
        } finally {
            // 释放锁
            if (lock != null) {
                lock.release();
            }
        }


    }

    private String makeDirSeqId(String bucket) {
        long v = HBaseServiceImpl.incrementColumnValue(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, bucket, HosUtil.BUCKET_DIR_SEQ_CF_BYTES, HosUtil.BUCKET_DIR_SEQ_QUALIFIER, 1);
        return String.format("%da%d", v % 64, v);
    }

    private short getBucketReplication(String bucket) {
        return 1;
    }

    private void getDirAllFiles(String bucket, String dir, String seqId, List<HosObjectSummary> keys, String endKey) throws IOException {
        byte[] max = Bytes.createMaxByteArray(100);
        byte[] tail = Bytes.add(Bytes.toBytes(seqId), max);
        if (endKey.startsWith(dir)) {
            String endKeyLeft = endKey.replace(dir, "");
            String fileNameMax = endKeyLeft;
            if (endKeyLeft.indexOf("/") > 0) {
                fileNameMax = endKeyLeft.substring(0, endKeyLeft.indexOf("/"));
            }
            tail = Bytes.toBytes(seqId + "_" + fileNameMax);
        }
        Scan scan = new Scan(Bytes.toBytes(seqId), tail);
        scan.setFilter(HosUtil.OBJ_META_SCAN_FILTER);
        ResultScanner scanner = HBaseServiceImpl.getScanner(connection, HosUtil.getObjTableName(bucket), scan);
        Result result = null;
        while ((result = scanner.next()) != null) {
            HosObjectSummary summary = this.resultToObjectSummary(result, bucket, dir);
            keys.add(summary);
        }
        if (scanner != null) {
            scanner.close();
        }
    }

    private HosObjectSummary dirObjectToSummary(Result result, String bucket, String dir) {
        HosObjectSummary summary = new HosObjectSummary();
        summary.setId(Bytes.toString(result.getRow())); // rowKey
        summary.setAttrs(new HashMap<>(0));
        summary.setBucket(bucket);
        summary.setLastModifyTime(result.rawCells()[0].getTimestamp());
        summary.setLength(0);
        summary.setMediaType("");
        if (dir.length() > 1) {
            summary.setName(dir.substring(dir.lastIndexOf("/") + 1));
        } else {
            summary.setName("");
        }
        return summary;
    }

    private HosObjectSummary resultToObjectSummary(Result result, String bucket, String dir) throws IOException {
        HosObjectSummary summary = new HosObjectSummary();
        long timestamp = result.rawCells()[0].getTimestamp();
        summary.setLastModifyTime(timestamp);
        String id = new String(result.getRow());
        summary.setId(id);
        String name = id.split("_", 2)[1];
        String key = dir + name;
        summary.setKey(key); // 文件的全路径
        summary.setName(name);
        summary.setBucket(bucket);
        summary.setLength(Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER)));
        summary.setMediaType(Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_MEDIATYPE_QUALIFIER)));

        String s = Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_PROPS_QUALIFIER));
        if (s != null) {
            summary.setAttrs(JsonUtil.fromJson(Map.class, s));
        }
        return summary;
    }

    private boolean isDirEmpty(String bucket, String dir) throws IOException {
        return listDir(bucket, dir, null, 2).getObjectSummaryList().size() == 0;
    }
}
