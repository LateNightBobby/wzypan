package com.wzypan.entity.enums;

public enum FileDelFlagEnum {
    DEL(0, "删除"),
    RECYCLE(1, "回收站"),
    USING(2, "使用中");

    private final Integer flag;

    private final String desc;

    FileDelFlagEnum (Integer code, String desc) {
        this.desc = desc;
        this.flag = code;
    }

    public Integer getFlag() {
        return flag;
    }

}
