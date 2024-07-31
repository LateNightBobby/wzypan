package com.wzypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzypan.entity.config.AppConfig;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.UploadResultDto;
import com.wzypan.entity.dto.UserSpaceDto;
import com.wzypan.entity.enums.*;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.wzypan.entity.po.UserInfo;
import com.wzypan.exception.BusinessException;
import com.wzypan.mapper.FileInfoMapper;
import com.wzypan.service.FileInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzypan.service.UserInfoService;
import com.wzypan.utils.RedisComponent;
import com.wzypan.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 文件信息表 服务实现类
 * </p>
 *
 * @author wzy
 * @since 2024-07-17
 */
@Service
@Slf4j
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;

    @Override
    public PageBean pageDataList(PageQuery pageQuery, SessionWebUserDto userDto, FileCategoryEnum categoryEnum, String filePid, String fileNameFuzzy) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        //code为null 即为all 选择所有文件
        if (categoryEnum!=null) {
            wrapper.eq(FileInfo::getFileCategory, categoryEnum.getCode());
        }
        wrapper .eq(FileInfo::getUserId, userDto.getUserId())
                .eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag())
                .eq(FileInfo::getFilePid, filePid).like(!fileNameFuzzy.isEmpty() && !fileNameFuzzy.isBlank(), FileInfo::getFileName, fileNameFuzzy);
        wrapper.orderByDesc(FileInfo::getLastUpdateTime);
        Page<FileInfo> page = new Page<>(pageQuery.getPageNo()==null? 1: pageQuery.getPageNo(), pageQuery.getPageSize()==null? 15: pageQuery.getPageSize());
        IPage<FileInfo> filePage = fileInfoMapper.selectPage(page, wrapper);
        return PageBean.convertFromPage(filePage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionWebUserDto userDto, String fileId, MultipartFile file,
                                      String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {

        UploadResultDto resultDto = new UploadResultDto();
        boolean uploadSuccess = true;
        File tempFileFolder = null;
        try {
            if (StringTools.isEmpty(fileId)) {
                fileId = StringTools.getRandomNumber(Constants.FILE_ID_LENGTH);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();
            UserSpaceDto userSpaceDto = redisComponent.getUserSpace(userDto.getUserId());

            //第一个分片
            if (chunkIndex == 0) {
                LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(FileInfo::getFileMd5, fileMd5).eq(FileInfo::getStatus, FileStatusEnum.USING.getStatus());
                FileInfo dbFile = fileInfoMapper.selectOne(wrapper);
                //存在文件 不重复上传
                if (dbFile != null) {
                    if (dbFile.getFileSize() + userSpaceDto.getUseSpace() > userSpaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    //将数据库中的文件直接复制
                    dbFile.setFileId(fileId).setUserId(userDto.getUserId());
                    dbFile.setCreateTime(curDate).setLastUpdateTime(curDate);
                    dbFile.setFileName(autoRename(fileName, userDto.getUserId(), filePid));
                    fileInfoMapper.insert(dbFile);
                    log.info("秒传");

                    //更新用户空间redis+UserInfo
                    updateUserSpace(userDto.getUserId(), dbFile.getFileSize());

                    resultDto.setStatus(UploadStatusEnum.UPLOAD_SECONDS.getStatus());
                    return resultDto;
                }
            }

            //判断空间
            Long currentTempSize= redisComponent.getFileTempSize(fileId, userDto.getUserId());
            if (file.getSize() + currentTempSize + userSpaceDto.getUseSpace() > userSpaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }
            //暂存 临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_TEMP_NAME;
            String currentUserFolderName = userDto.getUserId()+"_"+fileId;

            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }

            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            file.transferTo(newFile);
            //未传完所有分片
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnum.UPLOADING.getStatus());
                redisComponent.saveFileTempSize(userDto.getUserId(), fileId, file.getSize());
                return resultDto;
            }

            //上传完毕所有分片 更新数据库 合并
            String month = DateFormatUtils.format(new Date(), DateTimePatternEnum.YYYY_MM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            //完整用户文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnum fileType = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            //自动重命名
            fileName = autoRename(realFileName, userDto.getUserId(), filePid);
            //装入数据库
            FileInfo fileInfo = new FileInfo().setFileId(fileId).setUserId(userDto.getUserId());
            fileInfo.setFileMd5(fileMd5).setFilePid(filePid).setFileName(fileName);
            fileInfo.setFilePath(month+"/"+realFileName);
            fileInfo.setCreateTime(curDate).setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileType.getCategory().getCode()).setFileType(fileType.getType());
            fileInfo.setStatus(FileStatusEnum.TRANSFER.getStatus()).setDelFlag(FileDelFlagEnum.USING.getFlag());
            fileInfo.setFolderType(FileFolderTypeEnum.FILE.getType());
            fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(userDto.getUserId(), fileId);
            updateUserSpace(userDto.getUserId(), totalSize);

            resultDto.setStatus(UploadStatusEnum.UPLOAD_FINISH.getStatus());
            return resultDto;

        } catch (BusinessException e) {
            log.error("failed to upload file");
            uploadSuccess = false;
            throw e;
        } catch (Exception e) {
            log.error("failed to upload file {}", fileName);
            uploadSuccess = false;
        } finally {
            if (!uploadSuccess && tempFileFolder != null) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    log.error("删除临时目录失败", e);
                }
            }
        }

        return resultDto;
    }

    private void updateUserSpace(String userId, Long fileSize) {
        UserInfo userInfo = userInfoService.getById(userId);
        if (userInfo.getUseSpace() + fileSize <= userInfo.getTotalSpace()) {
            userInfo.setUseSpace(userInfo.getUseSpace() + fileSize);
        }
        userInfoService.updateById(userInfo);

        UserSpaceDto userSpaceDto = redisComponent.getUserSpace(userId);
        userSpaceDto.setUseSpace(userSpaceDto.getUseSpace() + fileSize);
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);
    }

    private String autoRename(String fileName, String userId, String filePid) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileName, fileName).eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId).eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag());
        Integer count = fileInfoMapper.selectCount(wrapper);
        //该目录下已经存在同名文件 进行重命名
        if (count > 0) {
            fileName = StringTools.rename(fileName);
        }
        return fileName;
    }

}
