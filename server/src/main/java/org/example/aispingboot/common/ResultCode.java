package org.example.aispingboot.common;

import lombok.Getter;

/**
 * 统一响应码。成功码固定 "200"，与接口文档保持一致；
 * 前端 request.js 会把 "200"/200/0 归一化为内部成功码。
 */
@Getter
public enum ResultCode {
    SUCCESS("200", "操作成功"),
    ERROR("-1", "操作失败"),

    // 参数相关
    PARAM_ERROR("400", "请求参数有误"),
    PARAM_MISSING("4001", "缺少必要参数"),
    PARAM_INVALID("4002", "参数格式不正确"),

    // 认证 / 授权
    UNAUTHORIZED("401", "暂未登录或登录已过期"),
    TOKEN_INVALID("401", "token无效"),
    TOKEN_EXPIRED("401", "token已过期"),
    TOKEN_BLOCKED("401", "token已加入黑名单"),
    FORBIDDEN("403", "没有权限执行此操作"),
    TOKEN_ACCESS_FORBIDDEN("403", "账号不可用"),
    ACCESS_UNAUTHORIZED("403", "访问未授权"),
    NOT_FOUND("404", "资源不存在"),

    // 系统
    SYSTEM_ERROR("500", "系统错误"),

    // 文件相关
    FILE_NOT_FOUND("5001", "文件不存在"),
    FILE_UPLOAD_FAILED("5002", "文件上传失败"),
    FILE_TYPE_NOT_SUPPORTED("5005", "不支持的文件类型"),
    FILE_SAVE_FAILED("5008", "文件保存失败"),

    // 业务相关
    BUSINESS_ERROR("6000", "业务处理失败"),
    ACCOUNT_SAME("6001", "用户名已存在"),
    USER_NOT_EXIST("6002", "用户不存在"),

    // 同意 / 隐私
    CONSENT_REQUIRED("6003", "未完成必要同意，无法使用该功能"),
    AGE_NOT_SUPPORTED("6004", "未确认已满 18 岁"),
    RESOURCE_FORBIDDEN("6005", "无权访问该资源"),
    DATA_DELETION_PENDING("6006", "账号删除申请处理中"),

    // AI 相关
    AI_ERROR("7000", "AI服务暂时不可用"),
    RISK_RESPONSE_BLOCKED("7001", "风险场景已阻断普通回复");

    private final String code;
    private final String msg;

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
