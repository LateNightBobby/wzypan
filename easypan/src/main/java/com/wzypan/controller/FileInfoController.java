package com.wzypan.controller;


import com.baomidou.mybatisplus.extension.api.R;
import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.FileInfoDto;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.UploadResultDto;
import com.wzypan.entity.enums.VerifyRegexEnum;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.Result;
import com.wzypan.entity.enums.FileCategoryEnum;
import com.wzypan.entity.po.FileInfo;
import com.wzypan.service.FileInfoService;
import com.wzypan.utils.CopyTools;
import com.wzypan.utils.StringTools;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ConcurrentModificationException;
import java.util.List;

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

    @RequestMapping("/getImage/{ImageFolder}/{ImageName}")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public void getImage(@PathVariable("ImageFolder") String imageFolder,
                           @PathVariable("ImageName") String imageName,
                           HttpServletResponse response) {
        if (StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName) ||
                !StringTools.pathIsOk(imageFolder) || !StringTools.pathIsOk(imageName)) {
            return;
        }
        fileInfoService.getThumbnail(response, imageFolder, imageName);
    }

    @RequestMapping("/getFile/{fileId}")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public void getFile(@PathVariable("fileId")String fileId, HttpServletResponse response, HttpSession session) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoService.getFile(response, fileId, webUserDto.getUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    @GlobalInterceptor(checkLogin = true)
    public void getVideo(@PathVariable("fileId")String fileId, HttpServletResponse response, HttpSession session) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileInfoService.getFile(response, fileId, webUserDto.getUserId());
    }

    @PostMapping("/newFoloder")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result newFolder(@VerifyParam(required = true) String filePid,
                            @VerifyParam(required = true) String fileName,
                            HttpSession session) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileInfo folder = fileInfoService.newFolder(filePid, fileName, webUserDto.getUserId());
        return Result.success(CopyTools.copy(folder, FileInfoDto.class));
    }
    @PostMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result getFolderInfo(@VerifyParam(required = true) String path, HttpSession session) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        List folderList = fileInfoService.getFolderInfo(webUserDto.getUserId(), path);
        return Result.success(CopyTools.copyList(folderList, FileInfoDto.class));
    }

    @PostMapping("/rename")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public Result rename(@VerifyParam(required = true) String fileId,
                         @VerifyParam(required = true) String fileName,
                         HttpSession session) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileInfo fileInfo = fileInfoService.renameFile(webUserDto.getUserId(), fileId, fileName);
        return Result.success(CopyTools.copy(fileInfo, FileInfoDto.class));
    }

    @PostMapping("/loadAllFolder")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public Result loadAllFolder(@VerifyParam(required = true) String filePid,
                                String currentFileIds, HttpSession session) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        List folderList = fileInfoService.loadAllFolder(webUserDto.getUserId(), filePid, currentFileIds);
        return Result.success(CopyTools.copyList(folderList, FileInfoDto.class));
    }

    @PostMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result changeFileFolder(HttpSession session,
                                   @VerifyParam(required = true) String fileIds,
                                   @VerifyParam(required = true) String filePid) {
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        List fileInfoList = fileInfoService.changeFileFolder(webUserDto.getUserId(), fileIds, filePid);
        return Result.success(CopyTools.copyList(fileInfoList, FileInfoDto.class));
    }
}

