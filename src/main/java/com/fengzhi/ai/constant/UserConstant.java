package com.fengzhi.ai.constant;

/**
 * 用户常量
 *
 */
public interface UserConstant {
    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String USER_KEY = "fengzhi_user:";

    /**
     * 密码前缀
     */
    String USER_PASSWORD = "fengzhi";

    /**
     * 过期时间
     */
    int EXPIRE_TIME = 30;


    // endregion
}
