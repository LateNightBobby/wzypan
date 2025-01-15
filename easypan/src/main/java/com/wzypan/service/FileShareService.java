package com.wzypan.service;

import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileShare;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wzy
 * @since 2024-08-28
 */
public interface FileShareService extends IService<FileShare> {

    PageBean pageShareList(String userId, PageQuery pageQuery);

    void saveShare(FileShare fileShare);

    void deleteFileShareBatch(String[] shareIdArray, String userId);

}
