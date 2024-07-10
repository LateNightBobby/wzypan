package com.wzypan.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {
    /**
     * 生成随机数
     * @param count
     * @return
     */
    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static boolean isEmpty(String s) {
        if (s == null || "".equals(s) || "null".equals(s) || "\u0000".equals(s)) {
            return true;
        } else if ("".equals(s.trim())) {
            return true;
        } return false;
    }

    public static String encodeByMd5(String s) {
        return isEmpty(s) ? null : DigestUtils.md5Hex(s);
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path))
            return true;
        if (path.contains("../") || path.contains("..\\"))
            return false;
        return true;
    }
}
