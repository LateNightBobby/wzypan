package com.wzypan.entity.dto;

import java.util.Date;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 *
 * </p>
 *
 * @author wzy
 * @since 2024-08-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class FileShareInfoDto implements Serializable {

    private String shareId;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 分享人id
     */
    private String userId;

    /**
     * 有效期类型：0-1天，1-7天，2-30天，3-永久
     */
    private Integer validType;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 分享时间
     */
    private Date shareTime;

    /**
     * 分享码
     */
    private String code;

    /**
     * 查看次数
     */
    private Integer showCount;


    private String fileName;

    /**
     * 0:文件 1:目录
     */
    private Integer folderType;

    /**
     * 1:视频 2:音频  3:图片 4:文档 5:其他
     */
    private Integer fileCategory;

    /**
     * 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
     */
    private Integer fileType;

    /**
     * 封面
     */
    private String fileCover;


}
