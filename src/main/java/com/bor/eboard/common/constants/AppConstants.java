package com.bor.eboard.common.constants;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String API_BASE = "/api/v1";

    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String USER_STATUS_INACTIVE = "INACTIVE";
    public static final String USER_STATUS_SUSPENDED = "SUSPENDED";

    public static final String MODULE_IDENTITY = "IDENTITY";
    public static final String MODULE_ORGANIZATION = "ORGANIZATION";
    public static final String MODULE_AUTH = "AUTH";

    public static final String AUDIT_ACTION_LOGIN = "LOGIN";
    public static final String AUDIT_ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String AUDIT_ACTION_LOGOUT = "LOGOUT";
    public static final String AUDIT_ACTION_TOKEN_REFRESH = "TOKEN_REFRESH";
    public static final String AUDIT_ACTION_CREATE = "CREATE";
    public static final String AUDIT_ACTION_UPDATE = "UPDATE";
    public static final String AUDIT_ACTION_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String AUDIT_ACTION_ROLE_CHANGE = "ROLE_CHANGE";
    public static final String AUDIT_ACTION_PERMISSION_CHANGE = "PERMISSION_CHANGE";
    public static final String AUDIT_ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String AUDIT_ACTION_CAPTCHA_VALIDATED = "CAPTCHA_VALIDATED";
    public static final String AUDIT_ACTION_CAPTCHA_FAILED = "CAPTCHA_FAILED";
    public static final String AUDIT_ACTION_OTP_GENERATED = "OTP_GENERATED";
    public static final String AUDIT_ACTION_OTP_VERIFIED = "OTP_VERIFIED";
    public static final String AUDIT_ACTION_OTP_FAILED = "OTP_FAILED";
    public static final String AUDIT_ACTION_OTP_EXPIRED = "OTP_EXPIRED";
    public static final String AUDIT_ACTION_OTP_RESENT = "OTP_RESENT";
    public static final String AUDIT_ACTION_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String AUDIT_ACTION_PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String AUDIT_ACTION_PASSWORD_RESET_FAILED = "PASSWORD_RESET_FAILED";
}
