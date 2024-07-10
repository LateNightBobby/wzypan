package com.wzypan.service;

import com.wzypan.entity.po.EmailCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 邮箱验证码 服务类
 * </p>
 *
 * @author wzy
 * @since 2024-05-31
 */
public interface EmailCodeService extends IService<EmailCode> {

    void sendEmailCode(String email, Integer type);

    void verifyEmailCode(String email, String emailCode);
}
