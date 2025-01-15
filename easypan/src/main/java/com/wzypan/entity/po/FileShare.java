package com.wzypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
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
public class FileShare implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分享链接id
     */
    @TableId(value = "share_id", type = IdType.INPUT)
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


}
