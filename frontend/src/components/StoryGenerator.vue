<template>
  <div class="story-generator-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>AI故事生成器</span>
        </div>
      </template>
      
      <el-form :model="formData" label-width="80px">
        <el-form-item label="关键词">
          <el-input
            v-model="formData.keywords"
            placeholder="请输入故事关键词，如：宇航员、小狗、月球"
            clearable
          ></el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="generateStory"
            :loading="isGenerating"
            :disabled="isGenerating || !formData.keywords.trim()"
          >
            生成故事
          </el-button>
          <el-button 
            @click="cancelGeneration" 
            v-if="isGenerating"
          >
            取消生成
          </el-button>
        </el-form-item>
      </el-form>
      
      <div v-if="storyContent" class="story-content">
        <!-- 结构化展示故事内容 -->
        <div class="story-section">
          <h2 class="story-title">{{ extractTitle(storyContent) }}</h2>
          
          <!-- 故事插图 -->
          <div v-if="storyImage" class="story-image-container">
            <img :src="storyImage" alt="故事插图" class="story-image" />
          </div>
          
          <div class="story-outline">
            <h3 class="section-label">梗概</h3>
            <p class="outline-content">{{ extractOutline(storyContent) }}</p>
          </div>
          
          <div class="story-body">
            <h3 class="section-label">正文</h3>
            <div class="body-content">
              <p v-for="(paragraph, index) in extractContent(storyContent).split('\n\n')" :key="index">
                {{ paragraph }}
              </p>
            </div>
          </div>
          
          <!-- 获取图片和语音按钮 -->
          <div class="image-action-container">
            <el-button 
              type="primary" 
              @click="fetchStoryImage"
              :loading="isLoadingImage"
              v-if="!isGenerating && storyContent && !storyImage"
            >
              获取故事插图
            </el-button>
            <el-button 
              type="primary" 
              @click="generateAndPlayAudio"
              :loading="isLoadingAudio"
              v-if="!isGenerating && storyContent"
              style="margin-left: 10px;"
            >
              {{ audioUrl ? '播放语音' : '生成语音' }}
            </el-button>
            <!-- 音频播放器 -->
            <audio 
              ref="audioElement" 
              controls 
              v-if="audioUrl"
              style="margin-top: 15px; width: 100%;"
            >
              <source :src="audioUrl" type="audio/mpeg">
              您的浏览器不支持音频播放
            </audio>
          </div>
        </div>
      </div>
      
      <div v-if="errorMessage" class="error-message">
        <el-alert
          :title="errorMessage"
          type="error"
          show-icon
          :closable="true"
          @close="clearError"
        ></el-alert>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus';

export default {
  name: 'StoryGenerator',
  data() {
    return {
      formData: {
        keywords: '',
        chatId: ''
      },
      isGenerating: false,
      storyContent: '',
      storyImage: '',
      errorMessage: '',
      abortController: null,
      timer: null,
      isLoadingImage: false,
      isLoadingAudio: false,
      isFirstChunk: true, // 标记是否是第一个数据块，用于提取故事ID
      audioUrl: '',
      audioPlayer: null
    };
  },
  
  mounted() {
    // 初始化时可以生成一个唯一的聊天ID，用于标识用户会话
    this.formData.chatId = this.generateChatId();
  },
  
  // 组件销毁时清理资源
  beforeUnmount() {
    this.cleanupAudio();
  },
  
  methods: {
    // 生成并播放故事语音
    async generateAndPlayAudio() {
      // 从sessionStorage获取故事ID
      const storyId = sessionStorage.getItem('currentStoryId');
      
      if (!storyId) {
        this.showError('无法生成语音：未找到故事ID');
        return;
      }
      
      if (this.audioUrl && !this.isLoadingAudio) {
        // 如果已经有音频URL，直接播放
        const audioElement = this.$refs.audioElement;
        if (audioElement) {
          audioElement.play().catch(error => {
            console.error('播放音频失败:', error);
            this.showError('播放音频失败，请稍后重试');
          });
        }
        return;
      }
      
      this.isLoadingAudio = true;
      
      try {
        // 清理之前的音频资源
        this.cleanupAudio();
        
        // 创建MediaSource对象
        const mediaSource = new MediaSource();
        // 创建音频URL
        this.audioUrl = URL.createObjectURL(mediaSource);
        
        // 监听MediaSource的sourceopen事件
        mediaSource.addEventListener('sourceopen', async () => {
          try {
            // 创建音频轨道 - 使用更通用的MIME类型以提高兼容性
            const sourceBuffer = mediaSource.addSourceBuffer('audio/mpeg');
            
            // 设置sourceBuffer的模式为sequence，确保数据块按顺序添加
            if (sourceBuffer.mode === 'segments') {
              sourceBuffer.mode = 'sequence';
            }
            
            // 调用流式语音合成接口
            console.log('正在为故事ID:', storyId, '生成流式语音');
            const response = await fetch(`http://localhost:8080/api/tts/story-stream/${storyId}`, {
              method: 'GET',
              headers: {
                'Accept': 'audio/mpeg'
              },
              credentials: 'include'
            });
            
            if (!response.ok) {
              throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            // 确保响应是流
            if (!response.body) {
              throw new Error('Response body is not a stream');
            }
            
            // 处理流式响应
            const reader = response.body.getReader();
            
            // 读取并处理流数据
            let isProcessing = false;
            const audioQueue = [];
            let totalBytesReceived = 0;
            
            function processQueue() {
              if (isProcessing || audioQueue.length === 0) return;
              
              isProcessing = true;
              
              try {
                if (!sourceBuffer.updating) {
                  const chunk = audioQueue.shift();
                  try {
                    sourceBuffer.appendBuffer(chunk);
                    totalBytesReceived += chunk.byteLength;
                    console.log('添加音频数据块，大小:', chunk.byteLength, '字节，累计:', totalBytesReceived, '字节');
                  } catch (appendError) {
                    console.error('添加音频数据块失败:', appendError);
                    // 如果添加失败，尝试清空队列并重新开始
                    if (mediaSource.readyState === 'open') {
                      audioQueue.length = 0;
                    }
                  }
                }
              } finally {
                isProcessing = false;
                if (audioQueue.length > 0) {
                  // 使用setTimeout避免递归调用导致的栈溢出
                  setTimeout(processQueue, 0);
                }
              }
            }
            
            // 监听sourceBuffer的update事件，继续处理队列
            sourceBuffer.addEventListener('update', () => {
              processQueue();
            });
            
            // 监听sourceBuffer的error事件
            sourceBuffer.addEventListener('error', (e) => {
              console.error('sourceBuffer错误:', e);
              // 尝试清空队列
              audioQueue.length = 0;
            });
            
            // 监听sourceBuffer的abort事件
            sourceBuffer.addEventListener('abort', () => {
              console.warn('sourceBuffer被中止');
            });
            
            // 自动开始播放音频
            this.$nextTick(() => {
              const audioElement = this.$refs.audioElement;
              if (audioElement) {
                audioElement.addEventListener('canplaythrough', () => {
                  audioElement.play().catch(err => {
                    console.warn('播放失败:', err);
                    // 忽略自动播放策略限制错误
                  });
                });
              }
            });
            
            ElMessage.success('开始接收语音数据，正在实时播放');
            
            // 读取流数据
            while (true) {
              const { done, value } = await reader.read();
              
              if (done) {
                console.log('流式数据接收完成，等待缓冲区处理完成');
                // 等待队列处理完成后标记MediaSource结束
                const waitForQueueEmpty = () => {
                  if (audioQueue.length === 0 && !isProcessing) {
                    if (mediaSource.readyState === 'open') {
                      console.log('所有音频数据处理完成，标记MediaSource结束');
                      mediaSource.endOfStream();
                    }
                  } else {
                    setTimeout(waitForQueueEmpty, 100);
                  }
                };
                waitForQueueEmpty();
                break;
              }
              
              // 将接收到的数据块添加到队列
              audioQueue.push(value);
              processQueue();
            }
          } catch (streamError) {
            console.error('流式音频处理错误:', streamError);
            if (mediaSource.readyState === 'open') {
              mediaSource.endOfStream('network');
            }
          }
        });
        
      } catch (error) {
        console.error('生成流式语音时出错:', error);
        this.showError('生成语音失败，请稍后重试');
      } finally {
        // 在try块外设置定时器，确保在流处理完成后重置加载状态
        setTimeout(() => {
          this.isLoadingAudio = false;
        }, 100);
      }
    },
    
    // 清理音频资源
    cleanupAudio() {
      if (this.audioUrl) {
        URL.revokeObjectURL(this.audioUrl);
        this.audioUrl = '';
      }
    },
    
    // 生成唯一的聊天ID
    generateChatId() {
      return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    },
    
    // 生成故事 - 使用原生 fetch API 处理流式响应
    async generateStory() {
      // 清理之前的音频资源
      this.cleanupAudio();
      if (!this.formData.keywords || this.formData.keywords.trim() === '') {
        this.showError('请输入故事关键词');
        return;
      }
      
      this.isGenerating = true;
      this.storyContent = '';
      this.storyImage = '';
      this.errorMessage = '';
      this.isFirstChunk = true; // 重置标志位
      
      // 创建 AbortController 用于取消请求
      this.abortController = new AbortController();
      
      try {
        // 构建查询参数
        const params = new URLSearchParams();
        params.append('keywords', this.formData.keywords.trim());
        params.append('chatId', this.formData.chatId);
        
        // 使用原生 fetch API 发起请求
        const response = await fetch(`http://localhost:8080/api/story/generate/stream?${params.toString()}`, {
          method: 'GET',
          signal: this.abortController.signal,
          headers: {
            'Content-Type': 'text/plain;charset=UTF-8'
          },
          credentials: 'include' // 包含凭证（cookies等）
        });
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // 确保响应是流
        if (!response.body) {
          throw new Error('Response body is not a stream');
        }
        
        // 处理流式响应
        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');
        
        await this.readStream(reader, decoder);
        
        // 故事生成完成后，尝试获取最新的故事信息（包含图片）
        this.fetchLatestStory();
      } catch (error) {
        if (error.name !== 'AbortError') {
          this.showError('故事生成失败，请稍后重试');
          console.error('生成故事时出错:', error);
        }
        this.isGenerating = false;
      }
    },
    
    // 读取流数据
    async readStream(reader, decoder) {
      try {
        const { done, value } = await reader.read();
        
        if (done) {
          // 流结束
          this.isGenerating = false;
          return;
        }
        
        // 解码并追加新数据
        let chunk = decoder.decode(value, { stream: true });
        
        // 处理第一个数据块，尝试提取故事ID
        if (this.isFirstChunk) {
          this.isFirstChunk = false;
          
          // 检查并提取故事ID
          const storyIdRegex = /【故事ID】(.*?)\n/;
          const idMatch = chunk.match(storyIdRegex);
          
          if (idMatch && idMatch[1]) {
            const storyId = idMatch[1].trim();
            console.log('提取到故事ID:', storyId);
            
            // 保存故事ID到sessionStorage
            try {
              sessionStorage.setItem('currentStoryId', storyId);
              console.log('故事ID已保存到sessionStorage');
            } catch (e) {
              console.error('保存故事ID到sessionStorage失败:', e);
            }
            
            // 从chunk中移除故事ID部分
            chunk = chunk.replace(storyIdRegex, '');
          }
        }
        
        this.storyContent += chunk;
        
        // 继续读取流
        await this.readStream(reader, decoder);
      } catch (error) {
        if (error.name !== 'AbortError') {
          this.showError('接收故事数据失败');
          console.error('读取流数据时出错:', error);
        }
        this.isGenerating = false;
      }
    },
    
    // 获取最新的故事信息（包含图片）
    async fetchLatestStory() {
      // 故事生成完成后不再自动获取图片，改为由用户手动点击按钮获取
      console.log('故事生成完成，您可以点击获取图片按钮查看插图');
    },
    
    // 手动获取故事图片
    async fetchStoryImage() {
      // 从sessionStorage获取故事ID
      const storyId = sessionStorage.getItem('currentStoryId');
      
      if (!storyId) {
        this.showError('无法获取图片：未找到故事ID');
        return;
      }
      
      this.isLoadingImage = true;
      
      try {
        // 调用后端API生成图片并获取图片URL
        console.log('正在为故事ID:', storyId, '生成图片');
        const response = await fetch(`http://localhost:8080/api/story/image/generate/${storyId}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include'
        });
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const result = await response.json();
        console.log('图片生成结果:', result);
        
        if (result.success) {
          // 直接从响应中获取图片URL
          if (result.data && result.data.imageUrl) {
            this.storyImage = result.data.imageUrl.replace(/[`\s]/g, '');
            ElMessage.success('成功获取故事插图');
          } else {
            ElMessage.success('图片生成成功');
          }
        } else {
          this.showError(result.message || result.msg || '图片生成失败');
        }
      } catch (error) {
        this.showError('获取图片失败，请稍后重试');
        console.error('获取故事图片时出错:', error);
      } finally {
        this.isLoadingImage = false;
      }
    },
    
    // 取消生成
    cancelGeneration() {
      if (this.abortController) {
        this.abortController.abort();
        this.abortController = null;
      }
      
      if (this.timer) {
        clearTimeout(this.timer);
        this.timer = null;
      }
      
      // 清理音频资源
      this.cleanupAudio();
      
      this.isGenerating = false;
      this.showError('已取消故事生成');
    },
    
    // 显示错误信息
    showError(message) {
      this.errorMessage = message;
      ElMessage.error(message);
      // 3秒后自动清除错误信息
      this.timer = setTimeout(() => {
        this.errorMessage = '';
      }, 3000);
    },
    
    // 提取故事标题
    extractTitle(content) {
      const titleRegex = /【标题】(.*?)(?=【梗概】|$)/s;
      const match = content.match(titleRegex);
      return match ? match[1].trim() : '未命名故事';
    },
    
    // 提取故事梗概
    extractOutline(content) {
      const outlineRegex = /【梗概】(.*?)(?=【正文】|$)/s;
      const match = content.match(outlineRegex);
      return match ? match[1].trim() : '暂无梗概';
    },
    
    // 提取故事正文
    extractContent(content) {
      const contentRegex = /【正文】(.*)/s;
      const match = content.match(contentRegex);
      return match ? match[1].trim() : '暂无内容';
    },
    
    // 清除错误信息
    clearError() {
      this.errorMessage = '';
      if (this.timer) {
        clearTimeout(this.timer);
        this.timer = null;
      }
    }
  }
};
</script>

<style scoped>
.story-generator-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.story-content {
  margin-top: 20px;
  padding: 0;
  background-color: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.story-section {
  padding: 20px;
}

/* 标题样式 */
.story-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #409eff;
}

/* 梗概样式 */
.story-outline {
  background-color: #f0f9ff;
  border: 1px solid #bae7ff;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 24px;
}

/* 正文样式 */
.story-body {
  margin-bottom: 16px;
}

/* 章节标签 */
.section-label {
  font-size: 18px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

/* 梗概内容 */
.outline-content {
  color: #606266;
  font-size: 15px;
  line-height: 1.6;
}

/* 正文内容 */
.body-content p {
  color: #303133;
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 16px;
  text-align: justify;
}

/* 故事插图样式 */
.story-image-container {
  display: flex;
  justify-content: center;
  margin: 20px 0;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.story-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: 4px;
  object-fit: contain;
}

/* 获取图片按钮容器样式 */
.image-action-container {
  margin-top: 20px;
  text-align: center;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

/* 最后一段不添加底 margin */
.body-content p:last-child {
  margin-bottom: 0;
}

.error-message {
  margin-top: 20px;
}
</style>