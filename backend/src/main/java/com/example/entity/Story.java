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
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String keywords;
    
    private String summary;
    
    private String content;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;


}