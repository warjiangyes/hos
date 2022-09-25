package cn.com.spotty.hos.server;

import cn.com.spotty.hos.core.HosException;

public class HosServerException extends HosException {
    private int code;
    private String message;

    public HosServerException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public HosServerException(int code, String message) {
        super(message, null);
        this.code = code;
        this.message = message;
    }

    @Override
    public int errorCode() {
        return this.code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
