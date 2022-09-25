package cn.com.spotty.hos.core.test;

import cn.com.spotty.hos.core.usermgr.IUserService;
import cn.com.spotty.hos.core.usermgr.model.SystemRole;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;
import cn.com.spotty.hos.mybatis.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class UserServiceTest extends BaseTest {
    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;


    @Test
    public void addUser() {
        UserInfo userInfo = new UserInfo("tom", "123456", SystemRole.ADMIN, "no desc");
        userService.addUser(userInfo);
    }


    @Test
    public void getUser() {
        UserInfo userInfo = userService.getUserInfoByName("tom");
        System.out.println(
                userInfo.getUserId() + "|" + userInfo.getUserName() + "|" + userInfo.getPassword()
        );
    }
    @Test
    public void deleteUser() {
        UserInfo userInfo = userService.getUserInfoByName("tom");
        userService.deleteUser(userInfo.getUserId());
    }
}
