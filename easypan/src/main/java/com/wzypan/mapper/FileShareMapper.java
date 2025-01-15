package com.wzypan.mapper;

import com.wzypan.entity.po.FileShare;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wzy
 * @since 2024-08-28
 */
public interface FileShareMapper extends BaseMapper<FileShare> {
    Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray, @Param("userId") String userId);
}
