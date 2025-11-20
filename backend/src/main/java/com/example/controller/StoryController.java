package com.example.controller;

import com.example.common.Result;
import com.example.entity.Story;
import com.example.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 故事控制器
 */
@RestController
@RequestMapping({"/api/story"})
public class StoryController {
    
    @Autowired
    private StoryService storyService;
    
    /**
     * 流式生成故事
     * @param keywords 关键词
     * @param chatId 对话id
     * @param theme 故事主题（可选）
     * @return 流式生成故事
     */
    @GetMapping(value = "/generate/stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> generateStoryStream(
            @RequestParam String keywords, 
            @RequestParam String chatId,
            @RequestParam(required = false) String theme) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Flux.just("错误：关键词不能为空");
        }
        return storyService.generateStoryStream(keywords, chatId, theme);
    }
    
    /**
     * 根据用户ID和关键词查询故事列表
     * @param userId 用户ID（必填）
     * @param keyword 关键词（可选，用于标题模糊匹配）
     * @return 故事列表
     */
    @GetMapping("/list")
    public Result<List<Story>> getStoriesByUserIdAndKeyword(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword) {
        
        // 参数校验：用户ID为必填参数
        if (userId == null || userId <= 0) {
            return Result.fail("用户ID不能为空且必须大于0");
        }
        
        try {
            List<Story> stories = storyService.getStoriesByUserIdAndKeyword(userId, keyword);
            return Result.success("查询成功", stories);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取故事详情
     * @param storyId 故事ID
     * @return 故事详情
     */
    @GetMapping("/{storyId}")
    public Result<Story> getStoryById(@PathVariable String storyId) {
        try {
            // 参数校验
            if (storyId == null || storyId.trim().isEmpty()) {
                return Result.fail("故事ID不能为空");
            }
            
            // 调用服务层方法获取故事
            Story story = storyService.getStoryById(storyId);
            if (story != null) {
                return Result.success("查询成功", story);
            } else {
                return Result.fail("故事不存在");
            }
        } catch (Exception e) {
            return Result.fail("服务器错误：" + e.getMessage());
        }
    }
    
    /**
     * 为指定故事生成插图并返回图片URL
     * @param storyId 故事ID
     * @param regenerate 是否重新生成图片，默认为false
     * @return 生成结果，包含图片URL
     */
    @PostMapping("/image/{storyId}")
    public Result<?> generateImage(@PathVariable String storyId, @RequestParam(required = false, defaultValue = "false") boolean regenerate) {
        try {
            // 生成图片并获取URL
            String imageUrl = storyService.generateStoryIllustration(storyId);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 返回包含图片URL的数据
                Map<String, String> data = new HashMap<>();
                data.put("imageUrl", imageUrl);
                return Result.success(regenerate ? "插图重新生成成功" : "插图生成成功", data);
            } else {
                return Result.fail(regenerate ? "插图重新生成失败" : "插图生成失败");
            }
        } catch (Exception e) {
            return Result.fail("服务器错误：" + e.getMessage());
        }
    }
    
    /**
     * 根据故事ID删除故事
     * @param storyId 故事ID
     * @return 删除结果
     */
    @DeleteMapping("/{storyId}")
    public Result<?> deleteStory(@PathVariable String storyId) {
        try {
            // 参数校验
            if (storyId == null || storyId.trim().isEmpty()) {
                return Result.fail("故事ID不能为空");
            }
            
            // 调用服务层方法删除故事
            boolean success = storyService.deleteStoryById(storyId);
            if (success) {
                return Result.success("故事删除成功");
            } else {
                return Result.fail("故事不存在或删除失败");
            }
        } catch (Exception e) {
            return Result.fail("服务器错误：" + e.getMessage());
        }
    }
}