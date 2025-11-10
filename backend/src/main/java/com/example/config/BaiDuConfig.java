package com.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度文心一格API配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baidu")
public class BaiDuConfig {

    /**
     * 文心一格图片生成API极速版URL
     */
    private String textToImageUrl = "https://aip.baidubce.com/rpc/2.0/wenxin/v1/extreme/textToImage";
    
    /**
     * 图片生成结果查询API极速版URL
     */
    private String getImgUrl = "https://aip.baidubce.com/rpc/2.0/wenxin/v1/extreme/getImg";
    
    /**
     * 访问令牌
     */
    private String accessToken = "24.553d2b5fc6a3d5d8c935e7af48dff3a6.2592000.1764472681.282335-120593977";
    
    /**
     * 默认图片风格
     */
    private String defaultStyle = "卡通画";
    
    /**
     * 默认图片分辨率
     */
    private String defaultResolution = "1024*1024";
    
    /**
     * 默认生成图片数量
     */
    private Integer defaultNum = 1;
    
    /**
     * 查询图片结果的最大等待时间（毫秒）
     */
    private Integer maxWaitTime = 30000;
    
    /**
     * 查询间隔时间（毫秒）
     */
    private Integer queryInterval = 2000;

}