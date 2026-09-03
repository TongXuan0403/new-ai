package org.example.aispingboot.exception;

import org.example.aispingboot.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * @author system
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final String message;
    private final Object data;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.message = message;
        this.data = null;
    }

}
