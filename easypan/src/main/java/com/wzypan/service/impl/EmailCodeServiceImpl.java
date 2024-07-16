package com.wzypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzypan.entity.config.AppConfig;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SysSettingsDto;
import com.wzypan.entity.enums.ResponseCodeEnum;
import com.wzypan.entity.po.EmailCode;
import com.wzypan.entity.po.UserInfo;
import com.wzypan.exception.BusinessException;
import com.wzypan.mapper.EmailCodeMapper;
import com.wzypan.mapper.UserInfoMapper;
import com.wzypan.service.EmailCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzypan.utils.RedisComponent;
import com.wzypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * 邮箱验证码 服务实现类
 * </p>
 *
 * @author wzy
 * @since 2024-05-31
 */
@Slf4j
@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode> implements EmailCodeService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private EmailCodeMapper emailCodeMapper;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    //发生异常则回滚
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        //注册
        if (type.equals(Constants.STATUS_UNUSED)) {
            LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserInfo::getEmail, email);
            UserInfo userInfo = userInfoMapper.selectOne(wrapper);
            if (userInfo != null) {
                throw new BusinessException(ResponseCodeEnum.CODE_601.getCode(), "existed email");
            }
        }
        String code = StringTools.getRandomNumber(Constants.LENGTH_5);
        // TODO 发送验证码
        sendEmailWithCode(email, code);

        //禁用上一条验证码
        emailCodeMapper.disableEmailCode(email);
        //插入新的验证码
//        log.info(email);
        EmailCode emailCode = new EmailCode()
                .setCode(code).setEmail(email)
                .setStatus(Constants.STATUS_USED)
                .setCreateTime(new Date());
//        log.info(emailCode.toString());
        emailCodeMapper.insert(emailCode);
    }

    public void sendEmailWithCode(String toEmail, String code){
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);

//            redisComponent.testRedis();
            SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
            messageHelper.setFrom(appConfig.getUsername());
            messageHelper.setTo(toEmail);
            messageHelper.setSubject(sysSettingsDto.getRegisterMailTitle());
            String text = String.format(sysSettingsDto.getRegisterMailContent(), code);
            messageHelper.setText(text);
//            messageHelper.setText(String.format(sysSettingsDto.getRegisterMailContent()), code);
            messageHelper.setSentDate(new Date());
            javaMailSender.send(message);

        } catch (Exception e) {
            log.error("fail to send message");
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "fail to send message");
        }
    }

    @Override
    public void verifyEmailCode(String email, String code) {
        LambdaQueryWrapper<EmailCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailCode::getEmail, email).eq(EmailCode::getStatus, Constants.STATUS_USED);
        EmailCode emailCode = emailCodeMapper.selectOne(wrapper);
        if (emailCode == null || !Objects.equals(emailCode.getCode(), code)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "wrong email code");
        }
        if (System.currentTimeMillis() - emailCode.getCreateTime().getTime() > Constants.EMAIL_CODE_VALID_PERIOD_MIN * 60 * 1000) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "email code expired");
        }
        emailCodeMapper.disableEmailCode(email);
    }
}
