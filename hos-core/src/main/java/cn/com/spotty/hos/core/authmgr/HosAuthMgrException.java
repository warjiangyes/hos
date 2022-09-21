package cn.com.spotty.hos.core.authmgr;

import cn.com.spotty.hos.core.HosException;
import lombok.Getter;

public class HosAuthMgrException extends HosException {
    @Getter
    private int code;
    @Getter
    private String message;

    public HosAuthMgrException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public HosAuthMgrException(int code, String message) {
        super(message, null);
        this.code = code;
        this.message = message;
    }

    @Override
    public int errorCode() {
        return this.code;
    }
}
