package com.wzypan.entity.enums;

import lombok.Getter;

public enum FileFolderTypeEnum {

    FILE(0, "文件"),
    FOLDER(1, "文件夹");

    @Getter
    private Integer type;

    @Getter
    private String desc;

    FileFolderTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
