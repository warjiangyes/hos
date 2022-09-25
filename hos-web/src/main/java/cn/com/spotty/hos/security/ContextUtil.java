package cn.com.spotty.hos.security;

import cn.com.spotty.hos.core.usermgr.model.UserInfo;

public class ContextUtil {
    public final static String SESSION_KEY = "USER_TOKEN";

    private static ThreadLocal<UserInfo> userInfoThreadLocal = new ThreadLocal<>();

    public static UserInfo getCurrentUser() {
        return userInfoThreadLocal.get();
    }

    static void setCurrentUser(UserInfo userInfo) {
        userInfoThreadLocal.set(userInfo);
    }

    static void clear() {
        userInfoThreadLocal.remove();
    }

}
