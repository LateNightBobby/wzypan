package com.wzypan.service;

import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.dto.UploadResultDto;
import com.wzypan.entity.enums.FileCategoryEnum;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

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
}
