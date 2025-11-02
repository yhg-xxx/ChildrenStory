package com.example.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ID生成器，生成基于日期时间的ID
 */
public class IdGenerator {
    
    // 日期时间格式化器，格式为：yyyyMMddHHmmss
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    /**
     * 生成基于日期时间的ID
     * 格式：年月日时分秒
     * @return 生成的ID字符串
     */
    public static String generateId() {
        // 直接返回当前日期时间的格式化字符串
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
}