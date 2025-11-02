package com.example.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 百度AI流式语音合成工具类
 */
public class BaiDuTTSUtil {

    private static final Logger logger = LoggerFactory.getLogger(BaiDuTTSUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 音频流回调接口，用于流式传输音频数据
     */
    public interface AudioStreamCallback {
        void onAudioData(byte[] audioData) throws IOException;
        void onComplete();
        void onError(Exception e);
    }
    
    // WebSocket连接URL模板
    private static final String WSS_URL = "wss://aip.baidubce.com/ws/2.0/speech/publiccloudspeech/v1/tts?access_token=%s&per=%s";
    
    // 百度AI access_token
    private static final String DEFAULT_ACCESS_TOKEN = "24.7772565614474de6ec94d2e61eabdad8.2592000.1764644537.282335-120616223";
    
    /**
     * 流式语音合成并保存为MP3文件
     * @param text 需要合成的文本
     * @param per 发音人
     * @param outputFileName 输出的MP3文件名（不包含路径）
     * @return 保存的文件路径
     * @throws Exception 异常信息
     */
    public static String textToSpeech(String text, String per, String outputFileName) throws Exception {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("文本内容不能为空");
        }

        if (StrUtil.isBlank(per)) {
            per = "0"; // 默认发音人
        }
        
        if (StrUtil.isBlank(outputFileName)) {
            outputFileName = "tts_output.mp3"; 
        }
        
        if (!outputFileName.endsWith(".mp3")) {
            outputFileName += ".mp3"; 
        }
        
        // 计算文本长度
        int textLength = text.length();
        logger.info("开始合成文本，总长度: {}字", textLength);
        
        // 准备音频数据输出流
        ByteArrayOutputStream audioOutputStream = new ByteArrayOutputStream();
        
        // 使用流式方法处理并收集所有音频数据
        textToSpeechStream(text, per, new AudioStreamCallback() {
            @Override
            public void onAudioData(byte[] audioData) throws IOException {
                audioOutputStream.write(audioData);
            }
            
            @Override
            public void onComplete() {
                logger.info("所有音频数据接收完成");
            }
            
            @Override
            public void onError(Exception e) {
                logger.error("流式合成过程中发生错误", e);
            }
        });
        
        logger.info("开始保存文件");
        
        // 保存音频文件到项目根目录
        String outputPath = System.getProperty("user.dir") + File.separator + outputFileName;
        byte[] audioBytes = audioOutputStream.toByteArray();
        
        if (audioBytes.length == 0) {
            throw new RuntimeException("未接收到音频数据");
        }
        
        FileUtil.writeBytes(audioBytes, outputPath);
        logger.info("语音文件已保存到: {}", outputPath);
        
        return outputPath;
    }
    
    /**
     * 流式语音合成，实时返回音频数据
     * @param text 需要合成的文本
     * @param per 发音人
     * @param callback 音频流回调接口，用于接收实时音频数据
     * @throws Exception 异常信息
     */
    public static void textToSpeechStream(String text, String per, AudioStreamCallback callback) throws Exception {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("文本内容不能为空");
        }
        
        if (StrUtil.isBlank(per)) {
            per = "0"; // 默认发音人
        }
        
        if (callback == null) {
            throw new IllegalArgumentException("回调接口不能为空");
        }
        
        // 计算文本长度
        int textLength = text.length();
        logger.info("开始流式合成文本，总长度: {}字", textLength);
        
        // 分段处理文本
        final int MAX_SEGMENT_LENGTH = 1000; // 每段最大长度
        int segmentCount = (textLength + MAX_SEGMENT_LENGTH - 1) / MAX_SEGMENT_LENGTH; // 计算需要分成多少段
        
        if (segmentCount > 1) {
            logger.info("文本过长，将分成{}段处理", segmentCount);
        }
        
        try {
            for (int i = 0; i < segmentCount; i++) {
                // 计算当前段的起始和结束位置
                int startIndex = i * MAX_SEGMENT_LENGTH;
                int endIndex = Math.min((i + 1) * MAX_SEGMENT_LENGTH, textLength);
                String segmentText = text.substring(startIndex, endIndex);
                
                logger.info("处理第{}段文本，长度: {}字", i + 1, segmentText.length());
                
                // 为每段创建新的WebSocket连接并流式处理
                processTextSegmentStream(segmentText, per, callback);
                logger.info("第{}段处理完成", i + 1);
            }
            
            // 所有段落处理完成
            callback.onComplete();
        } catch (Exception e) {
            callback.onError(e);
            throw e;
        }
    }
    
    /**
     * 处理单段文本，建立WebSocket连接并获取音频数据
     * @param segmentText 段落文本
     * @param per 发音人
     * @return 段落音频数据输出流
     * @throws Exception 异常信息
     */
    private static ByteArrayOutputStream processTextSegment(String segmentText, String per) throws Exception {
        ByteArrayOutputStream audioOutputStream = new ByteArrayOutputStream();
        
        // 使用流式方法收集音频数据
        processTextSegmentStream(segmentText, per, new AudioStreamCallback() {
            @Override
            public void onAudioData(byte[] audioData) throws IOException {
                audioOutputStream.write(audioData);
            }
            
            @Override
            public void onComplete() {
                // 不需要操作，因为我们只是收集数据
            }
            
            @Override
            public void onError(Exception e) {
                logger.error("处理文本段时发生错误", e);
            }
        });
        
        return audioOutputStream;
    }
    
    /**
     * 流式处理单段文本，建立WebSocket连接并实时返回音频数据
     * @param segmentText 段落文本
     * @param per 发音人
     * @param callback 音频流回调接口
     * @throws Exception 异常信息
     */
    private static void processTextSegmentStream(String segmentText, String per, AudioStreamCallback callback) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean synthesisSuccess = new AtomicBoolean(false);
        
        // 构建WebSocket连接URL
        String wsUrl = String.format(WSS_URL, DEFAULT_ACCESS_TOKEN, per);
        URI serverUri = new URI(wsUrl);
        
        // 创建WebSocket客户端
        WebSocketClient webSocketClient = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                logger.info("WebSocket连接已建立");
                try {
                    // 发送初始化消息
                    String initMessage = "{\"type\": \"system.start\", \"payload\": {\"spd\": 5, \"vol\": 5, \"aue\": 3}}";
                    this.send(initMessage);
                    logger.info("已发送初始化消息");
                } catch (Exception e) {
                    logger.error("发送初始化消息失败", e);
                    latch.countDown();
                }
            }
            
            @Override
            public void onMessage(String message) {
                logger.info("接收到文本消息: {}", message);
                try {
                    // 解析消息
                    if (message.contains("\"type\":\"system.started\"")) {
                        // 初始化成功，发送文本
                        // 对文本进行全面转义，特别是换行符和引号
                        String escapedText = segmentText.replace("\\", "\\\\")
                                               .replace("\"", "\\\"")
                                               .replace("\n", "\\n")
                                               .replace("\r", "\\r");
                        String textMessage = "{\"type\": \"text\", \"payload\": {\"text\": \"" + escapedText + "\"}}";
                        this.send(textMessage);
                        logger.info("已发送文本消息");
                        // 发送结束消息
                        String finishMessage = "{\"type\":\"system.finish\"}";
                        this.send(finishMessage);
                        logger.info("已发送结束消息");
                    } else if (message.contains("\"type\":\"system.finished\"")) {
                        // 合成完成
                        synthesisSuccess.set(true);
                        latch.countDown();
                    } else if (message.contains("\"type\":\"system.error\"")) {
                        // 发生错误
                        logger.error("合成过程中发生错误: {}", message);
                        latch.countDown();
                    }
                } catch (Exception e) {
                    logger.error("处理文本消息失败", e);
                    latch.countDown();
                }
            }
            
            @Override
            public void onMessage(ByteBuffer bytes) {
                try {
                    // 接收音频二进制数据
                    byte[] audioData = new byte[bytes.remaining()];
                    bytes.get(audioData);
                    // 通过回调实时返回音频数据
                    callback.onAudioData(audioData);
                    logger.debug("接收到音频数据，字节数: {}", audioData.length);
                } catch (Exception e) {
                    logger.error("处理音频数据失败", e);
                    callback.onError(e);
                }
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.info("WebSocket连接已关闭，代码: {}, 原因: {}", code, reason);
                if (latch.getCount() > 0) {
                    latch.countDown();
                }
            }
            
            @Override
            public void onError(Exception ex) {
                logger.error("WebSocket连接发生错误", ex);
                if (latch.getCount() > 0) {
                    latch.countDown();
                }
            }
        };
        
        // 连接WebSocket并等待连接成功
        // 注意：connectBlocking已经包含了connect方法的调用，不需要单独调用connect
        boolean connected = webSocketClient.connectBlocking(5, TimeUnit.SECONDS);
        if (!connected) {
            throw new RuntimeException("WebSocket连接失败");
        }
        
        // 等待合成完成或超时 - 增加超时时间以适应长文本处理
        // 1000字的文本大约需要300秒的处理时间
        boolean awaitResult = latch.await(300, TimeUnit.SECONDS);
        if (!awaitResult) {
            logger.warn("合成超时，可能需要更长的处理时间");
        }
        
        // 关闭连接
        if (webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        
        // 检查合成是否成功
        if (!synthesisSuccess.get()) {
            throw new RuntimeException("语音合成失败: 请检查文本内容是否包含特殊字符或API是否正常");
        }
    }
    
    /**
     * 简化版的语音合成方法，使用默认参数
     * @param text 需要合成的文本
     * @return 保存的文件路径
     * @throws Exception 异常信息
     */
    public static String textToSpeech(String text) throws Exception {
        return textToSpeech(text, "0", null);
    }
}