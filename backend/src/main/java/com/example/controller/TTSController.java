package com.example.controller;

import cn.hutool.core.util.URLUtil;
import com.example.entity.Story;
import com.example.mapper.StoryMapper;
import com.example.service.StoryService;
import com.example.service.TTSService;
import com.example.utils.BaiDuTTSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;


/**
 * 语音合成控制器
 */
@RestController
@RequestMapping("/api/tts")
public class TTSController {

    private static final Logger logger = LoggerFactory.getLogger(TTSController.class);
    
    @Autowired
    private TTSService ttsService;
    
    @Autowired
    private StoryService storyService;
    
    @Autowired
    private StoryMapper storyMapper;

    
    /**
     * 故事语音合成接口（非流式）
     * @param storyId 故事ID
     * @return 流式语音数据
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<Resource> synthesizeStory(@PathVariable String storyId) {
        try {
            // 查询故事信息
            Story story = storyService.getStoryById(storyId);
            if (story == null) {
                logger.error("故事不存在，ID: {}", storyId);
                throw new IllegalArgumentException("故事不存在");
            }
            
            // 使用故事内容进行语音合成
            String storyContent = story.getTitle() + "\n" + story.getContent();
            logger.info("开始为故事ID: {} 生成语音，标题: {}", storyId, story.getTitle());
            
            // 生成文件名（使用故事ID作为文件名一部分）
            String fileName = "story_" + storyId + ".mp3";
            
            // 调用TTS服务生成语音
            String filePath = ttsService.textToSpeech(storyContent, "0", fileName);
            logger.info("故事语音合成完成，保存路径: {}", filePath);
            
            // 将生成的音频文件移动到uploads目录，以便通过FileUploadController访问
            String uploadsPath = System.getProperty("user.dir") + "/uploads/";
            File sourceFile = new File(filePath);
            File targetFile = new File(uploadsPath + fileName);
            
            // 移动文件
            if (sourceFile.renameTo(targetFile)) {
                logger.info("音频文件已移动到uploads目录: {}", targetFile.getAbsolutePath());
                
                // 更新数据库中的audioUrl字段
                String encodedFileName = URLUtil.encode(fileName);
                String audioUrl = "http://localhost:8080/files/download/" + encodedFileName;
                story.setAudioUrl(audioUrl);
                story.setUpdateTime(LocalDateTime.now());
                
                // 保存更新后的故事信息
                storyMapper.updateById(story);
                logger.info("故事音频URL已保存到数据库: {}", audioUrl);
            } else {
                logger.warn("无法移动音频文件到uploads目录，将使用原始路径");
            }
            
            // 创建文件资源
            File audioFile = new File(targetFile.exists() ? targetFile.getAbsolutePath() : filePath);
            Resource resource = new FileSystemResource(audioFile);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(audioFile.length()));
            
            // 返回流式响应
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            logger.error("参数错误: {}", e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            logger.error("故事语音合成失败", e);
            throw new RuntimeException("故事语音合成失败: " + e.getMessage());
        }
    }
    
    /**
     * 流式故事语音合成接口
     * @param storyId 故事ID
     * @param response 响应对象
     */
    @GetMapping("/story-stream/{storyId}")
    public void synthesizeStoryStream(@PathVariable String storyId, jakarta.servlet.http.HttpServletResponse response) {
        // 声明资源变量，用于在finally块中关闭
        OutputStream outputStream = null;
        java.io.FileOutputStream fileOutputStream = null;
        
        try {
            // 查询故事信息
            Story story = storyService.getStoryById(storyId);
            if (story == null) {
                logger.error("故事不存在，ID: {}", storyId);
                response.setStatus(404);
                response.getWriter().write("故事不存在");
                return;
            }
            
            // 使用故事内容进行语音合成
            String storyContent = story.getTitle() + "\n" + story.getContent();
            logger.info("开始为故事ID: {} 进行流式语音合成，标题: {}", storyId, story.getTitle());
            
            // 生成文件名（使用故事ID作为文件名一部分）
            String fileName = "story_" + storyId + ".mp3";
            
            // 设置响应头
            response.setContentType("audio/mpeg");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("Transfer-Encoding", "chunked");
            
            // 获取输出流
            outputStream = response.getOutputStream();
            
            // 使用uploads目录保存音频文件
            String uploadsPath = System.getProperty("user.dir") + "/uploads/";
            
            // 同时保存音频文件的输出流
            File targetFile = new File(uploadsPath + fileName);
            fileOutputStream = new java.io.FileOutputStream(targetFile);
            
            // 创建一个标志来跟踪输出流是否已关闭
            final boolean[] isStreamClosed = {false};
            
            // 调用流式语音合成服务
            OutputStream finalOutputStream = outputStream;
            java.io.FileOutputStream finalFileOutputStream = fileOutputStream;
            ttsService.textToSpeechStream(storyContent, "0", new BaiDuTTSUtil.AudioStreamCallback() {
                @Override
                public void onAudioData(byte[] audioData) throws IOException {
                    try {
                        // 检查输出流是否已关闭
                        if (isStreamClosed[0]) {
                            logger.warn("输出流已关闭，跳过数据写入");
                            return;
                        }
                        
                        // 向客户端实时输出音频数据
                        try {
                            finalOutputStream.write(audioData);
                            finalOutputStream.flush();
                            logger.debug("已流式输出音频数据: {} 字节", audioData.length);
                        } catch (IOException e) {
                            logger.warn("向客户端输出数据失败，可能客户端已断开连接: {}", e.getMessage());
                            isStreamClosed[0] = true;
                            // 仍然尝试保存到文件，即使客户端断开连接
                        }
                        
                        // 同时保存到文件（即使客户端断开连接也继续保存）
                        try {
                            finalFileOutputStream.write(audioData);
                            finalFileOutputStream.flush();
                        } catch (IOException e) {
                            logger.error("保存音频数据到文件失败", e);
                            throw e;
                        }
                    } catch (IOException e) {
                        logger.error("处理音频数据失败", e);
                        // 标记流已关闭
                        isStreamClosed[0] = true;
                        // 客户端断开连接时，结束处理
                        closeResources();
                        throw e;
                    }
                }
                
                private void closeResources() {
                    try {
                        finalFileOutputStream.close();
                    } catch (IOException ex) {
                        // 忽略关闭文件流的错误
                    }
                    try {
                        if (finalOutputStream != null && !isStreamClosed[0]) {
                            isStreamClosed[0] = true;
                            finalOutputStream.close();
                        }
                    } catch (IOException ex) {
                        // 忽略关闭输出流的错误
                    }
                }
                
                @Override
                public void onComplete() {
                    logger.info("故事ID: {} 的流式语音合成完成", storyId);
                    try {
                        // 确保文件保存完成
                        finalFileOutputStream.flush();
                        finalFileOutputStream.close();

                        // 更新数据库中的audioUrl字段
                        String encodedFileName = URLUtil.encode(fileName);
                        String audioUrl = "http://localhost:8080/files/download/" + encodedFileName;
                        story.setAudioUrl(audioUrl);
                        story.setUpdateTime(LocalDateTime.now());
                        
                        // 保存更新后的故事信息
                        storyMapper.updateById(story);
                        logger.info("故事音频URL已保存到数据库: {}", audioUrl);
                    } catch (Exception e) {
                        logger.error("完成处理时发生错误", e);
                    } finally {
                        // 关闭所有资源
                        closeResources();
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    logger.error("流式语音合成过程中发生错误", e);
                    try {
                        // 尝试设置错误状态，但不尝试向已关闭的流写入数据
                        if (!isStreamClosed[0]) {
                            try {
                                response.setStatus(500);
                            } catch (Exception ex) {
                                // 忽略设置状态的错误
                            }
                        }
                    } finally {
                        // 关闭所有资源
                        closeResources();
                    }
                }
            });
        } catch (Exception e) {
            logger.error("流式故事语音合成失败", e);
            try {
                response.setStatus(500);
                response.getWriter().write("流式语音合成失败: " + e.getMessage());
            } catch (IOException ex) {
                // 忽略错误
            } finally {
                // 关闭资源
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException ex) {
                    // 忽略错误
                }
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException ex) {
                    // 忽略错误
                }
            }
        }
    }
}