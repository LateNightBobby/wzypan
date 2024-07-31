package com.wzypan.entity.enums;

import lombok.Getter;

public enum FileFolderTypeEnum {

    FOLDER(0, "文件夹"),
    FILE(1, "文件");

    @Getter
    private Integer type;

    @Getter
    private String desc;

    FileFolderTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
