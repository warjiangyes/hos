package cn.com.spotty.hos.core.usermgr;

import cn.com.spotty.hos.core.usermgr.model.UserInfo;

public interface IUserService {
    boolean addUser(UserInfo userInfo);

    boolean updateUserInfo(String userId, String password, String detail);

    boolean deleteUser(String userId);

    UserInfo getUserInfo(String userId);

    UserInfo getUserInfoByName(String userName);

    UserInfo checkPassword(String userName, String password);
}
