package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 * 用于处理前后端分离架构中的跨域请求问题
 */
@Configuration
public class CorsConfig {

    /**
     * 配置CORS过滤器
     * @return CorsFilter 跨域过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建CORS配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // 允许的源：设置为*表示允许所有来源的请求
        // 在生产环境中，应该设置为具体的前端域名以提高安全性
        corsConfiguration.addAllowedOriginPattern("*");
        
        // 允许的请求头
        corsConfiguration.addAllowedHeader("*");
        
        // 允许的请求方法
        corsConfiguration.addAllowedMethod("*");
        
        // 允许携带凭证（cookies等）
        corsConfiguration.setAllowCredentials(true);
        
        // 预检请求的有效期，单位为秒
        corsConfiguration.setMaxAge(3600L);
        
        // 创建URL映射配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        // 返回配置好的CORS过滤器
        return new CorsFilter(source);
    }
}