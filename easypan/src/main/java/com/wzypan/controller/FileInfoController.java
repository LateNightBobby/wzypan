package com.wzypan.controller;


import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.UploadResultDto;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.Result;
import com.wzypan.entity.enums.FileCategoryEnum;
import com.wzypan.service.FileInfoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/uploadFile")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public Result uploadFile(HttpSession session, String fileId,
                             MultipartFile file,
                             @VerifyParam(required = true) String fileName,
                             @VerifyParam(required = true) String filePid,
                             @VerifyParam(required = true) String fileMd5,
                             @VerifyParam(required = true) Integer chunkIndex,
                             @VerifyParam(required = true) Integer chunks) {

        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        UploadResultDto uploadResultDto = fileInfoService.uploadFile(userDto, fileId, file, fileName, filePid, fileMd5, chunkIndex ,chunks);
        return Result.success(uploadResultDto);
    }
}

