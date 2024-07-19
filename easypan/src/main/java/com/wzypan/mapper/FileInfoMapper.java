package com.wzypan.mapper;

import com.wzypan.entity.po.FileInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 文件信息表 Mapper 接口
 * </p>
 *
 * @author wzy
 * @since 2024-07-17
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    Long selectUseSpace(@Param("userId") String userId);
}
