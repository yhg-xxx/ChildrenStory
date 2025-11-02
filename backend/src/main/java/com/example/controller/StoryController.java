package com.example.controller;

import com.example.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 故事控制器
 */
@RestController
@RequestMapping("/api/story")
public class StoryController {
    
    @Autowired
    private StoryService storyService;
    
    /**
     * 流式生成故事
     * @param keywords 关键词
     * @param chatId 对话id
     * @return 流式生成故事
     */
    @GetMapping(value = "/generate/stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> generateStoryStream(@RequestParam String keywords, @RequestParam String chatId) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Flux.just("错误：关键词不能为空");
        }
        return storyService.generateStoryStream(keywords, chatId);
    }
}