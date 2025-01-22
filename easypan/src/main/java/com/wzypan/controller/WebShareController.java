package com.wzypan.controller;

import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.Result;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.FileShareDto;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.enums.FileDelFlagEnum;
import com.wzypan.entity.enums.ResponseCodeEnum;
import com.wzypan.entity.po.FileInfo;
import com.wzypan.entity.po.FileShare;
import com.wzypan.entity.po.UserInfo;
import com.wzypan.exception.BusinessException;
import com.wzypan.mapper.FileInfoMapper;
import com.wzypan.service.FileInfoService;
import com.wzypan.service.FileShareService;
import com.wzypan.service.UserInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController("WebShareController")
@RequestMapping("/showShare")
public class WebShareController {

    @Resource
    private FileShareService fileShareService;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        String curUserId = webUserDto.getUserId();
        FileShareDto fileShareDto = getShareFileDto(shareId);
        fileShareDto.setCurrentUser(curUserId.equals(fileShareDto.getUserId()));
        return Result.success(fileShareDto);
    }

    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public Result getShareInfo(HttpSession session, @VerifyParam(required = true) String shareId) {

        return Result.success(getShareFileDto(shareId));
    }

    private FileShareDto getShareFileDto(String shareId) {
        FileShare fileShare = fileShareService.getById(shareId);
        if (fileShare==null || fileShare.getExpireTime()!=null && new Date().after(fileShare.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        FileShareDto fileShareDto = new FileShareDto();
        BeanUtils.copyProperties(fileShare, fileShareDto);

        FileInfo fileInfo = fileInfoMapper.selectByUserIdAndFileId(fileShareDto.getUserId(), fileShareDto.getFileId());
        if (fileInfo==null || !FileDelFlagEnum.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        fileShareDto.setFileName(fileInfo.getFileName());

        UserInfo userInfo = userInfoService.getById(fileShare.getUserId());

        fileShareDto.setNickName(userInfo.getNickName()).setAvatar(userInfo.getQqAvatar()).setUserId(userInfo.getUserId());
        return fileShareDto;
    }

}
