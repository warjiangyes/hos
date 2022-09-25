package cn.com.spotty.hos;

import cn.com.spotty.hos.common.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface HosClient {
    void createBucket(String bucketName) throws IOException;

    void deleteBucket(String bucketName) throws IOException;

    List<BucketModel> listBucket() throws IOException;

    void putObject(PutRequest putRequest) throws IOException;

    void putObject(String bucket, String key) throws IOException;

    void putObject(String bucket, String key, byte[] content, String mediaType) throws IOException;

    HosObjectSummary getObjectSummary(String bucket, String key) throws IOException;

    void deleteObject(String bucket, String key) throws IOException;


    void putObject(String bucket, String key, byte[] content, String mediaType, String contentEncoding) throws IOException;

    void putObject(String bucket, String key, File content, String mediaType) throws IOException;

    void putObject(String bucket, String key, File content, String mediaType, String contentEncoding) throws IOException;

    void putObject(String bucket, String key, File content) throws IOException;


    ObjectListResult listObject(String bucket, String startKey, String endKey) throws IOException;

    ObjectListResult listObject(ListObjectRequest request) throws IOException;

    ObjectListResult listObjectByPrefix(String bucket, String dir, String prefix, String startKey) throws IOException;

    ObjectListResult listObjectByDir(String bucket, String dir, String startKey) throws IOException;

    HosObject getObject(String bucket, String key) throws IOException;

    void createBucket(String bucketName, String detail) throws IOException;

    BucketModel getBucketInfo(String bucketName) throws IOException;
}
