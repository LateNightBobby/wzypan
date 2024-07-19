package com.wzypan.entity.enums;

public enum UploadStatusEnum {
    UPLOAD_SECONDS("upload_seconds", "秒传"),
    UPLOADING("uploading", ""),
    UPLOAD_FINISH("upload_finish", "");

    private String status;
    private String desc;

    UploadStatusEnum(String status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public String getStatus() {
        return status;
    }
}
