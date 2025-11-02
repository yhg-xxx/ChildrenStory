package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故事实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("story")
public class Story {
    
    @TableId(type = IdType.INPUT)
    private String id;
    
    private String title;
    
    private String keywords;
    
    private String summary;
    
    private String content;
    
    private String imageUrl; // 故事插图URL
    
    private String imagePrompt; // 生成图片使用的提示词
    
    private String audioUrl; // 故事语音文件URL
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;


}