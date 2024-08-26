package com.wzypan.service;

import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.UploadResultDto;
import com.wzypan.entity.enums.FileCategoryEnum;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * <p>
 * 文件信息表 服务类
 * </p>
 *
 * @author wzy
 * @since 2024-07-17
 */
public interface FileInfoService extends IService<FileInfo> {

    PageBean pageDataList(PageQuery pageQuery, SessionWebUserDto userDto, FileCategoryEnum categoryEnum, String filePid, String fileNameFuzzy);

    UploadResultDto uploadFile(SessionWebUserDto userDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    void getThumbnail(HttpServletResponse response, String imageFolder, String imageName);

    void getFile(HttpServletResponse response, String fileId, String userId);

    FileInfo newFolder(String filePid, String folderName, String userId);

    List getFolderInfo(String userId, String path);

    FileInfo renameFile(String userId, String fileId, String fileName);

    List loadAllFolder(String userId, String filePid, String curFileIds);

    List changeFileFolder(String userId, String fileIds, String filePid);

    String createDownloadUrl(String userId, String fileId);

    void download(String code, HttpServletRequest request, HttpServletResponse response) throws Exception;

    void removeFile2RecycleBatch(String userId, String[] fileIdArray);

}
