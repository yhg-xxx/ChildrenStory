package com.example.service.impl;

import com.example.service.TTSService;
import com.example.utils.BaiDuTTSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 语音合成服务实现类
 */
@Service
public class TTSServiceImpl implements TTSService {

    private static final Logger logger = LoggerFactory.getLogger(TTSServiceImpl.class);

    @Override
    public String textToSpeech(String text, String per, String outputFileName) throws Exception {
        logger.info("开始语音合成，文本长度: {}, 发音人: {}", text.length(), per);
        try {
            String filePath = BaiDuTTSUtil.textToSpeech(text, per, outputFileName);
            logger.info("语音合成成功，文件已保存到: {}", filePath);
            return filePath;
        } catch (Exception e) {
            logger.error("语音合成失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String textToSpeech(String text) throws Exception {
        logger.info("开始语音合成（默认参数），文本长度: {}", text.length());
        try {
            String filePath = textToSpeech(text, "0", null);
            logger.info("语音合成（默认参数）成功");
            return filePath;
        } catch (Exception e) {
            logger.error("语音合成（默认参数）失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void textToSpeechStream(String text, String per, BaiDuTTSUtil.AudioStreamCallback callback) throws Exception {
        logger.info("开始流式语音合成，文本长度: {}, 发音人: {}", text.length(), per);
        try {
            BaiDuTTSUtil.textToSpeechStream(text, per, callback);
            logger.info("流式语音合成完成");
        } catch (Exception e) {
            logger.error("流式语音合成失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}