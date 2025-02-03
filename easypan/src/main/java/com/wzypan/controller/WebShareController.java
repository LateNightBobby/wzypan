package com.wzypan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.Result;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.FileInfoDto;
import com.wzypan.entity.dto.FileShareDto;
import com.wzypan.entity.dto.SessionShareDto;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.enums.FileDelFlagEnum;
import com.wzypan.entity.enums.ResponseCodeEnum;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.wzypan.entity.po.FileShare;
import com.wzypan.entity.po.UserInfo;
import com.wzypan.exception.BusinessException;
import com.wzypan.mapper.FileInfoMapper;
import com.wzypan.service.FileInfoService;
import com.wzypan.service.FileShareService;
import com.wzypan.service.UserInfoService;
import com.wzypan.utils.CopyTools;
import com.wzypan.utils.StringTools;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

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

    @PostMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkParams = true)
    public Result getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        if (sessionShareDto==null) {
//            return Result.error(ResponseCodeEnum.CODE_902.getCode(), "invalid shareId");
            return Result.success();
        }
        FileShareDto fileShareDto = getShareFileDto(shareId);
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        if (webUserDto!=null && webUserDto.getUserId().equals(fileShareDto.getUserId())) {
            fileShareDto.setCurrentUser(true);
        } else {
            fileShareDto.setCurrentUser(false);
        }
        return Result.success(fileShareDto);
    }

    @PostMapping("/getShareInfo")
    @GlobalInterceptor(checkParams = true)
    public Result getShareInfo(@VerifyParam(required = true) String shareId) {

        return Result.success(getShareFileDto(shareId));
    }

    @PostMapping("/checkShareCode")
    @GlobalInterceptor(checkParams = true)
    public Result checkShareCode(HttpSession session,
                                 @VerifyParam(required = true) String shareId,
                                 @VerifyParam(required = true) String code) {
//        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        SessionShareDto sessionShareDto = fileShareService.checkShareCode(shareId, code);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, sessionShareDto);
        return Result.success();
    }

    @PostMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true)
    public Result loadFileList(HttpSession session, PageQuery pageQuery,
                               @VerifyParam(required = true) String shareId,
                               String filePid) {
        SessionShareDto sessionShareDto = checkShare(session, shareId);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        if (filePid!=null && !StringTools.isEmpty(filePid) && !filePid.equals("0")) {
            fileInfoService.checkRootFilePid(sessionShareDto.getFileId(), sessionShareDto.getShareUserId(), filePid);
            wrapper.eq(FileInfo::getFilePid, filePid);
        }
        wrapper.eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag())
                .eq(FileInfo::getFileId, sessionShareDto.getFileId())
                .eq(FileInfo::getUserId, sessionShareDto.getShareUserId());
        PageBean fileInfoPage = fileInfoService.pageDataList(pageQuery, wrapper);
        fileInfoPage.setList(CopyTools.copyList(fileInfoPage.getList(), FileInfoDto.class));
        return Result.success(fileInfoPage);
    }

    @PostMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public Result getFolderInfo(@VerifyParam(required = true) String path, HttpSession session,
                                @VerifyParam(required = true) String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        List folderList = fileInfoService.getFolderInfo(sessionShareDto.getShareUserId(), path);
        return Result.success(CopyTools.copyList(folderList, FileInfoDto.class));
    }

    @RequestMapping("/getFile/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getFile(@PathVariable("fileId")String fileId, HttpServletResponse response, HttpSession session,
                        @PathVariable("shareId") String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        fileInfoService.getFile(response, fileId, sessionShareDto.getShareUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getVideo(@PathVariable("fileId")String fileId, @PathVariable("shareId") String shareId,
                         HttpServletResponse response, HttpSession session) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        fileInfoService.getFile(response, fileId, sessionShareDto.getShareUserId());
    }

    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public Result createDownloadUrl (HttpSession session,
                                     @PathVariable("fileId") @VerifyParam(required = true) String fileId,
                                     @PathVariable("shareId") String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        String downloadCode = fileInfoService.createDownloadUrl(sessionShareDto.getShareUserId(), fileId);
        return Result.success(downloadCode);
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public Result download(@VerifyParam(required = true) @PathVariable("code") String code,
                           HttpServletRequest request, HttpServletResponse response) throws Exception {
        fileInfoService.download(code, request, response);
        return Result.success();
    }

    @PostMapping("/saveShare")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public Result saveShare(HttpSession session, @VerifyParam(required = true) String shareId,
                            @VerifyParam(required = true) String shareFileIds,
                            @VerifyParam(required = true) String myFolderId) {
        SessionShareDto shareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        if (shareDto==null) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        if (userDto==null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if (shareDto.getShareUserId().equals(userDto.getUserId())) {
            throw new BusinessException("cannot save your share file to your own disk");
        }
        fileInfoService.saveShare(shareDto.getFileId(), shareFileIds, myFolderId, shareDto.getShareUserId(), userDto.getUserId());
        return Result.success();
    }

    private FileShareDto getShareFileDto(String shareId) {
        FileShare fileShare = fileShareService.getById(shareId);
        if (fileShare==null || fileShare.getExpireTime()!=null && new Date().after(fileShare.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        FileShareDto fileShareDto = CopyTools.copy(fileShare, FileShareDto.class);
//        BeanUtils.copyProperties(fileShare, fileShareDto);

        FileInfo fileInfo = fileInfoMapper.selectByUserIdAndFileId(fileShareDto.getUserId(), fileShareDto.getFileId());
        if (fileInfo==null || !FileDelFlagEnum.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        fileShareDto.setFileName(fileInfo.getFileName());

        UserInfo userInfo = userInfoService.getById(fileShare.getUserId());

        fileShareDto.setNickName(userInfo.getNickName()).setAvatar(userInfo.getQqAvatar()).setUserId(userInfo.getUserId());
        return fileShareDto;
    }

    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        if (sessionShareDto==null) {
            throw new BusinessException(ResponseCodeEnum.CODE_903);
        }
        if (sessionShareDto.getExpireTime()!=null && new Date().after(sessionShareDto.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        return sessionShareDto;
    }

}
