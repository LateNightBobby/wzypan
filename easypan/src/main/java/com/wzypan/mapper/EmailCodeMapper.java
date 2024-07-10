package com.wzypan.mapper;

import com.wzypan.entity.po.EmailCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 邮箱验证码 Mapper 接口
 * </p>
 *
 * @author wzy
 * @since 2024-05-31
 */
public interface EmailCodeMapper extends BaseMapper<EmailCode> {

    /**
     * 禁用指定邮箱的验证码记录
     * @param email
     */
    void disableEmailCode(@Param("email") String email);
}
