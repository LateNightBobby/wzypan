package com.wzypan.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzypan.entity.config.AppConfig;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.SysSettingsDto;
import com.wzypan.entity.dto.UserSpaceDto;
import com.wzypan.entity.enums.ResponseCodeEnum;
import com.wzypan.entity.enums.UserStatusEnum;
import com.wzypan.entity.po.UserInfo;
import com.wzypan.exception.BusinessException;
import com.wzypan.mapper.UserInfoMapper;
import com.wzypan.service.EmailCodeService;
import com.wzypan.service.UserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzypan.utils.FileUtils;
import com.wzypan.utils.OKHttpUtils;
import com.wzypan.utils.RedisComponent;
import com.wzypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author wzy
 * @since 2024-05-30
 */
@Slf4j
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password, String emailCode) {
        //检测邮箱是否已注册
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if (userInfo != null) {
            throw new BusinessException(ResponseCodeEnum.CODE_601.getCode(), "already exist this email");
        }
        //昵称是否已使用
        Integer nickNameCount = userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getNickName, nickName));
        if (nickNameCount != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_601.getCode(), "existed nick name");
        }
        //校验邮箱验证码
        emailCodeService.verifyEmailCode(email, emailCode);
        //用户注册
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
        UserInfo registerUser = new UserInfo().setUserId(StringTools.getRandomNumber(Constants.USER_ID_LENGTH))
                .setEmail(email).setPassword(StringTools.encodeByMd5(password)).setNickName(nickName)
                .setStatus(UserStatusEnum.ENABLE.getStatus()).setUseSpace(0L)
                .setJoinTime(new Date()).setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
        userInfoMapper.insert(registerUser);

    }

    @Override
    public SessionWebUserDto login(String email, String password) {

        //登录账号密码校验
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if (userInfo==null || !userInfo.getPassword().equals(password)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "wrong email or password");
        }

        if (userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "disabled account");
        }
        //更新登录时间
        userInfo.setLastLoginTime(new Date());
        userInfoMapper.updateById(userInfo);

        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setUserId(userInfo.getUserId()).setNickName(userInfo.getNickName());
        //判断是否为管理员
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)) {
            sessionWebUserDto.setAdmin(true);
        }
        else {
            sessionWebUserDto.setAdmin(false);
        }
        //
        UserSpaceDto userSpaceDto = new UserSpaceDto()
                .setTotalSpace(userInfo.getTotalSpace())
                .setUseSpace(userInfo.getUseSpace());
        redisComponent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);
        return sessionWebUserDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(String email, String password, String emailCode) {
        //检测邮箱是否已注册
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if (userInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "not found this email");
        }
        //邮箱验证码是否正确
        emailCodeService.verifyEmailCode(email, emailCode);
        //修改密码
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfoMapper.updateById(userInfo);
    }

    @Override
    public UserSpaceDto getUseSpace(String userId) {
        return redisComponent.getUserSpace(userId);
    }

    @Override
    public void getAvatar(HttpServletResponse response, String userId) {
        String avatarFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
//        log.info(avatarFolderName);
        File folder = new File(avatarFolderName);
        log.info(folder.getAbsolutePath());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String avatarPath = avatarFolderName + "/" + userId + Constants.AVATAR_SUFFIX;
//        log.info(avatarPath);
        File file = new File(avatarPath);
        if (!file.exists()) {
            File default_avatar = new File(avatarFolderName+Constants.AVATAR_DEFAULT);
            if (!default_avatar.exists()) {
                printNoDefaultImage(response);
            }
            avatarPath = default_avatar.getPath();
        }
        FileUtils.readFile(response, avatarPath);
    }

    @Override
    public void updateAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File targetFolder = new File(baseFolder);
        File targetFile = new File(targetFolder.getPath() + "/" + sessionWebUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            log.error("avatar update failed");
        }
        //更新信息
        UserInfo userInfo = userInfoMapper.selectById(sessionWebUserDto.getUserId());
        userInfo.setQqAvatar("");
        userInfoMapper.updateById(userInfo);
        sessionWebUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
    }

    @Override
    public String qqlogin(HttpSession session, String callBackUrl) {
        //生成随机数作为key 用于session跳回
        String state = StringTools.getRandomNumber(30);
        if (!StringTools.isEmpty(state)) {
            session.setAttribute(state, callBackUrl);
        }
        String url = String.format(appConfig.getQqUrlAuthorization(), appConfig.getQqAppId(),
                URLEncoder.encode(appConfig.getQqUrlRedirect(), StandardCharsets.UTF_8), state);
        return url;
    }

    @Override
    public SessionWebUserDto qqLoginCallback(String code) {
        //回调code获取accessToken
        String accessToken = getQQAccessToken(code);
        //获取qq token id
        String qqOpenId = getQQOpenId(accessToken);
        return null;
    }

    private String getQQAccessToken(String code) {
        String accessToken = null;
        String url = null;
        url = String.format(appConfig.getQqUrlAccessToken(), appConfig.getQqAppId(),
                appConfig.getQqAppKey(), code, URLEncoder.encode(appConfig.getQqUrlRedirect(), StandardCharsets.UTF_8));
        String tokenResult = OKHttpUtils.getRequest(url);
        if (tokenResult == null || tokenResult.contains(Constants.VIEW_OBJ_RESULT_KEY)) {
            log.error("获取qqToken失败{}", tokenResult);
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "获取qqToken失败");
        }
        String[] params = tokenResult.split("&");
        if (params != null && params.length > 0) {
            for (String p : params) {
                if (p.contains("access_toke")) {
                    accessToken = p.split("=")[1];
                    break;
                }
            }
        }
        return accessToken;
    }

    private String getQQOpenId(String accessToken) throws BusinessException {
        String url = String.format(appConfig.getQqUrlOpenid(), accessToken);
        String openIdResult = OKHttpUtils.getRequest(url);
        String tmpJson = this.getQQResp(openIdResult);
        if (tmpJson == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "调用qq接口获取openId失败");
        }

        Map jsonData = JSONObject.parseObject(tmpJson, Map.class);
        if (jsonData == null || jsonData.containsKey(Constants.VIEW_OBJ_RESULT_KEY)) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "调用qq接口获取openId失败");
        }
        return String.valueOf(jsonData.get("openid"));
    }

    private String getQQResp(String result) {
        if (StringUtils.isNotBlank(result)) {
            int pos = result.indexOf("callback");
            if (pos != -1) {
                int start = result.indexOf("(");
                int end = result.lastIndexOf(")");
                String jsonStr = result.substring(start + 1, end - 1);
                return jsonStr;
            }
        }
        return null;
    }

    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println("请在头像图像路径下放置默认头像.jpg");
            writer.close();
        } catch (Exception e) {
            log.error("no default image");
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }


}
