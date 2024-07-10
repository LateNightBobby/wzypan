package com.wzypan.utils;

import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SysSettingsDto;
import com.wzypan.entity.dto.UserSpaceDto;
import com.wzypan.exception.BusinessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedisUtils redisUtils;

    public String testRedis() {
        redisUtils.set("wzy", "very good");
        String test = (String) redisUtils.get("wzy");
        return test;
    }

    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
//        SysSettingsDto sysSettingsDto = new SysSettingsDto();
//        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        System.out.println("redis print sysSettingsDto");
        return sysSettingsDto;
    }

    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setEx(Constants.REDIS_KEY_USER_SPACE_USE+userId, userSpaceDto, Long.valueOf(Constants.REDIS_KEY_EXPIRES_DAY));

    }

    public UserSpaceDto getUserSpace(String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (userSpaceDto == null) {
            userSpaceDto = new UserSpaceDto().setUseSpace(0L).setTotalSpace(getSysSettingsDto().getUserInitUseSpace() * Constants.MB);
            saveUserSpaceUse(userId, userSpaceDto);
        }
        return userSpaceDto;
    }

}
