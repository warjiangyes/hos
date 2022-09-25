package cn.com.spotty.hos.server;

import cn.com.spotty.hos.common.BucketModel;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;

import java.util.List;

public interface IBucketService {
    boolean addBucket(UserInfo userInfo, String bucketName, String detail);

    boolean deleteBucket(String bucketName);

    boolean updateBucket(String bucketName, String detail);

    BucketModel getBucketById(String bucketId);

    BucketModel getBucketByName(String bucketName);

    List<BucketModel> getUserBuckets(String token);
}
