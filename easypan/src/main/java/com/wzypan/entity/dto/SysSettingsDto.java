package com.wzypan.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingsDto implements Serializable {

    private String registerMailTitle = "Email verify code";

    private String registerMailContent = "邮箱验证码为 %s , 15分钟有效";

    //初始可用空间 MB
    private Integer userInitUseSpace = 5;
}
