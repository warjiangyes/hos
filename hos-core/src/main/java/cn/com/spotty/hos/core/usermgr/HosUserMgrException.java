package cn.com.spotty.hos.core.usermgr;

import cn.com.spotty.hos.core.HosException;

import lombok.Getter;

public class HosUserMgrException extends HosException {
    @Getter
    private int code;
    @Getter
    private String message;

    public HosUserMgrException(String errorMessage, Throwable cause, int code, String message) {
        super(errorMessage, cause);
        this.code = code;
        this.message = message;
    }

    public HosUserMgrException(int code, String message) {
        super(message, null);
        this.code = code;
        this.message = message;
    }

    @Override
    public int errorCode() {
        return this.code;
    }

}
