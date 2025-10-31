package com.example.service.impl;

import com.example.entity.Story;
import com.example.mapper.StoryMapper;
import com.example.service.StoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    @Override
    public Flux<String> generateStoryStream(String keywords, String chatId) {
        // 创建提示词，指导AI生成儿童故事
        String prompt = String.format("请创作一个适合儿童的短故事，包含以下关键词：%s。\n" +
                "请按照以下格式返回：\n" +
                "【标题】故事的标题\n" +
                "【梗概】故事的简要内容概述（100字以内）\n" +
                "【正文】完整的故事内容（500-1000字）\n" +
                "故事应该积极向上，富有想象力，适合儿童阅读。", keywords);
        
        try {
            // 使用AtomicReference收集完整的故事内容
            AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());
            
            // 使用ChatClient进行流式调用
            return chatClient.prompt()
                    .user(prompt)
                    .advisors(a -> a.param(CONVERSATION_ID, chatId))
                    .stream()
                    .content()
                    // 收集内容并在完成时保存到数据库
                    .doOnNext(chunk -> {
                        fullContent.get().append(chunk);
                    })
                    .doFinally(type -> {
                        // 在流结束后异步保存到数据库
                        String completeStory = fullContent.get().toString();
                        saveStoryToDatabase(completeStory, keywords);
                    });
        } catch (Exception e) {
            // 错误处理，返回简单的故事内容
            String fallbackStory = "【标题】关键词的故事\n" +
                               "【梗概】这是一个关于关键词的有趣故事。\n" +
                               "【正文】很久很久以前，在一个神奇的王国里，关键词们快乐地生活着...";
            
            // 保存错误情况下的故事到数据库
            saveStoryToDatabase(fallbackStory, keywords);
            
            return Flux.just(fallbackStory);
        }
    }
    
    /**
     * 将故事保存到数据库
     */
    private void saveStoryToDatabase(String storyContent, String keywords) {
        // 在独立线程中异步保存，不阻塞流式响应
        Schedulers.boundedElastic().schedule(() -> {
            try {
                // 解析AI返回的结果
                Story story = parseStoryResponse(storyContent, keywords);
                
                // 设置创建时间
                story.setCreateTime(LocalDateTime.now());
                story.setUpdateTime(LocalDateTime.now());
                
                // 保存到数据库
                storyMapper.insert(story);
                
            } catch (Exception e) {
                // 记录保存失败的错误，不影响用户体验
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 解析AI返回的故事内容
     */
    private Story parseStoryResponse(String response, String keywords) {
        Story story = new Story();
        story.setKeywords(keywords);
        
        // 使用正则表达式提取标题、梗概和正文
        // 标题提取
        Pattern titlePattern = Pattern.compile("【标题】(.*?)(?=【梗概】|$)", Pattern.DOTALL);
        Matcher titleMatcher = titlePattern.matcher(response);
        if (titleMatcher.find()) {
            story.setTitle(titleMatcher.group(1).trim());
        } else {
            story.setTitle("AI生成的故事：" + keywords);
        }
        
        // 梗概提取
        Pattern summaryPattern = Pattern.compile("【梗概】(.*?)(?=【正文】|$)", Pattern.DOTALL);
        Matcher summaryMatcher = summaryPattern.matcher(response);
        if (summaryMatcher.find()) {
            story.setSummary(summaryMatcher.group(1).trim());
        } else {
            story.setSummary("一个关于" + keywords + "的故事。");
        }
        
        // 正文提取
        Pattern contentPattern = Pattern.compile("【正文】(.*)", Pattern.DOTALL);
        Matcher contentMatcher = contentPattern.matcher(response);
        if (contentMatcher.find()) {
            story.setContent(contentMatcher.group(1).trim());
        } else {
            // 如果没有按照格式返回，就使用整个响应作为正文
            story.setContent(response.trim());
        }
        
        return story;
    }
}