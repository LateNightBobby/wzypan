package com.wzypan.service;

import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.UserSpaceDto;
import com.wzypan.entity.po.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author wzy
 * @since 2024-05-30
 */
public interface UserInfoService extends IService<UserInfo> {
    void register(String email, String nickName, String password, String emailCode);

    SessionWebUserDto login(String email, String password);

    void resetPwd(String email, String password, String emailCode);

    UserSpaceDto getUseSpace(String userId);

    void getAvatar(HttpServletResponse response, String userId);

    void updateAvatar(HttpSession session, MultipartFile avatar);

    String qqlogin(HttpSession session, String callBackUrl) throws UnsupportedEncodingException;
}
