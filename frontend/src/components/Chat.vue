<template>
  <div class="story-app">
    <!-- 故事管理侧边栏 -->
    <aside class="story-sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
      <!-- 侧边栏头部 -->
      <div class="sidebar-header">
        <div class="app-logo">
          <span class="logo-icon">📚</span>
          <h1 v-show="!isSidebarCollapsed">童话王国</h1>
        </div>
        <button class="new-story-btn" @click="newStory">
          <span class="btn-icon">+</span>
          <span class="btn-text">新故事</span>
        </button>
        <button class="sidebar-toggle-btn" @click="toggleSidebar" :title="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
          {{ isSidebarCollapsed ? '→' : '←' }}
        </button>
      </div>

      <!-- 搜索区域 -->
      <div class="search-container">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
              type="text"
              v-model="searchKeyword"
              placeholder="搜索故事..."
              class="search-input"
              @keydown.enter="handleSearchKeydown"
          >
          <button v-if="searchKeyword" class="clear-search" @click="searchKeyword = ''">✕</button>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <span>正在加载故事...</span>
      </div>

      <!-- 故事列表 -->
      <div class="story-list" v-else>
        <div
            v-for="(story) in conversations"
            :key="story.id"
            class="story-item"
            :class="{ active: selectedConversation === story.id }"
            @click="selectConversation(story.id)"
        >
          <div class="story-preview">
            <div class="story-item-title">{{ story.title }}</div>
            <div class="story-item-preview">{{ story.preview }}</div>
          </div>
          <div class="story-item-meta">
            <div class="story-time">{{ story.time }}</div>
            <div class="story-actions">
              <button class="action-btn" title="删除" @click.stop="deleteStory(story.id)">🗑️</button>
            </div>
          </div>
        </div>
        <div v-if="conversations.length === 0" class="empty-state">
          <div class="empty-icon">📖</div>
          <p>还没有故事</p>
          <p class="empty-hint">输入关键词开始创作你的第一个童话故事吧！</p>
        </div>
      </div>

      <!-- 用户信息 -->
      <div class="user-profile">
        <div class="user-avatar" v-if="currentUser?.avatarUrl">
          <img :src="currentUser.avatarUrl" alt="用户头像" class="avatar-image" />
        </div>
        <div class="user-avatar" v-else>{{ userAvatar }}</div>
        <div class="user-info">
          <div class="user-name">{{ username }}</div>
          <button class="logout-btn" @click="logout">退出登录</button>
        </div>
      </div>
    </aside>

    <!-- 主内容区域 -->
    <main class="story-main" :class="{ 'input-collapsed': isInputCollapsed }">
      <!-- 顶部导航 -->
      <header class="story-header">
        <h2 class="current-story-title">{{ currentConversation?.title || '创建新故事' }}</h2>
        <div class="story-actions-header">
          <!-- 移动到这里：生成图片按钮、生成语音按钮和音频播放器 -->
          <div class="story-media-controls" v-if="currentStoryMessage">
            <!-- 生成图片按钮 -->
            <button
                class="header-action"
                @click="fetchStoryImage"
                :disabled="isLoadingImage"
                :title="isLoadingImage ? '生成中...' : (currentStoryMessage.imageUrl ? '重新生成插图' : '生成插图')"
            >
              🖼️
            </button>
            
            <!-- 图片生成进度条 -->
            <div v-if="showProgressBar" class="progress-bar-container">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressValue + '%' }"></div>
              </div>
              <span class="progress-text">{{ progressValue }}%</span>
            </div>

            <!-- 生成语音按钮 -->
            <button
                class="header-action"
                @click="generateAndPlayAudio"
                :disabled="isLoadingAudio"
                :title="isLoadingAudio ? '生成中...' : (currentStoryMessage.audioUrl ? '播放故事' : '生成语音')"
            >
              {{ currentStoryMessage.audioUrl ? '▶️' : '🔊' }}
            </button>

            <!-- 音频播放器 -->
            <div v-if="currentStoryMessage.audioUrl" class="header-audio-player">
              <audio
                  id="audio-player"
                  controls=""
                  class="audio-player"
              >
                <source :src="currentStoryMessage.audioUrl" type="audio/mpeg">
                您的浏览器不支持音频播放
              </audio>
            </div>
          </div>

          <!-- 主题设置按钮 -->
          <button class="theme-settings-btn" @click="showThemeDialog = true" title="故事主题设置">
            ⚙️
          </button>
          
          <!-- 拼音按钮 -->
          <button class="header-action" @click="togglePinyin" :title="showPinyin ? '隐藏拼音' : '显示拼音'">
            🔤
          </button>

          <!-- 输入框切换按钮 -->
          <button class="input-toggle-btn" @click="toggleInput" :title="isInputCollapsed ? '展开输入框' : '收起输入框'">
            {{ isInputCollapsed ? '↑' : '↓' }}
          </button>

        </div>
      </header>

      <!-- 故事内容展示区域 -->
      <div class="content-area">
        <!-- 欢迎界面 -->
        <div v-if="messages.length === 0" class="welcome-view">
          <div class="welcome-illustration">✨</div>
          <h3>欢迎来到童话王国！</h3>
          <p>在这里，你可以为孩子创建充满想象力的童话故事</p>
          <div class="prompt-suggestions">
            <div
                class="prompt-tag"
                @click="sendPrompt('可爱的小猫冒险')"
            >
              可爱的小猫冒险
            </div>
            <div
                class="prompt-tag"
                @click="sendPrompt('友谊的力量')"
            >
              友谊的力量
            </div>
            <div
                class="prompt-tag"
                @click="sendPrompt('太空探索')"
            >
              太空探索
            </div>
            <div
                class="prompt-tag"
                @click="sendPrompt('森林里的小动物们')"
            >
              森林里的小动物们
            </div>
          </div>
        </div>

        <!-- 故事和消息显示 -->
        <div v-else class="story-messages">
          <!-- 用户消息 -->
          <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message-wrapper"
              :class="{ 'user-message': msg.sender === 'user', 'story-message': msg.sender === 'assistant' }"
          >

            <!-- 故事内容卡片 -->
            <div v-if="msg.isStory" class="story-card">
              <!-- 故事卡片内容（流式和非流式共用） -->
              <div class="story-card-content">
                <!-- 故事标题 -->
                <h2 class="story-card-title">
                  <span v-if="!showPinyin">{{ msg.title || extractTitle(msg.content) }}</span>
                  <span v-else v-html="convertToPinyin(msg.title || extractTitle(msg.content))"></span>
                  <span v-if="msg.streaming" class="generating-indicator">生成中...</span>
                </h2>

                <!-- 故事插图 -->
                <div class="story-visual" v-if="msg.imageUrl">
                  <div class="story-image-wrapper">
                    <img :src="msg.imageUrl" :alt="msg.title || extractTitle(msg.content)" class="story-card-image" />
                    <div class="image-overlay">
                      <button
                          class="image-action"
                          @click.stop="regenerateStoryImage"
                          :disabled="isLoadingImage"
                          title="重新生成图片"
                      >
                        🔄
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 故事内容区域 -->
                <div class="story-details">
                  <!-- 故事梗概 -->
                  <div class="story-outline-section">
                    <h3 class="section-title">故事梗概</h3>
                    <p class="outline-text" v-if="!showPinyin">{{ msg.outline || extractOutline(msg.content) }}</p>
                    <p class="outline-text" v-else v-html="convertToPinyin(msg.outline || extractOutline(msg.content))"></p>
                  </div>

                  <!-- 故事正文 -->
                  <div class="story-body-section">
                    <h3 class="section-title">故事内容</h3>
                    <div class="story-text">
                      <div v-for="(paragraph, pIndex) in (msg.body || extractContent(msg.content)).split('\n\n')" :key="pIndex">
                        <p v-if="!showPinyin">{{ paragraph }}</p>
                        <p v-else v-html="convertToPinyin(paragraph)"></p>
                      </div>
                    </div>
                    <!-- 流式生成指示器 -->
                    <div v-if="msg.streaming" class="streaming-indicator">
                      <div class="streaming-dots">
                        <span></span>
                        <span></span>
                        <span></span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 普通AI回复 -->
            <div v-else-if="!msg.isStory" class="ai-bubble">
              <div class="ai-content">{{ msg.content }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <footer class="story-input-area">
        <div class="keyword-input-section">
          <div class="input-label">输入故事关键词：</div>
          <div class="input-container">
            <textarea
                v-model="inputMessage"
                placeholder="例如：可爱的小猫冒险、友谊的力量、太空探索..."
                class="keyword-textarea"
                @keydown.enter.prevent="handleEnterKey"
                rows="2"
            ></textarea>
            <button
                class="generate-btn"
                :disabled="!inputMessage.trim() || isGenerating"
                @click="sendMessage"
            >
              <span v-if="!isGenerating">生成故事</span>
              <span v-else class="generating-text">生成中...</span>
            </button>
          </div>
        </div>
      </footer>
    </main>

    <!-- 主题选择弹窗 -->
    <div v-if="showThemeDialog" class="theme-dialog-overlay" @click="showThemeDialog = false">
      <div class="theme-dialog" @click.stop>
        <div class="theme-dialog-header">
          <h3>故事主题设置</h3>
          <button class="dialog-close-btn" @click="showThemeDialog = false">×</button>
        </div>
        
        <div class="theme-dialog-content">
          <!-- 主题模式选择 -->
          <div class="theme-mode-section">
            <h4>选择模式</h4>
            <div class="mode-options">
              <label class="mode-option">
                <input type="radio" v-model="themeMode" value="single" />
                <span>单选</span>
              </label>
              <label class="mode-option">
                <input type="radio" v-model="themeMode" value="multiple" />
                <span>多选</span>
              </label>
              <label class="mode-option">
                <input type="radio" v-model="themeMode" value="custom" />
                <span>自定义</span>
              </label>
            </div>
          </div>

          <!-- 主题选择区域 -->
          <div class="theme-selection-section" v-if="themeMode !== 'custom'">
            <h4>选择主题</h4>
            <div class="theme-grid">
              <div 
                v-for="theme in availableThemes" 
                :key="theme.id"
                class="theme-card"
                :class="{ 
                  'selected': selectedThemes.includes(theme.id),
                  'disabled': themeMode === 'single' && selectedThemes.length > 0 && !selectedThemes.includes(theme.id)
                }"
                @click="toggleTheme(theme.id)"
              >
                <div class="theme-icon">{{ getThemeIcon(theme.id) }}</div>
                <div class="theme-info">
                  <div class="theme-name">{{ theme.name }}</div>
                  <div class="theme-description">{{ theme.description }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 自定义主题输入 -->
          <div class="custom-theme-section" v-if="themeMode === 'custom'">
            <h4>自定义主题</h4>
            <textarea 
              v-model="customTheme" 
              placeholder="请输入自定义主题，例如：科幻冒险、童话魔法、历史传奇等..."
              class="custom-theme-input"
              rows="3"
            ></textarea>
          </div>

          <!-- 当前选择显示 -->
          <div class="current-selection-section">
            <h4>当前选择</h4>
            <div class="selected-themes-display">
              <span v-if="getSelectedThemeText()" class="selected-themes-text">
                {{ getSelectedThemeText() }}
              </span>
              <span v-else class="no-selection">未选择主题</span>
            </div>
          </div>
        </div>

        <div class="theme-dialog-footer">
          <button class="dialog-btn secondary" @click="resetThemes">重置</button>
          <button class="dialog-btn primary" @click="saveThemes">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 进度条样式 */
.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 10px;
  min-width: 150px;
}
/* 特别为包含拼音的段落增加行高 */
.story-text p:has(.pinyin-char) {
  line-height: 2.2;
}

/* 标题中的拼音样式调整 */
.story-card-title .pinyin-char {
  height: 50px;
}

.story-card-title .char {
  font-size: 28px;
}

.story-card-title .pinyin {
  font-size: 12px;
}

/* 梗概中的拼音样式调整 */
.outline-text .pinyin-char {
  height: 38px;
}

.outline-text .char {
  font-size: 16px;
}

.outline-text .pinyin {
  font-size: 10px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .pinyin-char {
    height: 36px;
  }

  .char {
    font-size: 14px;
  }

  .pinyin {
    font-size: 9px;
  }

  .story-card-title .pinyin-char {
    height: 42px;
  }

  .story-card-title .char {
    font-size: 24px;
  }

  .story-card-title .pinyin {
    font-size: 11px;
  }
}
.progress-bar {
  flex: 1;
  height: 6px;
  background-color: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4CAF50, #8BC34A);
  border-radius: 3px;
  transition: width 0.3s ease;
  position: relative;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-text {
  font-size: 12px;
  color: #666;
  min-width: 35px;
  text-align: right;
}

/* 确保进度条在移动设备上也能正常显示 */
@media (max-width: 768px) {
  .progress-bar-container {
    min-width: 120px;
    margin-left: 5px;
  }
}


/* 拼音样式 - 优化为准确显示在汉字正上方 */
.pinyin-char {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  text-align: center;
  margin: 0 1px;
  vertical-align: top;
  height: 42px; /* 保持固定高度确保布局稳定 */
}

.char {
  display: block;
  font-size: 16px;
  line-height: 1.2;
  /* 移除固定top值，通过flex布局自然对齐 */
}

.pinyin {
  display: block;
  font-size: 10px;
  color: #666;
  line-height: 1;
  margin-bottom: 2px; /* 微调拼音与汉字间距 */
}

</style>

<script>
import { ElMessage } from 'element-plus';
import { BASE_URL } from '../main.js'
import { pinyin } from 'pinyin-pro';

export default {
  name: 'Chat',
  data() {
    return {
      selectedConversation: '1',
      conversations: [],
      messages: [],
      inputMessage: '',
      username: '',
      currentUser: null,
      searchKeyword: '',
      isLoading: false,
      // 新增：保存故事列表滚动位置
      storyListScrollTop: 0,
      // 新增：标记是否正在切换对话
      isSwitchingConversation: false,
      // StoryGenerator相关数据
      isGenerating: false,
      storyContent: '',
      storyImage: '',
      errorMessage: '',
      abortController: null,
      timer: null,
      isLoadingImage: false,
      isLoadingAudio: false,
      isFirstChunk: true,
      audioUrl: '',
      audioPlayer: null,
      // 新增：图片生成进度条相关
      showProgressBar: false,
      progressValue: 0,
      progressTimer: null,
      formData: {
        keywords: '',
        chatId: ''
      },
      // 新增：音频播放器引用
      currentAudioElement: null,
      // 新增：音频流相关
      mediaSource: null,
      sourceBuffer: null,
      audioQueue: [],
      isProcessingAudio: false,
      // 新增：布局交互相关
      isSidebarCollapsed: false,
      isInputCollapsed: false,
      // 新增：主题选择相关数据
      showThemeDialog: false,
      selectedThemes: [],
      customTheme: '',
      themeMode: 'single', // single, multiple, custom
      // 新增：拼音相关数据
      showPinyin: false,
      availableThemes: [
        { id: 'adventure', name: '冒险', description: '充满刺激和探索的故事' },
        { id: 'fantasy', name: '奇幻', description: '魔法和神秘的世界' },
        { id: 'friendship', name: '友谊', description: '关于友情和合作的故事' },
        { id: 'animal', name: '动物', description: '可爱动物们的故事' },
        { id: 'science', name: '科普', description: '知识和探索的故事' },
        { id: 'moral', name: '品德', description: '培养良好品德的故事' },
        { id: 'family', name: '家庭', description: '温馨的家庭故事' },
        { id: 'nature', name: '自然', description: '大自然和环境保护的故事' }
      ]
    };
  },

  computed: {
    currentConversation() {
      return this.conversations.find(c => c.id === this.selectedConversation);
    },

    userAvatar() {
      const name = this.username;
      return name ? name.slice(0, 2).toUpperCase() : '游客';
    },

    // 当前用户ID
    currentUserId() {
      return this.currentUser?.id || null;
    },

    // 当前故事消息
    currentStoryMessage() {
      return this.messages.find(msg => msg.isStory && msg.sender === 'assistant');
    }
  },

  mounted() {
    // 从localStorage获取用户名
    this.username = localStorage.getItem('username') || '未登录';
    // 获取当前用户信息
    this.getCurrentUserInfo();
    // 初始化时生成唯一的聊天ID，用于标识用户会话
    this.formData.chatId = this.generateChatId();
    // 加载主题设置
    this.loadThemesFromLocalStorage();

    // 监听故事列表容器的滚动事件
    this.$nextTick(() => {
      const storyList = this.$el?.querySelector('.story-list');
      if (storyList) {
        storyList.addEventListener('scroll', this.saveStoryListScrollPosition);
      }
    });
  },

  beforeUnmount() {
    // 清理滚动事件监听
    const storyList = this.$el?.querySelector('.story-list');
    if (storyList) {
      storyList.removeEventListener('scroll', this.saveStoryListScrollPosition);
    }
    this.cleanupAudio();
  },

  created() {
    // 组件创建时获取用户故事列表
    this.getCurrentUserInfo().then(() => {
      if (this.currentUserId) {
        this.fetchStories();
      }
    });
  },

  methods: {
    // 切换拼音显示状态
    togglePinyin() {
      this.showPinyin = !this.showPinyin;
    },

// 将中文文本转换为带拼音的HTML
    convertToPinyin(text) {
      if (!text) return '';

      let result = '';

      // 遍历文本中的每个字符
      for (let i = 0; i < text.length; i++) {
        const char = text[i];

        // 检查是否为中文字符
        if (/[\u4e00-\u9fa5]/.test(char)) {
          // 使用pinyin-pro获取拼音，保留声调
          const charPinyin = pinyin(char, {
            toneType: 'symbol', // 使用数字声调
            type: 'array'
          })[0] || '';

          // 添加额外的间距控制
          const width = charPinyin.length > 3 ? '1.5em' : '1em';
          
          // 为每个字创建拼音span元素
          result += `<span class="pinyin-char" style="width: ${width};">
                  <span class="pinyin">${charPinyin}</span>
                  <span class="char">${char}</span>
                </span>`;
        } else {
          // 非中文字符直接添加
          result += char;
        }
      }

      return result;
    },
    
    // 为中文词添加拼音包装
    wrapWithPinyin(word) {
      // 使用pinyin-pro获取拼音，保留声调
      const pinyinResult = pinyin(word, {
        toneType: 'mark', // 使用声调符号
        type: 'array' // 返回数组格式
      });
      
      let result = '';
      // 为每个字和对应的拼音创建span元素
      for (let i = 0; i < word.length; i++) {
        const char = word[i];
        const charPinyin = pinyinResult[i] || '';
        result += `<span class="pinyin-char">
                    <span class="char">${char}</span>
                    <span class="pinyin">${charPinyin}</span>
                  </span>`;
      }
      
      return result;
    },
    
    // 获取当前登录用户信息
    async getCurrentUserInfo() {
      try {
        const response = await fetch(`${BASE_URL}/api/users/current`, {
          credentials: 'include' // 包含cookie以维护Session
        });

        if (response.ok) {
          const user = await response.json();
          this.currentUser = user;
          this.username = user.username || '用户';
          return user;
        } else {
          // Session过期，重定向到登录页面
          this.$router.push('/login');
          return null;
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        return null;
      }
    },

    // 修改：获取故事列表方法 - 优化为保持滚动位置
    async fetchStories() {
      if (!this.currentUserId) {
        console.error('用户未登录或ID不存在');
        return;
      }

      // 保存当前滚动位置
      const previousScrollTop = this.storyListScrollTop;

      this.isLoading = true;

      try {
        // 构建查询参数
        let url = `${BASE_URL}/api/story/list?userId=${this.currentUserId}`;
        if (this.searchKeyword.trim()) {
          url += `&keyword=${encodeURIComponent(this.searchKeyword.trim())}`;
        }

        const response = await fetch(url, {
          credentials: 'include'
        });

        if (response.ok) {
          const result = await response.json();
          if (result.success && result.data) {
            // 转换后端返回的故事数据为前端对话列表格式
            this.conversations = result.data.map(story => ({
              id: story.id.toString(),
              title: story.title || '未命名故事',
              preview: this.generatePreview(story.content || ''),
              time: this.formatTime(story.createTime || new Date())
            }));

            // 如果有故事且没有选中的对话，选中第一个
            if (this.conversations.length > 0 && !this.currentConversation) {
              await this.selectConversation(this.conversations[0].id);
            }
          } else {
            console.error('获取故事列表失败:', result.message);
            this.conversations = [];
          }
        } else {
          console.error('获取故事列表请求失败');
        }
      } finally {
        this.isLoading = false;
        // 恢复滚动位置
        await this.$nextTick(() => {
          this.storyListScrollTop = previousScrollTop;
          this.restoreStoryListScrollPosition();
        });
      }
    },
    // 新增：保存故事列表滚动位置
    saveStoryListScrollPosition() {
      const storyList = this.$el?.querySelector('.story-list');
      if (storyList) {
        this.storyListScrollTop = storyList.scrollTop;
      }
    },

    // 新增：恢复故事列表滚动位置
    restoreStoryListScrollPosition() {
      this.$nextTick(() => {
        const storyList = this.$el?.querySelector('.story-list');
        if (storyList && this.storyListScrollTop > 0) {
          storyList.scrollTop = this.storyListScrollTop;
        }
      });
    },

    // 生成预览文本
    generatePreview(content) {
      if (!content) return '暂无内容';
      // 截取前30个字符作为预览
      return content.length > 30 ? content.substring(0, 30) + '...' : content;
    },

    // 格式化时间
    formatTime(date) {
      const now = new Date();
      const target = new Date(date);
      const diffDays = Math.floor((now - target) / (1000 * 60 * 60 * 24));

      if (diffDays === 0) return '今天';
      if (diffDays === 1) return '昨天';
      if (diffDays === 2) return '前天';
      if (diffDays < 7) return `${diffDays}天前`;

      // 超过7天显示具体日期
      return `${target.getMonth() + 1}月${target.getDate()}日`;
    },

    //搜索故事方法 - 优化为保持用户体验
    handleSearch() {
      // 保存当前滚动位置
      const previousScrollTop = this.storyListScrollTop;

      this.fetchStories().then(() => {
        // 搜索完成后，如果有结果，保持滚动位置；如果没有结果，滚动到顶部
        if (this.conversations.length > 0) {
          this.$nextTick(() => {
            this.storyListScrollTop = previousScrollTop;
            this.restoreStoryListScrollPosition();
          });
        }
      });
    },

    // 处理搜索框回车事件
    handleSearchKeydown(event) {
      if (event.key === 'Enter') {
        this.handleSearch();
      }
    },

    // 修改：选择对话方法 - 增强音频停止逻辑
    async selectConversation(id) {
      // 如果正在切换中或选择的是当前对话，则直接返回
      if (this.isSwitchingConversation || this.selectedConversation === id) {
        return;
      }

      this.isSwitchingConversation = true;

      // 停止当前播放的音频
      this.stopAllAudio();

      this.selectedConversation = id;

      try {
        // 保存当前滚动位置
        this.saveStoryListScrollPosition();

        // 加载对应对话的消息
        await this.loadMessages(id);

        // 恢复滚动位置
        this.restoreStoryListScrollPosition();
      } catch (error) {
        console.error('切换对话失败:', error);
        this.showError('加载故事失败，请稍后重试');
      } finally {
        this.isSwitchingConversation = false;
      }
    },

    // 新增：停止所有音频播放
    stopAllAudio() {
      // 停止所有音频播放器
      const audioElements = document.querySelectorAll('audio');
      audioElements.forEach(audio => {
        audio.pause();
        audio.currentTime = 0;
      });

      // 停止当前音频播放器
      if (this.currentAudioElement) {
        this.currentAudioElement.pause();
        this.currentAudioElement.currentTime = 0;
        this.currentAudioElement = null;
      }

      // 清理音频资源
      this.cleanupAudio();

      // 重置音频加载状态
      this.isLoadingAudio = false;

      // 强制更新当前故事消息的音频状态
      if (this.currentStoryMessage && this.currentStoryMessage.audioUrl) {
        // 创建一个新的对象来触发响应式更新
        const updatedMessage = { ...this.currentStoryMessage };
        updatedMessage.audioUrl = '';
        const index = this.messages.findIndex(msg =>
            msg.isStory && msg.sender === 'assistant'
        );
        if (index !== -1) {
          this.messages.splice(index, 1, updatedMessage);
        }
      }
    },

    // 修改：清理音频资源方法
    cleanupAudio() {
      if (this.audioUrl) {
        try {
          URL.revokeObjectURL(this.audioUrl);
        } catch (e) {
          console.warn('清理音频URL失败:', e);
        }
        this.audioUrl = '';
      }

      // 清理MediaSource
      if (this.mediaSource) {
        try {
          if (this.mediaSource.readyState === 'open') {
            this.mediaSource.endOfStream();
          }
        } catch (e) {
          console.warn('清理MediaSource失败:', e);
        }
        this.mediaSource = null;
      }

      // 清理SourceBuffer
      if (this.sourceBuffer) {
        this.sourceBuffer = null;
      }

      // 清理音频队列
      this.audioQueue = [];
      this.isProcessingAudio = false;
    },

    //加载消息方法 - 避免欢迎界面闪烁
    async loadMessages(conversationId) {
      // 保存当前滚动位置
      const previousScrollTop = this.storyListScrollTop;

      try {
        // 调用后端API获取故事详情
        await this.loadStoryDetails(conversationId);
      } catch (error) {
        console.error('加载故事详情失败:', error);
        throw error;
      } finally {
        // 恢复滚动位置
        await this.$nextTick(() => {
          this.storyListScrollTop = previousScrollTop;
          this.restoreStoryListScrollPosition();
        });
      }
    },

    //根据故事ID获取故事详情 - 直接替换消息内容
    async loadStoryDetails(storyId) {
      try {
        const response = await fetch(`${BASE_URL}/api/story/${storyId}`, {
          method: 'GET',
          credentials: 'include'
        });

        const result = await response.json();
        console.log('获取故事详情结果:', result);

        if (result.success && result.data) {
          const story = result.data;

          // 构建格式化的故事内容
          const formattedContent = `【标题】${story.title}\n\n【梗概】${story.summary || '暂无梗概'}\n\n【正文】${story.content || '暂无内容'}`;

          // 直接替换消息数组，而不是先清空再添加
          this.messages = [{
            sender: 'assistant',
            content: formattedContent,
            isStory: true,
            imageUrl: story.imageUrl ? story.imageUrl.replace(/[`\s]/g, '') : '',
            audioUrl: story.audioUrl || '',
            // 保存原始故事数据，方便后续使用
            rawData: story
          }];

          // 保存故事ID到sessionStorage
          sessionStorage.setItem('currentStoryId', storyId);
        } else {
          throw new Error(result.message || '获取故事详情失败');
        }
      } catch (error) {
        console.error('获取故事详情时出错:', error);
        throw error;
      }
    },

    // 用户登出
    async logout()
    {
      try {
        // 停止当前音频
        this.stopAllAudio();

        // 调用后端登出接口
        await fetch(`${BASE_URL}/api/users/logout`, {
          method: 'POST',
          credentials: 'include' // 包含cookie以维护Session
        });

        // 清除本地存储
        localStorage.removeItem('username');

        // 重定向到登录页面
        this.$router.push('/login');
      } catch (error) {
        console.error('登出失败:', error);
        // 即使出错也清除本地数据并重定向
        localStorage.removeItem('username');
        this.$router.push('/login');
      }
    }
,

    // 主题选择相关方法
    
    // 切换主题选择
    toggleTheme(themeId) {
      if (this.themeMode === 'single') {
        // 单选模式：直接替换当前选择
        this.selectedThemes = [themeId];
      } else if (this.themeMode === 'multiple') {
        // 多选模式：切换选择状态
        const index = this.selectedThemes.indexOf(themeId);
        if (index > -1) {
          this.selectedThemes.splice(index, 1);
        } else {
          this.selectedThemes.push(themeId);
        }
      }
    },

    // 获取主题图标
    getThemeIcon(themeId) {
      const icons = {
        'adventure': '🏔️',
        'fantasy': '🧙',
        'science': '🔬',
        'animal': '🐾',
        'fairy': '🧚',
        'history': '🏛️',
        'mystery': '🔍',
        'friendship': '🤝'
      };
      return icons[themeId] || '📖';
    },

    // 获取当前选择的主题文本
    getSelectedThemeText() {
      if (this.themeMode === 'custom' && this.customTheme.trim()) {
        return `自定义主题：${this.customTheme.trim()}`;
      } else if (this.selectedThemes.length > 0) {
        const selectedThemeNames = this.selectedThemes.map(themeId => {
          const theme = this.availableThemes.find(t => t.id === themeId);
          return theme ? theme.name : themeId;
        });
        return selectedThemeNames.join('、');
      }
      return '';
    },

    // 重置主题选择
    resetThemes() {
      this.selectedThemes = [];
      this.customTheme = '';
      this.themeMode = 'single';
    },

    // 保存主题设置
    saveThemes() {
      // 验证选择
      if (this.themeMode === 'custom' && !this.customTheme.trim()) {
        this.showError('请输入自定义主题');
        return;
      } else if (this.themeMode !== 'custom' && this.selectedThemes.length === 0) {
        this.showError('请至少选择一个主题');
        return;
      }

      // 保存到localStorage
      this.saveThemesToLocalStorage();
      
      // 关闭弹窗
      this.showThemeDialog = false;
      
      // 显示成功提示
      this.showSuccess('主题设置已保存');
    },

    // 保存主题到localStorage
    saveThemesToLocalStorage() {
      const themeSettings = {
        selectedThemes: this.selectedThemes,
        customTheme: this.customTheme,
        themeMode: this.themeMode,
        timestamp: Date.now()
      };
      localStorage.setItem('storyThemeSettings', JSON.stringify(themeSettings));
    },

    // 从localStorage加载主题设置
    loadThemesFromLocalStorage() {
      try {
        const saved = localStorage.getItem('storyThemeSettings');
        if (saved) {
          const themeSettings = JSON.parse(saved);
          this.selectedThemes = themeSettings.selectedThemes || [];
          this.customTheme = themeSettings.customTheme || '';
          this.themeMode = themeSettings.themeMode || 'single';
        }
      } catch (error) {
        console.error('加载主题设置失败:', error);
      }
    },

    // 发送消息
    async sendMessage() {
      if (!this.inputMessage.trim()) return;

      const message = this.inputMessage.trim();

      // 清空输入框
      this.inputMessage = '';

      // 滚动到底部
      await this.$nextTick(() => {
        const chatContent = this.$el?.querySelector('.chat-content');
        if (chatContent) {
          try {
            chatContent.scrollTop = chatContent.scrollHeight;
          } catch (err) {
            console.warn('滚动操作失败:', err);
          }
        }
      });

      // 使用StoryGenerator的功能生成故事
      await this.generateStoryFromMessage(message);
    },

    // 修改 generateStoryFromMessage 方法，确保 storyMessage 是响应式的
    async generateStoryFromMessage(keywords) {
      // 清理之前的音频资源
      this.stopAllAudio();
      this.isGenerating = true;
      this.errorMessage = '';

      // 创建 AbortController 用于取消请求
      this.abortController = new AbortController();

      // 创建新的故事消息对象 - 使用响应式方式创建
      const storyMessage = {
        sender: 'assistant',
        content: '',
        id: Date.now().toString(),
        isStory: true,
        streaming: true,
        storyId: '',
        title: '故事生成中...',
        outline: '正在构思故事梗概...',
        body: '正在创作故事内容...',
        imageUrl: '',
        audioUrl: '',
        rawData: null
      };

      // 使用 push 方法添加消息到响应式数组
      this.messages.push(storyMessage);

      // 等待 Vue 更新 DOM
      await this.$nextTick();

      // 滚动到底部
      this.scrollToBottom();

      try {
        // 构建查询参数
        const params = new URLSearchParams();
        params.append('keywords', keywords.trim());
        params.append('chatId', this.formData.chatId);
        
        // 添加主题参数
        const themeText = this.getSelectedThemeText();
        if (themeText) {
          params.append('theme', themeText);
          console.log('添加主题参数:', themeText);
        }

        console.log('开始请求流式故事生成，关键词:', keywords);

        // 使用原生 fetch API 发起请求
        const response = await fetch(`${BASE_URL}/api/story/generate/stream?${params.toString()}`, {
          method: 'GET',
          signal: this.abortController.signal,
          headers: {
            'Accept': 'text/plain;charset=UTF-8'
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
        const decoder = new TextDecoder('utf-8');

        // 找到刚刚添加的消息的引用
        const currentStoryMessage = this.messages[this.messages.length - 1];
        await this.readStream(reader, decoder, currentStoryMessage);

        // 流读取完成后，尝试获取最新的故事信息
        if (currentStoryMessage.storyId) {
          await this.fetchLatestStory();
        }
      } catch (error) {
        console.error('生成故事时出错:', error);
        if (error.name !== 'AbortError') {
          let errorMsg = '故事生成失败';
          if (error.message && error.message.includes('Network')) {
            errorMsg = '网络连接失败，请检查网络后重试';
          } else if (error.message && error.message.includes('timeout')) {
            errorMsg = '请求超时，请稍后重试';
          }
          this.showError(errorMsg + '，请稍后重试');
        }
        this.isGenerating = false;
        // 找到当前的故事消息并更新状态
        const currentStoryMessage = this.messages.find(msg => msg.id === storyMessage.id);
        if (currentStoryMessage) {
          currentStoryMessage.streaming = false;
        }
      }
    },
    // 添加滚动到底部的辅助方法
    scrollToBottom() {
      const contentArea = this.$el?.querySelector('.content-area');
      if (contentArea) {
        try {
          contentArea.scrollTop = contentArea.scrollHeight;
        } catch (err) {
          console.warn('滚动操作失败:', err);
        }
      }
    },

    async readStream(reader, decoder, storyMessage) {
      try {
        while (true) {
          const { done, value } = await reader.read();

          if (done) {
            console.log('流式数据接收完成');
            this.isGenerating = false;

            // 直接赋值，Vue 3 会自动处理响应式
            storyMessage.streaming = false;

            // 最终更新结构化数据
            this.updateStructuredData(storyMessage);
            return;
          }

          // 解码数据
          let chunk = decoder.decode(value, { stream: true });

          // 直接赋值更新内容
          storyMessage.content = storyMessage.content + chunk;

          // 尝试提取故事ID
          if (!storyMessage.storyId) {
            const storyIdRegex = /【故事ID】(.*?)\n/;
            const idMatch = storyMessage.content.match(storyIdRegex);
            if (idMatch && idMatch[1]) {
              const storyId = idMatch[1].trim();
              console.log('提取到故事ID:', storyId);
              storyMessage.storyId = storyId;

              try {
                sessionStorage.setItem('currentStoryId', storyId);
              } catch (e) {
                console.error('保存故事ID到sessionStorage失败:', e);
              }
            }
          }

          // 实时更新结构化数据
          this.updateStructuredData(storyMessage);

          // 滚动到底部
          this.scrollToBottom();
        }
      } catch (error) {
        if (error.name !== 'AbortError') {
          console.error('读取流数据时出错:', error);
        }
        this.isGenerating = false;
        storyMessage.streaming = false;
      }
    },
// 修改 updateStructuredData 方法，移除 this.$set 的使用
    updateStructuredData(storyMessage) {
      const content = storyMessage.content;

      // 直接赋值更新属性
      storyMessage.title = this.extractTitle(content);
      storyMessage.outline = this.extractOutline(content);
      storyMessage.body = this.extractContent(content);
    },



    // 生成唯一的聊天ID
    generateChatId() {
      return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    },

    // 改进提取方法，添加更强大的匹配逻辑
    extractTitle(content) {
      if (!content) return '故事生成中...';

      // 多种匹配模式，按优先级尝试
      const titlePatterns = [
        /【标题】([^【]+)(?=【|$)/,
        /标题[：:]\s*([^\n]+)/,
        /^([^【\n]+)/,
        /故事名称[：:]\s*([^\n]+)/
      ];

      for (let pattern of titlePatterns) {
        const match = content.match(pattern);
        if (match && match[1] && match[1].trim()) {
          let title = match[1].trim();
          // 清理可能的标点符号
          title = title.replace(/^[：:]\s*/, '').replace(/[。，,.！!?？]$/, '');
          return title.length > 50 ? title.substring(0, 50) + '...' : title;
        }
      }

      // 如果没有匹配到标题，从内容开头提取
      if (content.length > 0) {
        const firstLine = content.split('\n')[0].trim();
        if (firstLine && firstLine.length > 5 && !firstLine.includes('【') && !firstLine.includes('】')) {
          let title = firstLine.replace(/^[：:]\s*/, '').replace(/[。，,.！!?？]$/, '');
          return title.length > 50 ? title.substring(0, 50) + '...' : title;
        }
      }

      return '精彩故事即将呈现...';
    },

    extractOutline(content) {
      if (!content) return '正在构思故事梗概...';

      const outlinePatterns = [
        /【梗概】([^【]+)(?=【|$)/,
        /梗概[：:]\s*([^\n]+)/,
        /故事梗概[：:]\s*([^\n]+)/,
        /内容简介[：:]\s*([^\n]+)/
      ];

      for (let pattern of outlinePatterns) {
        const match = content.match(pattern);
        if (match && match[1] && match[1].trim()) {
          let outline = match[1].trim();
          outline = outline.replace(/^[：:]\s*/, '');
          return outline;
        }
      }

      // 如果没有明确的梗概，从内容中提取前几句话
      const lines = content.split('\n').filter(line =>
          line.trim() &&
          !line.includes('【') &&
          !line.includes('标题') &&
          !line.includes('故事ID')
      );

      if (lines.length > 0) {
        // 取前2-3行作为梗概
        const potentialOutline = lines.slice(0, 3).join(' ');
        if (potentialOutline.length > 20) {
          return potentialOutline.length > 100 ? potentialOutline.substring(0, 100) + '...' : potentialOutline;
        }
      }

      return '正在生成故事梗概...';
    },

    extractContent(content) {
      if (!content) return '正在创作故事内容...';

      const contentPatterns = [
        /【正文】([\s\S]*)/,
        /正文[：:]\s*([\s\S]*)/,
        /故事内容[：:]\s*([\s\S]*)/
      ];

      for (let pattern of contentPatterns) {
        const match = content.match(pattern);
        if (match && match[1] && match[1].trim()) {
          return match[1].trim();
        }
      }

      // 如果没有标记，尝试排除标题和梗概部分
      const withoutTitle = content.replace(/【标题】[^】]*】/, '').replace(/标题[：:][^\n]*\n/, '');
      const withoutOutline = withoutTitle.replace(/【梗概】[^】]*】/, '').replace(/梗概[：:][^\n]*\n/, '');

      const cleanedContent = withoutOutline.trim();
      if (cleanedContent && cleanedContent.length > 10) {
        return cleanedContent;
      }

      return content || '正在生成故事内容...';
    },

    // 获取最新的故事信息（包含图片）
    async fetchLatestStory() {
      const storyId = sessionStorage.getItem('currentStoryId');
      if (storyId) {
        try {
          // 尝试获取最新的故事详情，包括可能已经生成的图片
          const response = await fetch(`${BASE_URL}/api/story/${storyId}`, {
            method: 'GET',
            credentials: 'include'
          });

          if (response.ok) {
            const result = await response.json();
            if (result.success && result.data) {
              // 找到对应的故事消息并更新信息
              const storyMessage = this.messages.find(msg => msg.isStory && msg.sender === 'assistant');
              if (storyMessage) {
                // 更新图片URL
                if (result.data.imageUrl) {
                  storyMessage.imageUrl = result.data.imageUrl.replace(/[`\s]/g, '');
                }
                // 更新音频URL
                if (result.data.audioUrl) {
                  storyMessage.audioUrl = result.data.audioUrl;
                }
                // 保存原始故事数据
                storyMessage.rawData = result.data;
              }
            }
          }
        } catch (error) {
          console.error('获取最新故事信息失败:', error);
          // 这里不显示错误，因为这是可选的优化
        }
      }
    },

    // 初始化进度条
    initProgressBar() {
      // 清除之前的定时器
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
      }
      
      // 重置进度条
      this.showProgressBar = true;
      this.progressValue = 0;
      
      // 设置定时器模拟进度增长
      this.progressTimer = setInterval(() => {
        // 每次增加一个随机值（1-5%），但不超过90%
        const increment = Math.floor(Math.random() * 5) + 1;
        if (this.progressValue < 90) {
          this.progressValue = Math.min(90, this.progressValue + increment);
        }
      }, 300);
    },
    
    // 完成进度条
    completeProgressBar() {
      // 清除定时器
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
        this.progressTimer = null;
      }
      
      // 快速将进度设置为100%
      this.progressValue = 100;
      
      // 延迟隐藏进度条
      setTimeout(() => {
        this.showProgressBar = false;
      }, 500);
    },

    // 手动获取故事图片
    async fetchStoryImage(event) {
      // 防止事件对象被错误地作为storyId
      let storyId;
      if (event && event instanceof Event) {
        // 如果是事件对象，从sessionStorage获取storyId
        storyId = sessionStorage.getItem('currentStoryId');
      } else {
        // 如果是直接传递的storyId
        storyId = event; // 这里event实际上是storyId参数
      }

      if (!storyId) {
        this.showError('无法获取图片：未找到故事ID');
        return;
      }

      this.isLoadingImage = true;
      // 初始化进度条
      this.initProgressBar();

      try {
        // 调用后端API生成图片并获取图片URL
        console.log('正在为故事ID:', storyId, '生成图片');
        const response = await fetch(`${BASE_URL}/api/story/image/${storyId}`, {
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
          // 完成进度条
          this.completeProgressBar();
          
          // 直接从响应中获取图片URL
          if (result.data && result.data.imageUrl) {
            // 找到对应的故事消息并更新图片URL
            const storyMessage = this.messages.find(msg => msg.isStory && msg.sender === 'assistant');
            if (storyMessage) {
              storyMessage.imageUrl = result.data.imageUrl.replace(/[`\s]/g, '');
            }
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
        // 如果出错，隐藏进度条
        if (this.progressValue < 100) {
          if (this.progressTimer) {
            clearInterval(this.progressTimer);
            this.progressTimer = null;
          }
          this.showProgressBar = false;
        }
      }
    },

    // 重新生成故事图片
    async regenerateStoryImage(event) {
      // 防止事件对象被错误地作为storyId
      let storyId;
      if (event && event instanceof Event) {
        // 如果是事件对象，从sessionStorage获取storyId
        storyId = sessionStorage.getItem('currentStoryId');
      } else {
        // 如果是直接传递的storyId
        storyId = event; // 这里event实际上是storyId参数
      }

      if (!storyId) {
        this.showError('无法重新生成图片：未找到故事ID');
        return;
      }

      this.isLoadingImage = true;
      // 初始化进度条
      this.initProgressBar();

      try {
        // 调用后端API重新生成图片，添加regenerate参数
        console.log('正在为故事ID:', storyId, '重新生成图片');
        const response = await fetch(`${BASE_URL}/api/story/image/${storyId}?regenerate=true`, {
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
        console.log('图片重新生成结果:', result);

        if (result.success) {
          // 完成进度条
          this.completeProgressBar();
          
          // 直接从响应中获取图片URL
          if (result.data && result.data.imageUrl) {
            // 找到对应的故事消息并更新图片URL
            const storyMessage = this.messages.find(msg => msg.isStory && msg.sender === 'assistant');
            if (storyMessage) {
              storyMessage.imageUrl = result.data.imageUrl.replace(/[`\s]/g, '');
            }
            ElMessage.success('成功重新生成故事图片');
          } else {
            ElMessage.success('图片重新生成成功');
          }
        } else {
          this.showError(result.message || result.msg || '图片重新生成失败');
        }
      } catch (error) {
        this.showError('重新生成图片失败，请稍后重试');
        console.error('重新生成故事图片时出错:', error);
      } finally {
        this.isLoadingImage = false;
        // 如果出错，隐藏进度条
        if (this.progressValue < 100) {
          if (this.progressTimer) {
            clearInterval(this.progressTimer);
            this.progressTimer = null;
          }
          this.showProgressBar = false;
        }
      }
    },

    // 修改：生成并播放故事语音 - 增强错误处理
    async generateAndPlayAudio(event) {
      // 防止事件对象被错误地作为storyId
      let storyId;
      if (event && event instanceof Event) {
        // 如果是事件对象，从sessionStorage获取storyId
        storyId = sessionStorage.getItem('currentStoryId');
      } else {
        // 如果是直接传递的storyId
        storyId = event;
      }

      if (!storyId) {
        this.showError('无法生成语音：未找到故事ID');
        return;
      }

      // 获取对应的故事消息
      const storyMessage = this.messages.find(msg => msg.isStory && msg.sender === 'assistant');
      if (!storyMessage) {
        this.showError('未找到故事消息');
        return;
      }

      // 如果已经有音频URL，直接播放
      if (storyMessage.audioUrl && !this.isLoadingAudio) {
        // 先停止所有其他音频
        this.stopAllAudio();

        await this.$nextTick(() => {
          const audioElement = document.getElementById('audio-player');
          if (audioElement) {
            // 保存当前音频元素引用
            this.currentAudioElement = audioElement;
            audioElement.play().catch(err => {
              console.warn('播放失败:', err);
              ElMessage.warning('请点击音频播放器开始播放');
            });
          }
        });
        return;
      }

      this.isLoadingAudio = true;

      try {
        // 清理之前的音频资源
        this.cleanupAudio();

        // 创建MediaSource对象
        this.mediaSource = new MediaSource();
        // 创建音频URL
        this.audioUrl = URL.createObjectURL(this.mediaSource);

        // 找到对应的故事消息并更新音频URL
        const currentStoryMessage = this.messages.find(msg => msg.isStory && msg.sender === 'assistant');
        if (currentStoryMessage) {
          currentStoryMessage.audioUrl = this.audioUrl;
        }

        // 监听MediaSource的sourceopen事件
        this.mediaSource.addEventListener('sourceopen', async () => {
          try {
            // 创建音频轨道 - 使用更通用的MIME类型以提高兼容性
            this.sourceBuffer = this.mediaSource.addSourceBuffer('audio/mpeg');

            // 设置sourceBuffer的模式为sequence，确保数据块按顺序添加
            if (this.sourceBuffer.mode === 'segments') {
              this.sourceBuffer.mode = 'sequence';
            }

            // 调用流式语音合成接口
            console.log('正在为故事ID:', storyId, '生成流式语音');
            const response = await fetch(`${BASE_URL}/api/tts/story-stream/${storyId}`, {
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
            this.isProcessingAudio = false;
            this.audioQueue = [];
            let totalBytesReceived = 0;

            const processQueue = () => {
              if (this.isProcessingAudio || this.audioQueue.length === 0) return;

              this.isProcessingAudio = true;

              try {
                if (!this.sourceBuffer.updating) {
                  const chunk = this.audioQueue.shift();
                  try {
                    this.sourceBuffer.appendBuffer(chunk);
                    totalBytesReceived += chunk.byteLength;
                    console.log('添加音频数据块，大小:', chunk.byteLength, '字节，累计:', totalBytesReceived, '字节');
                  } catch (appendError) {
                    console.error('添加音频数据块失败:', appendError);
                    // 如果添加失败，尝试清空队列并重新开始
                    if (this.mediaSource.readyState === 'open') {
                      this.audioQueue.length = 0;
                    }
                  }
                }
              } finally {
                this.isProcessingAudio = false;
                if (this.audioQueue.length > 0) {
                  // 使用setTimeout避免递归调用导致的栈溢出
                  setTimeout(processQueue, 0);
                }
              }
            };

            // 监听sourceBuffer的update事件，继续处理队列
            this.sourceBuffer.addEventListener('update', () => {
              processQueue();
            });

            // 监听sourceBuffer的error事件
            this.sourceBuffer.addEventListener('error', (e) => {
              console.error('sourceBuffer错误:', e);
              // 尝试清空队列
              this.audioQueue.length = 0;
            });

            // 监听sourceBuffer的abort事件
            this.sourceBuffer.addEventListener('abort', () => {
              console.warn('sourceBuffer被中止');
            });

            // 自动开始播放音频
            await this.$nextTick(() => {
              const audioElement = document.getElementById('audio-player');
              if (audioElement) {
                // 保存当前音频元素引用
                this.currentAudioElement = audioElement;

                // 添加播放结束监听器
                audioElement.addEventListener('ended', () => {
                  console.log('音频播放结束');
                  this.currentAudioElement = null;
                });

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
                  if (this.audioQueue.length === 0 && !this.isProcessingAudio) {
                    if (this.mediaSource.readyState === 'open') {
                      console.log('所有音频数据处理完成，标记MediaSource结束');
                      this.mediaSource.endOfStream();
                    }
                  } else {
                    setTimeout(waitForQueueEmpty, 100);
                  }
                };
                waitForQueueEmpty();
                break;
              }

              // 将接收到的数据块添加到队列
              this.audioQueue.push(value);
              processQueue();
            }
          } catch (streamError) {
            console.error('流式音频处理错误:', streamError);
            if (this.mediaSource.readyState === 'open') {
              this.mediaSource.endOfStream('network');
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

    // 显示错误信息
    showError(message) {
      this.errorMessage = message;
      ElMessage.error(message);
      // 3秒后自动清除错误信息
      this.timer = setTimeout(() => {
        this.errorMessage = '';
      }, 3000);
    },

    // 清除错误信息
    clearError() {
      this.errorMessage = '';
      if (this.timer) {
        clearTimeout(this.timer);
        this.timer = null;
      }
    },

    // 修改：删除故事方法 - 优化滚动位置保持
    async deleteStory(storyId) {
      // 查找对应的故事标题
      const story = this.conversations.find(conv => conv.id === storyId);
      const storyTitle = story ? story.title : '未命名故事';

      // 保存当前滚动位置
      const previousScrollTop = this.storyListScrollTop;

      try {
        // 使用Element UI的MessageBox对话框代替原生confirm
        await this.$confirm(
            `您确定要删除"${storyTitle}"这个故事吗？`,
            '删除确认',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
        );

        // 调用后端删除接口
        const response = await fetch(`${BASE_URL}/api/story/${storyId}`, {
          method: 'DELETE',
          credentials: 'include'
        });

        const result = await response.json();

        if (response.ok && result.success) {
          // 删除成功，更新前端状态
          // 从conversations数组中移除指定ID的故事
          this.conversations = this.conversations.filter(conv => conv.id !== storyId);

          // 如果删除的是当前选中的故事，切换到其他故事或清空
          if (this.selectedConversation === storyId) {
            if (this.conversations.length > 0) {
              await this.selectConversation(this.conversations[0].id);
            } else {
              this.selectedConversation = null;
              this.currentConversation = null;
              this.messages = [];
            }
          }

          ElMessage.success('故事删除成功');

          // 恢复滚动位置
          await this.$nextTick(() => {
            this.storyListScrollTop = previousScrollTop;
            this.restoreStoryListScrollPosition();
          });
        } else {
          // 删除失败
          this.showError(result.message || '故事删除失败');
        }
      } catch (error) {
        // 捕获MessageBox的取消或关闭操作
        if (error !== 'cancel' && error !== 'close') {
          console.error('删除故事时出错:', error);
          this.showError('删除失败，请稍后重试');
        }
      }
    },

    // 新增方法：创建新故事
    newStory() {
      // 停止当前音频
      this.stopAllAudio();

      // 清空当前消息列表
      this.messages = [];

      // 重置当前对话状态
      this.selectedConversation = null;
      this.currentConversation = null;

      // 清空输入框
      this.inputMessage = '';
    },

    // 发送示例提示
    sendPrompt(prompt) {
      this.inputMessage = prompt;
      this.sendMessage();
    },

    // 处理回车键
    handleEnterKey(event) {
      if (!event.shiftKey) {
        this.sendMessage();
      }
    },

    // 新增：切换侧边栏
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed;
    },

    // 新增：切换输入框
    toggleInput() {
      this.isInputCollapsed = !this.isInputCollapsed;
    }
  }
};
</script>

<style scoped>
/* 全局样式重置 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.story-app {
  display: flex;
  height: 100vh;
  background-color: #f8fafc;
  font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  color: #334155;
  overflow: hidden;
}

/* 故事侧边栏 */
.story-sidebar {
  width: 320px;
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

/* 侧边栏收起状态 */
.story-sidebar.collapsed {
  width: 60px;
}

.story-sidebar.collapsed .sidebar-header {
  padding: 15px 5px;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  transition: padding 0.3s cubic-bezier(0.4, 0, 0.2, 1), gap 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-header {
  transition: padding 0.3s cubic-bezier(0.4, 0, 0.2, 1), gap 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.story-sidebar.collapsed .app-logo h1,
.story-sidebar.collapsed .search-container,
.story-sidebar.collapsed .story-list,
.story-sidebar.collapsed .user-profile {
  opacity: 0;
  transform: translateX(-10px);
  pointer-events: none;
}

/* 为内部元素添加平滑过渡效果 */
.app-logo h1,
.search-container,
.story-list,
.user-profile {
  transition: opacity 0.25s ease, transform 0.25s ease;
  transform: translateX(0);
  opacity: 1;
}

/* 收缩状态下的新故事按钮 - 只显示图标 */
.story-sidebar.collapsed .new-story-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  margin: 0 auto;
  border-radius: 8px;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
  border: 1px solid #bbdefb;
  font-size: 18px;
}

/* 新故事按钮文字的过渡效果 */
.story-sidebar.collapsed .new-story-btn .btn-text {
  opacity: 0;
  width: 0;
  overflow: hidden;
}

.new-story-btn {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.new-story-btn .btn-text {
  transition: opacity 0.2s ease, width 0.3s ease;
  opacity: 1;
  width: auto;
}

.story-sidebar.collapsed .sidebar-toggle-btn {
  position: static;
  margin: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: position 0.3s cubic-bezier(0.4, 0, 0.2, 1), margin 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 优化切换按钮的图标过渡 */
.sidebar-toggle-btn {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 收缩状态下的logo图标调整 */
.story-sidebar.collapsed .app-logo {
  justify-content: center;
  margin-bottom: 0;
  gap: 0;
  transition: gap 0.3s cubic-bezier(0.4, 0, 0.2, 1), margin 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.story-sidebar.collapsed .logo-icon {
  font-size: 24px;
  transition: font-size 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-logo {
  transition: gap 0.3s cubic-bezier(0.4, 0, 0.2, 1), margin 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.logo-icon {
  transition: font-size 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 侧边栏头部 */
.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
}

/* 确保logo和标题在展开状态下布局合理 */
.app-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 0;
  padding-right: 40px; /* 为切换按钮留出空间 */
}

/* 侧边栏切换按钮 - 展开状态 */
.sidebar-toggle-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 28px;
  height: 28px;
  border: none;
  background-color: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #64748b;
  transition: all 0.2s ease;
  z-index: 10;
}

.sidebar-toggle-btn:hover {
  background-color: #f1f5f9;
  color: #1976d2;
  transform: scale(1.05);
}

.app-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.logo-icon {
  font-size: 28px;
}

.app-logo h1 {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.new-story-btn {
  width: 100%;
  padding: 12px 16px;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
  border: 2px solid #bbdefb;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.new-story-btn:hover {
  background: linear-gradient(135deg, #bbdefb 0%, #90caf9 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(187, 222, 251, 0.4);
}

/* 搜索容器 */
.search-container {
  padding: 16px 20px 16px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  background-color: #f1f5f9;
  border-radius: 8px;
  padding: 0 12px;
}

.search-icon {
  color: #94a3b8;
  font-size: 16px;
}

.search-input {
  flex: 1;
  padding: 10px 12px;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
  color: #334155;
}

.clear-search {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.clear-search:hover {
  background-color: #e2e8f0;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #64748b;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 故事列表 */
.story-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.story-item {
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
  position: relative;
}

.story-item:hover {
  background-color: #f8fafc;
}

.story-item.active {
  background-color: #f0f9ff;
  border-left-color: #3b82f6;
}

.story-preview {
  margin-bottom: 8px;
}

.story-item-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.story-item-preview {
  font-size: 14px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.story-item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.story-time {
  font-size: 12px;
  color: #94a3b8;
}

.story-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.2s;
  font-size: 16px;
}

.story-item:hover .action-btn {
  opacity: 1;
}

.action-btn:hover {
  background-color: #f1f5f9;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin-bottom: 8px;
  font-size: 16px;
}

.empty-hint {
  font-size: 14px;
  opacity: 0.7;
}

/* 用户资料 */
.user-profile {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-weight: 500;
  color: #1e293b;
}

.logout-btn {
  padding: 6px 12px;
  background-color: #f1f5f9;
  border: none;
  border-radius: 6px;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  align-self: flex-start;
  transition: all 0.2s;
}

.logout-btn:hover {
  background-color: #e2e8f0;
  color: #334155;
}

/* 主内容区域 */
.story-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 输入框收起状态 */
.story-main.input-collapsed {
  margin-bottom: 0;
}

.story-main.input-collapsed .story-input-area {
  height: 0;
  padding: 0;
  overflow: hidden;
  border: none;
}

/* 故事头部 */
.story-header {
  padding: 20px 32px;
  background-color: white;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 输入框切换按钮 */
.input-toggle-btn {
  width: 28px;
  height: 28px;
  border: none;
  background-color: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #64748b;
  transition: all 0.2s ease;
  margin-left: 8px;
}

.input-toggle-btn:hover {
  background-color: #f1f5f9;
  color: #1976d2;
  transform: scale(1.05);
}

.current-story-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.story-actions-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 故事媒体控制区域 */
.story-media-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 头部音频播放器 */
.header-audio-player {
  min-width: 200px;
}

.header-audio-player .audio-player {
  width: 100%;
  height: 32px;
  border-radius: 16px;
  background-color: white;
  border: 1px solid #e2e8f0;
}

.header-action {
  width: 40px;
  height: 40px;
  border: none;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-action:hover {
  background-color: #e2e8f0;
  transform: translateY(-1px);
}

/* 内容区域 */
.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 40px;
  background-color: #f8fafc;
}

/* 欢迎视图 */
.welcome-view {
  max-width: 600px;
  margin: 100px auto;
  text-align: center;
  padding: 60px 40px;
  background-color: white;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.05);
}

.welcome-illustration {
  font-size: 80px;
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.welcome-view h3 {
  font-size: 28px;
  color: #1e293b;
  margin-bottom: 16px;
  font-weight: 700;
}

.welcome-view p {
  font-size: 18px;
  color: #64748b;
  margin-bottom: 32px;
  line-height: 1.6;
}

.prompt-suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

.prompt-tag {
  padding: 12px 20px;
  background-color: #f0f9ff;
  color: #0284c7;
  border-radius: 25px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #bae6fd;
  white-space: nowrap;
}

.prompt-tag:hover {
  background-color: #0284c7;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.3);
}

/* 故事消息容器 */
.story-messages {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.message-wrapper {
  display: flex;
  width: 100%;
  justify-content: flex-start;
}

.story-card {
  width: 100%;
  background-color: white;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  border: 1px solid #e2e8f0;
}


.generating-state p {
  font-size: 16px;
}

/* 故事卡片内容 */
.story-card-content {
  padding: 32px;
}

.story-card-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 3px solid #4f46e5;
}

/* 故事视觉区域 */
.story-visual {
  margin-bottom: 32px;
  position: relative;
}

.story-image-wrapper {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.story-image-wrapper:hover {
  transform: translateY(-4px);
}

.story-card-image {
  width: 100%;
  height: auto;
  max-height: 600px; /* 增加最大高度限制 */
  object-fit: contain; /* 改为contain以完整显示图片 */
  display: block;
  background-color: #f5f5f5; /* 添加背景色避免空白区域过于突兀 */
}

.image-overlay {
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.story-image-wrapper:hover .image-overlay {
  opacity: 1;
}

.image-action {
  background-color: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
}

.image-action:hover {
  background-color: white;
  transform: scale(1.1);
}

/* 故事详情区域 */
.story-details {
  margin-bottom: 24px;
}

.story-outline-section {
  margin-bottom: 24px;
  background-color: #f8fafc;
  padding: 20px;
  border-radius: 12px;
  border-left: 4px solid #0ea5e9;
}

.story-body-section {
  background-color: white;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 20px;
  background-color: #4f46e5;
  border-radius: 2px;
}

.outline-text {
  font-size: 16px;
  color: #64748b;
  line-height: 1.7;
}

.story-text {
  font-size: 16px;
  color: #334155;
  line-height: 1.8;
}

.story-text p {
  line-height: 1.8;
  margin-bottom: 16px;
}

.story-text p:last-child {
  margin-bottom: 0;
}

/* 输入区域 */
.story-input-area {
  background-color: white;
  border-top: 1px solid #e2e8f0;
  padding: 24px 32px;
}

.keyword-input-section {
  max-width: 800px;
  margin: 0 auto;
}

.input-label {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 12px;
}

.input-container {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.keyword-textarea {
  flex: 1;
  min-height: 80px;
  max-height: 160px;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 16px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: all 0.3s ease;
  font-family: inherit;
}

.keyword-textarea:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
}

.generate-btn {
  padding: 16px 32px;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
  border: 2px solid #bbdefb;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 56px;
  white-space: nowrap;
}

.generate-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #bbdefb 0%, #90caf9 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(187, 222, 251, 0.4);
}

.generate-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.generating-text {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.generating-text::after {
  content: '';
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .story-sidebar {
    width: 280px;
  }

  .content-area {
    padding: 30px 20px;
  }

  .story-card-content {
    padding: 24px;
  }
}

@media (max-width: 768px) {
  .story-app {
    position: relative;
  }

  .story-sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 100%;
    transform: translateX(-100%);
    z-index: 100;
  }

  .story-main {
    width: 100%;
  }

  .story-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .current-story-title {
    font-size: 20px;
  }

  .story-actions-header {
    width: 100%;
    justify-content: space-between;
  }

  .story-media-controls {
    margin-right: auto; /* 向左对齐 */
    gap: 16px; /* 增加按钮之间的间距 */
  }

  .header-audio-player {
    min-width: 150px;
  }

  .content-area {
    padding: 20px 16px;
  }

  .welcome-view {
    margin: 40px auto;
    padding: 40px 20px;
  }

  .story-card-content {
    padding: 20px;
  }

  .story-card-title {
    font-size: 24px;
  }

  .story-input-area {
    padding: 16px 20px;
  }

  .input-container {
    flex-direction: column;
    align-items: stretch;
  }

  .generate-btn {
    align-self: stretch;
    min-height: 48px;
  }
}

/* 滚动条样式 */
.story-list::-webkit-scrollbar,
.content-area::-webkit-scrollbar {
  width: 6px;
}

.story-list::-webkit-scrollbar-track,
.content-area::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.story-list::-webkit-scrollbar-thumb,
.content-area::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.story-list::-webkit-scrollbar-thumb:hover,
.content-area::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 生成中指示器 */
.generating-indicator {
  font-size: 14px;
  color: #4CAF50;
  margin-left: 10px;
  font-weight: normal;
}

/* 流式生成指示器 */
.streaming-indicator {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 10px 0;
}

.streaming-dots {
  display: flex;
  gap: 6px;
}

.streaming-dots span {
  width: 8px;
  height: 8px;
  background-color: #4CAF50;
  border-radius: 50%;
  animation: pulse 1.4s infinite ease-in-out both;
}

.streaming-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.streaming-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes pulse {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.6;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 主题选择功能样式 */

/* 主题设置按钮 */
.theme-settings-btn {
  width: 40px;
  height: 40px;
  border: none;
  background-color: #f1f5f9;
  border-radius: 8px;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.theme-settings-btn:hover {
  background-color: #e2e8f0;
  transform: translateY(-1px);
}

/* 主题弹窗遮罩 */
.theme-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

/* 主题弹窗 */
.theme-dialog {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

/* 弹窗头部 */
.theme-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.theme-dialog-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.dialog-close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.dialog-close-btn:hover {
  background-color: #f1f5f9;
  color: #dc2626;
}

/* 弹窗内容 */
.theme-dialog-content {
  padding: 24px;
  max-height: 400px;
  overflow-y: auto;
}

/* 主题模式选择 */
.theme-mode-section {
  margin-bottom: 24px;
}

.theme-mode-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.mode-options {
  display: flex;
  gap: 16px;
}

.mode-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.mode-option:hover {
  background-color: #f8fafc;
}

.mode-option input[type="radio"] {
  margin: 0;
}

.mode-option span {
  font-size: 14px;
  color: #475569;
}

/* 主题选择区域 */
.theme-selection-section {
  margin-bottom: 24px;
}

.theme-selection-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.theme-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
}

.theme-card:hover {
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.theme-card.selected {
  border-color: #1976d2;
  background-color: #f0f9ff;
}

.theme-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.theme-card.disabled:hover {
  border-color: #e2e8f0;
  transform: none;
}

.theme-icon {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border-radius: 6px;
}

.theme-info {
  flex: 1;
}

.theme-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.theme-description {
  font-size: 12px;
  color: #64748b;
  line-height: 1.3;
}

/* 自定义主题区域 */
.custom-theme-section {
  margin-bottom: 24px;
}

.custom-theme-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.custom-theme-input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  transition: border-color 0.2s ease;
}

.custom-theme-input:focus {
  outline: none;
  border-color: #1976d2;
}

/* 当前选择显示 */
.current-selection-section {
  margin-bottom: 24px;
}

.current-selection-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.selected-themes-display {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.selected-themes-text {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.no-selection {
  font-size: 14px;
  color: #64748b;
  font-style: italic;
}

/* 弹窗底部 */
.theme-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e2e8f0;
}

.dialog-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dialog-btn.secondary {
  background: #f1f5f9;
  color: #475569;
}

.dialog-btn.secondary:hover {
  background: #e2e8f0;
}

.dialog-btn.primary {
  background: #1976d2;
  color: white;
}

.dialog-btn.primary:hover {
  background: #1565c0;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .theme-dialog {
    width: 95%;
    margin: 20px;
  }
  
  .theme-grid {
    grid-template-columns: 1fr;
  }
  
  .mode-options {
    flex-direction: column;
    gap: 8px;
  }
  
  .theme-dialog-content {
    padding: 16px;
  }
  
  .theme-dialog-header {
    padding: 16px 20px;
  }
  
  .theme-dialog-footer {
    padding: 16px 20px;
  }
}
</style>