package cn.com.spotty.hos.core.usermgr.model;

import cn.com.spotty.hos.core.usermgr.CoreUtil;
import lombok.Data;

import java.util.Date;

@Data
public class UserInfo {
    private String userId;
    private String userName;
    private String password;
    private String detail;
    private SystemRole systemRole;
    private Date createTime;

    public UserInfo(String userName, String password, String detail, SystemRole systemRole) {
        this.userId = CoreUtil.getUUID();
        this.userName = userName;
        this.password = CoreUtil.getMd5Password(password);
        this.detail = detail;
        this.systemRole = systemRole;
        this.createTime = new Date();
    }
}
