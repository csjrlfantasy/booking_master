package com.ticket.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码(加密)
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别: 0-未知, 1-男, 2-女
     */
    private Integer gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 用户类型: 1-普通用户, 2-VIP用户, 3-企业用户
     */
    private Integer userType;

    /**
     * 用户等级
     */
    private Integer userLevel;

    /**
     * 状态: 0-禁用, 1-正常, 2-待激活
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 连续登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 账户锁定截止时间
     */
    private LocalDateTime lockUntil;

    /**
     * 用户来源: app, h5, miniapp
     */
    private String source;

    /**
     * 邀请人ID
     */
    private Long inviterId;

    /**
     * 累计消费金额
     */
    private java.math.BigDecimal totalOrderAmount;

    private BigDecimal wechatBalance;

    private BigDecimal alipayBalance;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 用户状态常量
     */
    public static final Integer STATUS_DISABLED = 0;
    public static final Integer STATUS_NORMAL = 1;
    public static final Integer STATUS_PENDING = 2;

    /**
     * 用户类型常量
     */
    public static final Integer USER_TYPE_NORMAL = 1;
    public static final Integer USER_TYPE_VIP = 2;
    public static final Integer USER_TYPE_ENTERPRISE = 3;

    /**
     * 性别常量
     */
    public static final Integer GENDER_UNKNOWN = 0;
    public static final Integer GENDER_MALE = 1;
    public static final Integer GENDER_FEMALE = 2;
}
