package com.example.service;

import com.example.entity.Story;
import reactor.core.publisher.Flux;

/**
 * 故事服务接口
 */
public interface StoryService {
    
    /**
     * 流式生成故事
     * @param keywords 故事关键词
     * @param chatId 会话ID
     * @return 故事内容的流式输出
     */
    Flux<String> generateStoryStream(String keywords, String chatId);
    
    /**
     * 为指定故事生成插图并返回图片URL
     * @param storyId 故事ID
     * @return 生成的图片URL，失败返回null
     */
    String generateStoryIllustration(String storyId);
    
    /**
     * 根据ID获取故事
     * @param storyId 故事ID
     * @return 故事对象，不存在返回null
     */
    Story getStoryById(String storyId);
    

}