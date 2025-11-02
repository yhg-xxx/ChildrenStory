package com.example.service;

import com.example.utils.BaiDuTTSUtil;

import java.io.IOException;

/**
 * 语音合成服务接口
 */
public interface TTSService {
    
    /**
     * 将文本转换为语音并保存为MP3文件
     * @param text 需要合成的文本
     * @param per 发音人
     * @param outputFileName 输出文件名
     * @return 保存的文件路径
     * @throws Exception 异常信息
     */
    String textToSpeech(String text, String per, String outputFileName) throws Exception;
    
    /**
     * 使用默认参数将文本转换为语音
     * @param text 需要合成的文本
     * @return 保存的文件路径
     * @throws Exception 异常信息
     */
    String textToSpeech(String text) throws Exception;
    
    /**
     * 流式语音合成，实时返回音频数据
     * @param text 需要合成的文本
     * @param per 发音人
     * @param callback 音频流回调接口
     * @throws Exception 异常信息
     */
    void textToSpeechStream(String text, String per, BaiDuTTSUtil.AudioStreamCallback callback) throws Exception;
}