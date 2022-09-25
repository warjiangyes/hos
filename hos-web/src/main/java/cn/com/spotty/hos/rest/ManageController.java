package cn.com.spotty.hos.rest;

import cn.com.spotty.hos.core.ErrorCodes;
import cn.com.spotty.hos.core.authmgr.IAuthService;
import cn.com.spotty.hos.core.authmgr.model.ServiceAuth;
import cn.com.spotty.hos.core.authmgr.model.TokenInfo;
import cn.com.spotty.hos.core.usermgr.IUserService;
import cn.com.spotty.hos.core.usermgr.model.SystemRole;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;
import cn.com.spotty.hos.security.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("hos/v1/sys/")
public class ManageController extends BaseController {
    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @RequestMapping(value = "user", method = RequestMethod.POST)
    public Object createUser(@RequestParam("userName") String userName,
                             @RequestParam("password") String password,
                             @RequestParam(name = "detail", required = false, defaultValue = "") String detail,
                             @RequestParam(name = "role", required = false, defaultValue = "USER") String role) {
        // 添加用户
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkSystemRole(currentUser.getSystemRole(), SystemRole.valueOf(role))) {
            UserInfo userInfo = new UserInfo(userName, password, SystemRole.valueOf(role), detail);
            userService.addUser(userInfo);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "NOT ADMIN");
    }

    @RequestMapping(value = "user", method = RequestMethod.DELETE)
    public Object deleteUser(@RequestParam("userId") String userId) {
        // 删除用户
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkSystemRole(currentUser.getSystemRole(), userId)) {
            userService.deleteUser(userId);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");
    }

    @RequestMapping(value = "user", method = RequestMethod.PUT)
    public Object updateUserInfo(
            @RequestParam(name = "password", required = false, defaultValue = "") String password,
            @RequestParam(name = "detail", required = false, defaultValue = "") String detail) {

        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (currentUser.getSystemRole().equals(SystemRole.VISITOR)) {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");
        }

        userService.updateUserInfo(currentUser.getUserId(), password, detail);
        return getResult("success");
    }

    @RequestMapping(value = "user", method = RequestMethod.GET)
    public Object getUserInfo() {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        return getResult(currentUser);
    }

    @RequestMapping(value = "token", method = RequestMethod.POST)
    public Object createToken(
            @RequestParam(name = "expireTime", required = false, defaultValue = "7") String expireTime,
            @RequestParam(name = "isActive", required = false, defaultValue = "true") String isActive) {
        // 添加token
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!currentUser.getSystemRole().equals(SystemRole.VISITOR)) {
            TokenInfo tokenInfo = new TokenInfo(currentUser.getUserName());
            tokenInfo.setExpireTime(Integer.parseInt(expireTime));
            tokenInfo.setActive(Boolean.parseBoolean(isActive));
            authService.addToken(tokenInfo);
            return getResult(tokenInfo);
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "NOT USER");
    }

    @RequestMapping(value = "token", method = RequestMethod.DELETE)
    public Object deleteToken(@RequestParam("token") String token) {
        // 删除token
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(currentUser.getUserName(), token)) {
            authService.deleteToken(token);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");
    }

    @RequestMapping(value = "token", method = RequestMethod.PUT)
    public Object updateTokenInfo(
            @RequestParam("token") String token,
            @RequestParam(name = "expireTime", required = false, defaultValue = "7") String expireTime,
            @RequestParam(name = "isActive", required = false, defaultValue = "true") String isActive) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(currentUser.getUserName(), token)) {
            authService.updateToken(token, Integer.parseInt(expireTime), Boolean.parseBoolean(isActive));
            return getResult("success");
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");
    }

    @RequestMapping(value = "token", method = RequestMethod.GET)
    public Object getTokenInfo(@RequestParam("token") String token) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(currentUser.getUserName(), token)) {
            TokenInfo tokenInfo = authService.getTokenInfo(token);
            return getResult(tokenInfo);
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");

    }

    @RequestMapping(value = "token/list", method = RequestMethod.GET)
    public Object getTokenInfoList() {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!currentUser.getSystemRole().equals(SystemRole.VISITOR)) {
            List<TokenInfo> tokenInfos = authService.getTokenInfoList(currentUser.getUserName());
            return getResult(tokenInfos);
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");

    }

    @RequestMapping(value = "token/refresh", method = RequestMethod.POST)
    public Object refreshToken(@RequestParam("token") String token) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(currentUser.getUserName(), token)) {
            authService.refreshToken(token);
            return getResult("success");
        }

        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");
    }

    @RequestMapping(value = "auth", method = RequestMethod.POST)
    public Object createAuth(@RequestBody ServiceAuth serviceAuth) {
        // 授权
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkBucketOwner(currentUser.getUserName(), serviceAuth.getBucketName())
                && operationAccessControl.checkTokenOwner(currentUser.getUserName(), serviceAuth.getTargetToken())) {
            authService.addAuth(serviceAuth);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");
    }

    @RequestMapping(value = "auth", method = RequestMethod.DELETE)
    public Object deleteAuth(@RequestParam("bucket") String bucket,
                             @RequestParam("token") String token) {
        // 取消授权
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkBucketOwner(currentUser.getUserName(), bucket)
                && operationAccessControl.checkTokenOwner(currentUser.getUserName(), token)) {
            authService.deleteAuth(bucket, token);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "PERMISSION DENIED");
    }
}
