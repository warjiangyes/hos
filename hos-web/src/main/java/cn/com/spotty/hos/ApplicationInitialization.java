package cn.com.spotty.hos;

import cn.com.spotty.hos.core.usermgr.CoreUtil;
import cn.com.spotty.hos.core.usermgr.IUserService;
import cn.com.spotty.hos.core.usermgr.model.SystemRole;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;
import cn.com.spotty.hos.server.IHosStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInitialization implements ApplicationRunner {
    @Autowired
    @Qualifier("hosStoreService")
    IHosStore hosStoreService;

    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        UserInfo userInfo = userService.getUserInfoByName(CoreUtil.SYSTEM_USER);
        if (userInfo == null) {
            UserInfo userInfo1 = new UserInfo(CoreUtil.SYSTEM_USER, "superadmin", SystemRole.SUPER_ADMIN, "this is super admin");
            userService.addUser(userInfo1);
        }
        hosStoreService.createSeqTable();
    }
}
