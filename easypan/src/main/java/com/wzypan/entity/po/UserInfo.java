package com.wzypan.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 * @author wzy
 * @since 2024-05-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @TableId(value = "user_id", type = IdType.INPUT)
    private String userId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * QQ open id
     */
    private String qqOpenId;

    /**
     * QQ头像路径
     */
    private String qqAvatar;

    /**
     * 密码
     */
    private String password;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 上次登录时间
     */
    private Date lastLoginTime;

    /**
     * 账户状态0禁用 1启用
     */
    private Integer status;

    /**
     * 使用空间大小 byte
     */
    private Long useSpace;

    /**
     * 总空间 byte
     */
    private Long totalSpace;


}
