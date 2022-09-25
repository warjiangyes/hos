package cn.com.spotty.hos.common;

import cn.com.spotty.hos.core.usermgr.CoreUtil;
import lombok.Data;

import java.util.Date;

@Data
public class BucketModel {
    private String bucketId;
    private String bucketName;
    private String creator;
    private String detail;
    private Date createTime;

    public BucketModel(String bucketName, String creator, String detail) {
        this.bucketId = CoreUtil.getUUID();
        this.bucketName = bucketName;
        this.createTime = new Date();
        this.creator = creator;
        this.detail = detail;
    }

    public BucketModel() {

    }
}
