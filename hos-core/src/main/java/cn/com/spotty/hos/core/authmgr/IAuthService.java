package cn.com.spotty.hos.core.authmgr;

import cn.com.spotty.hos.core.authmgr.model.ServiceAuth;
import cn.com.spotty.hos.core.authmgr.model.TokenInfo;

import java.util.List;

public interface IAuthService {
    // service auth start
    boolean addAuth(ServiceAuth serviceAuth);

    boolean deleteAuth(String bucketName, String token);

    boolean deleteAuthByBucket(String bucketName);

    boolean deleteAuthByToken(String token);

    ServiceAuth getServiceAuth(String bucketName, String token);
    // service auth end

    // token start
    boolean addToken(TokenInfo tokenInfo);

    boolean updateToken(String token, int expireTime, boolean isActive);

    boolean refreshToken(String token);

    boolean deleteToken(String token);

    boolean checkToken(String token);

    TokenInfo getTokenInfo(String token);

    List<TokenInfo> getTokenInfoList(String creator);
    // token end
}
