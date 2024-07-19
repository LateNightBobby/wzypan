package com.wzypan.service;

import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.enums.FileCategoryEnum;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
