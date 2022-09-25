package cn.com.spotty.hos.rest;

import cn.com.spotty.hos.core.ErrorCodes;
import cn.com.spotty.hos.core.usermgr.model.UserInfo;
import cn.com.spotty.hos.security.ContextUtil;
import com.google.common.base.Strings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
public class LoginController extends BaseController {
    @RequestMapping("/")
    public String logIn(HttpServletRequest request) {
        return "index.html";
    }

    @RequestMapping("/loginPost")
    @ResponseBody
    public Object loginPost(String username, String password, HttpSession session)
            throws IOException {
        if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "username or password can not be null");
        }
        UserInfo userInfo = operationAccessControl.checkLogin(username, password);
        if (userInfo != null) {
            session.setAttribute(ContextUtil.SESSION_KEY, userInfo.getUserId());
            return getResult("success");
        } else {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "login error");
        }
    }

    @GetMapping("/logout")
    @ResponseBody
    public Object logout(HttpSession session) {
        session.removeAttribute(ContextUtil.SESSION_KEY);
        return getResult("success");
    }
}
