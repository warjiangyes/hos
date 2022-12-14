package cn.com.spotty.hos.server;

import cn.com.spotty.hos.common.HosObject;
import cn.com.spotty.hos.common.HosObjectSummary;
import cn.com.spotty.hos.common.ObjectListResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface IHosStore {
    void createBucketStore(String bucket) throws IOException;

    void deleteBucketStore(String bucket) throws IOException;

    void createSeqTable() throws IOException;

    void put(String bucket, String key, ByteBuffer content, long length, String mediaType, Map<String, String> properties) throws Exception;

    HosObjectSummary getSummary(String bucket, String key) throws IOException;

    List<HosObjectSummary> list(String bucket, String startKey, String endKey) throws IOException;

    ObjectListResult listDir(String bucket, String dir, String start, int maxCount) throws IOException;

    ObjectListResult listByPrefix(String bucket, String dir, String keyPrefix, String start, int maxCount) throws IOException;

    HosObject getObject(String bucket, String key) throws IOException;

    void deleteObject(String bucket, String key) throws Exception;


}
