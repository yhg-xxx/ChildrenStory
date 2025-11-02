package com.example.controller;

import com.example.common.Result;
import com.example.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 故事图片控制器
 */
@RestController
@RequestMapping("/api/story/image")
public class StoryImageController {
    
    @Autowired
    private StoryService storyService;
    
    /**
     * 为指定故事生成插图并返回图片URL
     * @param storyId 故事ID
     * @return 生成结果，包含图片URL
     */
    @PostMapping("/generate/{storyId}")
    public Result<?> generateImage(@PathVariable String storyId) {
        try {
            // 生成图片并获取URL
            String imageUrl = storyService.generateStoryIllustration(storyId);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 返回包含图片URL的数据
                Map<String, String> data = new HashMap<>();
                data.put("imageUrl", imageUrl);
                return Result.success("插图生成成功", data);
            } else {
                return Result.fail("插图生成失败");
            }
        } catch (Exception e) {
            return Result.fail("服务器错误：" + e.getMessage());
        }
    }

}