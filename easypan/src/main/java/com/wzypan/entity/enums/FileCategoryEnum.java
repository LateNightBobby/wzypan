package com.wzypan.entity.enums;

public enum FileCategoryEnum {
    VIDEO(1, "video", "视频"),
    MUSIC(2, "music", "音频"),
    IMAGE(3, "image", "图像"),
    DOC(4, "doc", "文档"),
    OTHERS(5, "others", "其他");


    private Integer code;
    private String category;
    private String desc;

    FileCategoryEnum(Integer code, String category, String desc) {
        this.category = category;
        this.code = code;
        this.desc = desc;
    }

    public static FileCategoryEnum getByCategory(String category) {
        for (FileCategoryEnum item: FileCategoryEnum.values()) {
            if (item.getCode().equals(category)) {
                return item;
            }
        }
        return null;
    }

    public String getCategory() {
        return category;
    }

    public Integer getCode() {
        return code;
    }
}
