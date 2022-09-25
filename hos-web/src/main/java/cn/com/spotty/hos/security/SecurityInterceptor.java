package cn.com.spotty.hos.security;

import cn.com.spotty.hos.core.authmgr.IAuthService;
import cn.com.spotty.hos.core.authmgr.model.TokenInfo;
import cn.com.spotty.hos.core.usermgr.IUserService;
import cn.com.spotty.hos.core.usermgr.model.SystemRole;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.hadoop.hbase.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Component
public class SecurityInterceptor implements HandlerInterceptor {
    @Autowired
    @Qualifier("authServiceImpl")
    private IAuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService userService;

    private Cache<String, UserInfo> userInfoCache = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 登录页面不做校验
        if (request.getRequestURI().equals("/loginPost")) {
            return true;
        }
        String token = "";
        HttpSession session = request.getSession();
        if (session.getAttribute(ContextUtil.SESSION_KEY) != null) {
            // 如果用户已经登录，优先从threadLocal中获取用户信息
            token = session.getAttribute(ContextUtil.SESSION_KEY).toString();
        } else {
            // 此时客户可能是游客
            token = request.getHeader("X-Auth-Token");
        }
        TokenInfo tokenInfo = authService.getTokenInfo(token);
        if (tokenInfo == null) {
            String url = "/loginPost";
            response.sendRedirect(url);
            return false;
        }
        UserInfo userInfo = userInfoCache.getIfPresent(tokenInfo.getToken());
        if (userInfo == null) {
            userInfo = userService.getUserInfo(token);
            if (userInfo == null) {
                userInfo = new UserInfo();
                userInfo.setUserId(token);
                userInfo.setUserName("visitor");
                userInfo.setDetail("a temporary visitor");
                userInfo.setSystemRole(SystemRole.VISITOR);
            }
            userInfoCache.put(tokenInfo.getToken(), userInfo);
        }
        ContextUtil.setCurrentUser(userInfo);
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) throws Exception {

    }
}
