package com.wzypan.controller.login;


import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.Result;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.CreateImageCode;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.UserSpaceDto;
import com.wzypan.entity.enums.VerifyRegexEnum;
import com.wzypan.entity.po.UserInfo;
import com.wzypan.exception.BusinessException;
import com.wzypan.service.EmailCodeService;
import com.wzypan.service.UserInfoService;
import com.wzypan.utils.RedisComponent;
import com.wzypan.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;


/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author wzy
 * @since 2024-05-30
 */
@RestController
//@RequestMapping("/userInfo")
public class AccountController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        }
        else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @GlobalInterceptor(checkParams = true)
    @PostMapping("/sendEmailCode")
    public Result sendEmailCode(HttpSession session,
                                @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                @VerifyParam(required = true) String checkCode,
                                @VerifyParam(required = true) Integer type) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("wrong verify code");
            }
            emailCodeService.sendEmailCode(email, type);
            return Result.success();
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    @GlobalInterceptor(checkParams = true)
    @PostMapping("/register")
    public Result register(HttpSession session,
                                @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                @VerifyParam(required = true) String nickName,
                                @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, max = 18, min = 8) String password,
                                @VerifyParam(required = true) String checkCode,
                                @VerifyParam(required = true) String emailCode)
    {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("wrong verify code");
            }
            userInfoService.register(email, nickName, password, emailCode);
            return Result.success();
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @GlobalInterceptor(checkParams = true)
    @PostMapping("/login")
    public Result login(HttpSession session,
                        @VerifyParam(required = true) String email,
                        @VerifyParam(required = true) String password,
                        @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("wrong verify code");
            }
            SessionWebUserDto userDto = userInfoService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY, userDto);
            return Result.success(userDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @GlobalInterceptor(checkParams = true)
    @PostMapping("/resetPwd")
    public Result resetPwd(HttpSession session,
                        @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                        @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, max = 18, min = 8) String password,
                        @VerifyParam(required = true) String checkCode,
                        @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("wrong verify code");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return Result.success();
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true)
    public Result getAvatar(HttpServletResponse response, @VerifyParam(required = true) @PathVariable("userId") String userId) {
        userInfoService.getAvatar(response, userId);
        response.setContentType("image/jpg");
        return Result.success();
    }

    @PostMapping("/updateUserAvatar")
    @GlobalInterceptor(checkParams = true)
    public Result updateUserAvatar(HttpSession session, @VerifyParam(required = true) MultipartFile avatar) {
        userInfoService.updateAvatar(session, avatar);
        return Result.success();
    }

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkParams = true)
    public Result getUserInfo(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return Result.success(sessionWebUserDto);
    }

    @RequestMapping("/logout")
    @GlobalInterceptor(checkParams = true)
    public Result logout(HttpSession session) {
        session.invalidate();
        return Result.success();
    }

    @RequestMapping("/getUseSpace")
    @GlobalInterceptor(checkParams = true)
    public Result getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserSpaceDto userSpaceDto = userInfoService.getUseSpace(sessionWebUserDto.getUserId());
        return Result.success(userSpaceDto);
    }

    @PostMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public Result updatePassword(HttpSession session,
            @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        UserInfo userInfo = userInfoService.getById(webUserDto.getUserId());
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfoService.updateById(userInfo);
        return Result.success();
    }

    private SessionWebUserDto getUserInfoFromSession(HttpSession session) {
        return (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
    }
}

