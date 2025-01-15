package com.wzypan.controller;


import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.Result;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileShare;
import com.wzypan.service.FileShareService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author wzy
 * @since 2024-08-28
 */
@RestController
@RequestMapping("/share")
public class FileShareController {

    @Resource
    private FileShareService fileShareService;

    @PostMapping("/loadShareList")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result loadShareList(PageQuery pageQuery, HttpSession session) {
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        PageBean pageBean = fileShareService.pageShareList(userDto.getUserId(), pageQuery);
        return Result.success(pageBean);
    }
    @PostMapping("/shareFile")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result shareFile(@VerifyParam(required = true)String fileId, @VerifyParam(required = true)Integer validType,
                            HttpSession session, String code) {
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileShare fileShare = new FileShare();
        fileShare.setCode(code).setValidType(validType).setFileId(fileId).setUserId(userDto.getUserId()).setShowCount(0);
        fileShareService.saveShare(fileShare);
        return Result.success(fileShare);
    }
    @PostMapping("/cancelShare")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result cancelShare(@VerifyParam(required = true)String shareIds, HttpSession session) {
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        String[] shareIdArray = shareIds.split(",");
        fileShareService.deleteFileShareBatch(shareIdArray, userDto.getUserId());
        return Result.success();
    }
}

