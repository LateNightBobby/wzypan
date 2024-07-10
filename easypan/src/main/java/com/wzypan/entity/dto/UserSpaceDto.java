package com.wzypan.entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class UserSpaceDto implements Serializable {
    Long useSpace;
    Long totalSpace;
}
