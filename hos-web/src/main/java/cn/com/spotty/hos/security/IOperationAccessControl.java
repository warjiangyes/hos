package cn.com.spotty.hos.security;

import cn.com.spotty.hos.core.usermgr.model.SystemRole;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;

public interface IOperationAccessControl {
    // 校验用户名、密码是否正确
    UserInfo checkLogin(String userName, String password);

    // 判断systemRole1是否具备systemRole2的权限
    boolean checkSystemRole(SystemRole systemRole1, SystemRole systemRole2);

    boolean checkSystemRole(SystemRole systemRole1, String userId);

    boolean checkTokenOwner(String userName, String token);

    boolean checkBucketOwner(String userName, String bucketName);

    boolean checkPermission(String token, String bucket);


}
