package com.wzypan.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class FileShareDto {

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 分享人id
     */
    private String userId;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 分享时间
     */
    private Date shareTime;

    private String fileName;

    private String nickName;

    private Boolean currentUser;

    private String avatar;
}
