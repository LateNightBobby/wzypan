package com.wzypan.controller;


import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.Result;
import com.wzypan.entity.enums.FileCategoryEnum;
import com.wzypan.service.FileInfoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ConcurrentModificationException;

/**
 * <p>
 * 文件信息表 前端控制器
 * </p>
 *
 * @author wzy
 * @since 2024-07-17
 */
@RestController
@RequestMapping("/file")
public class FileInfoController {

    @Resource
    private FileInfoService fileInfoService;

    @PostMapping("/loadDataList")
    @GlobalInterceptor(checkLogin = true)
    public Result loadDataList(HttpSession session, PageQuery pageQuery, String category, String filePid, String fileNameFuzzy) {

        FileCategoryEnum categoryCode = FileCategoryEnum.getByCategory(category);
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        PageBean pageResult = fileInfoService.pageDataList(pageQuery, userDto, categoryCode, filePid, fileNameFuzzy);
        return Result.success(pageResult);
    }
}

