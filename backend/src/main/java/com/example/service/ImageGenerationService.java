package com.example.service;

import com.example.config.BaiDuConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图片生成服务类
 */
@Service
public class ImageGenerationService {
    
    @Autowired
    private BaiDuConfig baiDuConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 生成图片
     * @param prompt 提示词
     * @return 生成的图片URL
     * @throws Exception 生成图片时的异常
     */
    public String generateImage(String prompt) throws Exception {
        // 构建图片生成请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", prompt);
        requestBody.put("style", baiDuConfig.getDefaultStyle());
        requestBody.put("resolution", baiDuConfig.getDefaultResolution());
        requestBody.put("num", baiDuConfig.getDefaultNum());
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 构建完整的请求URL
        String textToImageUrl = baiDuConfig.getTextToImageUrl() + "?access_token=" + baiDuConfig.getAccessToken();
        
        // 发送请求
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        String response = restTemplate.postForObject(textToImageUrl, requestEntity, String.class);
        
        // 解析响应获取taskId
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode dataNode = rootNode.path("data");
        String taskId = dataNode.path("primaryTaskId").asText();
        
        if (taskId == null || taskId.isEmpty()) {
            throw new Exception("获取图片生成任务ID失败: " + response);
        }
        
        // 查询图片生成结果
        return queryImageResult(taskId);
    }
    
    /**
     * 查询图片生成结果
     * @param taskId 任务ID
     * @return 生成的图片URL
     * @throws Exception 查询结果时的异常
     */
    private String queryImageResult(String taskId) throws Exception {
        long startTime = System.currentTimeMillis();
        long maxWaitTime = baiDuConfig.getMaxWaitTime();
        long queryInterval = baiDuConfig.getQueryInterval();
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 构建请求URL
        String getImgUrl = baiDuConfig.getGetImgUrl() + "?access_token=" + baiDuConfig.getAccessToken();
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("taskId", taskId);
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(getImgUrl, requestEntity, String.class);
            
            // 解析响应
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode dataNode = rootNode.path("data");
            
            // 检查状态
            int status = dataNode.path("status").asInt(0);
            if (status == 1) {
                // 任务已完成，获取图片URL
                String imageUrl = dataNode.path("img").asText();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // 清理URL中的空格和引号
                    imageUrl = imageUrl.trim().replaceAll("^\"|\"$|^`|`$", "");
                    return imageUrl;
                }
                
                // 尝试从imgUrls数组获取
                JsonNode imgUrlsNode = dataNode.path("imgUrls");
                if (imgUrlsNode.isArray() && !imgUrlsNode.isEmpty()) {
                    imageUrl = imgUrlsNode.get(0).path("image").asText();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // 清理URL中的空格和引号
                        imageUrl = imageUrl.trim().replaceAll("^\"|\"$|^`|`$", "");
                        return imageUrl;
                    }
                }
                
                throw new Exception("获取图片URL失败，响应中未包含有效的图片链接");
            }
            
            // 等待一段时间后再次查询
            TimeUnit.MILLISECONDS.sleep(queryInterval);
        }
        
        throw new Exception("图片生成超时，请稍后重试");
    }
    
    /**
     * 为故事生成插图
     * @param storySummary 故事梗概
     * @return 生成的图片URL和使用的提示词
     * @throws Exception 生成图片时的异常
     */
    public Map<String, String> generateStoryIllustration(String storySummary) throws Exception {
        // 构建适合儿童故事的图片提示词
        String prompt = String.format("儿童插画，%s，色彩明亮，风格可爱，适合儿童阅读，高质量，高清", storySummary);
        
        // 限制提示词长度
        if (prompt.length() > 190) {
            prompt = prompt.substring(0, 187) + "...";
        }
        
        // 生成图片
        String imageUrl = generateImage(prompt);
        
        // 返回结果
        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", imageUrl);
        result.put("imagePrompt", prompt);
        
        return result;
    }
}