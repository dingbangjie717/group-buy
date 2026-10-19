package com.groupbuy.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.groupbuy.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表（一用户一角色：USER / MERCHANT / ADMIN）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String avatar;
    /** 角色编码 */
    private String roleCode;
    /** 所属店铺（商家角色） */
    private Long shopId;
    /** 1 正常 0 禁用 */
    private Integer status;
}
