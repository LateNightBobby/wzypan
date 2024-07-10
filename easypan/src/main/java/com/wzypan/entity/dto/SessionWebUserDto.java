package com.wzypan.entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SessionWebUserDto {
    private String nickName;
    private String userId;
    private boolean isAdmin;
    private String avatar;
}
