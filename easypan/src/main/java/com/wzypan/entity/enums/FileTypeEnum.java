package com.wzypan.entity.enums;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

public enum FileTypeEnum {
//    1视频 2音频 3图片 4pdf 5doc 6excel 7txt 8code 9zip 10其他
    VIDEO(FileCategoryEnum.VIDEO, 1, new String[]{".mp4",".avi",".rmvb"," .mkv"," .mov"}, "视频"),
    MUSIC(FileCategoryEnum.MUSIC, 2, new String[]{".mps", ".wav", ".wma", ".mp2", ".flac", ".midi", ".ra"
    , ".ape", ".aac", ".cda"}, "音频"),
    IMAGE(FileCategoryEnum.IMAGE, 3, new String[]{".jpeg", ".jpg", ".png", ".gif", ".bmp", ".dds", ".psd"
    , ".pdt", ".webp", ".xmp", ".svg", ".tiff"}, "图片"),
    PDF(FileCategoryEnum.DOC, 4, new String[]{".pdf"}, "PDF"),
    WORD(FileCategoryEnum.DOC, 5, new String[]{".doc", ".docx"}, "WORD"),
    EXCEL(FileCategoryEnum.DOC, 6, new String[]{".xlsx", ".xls", ".csv"}, "Excel"),
    TXT(FileCategoryEnum.DOC, 7, new String[]{".txt"}, "Txt"),
    PROGRAMME(FileCategoryEnum.OTHERS, 8, new String[]{".h", ".c", ".cpp", ".hxx", ".cc", ".c++", ".m"
    , ".o", ".s", ".dll", ".css", ".html", ".js", ".ts", ".java", ".class", ".json", ".xml", ".sql", ".py"}, "代码"),
    ZIP(FileCategoryEnum.OTHERS, 9, new String[]{".rar", ".zip", ".7z", ".tar", ".gz", ".cab", ".arj"
    , ".lzh", ".ace", ".uue", ".be", ".jar", ".iso", ".mpg"}, "压缩包"),
    OTHERS(FileCategoryEnum.OTHERS, 10, new String[]{}, "其他");

    @Getter
    private FileCategoryEnum category;
    @Getter
    private Integer type;
    @Getter
    private String[] suffix;
    @Getter
    private String desc;

    FileTypeEnum(FileCategoryEnum category, Integer type, String[] suffix, String desc) {
        this.category = category;
        this.type = type;
        this.suffix = suffix;
        this.desc = desc;
    }

    public static FileTypeEnum getFileTypeBySuffix(String suffix) {
        for (FileTypeEnum item : FileTypeEnum.values()) {
            if (ArrayUtils.contains(item.getSuffix(), suffix)) {
                return item;
            }
        }
        return FileTypeEnum.OTHERS;
    }

    public static FileTypeEnum getFileTypeByType(Integer type) {
        for (FileTypeEnum item : FileTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }

}
