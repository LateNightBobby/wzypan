package com.wzypan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.Result;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.enums.FileDelFlagEnum;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.wzypan.service.FileInfoService;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 回收站控制
 */
@RestController
@RequestMapping("/recycle")
public class RecycleController {

    @Resource
    private FileInfoService fileInfoService;

    @PostMapping("/loadRecycleList")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public Result loadRecycleList(HttpSession session, @VerifyParam(required = true) PageQuery pageQuery) {
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userDto.getUserId()).eq(FileInfo::getDelFlag, FileDelFlagEnum.RECYCLE.getFlag());
        wrapper.orderByDesc(FileInfo::getRecoveryTime);
        PageBean pageBean = fileInfoService.pageDataList(pageQuery, wrapper);
        return Result.success(pageBean);
    }

    @PostMapping("/recoverFile")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result recoverFile(@VerifyParam(required = true)String fileIds, HttpSession session) {
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        List<String> fileIdList = Arrays.asList(fileIds.split(","));
        fileInfoService.recoverFileBatch(userDto.getUserId(), fileIdList);
        return Result.success();
    }
    @PostMapping("/delFile")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result delFile(@VerifyParam(required = true)String fileIds, HttpSession session) {
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        List<String> fileIdList = Arrays.asList(fileIds.split(","));

        fileInfoService.delFileBatch(userDto.getUserId(), fileIdList, false);
        return Result.success();
    }
}
