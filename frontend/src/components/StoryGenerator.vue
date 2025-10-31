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
          
          <div class="story-outline">
            <h3 class="section-label">梗概</h3>
            <p class="outline-content">{{ extractOutline(storyContent) }}</p>
          </div>
          
          <div class="story-body">
            <h3 class="section-label">正文</h3>
            <div class="body-content">
              <p v-for="(paragraph, index) in extractBody(storyContent)" :key="index">
                {{ paragraph }}
              </p>
            </div>
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
      errorMessage: '',
      abortController: null,
      timer: null
    };
  },
  created() {
    // 生成一个唯一的聊天ID
    this.formData.chatId = this.generateChatId();
  },
  methods: {
    // 生成唯一聊天ID
    generateChatId() {
      return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    },
    
    // 提取故事标题
    extractTitle(content) {
      const titleMatch = content.match(/【标题】([^【]+)/);
      return titleMatch ? titleMatch[1].trim() : '未命名故事';
    },
    
    // 提取故事梗概
    extractOutline(content) {
      const outlineMatch = content.match(/【梗概】([^【]+)/);
      return outlineMatch ? outlineMatch[1].trim() : '';
    },
    
    // 提取故事正文并分段
    extractBody(content) {
      const bodyMatch = content.match(/【正文】([\s\S]*)/);
      if (!bodyMatch) return [];
      
      const bodyText = bodyMatch[1].trim();
      // 按换行符分割段落
      return bodyText.split(/\n+/).filter(para => para.trim().length > 0);
    },
    
    // 生成故事 - 使用原生 fetch API 处理流式响应
    async generateStory() {
      if (!this.formData.keywords || this.formData.keywords.trim() === '') {
        this.showError('请输入故事关键词');
        return;
      }
      
      this.isGenerating = true;
      this.storyContent = '';
      this.errorMessage = '';
      
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
        
        this.readStream(reader, decoder);
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
        const chunk = decoder.decode(value, { stream: true });
        this.storyContent += chunk;
        
        // 继续读取流
        this.readStream(reader, decoder);
      } catch (error) {
        if (error.name !== 'AbortError') {
          this.showError('接收故事数据失败');
          console.error('读取流数据时出错:', error);
        }
        this.isGenerating = false;
      }
    },
    
    // 取消生成
    cancelGeneration() {
      if (this.abortController) {
        this.abortController.abort();
        this.abortController = null;
      }
      this.isGenerating = false;
      this.showError('已取消故事生成');
    },
    
    // 显示错误信息
    showError(message) {
      this.errorMessage = message;
      // 3秒后自动清除错误信息
      if (this.timer) {
        clearTimeout(this.timer);
      }
      this.timer = setTimeout(() => {
        this.clearError();
      }, 3000);
    },
    
    // 清除错误信息
    clearError() {
      this.errorMessage = '';
      if (this.timer) {
        clearTimeout(this.timer);
        this.timer = null;
      }
    }
  },
  beforeUnmount() {
    // 组件卸载前清理
    if (this.timer) {
      clearTimeout(this.timer);
    }
    if (this.abortController) {
      this.abortController.abort();
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

/* 最后一段不添加底 margin */
.body-content p:last-child {
  margin-bottom: 0;
}

.error-message {
  margin-top: 20px;
}
</style>