package com.fengzhi.ai.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户更新请求
 *
 */
@Data
public class UserUpdateRequest implements Serializable {


    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}