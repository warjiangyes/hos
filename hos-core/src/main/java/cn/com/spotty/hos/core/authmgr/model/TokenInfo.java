package cn.com.spotty.hos.core.authmgr.model;

import cn.com.spotty.hos.core.usermgr.CoreUtil;
import lombok.Data;

import java.util.Date;

@Data
public class TokenInfo {
    private String token;
    private int expireTime;
    private Date refreshTime;
    private Date createTime;
    private boolean active;
    private String creator;

    public TokenInfo() {
    }

    public TokenInfo(String creator) {
        this.token = CoreUtil.getUUID();
        this.expireTime = 7;
        Date date = new Date();
        this.refreshTime = date;
        this.createTime = date;
        this.active = true;
        this.creator = creator;
    }
}
