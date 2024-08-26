package com.wzypan.entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DownloadFileDto {

    private String downloadCode;
    private String fileId;
    private String fileName;
    private String filePath;
}
