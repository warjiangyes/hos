package cn.com.spotty.hos.server;

import cn.com.spotty.hos.core.ErrorCodes;
import cn.com.spotty.hos.core.HosConfiguration;
import org.apache.commons.io.FileExistsException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.log4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class HdfsServiceImpl implements IHdfsService {
    private static Logger logger = Logger.getLogger(HdfsServiceImpl.class);
    private FileSystem fileSystem;
    private long defaultBlockSize = 128 * 1024 * 1024;
    private long initBlockSize = defaultBlockSize / 2;

    public HdfsServiceImpl() throws Exception {
        try{
            // 1. 读取hdfs相关配置信息
            HosConfiguration hosConfiguration = HosConfiguration.getConfiguration();
            String confDir = hosConfiguration.getString("hadoop.conf.dir");
            String hdfsUri = hosConfiguration.getString("hadoop.uri");
            // 2. 通过配置获取一个fileSystem实例对象
            DefaultResourceLoader loader = new DefaultResourceLoader();
            Configuration conf = new Configuration();
            conf.addResource(new Path(confDir + "/core-site.xml"));
            conf.addResource(new Path(confDir + "/hdfs-site.xml"));
            fileSystem = FileSystem.get(new URI(hdfsUri), conf);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveFile(String dir, String name, InputStream inputStream, long length, short replication) throws IOException {
        // 1. 判断dir是否存在，不存在则创建
        Path dirPath = new Path(dir);
        try {
            if (!fileSystem.exists(dirPath)) {
                boolean success = fileSystem.mkdirs(dirPath, FsPermission.getDirDefault());
                logger.info("create dir " + dirPath + " success" + success);
                if (!success) {
                    throw new HosServerException(ErrorCodes.ERROR_HDFS, "create dir " + dir + " error");
                }
            }
        } catch (FileExistsException ex) {
            ex.printStackTrace();
        }
        // 2. 保存文件
        Path path = new Path(dir + "/" + name);
        long blockSize = length <= initBlockSize ? initBlockSize : defaultBlockSize;
        FSDataOutputStream outputStream = fileSystem.create(path, true, 512 * 1024, replication, blockSize);
        try {
            fileSystem.setPermission(path, FsPermission.getFileDefault());
            byte[] buffer = new byte[512 * 1024];
            int len = -1;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    @Override
    public void deleteFile(String dir, String name) throws IOException {
        fileSystem.delete(new Path(dir + "/" + name), false);
    }

    @Override
    public InputStream openFile(String dir, String name) throws IOException {
        return fileSystem.open(new Path(dir + "/" + name));
    }

    @Override
    public void mkDir(String dir) throws IOException {
        fileSystem.mkdirs(new Path(dir));
    }

    @Override
    public void deleteDir(String dir) throws IOException {
        fileSystem.delete(new Path(dir), true);
    }
}
