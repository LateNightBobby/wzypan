package com.wzypan.mapper;

import com.wzypan.entity.po.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 用户信息表 Mapper 接口
 * </p>
 *
 * @author wzy
 * @since 2024-05-30
 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {
    //根据邮箱选择用户
    UserInfo selectByEmail(String email);
}
