package cn.com.spotty.hos.core.usermgr;

import cn.com.spotty.hos.core.authmgr.dao.TokenInfoMapper;
import cn.com.spotty.hos.core.authmgr.model.TokenInfo;
import cn.com.spotty.hos.core.usermgr.dao.UserInfoMapper;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Transactional
@Service("userServiceImpl")
public class UserServiceImpl implements IUserService {
    private long LONG_REFRESH_TIME = 4670409600000L;
    private int LONG_EXPIRE_TIME = 36500;


    @Autowired
    UserInfoMapper userInfoMapper;
    @Autowired
    TokenInfoMapper tokenInfoMapper;

    public boolean addUser(UserInfo userInfo) {
        userInfoMapper.addUser(userInfo);
        Date date = new Date();
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setToken(userInfo.getUserId());
        tokenInfo.setActive(true);
        tokenInfo.setCreateTime(date);
        tokenInfo.setCreator(CoreUtil.SYSTEM_USER);
        tokenInfo.setExpireTime(LONG_EXPIRE_TIME);
        tokenInfo.setRefreshTime(date);
        tokenInfoMapper.addToken(tokenInfo);
        return true;
    }

    public boolean updateUserInfo(String userId, String password, String detail) {
        userInfoMapper.updateUserInfo(userId,
                Strings.isNullOrEmpty(password) ? null : CoreUtil.getMd5Password(password),
                Strings.emptyToNull(detail));
        return true;
    }

    public boolean deleteUser(String userId) {
        userInfoMapper.deleteUser(userId);
        tokenInfoMapper.deleteToken(userId);
        return false;
    }

    public UserInfo getUserInfo(String userId) {
        return userInfoMapper.getUserInfo(userId);
    }

    public UserInfo getUserInfoByName(String userName) {
        return userInfoMapper.getUserInfoByName(userName);
    }

    public UserInfo checkPassword(String userName, String password) {
        return userInfoMapper.checkPassword(userName, password);
    }
}
