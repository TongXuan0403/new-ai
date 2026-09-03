package org.example.aispingboot.exception;

import lombok.Getter;
import org.example.aispingboot.common.ResultCode;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final Object data;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.data = null;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.data = null;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.data = null;
    }

    public BusinessException(String message, Object data) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.data = data;
    }
}
