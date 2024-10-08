package com.wzypan.mapper;

import com.wzypan.entity.po.FileInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

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

    void updateWithOldStatus(@Param("fileInfo") FileInfo fileInfo, @Param("oldStatus") Integer oldStatus);

    FileInfo selectByUserIdAndFileId(@Param("userId") String userId, @Param("fileId") String fileId);

//    void updateBatchIds(@Param("fileInfoList")List<FileInfo> fileInfoList);

    void updateDelFlagBatchIds(@Param("userId") String userId, @Param("bean")FileInfo fileInfo,
                               @Param("fileIdList") List<String> fileIdList, @Param("filePidList") List<String> filePidList,
                               @Param("oldDelFlag") Integer oldDelFlag);
    void delFileBatchIds(@Param("userId") String userId,
                               @Param("fileIdList") List<String> fileIdList, @Param("filePidList") List<String> filePidList,
                               @Param("oldDelFlag") Integer oldDelFlag);
}
