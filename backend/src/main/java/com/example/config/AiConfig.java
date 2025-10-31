package com.example.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        //聊天记忆库实现，来替换为Redis
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        //注册 聊天上下文机制
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20) //聊天记忆条数
                .build();
    }

    //角色预设 - 儿童故事生成助手
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,ChatMemory chatMemory) {
        return chatClientBuilder
                .defaultSystem("你是一个专业的儿童故事生成专家。请根据用户提供的关键词，创作一个温馨、有趣、富有教育意义的儿童故事。故事应该简短易懂，语言生动活泼，适合5-10岁的儿童阅读。故事中应包含积极的价值观，如友谊、勇气、好奇心等。")
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        // 添加会话记忆
                        MessageChatMemoryAdvisor.builder(chatMemory).build()) //配置日志Advisor
                .build();
    }
}
