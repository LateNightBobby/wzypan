package com.wzypan.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzypan.entity.po.FileShare;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wzy
 * @since 2024-08-28
 */
public interface FileShareMapper extends BaseMapper<FileShare> {

    List<FileShare> selectPageWithJoin(Page<FileShare> page, @Param("userId") String userId);

    Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray, @Param("userId") String userId);
}
