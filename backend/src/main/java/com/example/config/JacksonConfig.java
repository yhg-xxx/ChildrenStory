package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson配置类
 * 用于自定义JSON序列化和反序列化配置
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义的日期时间格式化器
     * 将LocalDateTime格式化为不带T的格式，如：2025-11-20 15:27:02
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 配置Jackson的ObjectMapper
     * 配置LocalDateTime的序列化和反序列化格式
     * @return 配置好的ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        // 创建一个新的ObjectMapper实例
        ObjectMapper mapper = new ObjectMapper();
        
        // 创建JavaTimeModule
        JavaTimeModule timeModule = new JavaTimeModule();
        
        // 添加LocalDateTime序列化器和反序列化器，使用自定义的格式化器
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        
        // 注册模块到mapper
        mapper.registerModule(timeModule);
        
        // 禁用将日期序列化为时间戳
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        return mapper;
    }
    
    /**
     * 配置Spring MVC使用的Jackson2ObjectMapperBuilder
     * 确保所有的JSON序列化都使用相同的日期时间格式
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .serializers(new LocalDateTimeSerializer(DATE_TIME_FORMATTER))
                .deserializers(new LocalDateTimeDeserializer(DATE_TIME_FORMATTER))
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}