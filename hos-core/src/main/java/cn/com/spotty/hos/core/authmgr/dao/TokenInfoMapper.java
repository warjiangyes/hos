package cn.com.spotty.hos.core.authmgr.dao;

import cn.com.spotty.hos.core.authmgr.model.TokenInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

import java.util.Date;
import java.util.List;

@Mapper
public interface TokenInfoMapper {
    void addToken(@Param("token") TokenInfo tokenInfo);

    void updateToken(@Param("token") String token, @Param("expireTime") int expireTime, @Param("isActive") int isActive);

    void refreshToken(@Param("token") String token, @Param("refreshTime") Date refreshTime);

    void deleteToken(@Param("token") String token);

    @ResultMap("TokenInfoResultMap")
    TokenInfo getTokenInfo(@Param("token") String token);

    @ResultMap("TokenInfoResultMap")
    List<TokenInfo> getTokenInfoList(@Param("creator") String creator);
}
