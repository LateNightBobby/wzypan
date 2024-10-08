package com.wzypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzypan.entity.config.AppConfig;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.*;
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
import com.wzypan.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;


    @Override
    public PageBean pageDataList(PageQuery pageQuery, LambdaQueryWrapper wrapper) {
        //code为null 即为all 选择所有文件

        Page<FileInfo> page = new Page<>(pageQuery.getPageNo()==null? 1: pageQuery.getPageNo(), pageQuery.getPageSize()==null? 15: pageQuery.getPageSize());
        IPage filePage = fileInfoMapper.selectPage(page, wrapper);
        return PageBean.convertFromPage(filePage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, List<String> fileIdList, boolean adminOp) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getDelFlag, FileDelFlagEnum.RECYCLE.getFlag()).in(FileInfo::getFileId, fileIdList);
        List<FileInfo> dbFileList = fileInfoMapper.selectList(wrapper);
        //找到子文件
        List<String> delFileSubFilePidList = new ArrayList<>();
        for (FileInfo item: dbFileList) {
            findAllSubFolderFileList(delFileSubFilePidList, item.getFileId(), userId, FileDelFlagEnum.DEL.getFlag());
        }
        //删除子文件
        if (!delFileSubFilePidList.isEmpty()) {
            fileInfoMapper.delFileBatchIds(userId, null, delFileSubFilePidList,
                    adminOp? null: FileDelFlagEnum.DEL.getFlag());
        }
        //删除所选文件
        fileInfoMapper.delFileBatchIds(userId, fileIdList, null,
                adminOp? null: FileDelFlagEnum.RECYCLE.getFlag());
        //更新缓存和用户使用空间
        Long usedSpace = fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = userInfoService.getById(userId);
        userInfo.setUseSpace(usedSpace);
        userInfoService.updateById(userInfo);

        UserSpaceDto userSpaceDto = redisComponent.getUserSpace(userId);
        userSpaceDto.setUseSpace(usedSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);

        //？物理删除 --》 判断是否有相同的md5，没有则可以删除
    }

    @Override
    //尝试多线程？
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
                wrapper.eq(FileInfo::getFileMd5, fileMd5).eq(FileInfo::getDelFlag, FileStatusEnum.USING.getStatus());
                List<FileInfo> dbFileList = fileInfoMapper.selectList(wrapper);
//                FileInfo dbFile = fileInfoMapper.selectOne(wrapper);
                //存在文件 不重复上传
                if (!dbFileList.isEmpty()) {
                    FileInfo dbFile = dbFileList.get(0);
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

            redisComponent.saveFileTempSize(userDto.getUserId(), fileId, file.getSize());
            //上传完毕所有分片 更新数据库 合并
            String month = DateFormatUtils.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            //完整用户文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnum fileType = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            //自动重命名
//            fileName = autoRename(realFileName, userDto.getUserId(), filePid);
            //装入数据库
            FileInfo fileInfo = new FileInfo().setFileId(fileId).setUserId(userDto.getUserId());
            fileInfo.setFileMd5(fileMd5).setFilePid(filePid).setFileName(fileName);
            fileInfo.setFilePath(month+"/"+realFileName);
//            fileInfo.setFileSize() 转码合并后更新文件大小
            fileInfo.setCreateTime(curDate).setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileType.getCategory().getCode()).setFileType(fileType.getType());
            fileInfo.setStatus(FileStatusEnum.TRANSFER.getStatus()).setDelFlag(FileDelFlagEnum.USING.getFlag());
            fileInfo.setFolderType(FileFolderTypeEnum.FILE.getType());
            fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(userDto.getUserId(), fileId);
            updateUserSpace(userDto.getUserId(), totalSize);

            resultDto.setStatus(UploadStatusEnum.UPLOAD_FINISH.getStatus());

            //异步合并
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), userDto);
                }
            });
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

    @Override
    public void getThumbnail(HttpServletResponse response, String imageFolder, String imageName) {
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/"+imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        com.wzypan.utils.FileUtils.readFile(response, filePath);
    }

    @Override
    public void getFile(HttpServletResponse response, String fileId, String userId) {

        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsArray = fileId.split("_");
            String realFileId = tsArray[0];
            FileInfo fileInfo = fileInfoMapper.selectByUserIdAndFileId(userId, realFileId);
            if (fileInfo == null) return;
            filePath = appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+
                    StringTools.getFileNameNoSuffix(fileInfo.getFilePath())+"/"+fileId;
        }
        else {
            FileInfo fileInfo = fileInfoMapper.selectByUserIdAndFileId(userId, fileId);

            if (fileInfo == null) {
                return;
            }
            if (FileCategoryEnum.VIDEO.getCode().equals(fileInfo.getFileCategory())) {
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE +
                        fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                filePath = appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+fileInfo.getFilePath();
            }
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        com.wzypan.utils.FileUtils.readFile(response, filePath);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo newFolder(String filePid, String folderName, String userId) {
        checkFileNameOk(folderName, FileFolderTypeEnum.FOLDER.getType(), filePid, userId);
        Date curDate = new Date();
        FileInfo folderFileInfo = new FileInfo();
        folderFileInfo.setFolderType(FileFolderTypeEnum.FOLDER.getType()).setFilePid(filePid)
                .setUserId(userId).setFileName(folderName).setFileId(StringTools.getRandomNumber(10));
        folderFileInfo.setCreateTime(curDate).setLastUpdateTime(curDate);
        folderFileInfo.setStatus(FileStatusEnum.USING.getStatus()).setDelFlag(FileDelFlagEnum.USING.getFlag());
        fileInfoMapper.insert(folderFileInfo);
        return folderFileInfo;
    }

    @Override
    public List getFolderInfo(String userId, String path) {
        String[] pathArray = path.split("/");
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getFolderType, FileFolderTypeEnum.FOLDER.getType())
                .in(FileInfo::getFileId, Arrays.asList(pathArray));
        String orderSql = String.join(",", pathArray);
        wrapper.last("ORDER BY FIELD(file_id, " + orderSql + ")");
        List<FileInfo> folderList = fileInfoMapper.selectList(wrapper);
        return folderList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo renameFile(String userId, String fileId, String fileName) {
        //获取原本文件
        FileInfo fileInfo = fileInfoMapper.selectByUserIdAndFileId(userId, fileId);
        if (fileInfo==null) {
            throw new BusinessException("no such file");
        }

        String filePid = fileInfo.getFilePid();
        checkFileNameOk(fileName, fileInfo.getFolderType(), filePid, userId);
        if (FileFolderTypeEnum.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }

        Date curDate = new Date();
        fileInfo.setFileName(fileName).setLastUpdateTime(curDate);
        fileInfoMapper.updateById(fileInfo);

        //再次查一遍，并发时若重复回滚
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileName, fileName).eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getUserId, userId).eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag());
        Integer count = fileInfoMapper.selectCount(wrapper);
        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已存在");
        }

        return fileInfo;
    }

    @Override
    public List loadAllFolder(String userId, String filePid, String curFileIds) {
        //curFileIds要移动的文件夹列表
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getFolderType, FileFolderTypeEnum.FOLDER.getType())
                .eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag());
        if (curFileIds!=null&&!curFileIds.isBlank()) {
            wrapper.notIn(FileInfo::getFileId, Arrays.asList(curFileIds.split(",")));
        }
        wrapper.orderByDesc(FileInfo::getCreateTime);
        return fileInfoMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String[] fileIdArray) {
        //查要删除的文件
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag());
        wrapper.in(FileInfo::getFileId, Arrays.asList(fileIdArray));
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(wrapper);
        if (fileInfoList==null) {
            return;
        }
        //文件夹递归删除子文件
        List<String> delFilePidList = new ArrayList<>();
        for (FileInfo fileInfo: fileInfoList) {
            if (fileInfo.getFolderType().equals(FileFolderTypeEnum.FOLDER.getType())) {
                this.findAllSubFolderFileList(delFilePidList, fileInfo.getFileId(), userId, FileDelFlagEnum.USING.getFlag());
            }
        }
        FileInfo updateInfo = new FileInfo().setRecoveryTime(new Date()).setDelFlag(FileDelFlagEnum.DEL.getFlag());
        //子文件及子文件夹设置为删除
        if (!delFilePidList.isEmpty()) {
            fileInfoMapper.updateDelFlagBatchIds(userId, updateInfo, null, delFilePidList, FileDelFlagEnum.USING.getFlag());
        }
        //直接选中的放在回收站
        updateInfo = new FileInfo().setRecoveryTime(new Date()).setDelFlag(FileDelFlagEnum.RECYCLE.getFlag());
        fileInfoMapper.updateDelFlagBatchIds(userId, updateInfo, Arrays.asList(fileIdArray), null, FileDelFlagEnum.USING.getFlag());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, List<String> fileIdList) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, fileIdList)
                .eq(FileInfo::getDelFlag, FileDelFlagEnum.RECYCLE.getFlag());
        List<FileInfo> recoverFileList = fileInfoMapper.selectList(wrapper);
        //将delete的子文件找出
        List<String> recoverFilePidList = new ArrayList<>();
        for (FileInfo fileInfo: recoverFileList) {
            if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileList(recoverFilePidList, fileInfo.getFileId(), userId, FileDelFlagEnum.DEL.getFlag());
            }
        }
        //获取根目录下文件名的map
        LambdaQueryWrapper<FileInfo> rootFileWrapper = new LambdaQueryWrapper<>();
        rootFileWrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, Constants.ROOT_DIR_ID)
                .eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag());
        List<FileInfo> rootFileList = fileInfoMapper.selectList(rootFileWrapper);
        Map<String, FileInfo> rootFileNameMap = rootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, e-> e));
        FileInfo updateInfo = new FileInfo().setDelFlag(FileDelFlagEnum.USING.getFlag());
        //恢复子文件
        if (!recoverFilePidList.isEmpty()) {
            fileInfoMapper.updateDelFlagBatchIds(userId,updateInfo, null, recoverFilePidList,FileDelFlagEnum.DEL.getFlag());
        }
        //回复父文件到根目录
        updateInfo = new FileInfo().setDelFlag(FileDelFlagEnum.USING.getFlag()).setLastUpdateTime(new Date()).setFilePid(Constants.ROOT_DIR_ID);
        fileInfoMapper.updateDelFlagBatchIds(userId, updateInfo, fileIdList, null, FileDelFlagEnum.RECYCLE.getFlag());
        //检查父文件是否出现重名
        for (FileInfo fileInfo: recoverFileList) {
            FileInfo value = rootFileNameMap.get(fileInfo.getFileName());
            if (value!=null) {
                fileInfo.setFileName(StringTools.rename(fileInfo.getFileName()));
                fileInfoMapper.updateById(fileInfo);
            }
        }
    }

    @Override
    public void download(String code, HttpServletRequest request, HttpServletResponse response) throws Exception{
        DownloadFileDto fileDto = redisComponent.getDownloadFile(code);
        if (fileDto==null) {
            return;
        }
        String filePath = appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+fileDto.getFilePath();
        String fileName = fileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {
            fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        } else {
            fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\""+fileName+"\"");
        com.wzypan.utils.FileUtils.readFile(response, filePath);
    }

    @Override
    public String createDownloadUrl(String userId, String fileId) {
        FileInfo fileInfo = fileInfoMapper.selectByUserIdAndFileId(userId, fileId);
        if (fileInfo==null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        String code = StringTools.getRandomNumber(Constants.LENGTH_50);
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code).setFilePath(fileInfo.getFilePath())
                .setFileName(fileInfo.getFileName()).setFileId(fileId);
        redisComponent.saveDownloadFile(code, downloadFileDto);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List changeFileFolder(String userId, String fileIds, String filePid) {
        String[] fileIdArray = fileIds.split(",");
        for (String s : fileIdArray) {
            if (s.equals(filePid)) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        //检查父目录是否可用
        if (!filePid.equals(Constants.ROOT_DIR_ID)) {
            FileInfo parentFolderInfo = fileInfoMapper.selectByUserIdAndFileId(userId, filePid);
            if (parentFolderInfo==null || !FileDelFlagEnum.USING.getFlag().equals(parentFolderInfo.getDelFlag())) {
                //父文件夹为空或不可用
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        //读取父目录下文件，使用文件名作为key
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFilePid, filePid).eq(FileInfo::getUserId, userId);
        List<FileInfo> fileList = fileInfoMapper.selectList(wrapper);
        Map<String, FileInfo> fileNameMap = fileList.stream()
                .collect(Collectors.toMap(FileInfo::getFileName, fileInfo -> fileInfo,
                        (existing, replacement)->replacement));

        //选出所有要修改目录的文件
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, Arrays.asList(fileIdArray));
        List<FileInfo> selectedFileList = fileInfoMapper.selectList(wrapper);

        Date curDate = new Date();
        //查询文件名是否重复
        for (FileInfo selectedFile : selectedFileList) {
            FileInfo parDirFile = fileNameMap.get(selectedFile.getFileName());
            if (parDirFile != null) {
                selectedFile.setFileName(StringTools.rename(selectedFile.getFileName()));
            }
            selectedFile.setFilePid(filePid).setLastUpdateTime(curDate);
            fileInfoMapper.updateById(selectedFile);
        }
//        fileInfoMapper.updateBatchIds(selectedFileList);
        return selectedFileList;
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

    @Async("applicationTaskExecutor")
    public void transferFile(String fileId, SessionWebUserDto webUserDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnum fileTypeEnum = null;

        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        try {
            if (fileInfo==null || !fileInfo.getStatus().equals(FileStatusEnum.TRANSFER.getStatus())) {
                return;
            }
        //临时目录
        String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_TEMP_NAME;
        String currentUserFolderName = webUserDto.getUserId()+"_"+fileId;
        File tempFileFolder = new File(tempFolderName + currentUserFolderName);

        String fileNameSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
        String month = DateFormatUtils.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());

        //目标目录
        String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFolder = new File(targetFolderName+"/"+month);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        String realFileName = currentUserFolderName + fileNameSuffix;
        targetFilePath = targetFolder.getPath() + "/" + realFileName;

        //合并文件
        union(tempFileFolder.getPath(), targetFilePath, fileInfo.getFileName(), false);

        //视频文件切片
        fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileNameSuffix);
        if (FileTypeEnum.VIDEO==fileTypeEnum) {
            cutFile4Video(fileId, targetFilePath);
//            生成缩略图
            cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
            String coverPath = targetFolderName + "/" + cover;
            ScaleFilter.createCover4Video(new File(targetFilePath), 150, new File(coverPath));
        } else if (FileTypeEnum.IMAGE==fileTypeEnum) {
            //在同一目录下最后的.前加上_的即为缩略图
            cover = month + "/" + realFileName.replace(".", "_.");
            String coverPath = targetFolderName + "/" + cover;
            Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), 150,
                    new File(coverPath), false);
            if (!created) {
                //图片太小无需压缩，复制一份到缩略图
                FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
            }
        }

        } catch (Exception e) {
            log.error("文件{}转码失败", fileInfo.getFilePath());
            transferSuccess = false;
        } finally {
            Integer oldStatus = null;
            if (fileInfo != null) {
                oldStatus = fileInfo.getStatus();
                fileInfo.setFileSize(new File(targetFilePath).length()).setFileCover(cover);
                fileInfo.setStatus(transferSuccess ? FileStatusEnum.USING.getStatus() : FileStatusEnum.TRANSFER_FAIL.getStatus());
                fileInfo.setLastUpdateTime(new Date());
                //乐观锁
                fileInfoMapper.updateWithOldStatus(fileInfo, oldStatus);
//            if (fileInfoMapper.selectById(fileInfo.getFileId()).getStatus().equals(fileInfo.getStatus()))
//                fileInfoMapper.updateById(fileInfo);
            }
        }
    }

    private static void union(String dirPath, String toFilePath, String fileName, Boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("not exist dir");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; ++i) {
                int len = -1;
                File chunkFile = new File(dirPath + "/" + fileList[i].getName());
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len= readFile.read(b))!= -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并失败", e);
                    throw new BusinessException("合并文件"+fileName+"失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            log.error("合并失败", e);
            throw new BusinessException("合并文件"+fileName+"出错");
        } finally {
            if (writeFile!=null) {
                try {
                    writeFile.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
            if (delSource && dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -vcodec copy -bsf:v h264_mp4toannexb %s";
        String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 15 %s/%s_%%04d.ts";
        String tsPath = tsFolder+"/"+Constants.TS_NAME;
        //执行ffmpeg 原视频分片生成index.ts文件
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //ts分片生成m3u8
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath()+"/"+Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }

    private void checkFileNameOk(String fileName, Integer folderType, String filePid, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileName, fileName).eq(FileInfo::getFolderType, folderType)
                .eq(FileInfo::getFilePid, filePid).eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag());
        Integer count = fileInfoMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("该目录下存在同名文件，请重新命名");
        }
    }

    /**
     *
     * @param fileIdList 用于记录要被删除的Pid
     * @param fileId
     * @param userId
     * @param delFlag
     */
    private void findAllSubFolderFileList(List<String> fileIdList, String fileId, String userId, Integer delFlag) {
        fileIdList.add(fileId);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, fileId).eq(FileInfo::getDelFlag, delFlag);
        List<FileInfo> subFileInfoList = fileInfoMapper.selectList(wrapper);
        for (FileInfo subFile: subFileInfoList) {
            if (FileFolderTypeEnum.FOLDER.getType().equals(subFile.getFolderType())) {
                findAllSubFolderFileList(fileIdList, subFile.getFileId(), userId, delFlag);
            }
//            else {
//                fileIdList.add(subFile.getFileId());
//            }
        }
    }
}
