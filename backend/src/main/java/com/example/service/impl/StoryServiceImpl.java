package com.example.service.impl;

import com.example.entity.Story;
import com.example.mapper.StoryMapper;
import com.example.service.ImageGenerationService;
import com.example.service.StoryService;
import com.example.utils.IdGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 故事服务实现类
 */
@Service
public class StoryServiceImpl implements StoryService {
    
    @Autowired
    private StoryMapper storyMapper;
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private ImageGenerationService imageGenerationService;

    private static final Logger logger = LoggerFactory.getLogger(StoryServiceImpl.class);

    /**
     * 流式生成故事
     * @param keywords 故事关键词
     * @param chatId 会话ID
     * @return 故事内容的流式输出
     */
    @Override
    public Flux<String> generateStoryStream(String keywords, String chatId) {
        // 创建提示词，指导AI生成儿童故事
        String prompt = String.format("""
                请创作一个适合儿童的短故事，包含以下关键词：%s。
                请按照以下格式返回：
                【标题】故事的标题
                【梗概】故事的简要内容概述（100字以内）
                【正文】完整的故事内容（500-1000字）
                故事应该积极向上，富有想象力，适合儿童阅读。""", keywords);
        
        try {
            // 先生成故事ID，确保可以立即返回给前端
            String storyId = IdGenerator.generateId();
            
            // 使用AtomicReference收集完整的故事内容
            AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());
            
            // 使用ChatClient进行流式调用
            return chatClient.prompt()
                    .user(prompt)
                    .advisors(a -> a.param(CONVERSATION_ID, chatId))
                    .stream()
                    .content()
                    // 收集内容并在完成时保存到数据库
                    .doOnNext(chunk -> fullContent.get().append(chunk))
                    .doFinally(type -> {
                        // 在流结束后异步保存到数据库
                        String completeStory = fullContent.get().toString();
                        // 创建storyIdRef并设置值
                        AtomicReference<String> storyIdRef = new AtomicReference<>(storyId);
                        saveStoryToDatabase(completeStory, keywords, storyIdRef);
                    })
                    // 在流开始时添加故事ID
                    .startWith("【故事ID】" + storyId + "\n");
        } catch (Exception e) {
            // 错误处理，返回简单的故事内容
            String fallbackStory = """
                    【标题】关键词的故事
                    【梗概】这是一个关于关键词的有趣故事。
                    【正文】很久很久以前，在一个神奇的王国里，关键词们快乐地生活着...""";
            
            // 生成错误情况下的故事ID
            String errorStoryId = IdGenerator.generateId();
            AtomicReference<String> storyIdRef = new AtomicReference<>(errorStoryId);
            
            // 保存错误情况下的故事到数据库
            saveStoryToDatabase(fallbackStory, keywords, storyIdRef);
            
            return Flux.just("【故事ID】" + errorStoryId + "\n" + fallbackStory);
        }
    }
    
    /**
     * 将故事保存到数据库
     */
    private void saveStoryToDatabase(String storyContent, String keywords, AtomicReference<String> storyIdRef) {
        // 在独立线程中异步保存，不阻塞流式响应
        Schedulers.boundedElastic().schedule(() -> {
            try {
                // 解析AI返回的结果
                Story story = parseStoryResponse(storyContent, keywords);
                LocalDateTime now = LocalDateTime.now();
                
                // 使用已生成的ID，更新故事对象
                story = Story.builder()
                        .id(storyIdRef.get())
                        .userId(1L) // 暂时使用默认用户ID，实际应该从会话或参数中获取
                        .keywords(story.getKeywords())
                        .title(story.getTitle())
                        .summary(story.getSummary())
                        .content(story.getContent())
                        .createTime(now)
                        .updateTime(now)
                        .build();
                
                // 保存到数据库
                storyMapper.insert(story);
                
                logger.info("故事已保存到数据库，ID: {}", storyIdRef.get());
                
            } catch (Exception e) {
                // 记录保存失败的错误，不影响用户体验
                logger.error("保存故事到数据库失败: {}", e.getMessage(), e);
            }
        });
    }
    

    
    /**
     * 解析AI返回的故事内容
     */
    private Story parseStoryResponse(String response, String keywords) {
        // 标题提取
        Pattern titlePattern = Pattern.compile("【标题】(.*?)(?=【梗概】|$)", Pattern.DOTALL);
        Matcher titleMatcher = titlePattern.matcher(response);
        String title = titleMatcher.find() ? titleMatcher.group(1).trim() : "AI生成的故事：" + keywords;
        
        // 梗概提取
        Pattern summaryPattern = Pattern.compile("【梗概】(.*?)(?=【正文】|$)", Pattern.DOTALL);
        Matcher summaryMatcher = summaryPattern.matcher(response);
        String summary = summaryMatcher.find() ? summaryMatcher.group(1).trim() : "一个关于" + keywords + "的故事。";
        
        // 正文提取
        Pattern contentPattern = Pattern.compile("【正文】(.*)", Pattern.DOTALL);
        Matcher contentMatcher = contentPattern.matcher(response);
        String content = contentMatcher.find() ? contentMatcher.group(1).trim() : response.trim();
        
        // 使用Builder模式创建Story对象
        return Story.builder()
                .keywords(keywords)
                .title(title)
                .summary(summary)
                .content(content)
                .build();
    }
    /**
     * 为指定故事生成插图并返回图片URL
     * @param storyId 故事ID
     * @return 生成的图片URL，失败返回null
     */
    @Override
    public String generateStoryIllustration(String storyId) {
        try {
            // 查询故事信息
            Story story = getStoryById(storyId);
            if (story == null) {
                logger.error("故事ID: {} 不存在", storyId);
                return null;
            }

            // 使用故事梗概生成插图
            String storySummary = story.getSummary();
            if (storySummary == null || storySummary.isEmpty()) {
                storySummary = story.getTitle();
            }

            // 调用图片生成服务
            Map<String, String> result = imageGenerationService.generateStoryIllustration(storySummary);
            String imageUrl = result.get("imageUrl");

            // 更新故事的图片信息，使用Builder模式
            story = Story.builder()
                    .id(story.getId())
                    .keywords(story.getKeywords())
                    .title(story.getTitle())
                    .summary(story.getSummary())
                    .content(story.getContent())
                    .userId(story.getUserId())
                    .imageUrl(imageUrl)
                    .imagePrompt(result.get("imagePrompt"))
                    .audioUrl(story.getAudioUrl())
                    .createTime(story.getCreateTime())
                    .updateTime(LocalDateTime.now())
                    .build();

            // 保存更新
            storyMapper.updateById(story);

            logger.info("成功为故事ID: {} 生成插图: {}", storyId, imageUrl);
            return imageUrl;
        } catch (Exception e) {
            logger.error("为故事ID: {} 生成插图失败: {}", storyId, e.getMessage(), e);
            return null;
        }
    }

        @Override
    public Story getStoryById(String storyId) {
        if (storyId == null || storyId.trim().isEmpty()) {
            return null;
        }
        try {
            return storyMapper.selectById(storyId);
        } catch (Exception e) {
            logger.error("查询故事ID: {} 失败: {}", storyId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 根据用户ID和关键词查询故事
     * @param userId 用户ID（必填）
     * @param keyword 关键词（可选，用于标题模糊匹配）
     * @return 符合条件的故事列表
     */
    @Override
    public List<Story> getStoriesByUserIdAndKeyword(Long userId, String keyword) {
        try {
            QueryWrapper<Story> queryWrapper = new QueryWrapper<>();
            
            // 必传参数：用户ID必须匹配
            if (userId == null || userId <= 0) {
                logger.warn("查询故事时用户ID无效: {}", userId);
                return List.of(); // 返回空列表
            }
            queryWrapper.eq("user_id", userId);
            
            // 可选参数：如果有关键词，则进行标题模糊匹配
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.like("title", keyword.trim());
            }
            
            // 按创建时间倒序排列，最新的在前面
            queryWrapper.orderByDesc("create_time");
            
            List<Story> stories = storyMapper.selectList(queryWrapper);
            logger.info("查询到 {} 个符合条件的故事", stories.size());
            return stories;
        } catch (Exception e) {
            logger.error("查询故事失败: 用户ID={}, 关键词={}, 错误信息: {}", 
                    userId, keyword, e.getMessage(), e);
            return List.of(); // 异常情况下返回空列表
        }
    }
    
    @Override
    public boolean deleteStoryById(String storyId) {
        try {
            // 使用MyBatis Plus的deleteById方法删除故事
            int result = storyMapper.deleteById(storyId);
            // 如果影响行数大于0，表示删除成功
            return result > 0;
        } catch (Exception e) {
            logger.error("删除故事失败，故事ID: {}", storyId, e);
            return false;
        }
    }
}