package cn.com.spotty.hos.core.test;

import cn.com.spotty.hos.core.authmgr.IAuthService;
import cn.com.spotty.hos.core.authmgr.model.TokenInfo;
import cn.com.spotty.hos.mybatis.test.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.junit.Test;

public class AuthServiceTest extends BaseTest {
    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;

    @Test
    public void addToken() {
        TokenInfo tokenInfo = new TokenInfo("tom");
        authService.addToken(tokenInfo);
    }
}
