package cn.com.spotty.hos.core.test;

import cn.com.spotty.hos.core.authmgr.IAuthService;
import cn.com.spotty.hos.core.authmgr.model.ServiceAuth;
import cn.com.spotty.hos.core.authmgr.model.TokenInfo;
import cn.com.spotty.hos.mybatis.test.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class AuthServiceTest extends BaseTest {
    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;

    @Test
    public void addToken() {
        TokenInfo tokenInfo = new TokenInfo("tom");
        authService.addToken(tokenInfo);
    }

    @Test
    public void refreshToken() {
        List<TokenInfo> tokenInfoList = authService.getTokenInfoList("tom");
        tokenInfoList.forEach(tokenInfo -> {
            authService.refreshToken(tokenInfo.getToken());
        });
    }

    @Test
    public void deleteToken() {
        List<TokenInfo> tokenInfoList = authService.getTokenInfoList("tom");
        if(tokenInfoList.size()> 0) {
            authService.deleteToken(tokenInfoList.get(0).getToken());
        }
    }

    @Test
    public void addAuth() {
        List<TokenInfo> tokenInfoList = authService.getTokenInfoList("tom");
        if(tokenInfoList.size() > 0) {
            ServiceAuth serviceAuth = new ServiceAuth();
            serviceAuth.setAuthTime(new Date());
            serviceAuth.setBucketName("testBucket");
            serviceAuth.setTargetToken(tokenInfoList.get(0).getToken());
            authService.addAuth(serviceAuth);
        }
    }

    @Test
    public void deleteAuth() {
        List<TokenInfo> tokenInfoList = authService.getTokenInfoList("tom");
        if(tokenInfoList.size() > 0) {
            authService.deleteAuth("testBucket",tokenInfoList.get(0).getToken());
        }
    }

}
