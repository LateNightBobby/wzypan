package com.wzypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文件信息表
 * </p>
 *
 * @author wzy
 * @since 2024-07-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件id
     */
    @TableId(value = "file_id", type = IdType.INPUT)
    private String fileId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 文件md5值
     */
    private String fileMd5;

    /**
     * 父级id parentId
     */
    private String filePid;

    /**
     * 文件大小 Byte
     */
    private Long fileSize;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件封面
     */
    private String fileCover;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 最近更新时间
     */
    private Date lastUpdateTime;

    /**
     * 0 文件 1 目录
     */
    private Boolean folderType;

    /**
     * 文件分类 1视频 2音频 3图片 4文档 5其他
     */
    private Boolean fileCategory;

    /**
     * 详细文件类型 1视频 2音频 3图片 4pdf 5doc 6excel 7txt 8code 9zip 10其他
     */
    private Boolean fileType;

    /**
     * 状态 0转码中 1转码失败 2转码成功
     */
    private Boolean status;

    /**
     * 进入回收站时间
     */
    private Date recoveryTime;

    /**
     * 标记删除 0删除 1回收站 2正常
     */
    private Boolean delFlag;


}
