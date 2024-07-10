package com.wzypan.controller;

import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SysSettingsDto;
import com.wzypan.utils.RedisUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class TestController {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedisUtils redisUtils;

    @RequestMapping("/test")
    public String test() {
        return "wzy pan test";
    }

    @RequestMapping("/getSysSettings")
    public SysSettingsDto getSysSetting() {
        return (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
    }
}
