package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故事实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("story")
public class Story {
    
    @TableId(type = IdType.INPUT)
    private String id;
    
    private Long userId; // 用户ID外键，关联user表
    
    private String title;
    
    private String keywords;
    
    private String summary;
    
    private String content;
    
    private String imageUrl; // 故事插图URL
    
    private String imagePrompt; // 生成图片使用的提示词
    
    private String audioUrl; // 故事语音文件URL
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private User user; // 非数据库字段，用于关联查询时存储用户信息
}