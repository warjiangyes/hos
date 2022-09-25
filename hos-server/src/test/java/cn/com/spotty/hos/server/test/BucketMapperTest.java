package cn.com.spotty.hos.server.test;

import cn.com.spotty.hos.common.BucketModel;
import cn.com.spotty.hos.core.authmgr.IAuthService;
import cn.com.spotty.hos.core.authmgr.model.ServiceAuth;
import cn.com.spotty.hos.core.usermgr.IUserService;
import cn.com.spotty.hos.core.usermgr.model.SystemRole;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;
import cn.com.spotty.hos.mybatis.test.BaseTest;
import cn.com.spotty.hos.server.dao.BucketMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class BucketMapperTest extends BaseTest {
    @Autowired
    BucketMapper bucketMapper;
    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;
    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @Test
    public void addBucket() {
        UserInfo userInfo = new UserInfo("tom", "123456", SystemRole.ADMIN, "");
        userService.addUser(userInfo);


        BucketModel bucketModel1 = new BucketModel("test1", "tom", "this is a test bucket");
        bucketMapper.addBucket(bucketModel1);
        BucketModel bucketModel2 = new BucketModel("test2", "tom", "this is another bucket");
        bucketMapper.addBucket(bucketModel2);


        ServiceAuth serviceAuth = new ServiceAuth();
        serviceAuth.setTargetToken(userInfo.getUserId());
        serviceAuth.setBucketName(bucketModel1.getBucketName());
        authService.addAuth(serviceAuth);
    }

    @Test
    public void getBucket(){
        BucketModel bucketModel1 = bucketMapper.getBucketByName("test1");
        System.out.println(bucketModel1.getBucketId() + "|" + bucketModel1.getBucketName());
    }

    @Test
    public void getUserAuthorizedBuckets(){
        UserInfo userInfo = userService.getUserInfoByName("tom");
        List<BucketModel> bucketModels = bucketMapper.getUserAuthorizedBuckets(userInfo.getUserId());
        bucketModels.forEach(bucketModel -> {
            System.out.println(bucketModel.getBucketId() + "|" + bucketModel.getBucketName());
        });
    }

    @Test
    public void deleteBucket(){
        UserInfo userInfo = userService.getUserInfoByName("tom");
        List<BucketModel> bucketModels = bucketMapper.getUserAuthorizedBuckets(userInfo.getUserId());
        bucketModels.forEach(bucketModel -> {
            bucketMapper.deleteBucket(bucketModel.getBucketName());
        });
    }

}
