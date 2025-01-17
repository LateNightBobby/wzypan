package com.wzypan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.Result;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.SysSettingsDto;
import com.wzypan.entity.dto.UserDto;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.wzypan.entity.po.UserInfo;
import com.wzypan.mapper.UserInfoMapper;
import com.wzypan.service.FileInfoService;
import com.wzypan.service.UserInfoService;
import com.wzypan.utils.RedisComponent;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController("adminController")
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/getSysSettings")
    @GlobalInterceptor(checkLogin = true, checkAdmin = true)
    public Result getSysSettings() {
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
        return Result.success(sysSettingsDto);
    }

    @RequestMapping("/saveSysSettings")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result saveSysSettings(
            @VerifyParam(required = true) String registerEmailTitle,
            @VerifyParam(required = true) String registerEmailContent,
            @VerifyParam(required = true) Integer userInitUseSpace) {
        SysSettingsDto sysSettingsDto = new SysSettingsDto();
        sysSettingsDto.setRegisterMailTitle(registerEmailTitle);
        sysSettingsDto.setRegisterMailContent(registerEmailContent);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        redisComponent.saveSysSettingsDto(sysSettingsDto);
        return Result.success();
    }

    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result loadUserList(PageQuery pageQuery, String nickNameFuzzy, Integer status) {
        Page<UserInfo> page = new Page<>(pageQuery.getPageNo()==null? 1: pageQuery.getPageNo(), pageQuery.getPageSize()==null? 15: pageQuery.getPageSize());
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(nickNameFuzzy!=null && !nickNameFuzzy.isEmpty()&&!nickNameFuzzy.isBlank(), UserInfo::getNickName, nickNameFuzzy)
                .eq(status!=null, UserInfo::getStatus, status);
        IPage filePage = userInfoMapper.selectPage(page, wrapper);
        List<UserInfo> userInfoList = filePage.getRecords();
        List<UserDto> userDtos = userInfoList.stream()
                .map(userInfo -> {
                    UserDto dto = new UserDto();
                    BeanUtils.copyProperties(userInfo, dto);
                    return dto;
                }).collect(Collectors.toList());
        return Result.success(PageBean.convertFromPage(filePage).setList(userDtos));
    }

    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkAdmin = true, checkParams = true)
    public Result updateUserStatus(@VerifyParam(required = true) String userId,
                                   @VerifyParam(required = true) Integer status) {
        userInfoService.updateUserStatus(userId, status);
        return Result.success();
    }

    @RequestMapping("/updateUserSpace")
    @GlobalInterceptor(checkAdmin = true, checkParams = true)
    public Result updateUserSpace(@VerifyParam(required = true) String userId,
                                   @VerifyParam(required = true) Integer changeSpace) {
        userInfoService.changeUserSpace(userId, changeSpace);
        return Result.success();
    }

    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkAdmin = true, checkParams = true)
    public Result loadFileList(PageQuery pageQuery, String fileNameFuzzy, String filePid) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(fileNameFuzzy!=null && !fileNameFuzzy.isBlank() && !fileNameFuzzy.isEmpty(), FileInfo::getFileName, fileNameFuzzy)
                .eq(filePid!=null, FileInfo::getFilePid, filePid);
        PageBean pageBean = fileInfoService.pageDataList(pageQuery, wrapper);
        return Result.success(pageBean);
    }

    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result getFolderInfo(@VerifyParam(required = true) String path, String shareId) {
//        fileInfoService.getFolderInfo()
        return Result.success(fileInfoService.getFolderInfo(null, path));
    }

    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result getFile(HttpServletResponse response,
                          @PathVariable("userId") String userId,
                          @PathVariable("fileId") String fileId) {
        fileInfoService.getFile(response, fileId, userId);
        return Result.success();
    }

    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result getFile(HttpServletResponse response, HttpSession session,
                          @PathVariable("userId") String userId,
                          @PathVariable("fileId") String fileId) {
        fileInfoService.getFile(response, fileId, userId);
        return Result.success();
    }

    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result getFile(@PathVariable("userId") String userId,
                          @PathVariable("fileId") String fileId) {
        return Result.success(fileInfoService.createDownloadUrl(userId, fileId));
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkAdmin = true, checkParams = true)
    public Result download(@VerifyParam(required = true) @PathVariable("code") String code,
                           HttpServletRequest request, HttpServletResponse response) throws Exception {
        fileInfoService.download(code, request, response);
        return Result.success();
    }

    @PostMapping("/delFile")
    @GlobalInterceptor(checkAdmin = true, checkParams = true)
    public Result delFile(@VerifyParam(required = true) String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId: fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0], Collections.singletonList(itemArray[1]), true);
        }
        return Result.success();
    }


}
