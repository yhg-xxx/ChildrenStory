<template>
  <div class="login-container">
    <!-- 动态背景 -->
    <div class="background-animation">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
    </div>

    <div class="login-content">
      <!-- 顶部Logo -->
      <div class="logo-container">
        <div class="logo">
          <span class="logo-icon">📚</span>
          童话王国
        </div>
        <p class="logo-subtitle">开启童话创作之旅</p>
      </div>

      <!-- 登录卡片 -->
      <div class="login-card">
        <!-- 卡片装饰 -->
        <div class="card-decoration"></div>

        <!-- 标签页切换 -->
        <div class="tabs">
          <div class="tab active">密码登录</div>
          <div class="tab">注册账号</div>
        </div>

        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <div class="input-container">
              <span class="input-icon">👤</span>
              <input
                  type="text"
                  v-model="username"
                  placeholder="请输入用户名"
                  class="form-input"
                  required
                  @focus="inputFocus('username')"
                  @blur="inputBlur('username')"
                  :class="{ 'has-value': username }"
              />
            </div>
          </div>

          <div class="form-group">
            <div class="input-container">
              <span class="input-icon">🔒</span>
              <input
                  :type="showPassword ? 'text' : 'password'"
                  v-model="password"
                  placeholder="请输入密码"
                  class="form-input"
                  required
                  @focus="inputFocus('password')"
                  @blur="inputBlur('password')"
                  :class="{ 'has-value': password }"
              />
              <button
                  type="button"
                  class="password-toggle"
                  @click="togglePasswordVisibility"
              >
                {{ showPassword ? '👁️' : '👁️‍🗨️' }}
              </button>
            </div>
          </div>

          <!-- 记住密码和忘记密码 -->
          <div class="form-options">
            <label class="remember-me">
              <input type="checkbox" v-model="rememberMe" />
              <span class="checkmark"></span>
              记住密码
            </label>
            <a href="#" class="forgot-password">忘记密码？</a>
          </div>

          <div class="form-group">
            <button
                type="submit"
                class="login-btn"
                :disabled="isLoading"
                :class="{ 'loading': isLoading }"
            >
              <span class="btn-text">{{ isLoading ? '登录中...' : '立即登录' }}</span>
              <span class="btn-spinner" v-if="isLoading"></span>
            </button>
          </div>
        </form>

        <!-- 其他登录方式 -->
        <div class="other-login">
          <div class="divider">
            <span class="divider-text">或使用以下方式登录</span>
          </div>

          <div class="third-party-login">
            <div class="login-methods">
              <button class="login-method wechat">
                <span class="method-icon">💬</span>
                <span class="method-text">微信登录</span>
              </button>
              <button class="login-method qq">
                <span class="method-icon">🐧</span>
                <span class="method-text">QQ登录</span>
              </button>
            </div>

            <!-- 二维码区域 -->
            <div class="qrcode-section" v-if="showWechatQR">
              <div class="qrcode-container">
                <div class="qrcode">
                  <div class="qrcode-placeholder">
                    <div class="qr-animation">
                      <div class="qr-line"></div>
                    </div>
                    <div class="qr-text">微信扫码登录</div>
                  </div>
                </div>
                <div class="qrcode-tips">
                  <p>请使用微信扫描二维码登录</p>
                  <p>首次使用需绑定账号</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 隐私协议 -->
        <div class="privacy-agreement">
          登录即代表您同意我们的
          <a href="#" class="privacy-link">用户协议</a>
          和
          <a href="#" class="privacy-link">隐私政策</a>
        </div>
      </div>

      <!-- 底部版权信息 -->
      <div class="footer">
        <p>© 2025 童话王国 儿童故事管理平台 · 浙ICP备20251102号-1</p>
      </div>
    </div>
  </div>
</template>

<script>
import { BASE_URL } from '../main.js'
export default {
  name: 'Login',
  data() {
    return {
      username: '',
      password: '',
      isLoading: false,
      showPassword: false,
      rememberMe: false,
      showWechatQR: false,
      activeInput: null
    };
  },

  mounted() {
    // 尝试从本地存储加载记住的用户名
    const savedUsername = localStorage.getItem('savedUsername');
    if (savedUsername) {
      this.username = savedUsername;
      this.rememberMe = true;
    }
  },

  methods: {

    // 处理登录
    async handleLogin() {
      if (!this.username || !this.password) {
        this.showError('请填写完整信息');
        return;
      }

      this.isLoading = true;

      try {
        const response = await fetch(`${BASE_URL}/api/users/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          credentials: 'include',
          body: new URLSearchParams({
            username: this.username,
            password: this.password
          })
        });

        if (response.ok) {
            const user = await response.json();
            console.log('登录成功:', user);

            // 保存用户信息
            localStorage.setItem('username', user.username);

            // 如果勾选了记住密码，保存用户名
            if (this.rememberMe) {
              localStorage.setItem('savedUsername', this.username);
            } else {
              localStorage.removeItem('savedUsername');
            }

            // 登录成功后，直接跳转到目标页面
            const redirect = this.$route.query.redirect || '/chat';
            this.$router.push(redirect);

        } else if (response.status === 401) {
          this.showError('用户名或密码错误');
        } else {
          this.showError('登录失败，请稍后重试');
        }
      } catch (error) {
        console.error('登录失败:', error);
        this.showError('网络错误，请检查连接后重试');
      } finally {
        this.isLoading = false;
      }
    },

    // 切换密码可见性
    togglePasswordVisibility() {
      this.showPassword = !this.showPassword;
    },

    // 输入框聚焦效果
    inputFocus(field) {
      this.activeInput = field;
    },

    // 输入框失去焦点效果
    inputBlur(field) {
      this.activeInput = null;
    },

    // 显示错误提示
    showError(message) {
      // 这里可以替换为更优雅的提示组件
      alert(message);
    },

    // 显示成功提示
    showSuccess(message) {
      // 这里可以替换为更优雅的提示组件
      alert(message);
    }
  }
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  position: relative;
  overflow: hidden;
}

.background-animation {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  z-index: 0;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  animation: float 15s infinite ease-in-out;
}

.shape-1 {
  width: 100px;
  height: 100px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 150px;
  height: 150px;
  top: 60%;
  right: 10%;
  animation-delay: 5s;
}

.shape-3 {
  width: 70px;
  height: 70px;
  bottom: 20%;
  left: 20%;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

.login-content {
  width: 100%;
  max-width: 450px;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 1;
}

.logo-container {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: bold;
  color: #1976d2;
  margin-bottom: 10px;
}

.logo-icon {
  font-size: 40px;
  margin-right: 10px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.logo-subtitle {
  font-size: 16px;
  color: #42a5f5;
  margin: 0;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  width: 100%;
  padding: 40px 30px;
  position: relative;
  overflow: hidden;
}

.card-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 0 0 100px;
  opacity: 0.1;
}

/* 标签页样式 */
.tabs {
  display: flex;
  margin-bottom: 30px;
  border-bottom: 1px solid #e0e0e0;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 15px 0;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  transition: all 0.3s;
  border-bottom: 3px solid transparent;
  font-weight: 500;
}

.tab.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

/* 表单样式 */
.login-form {
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 15px;
  font-size: 18px;
  z-index: 2;
  transition: all 0.3s;
}

.form-input {
  width: 100%;
  padding: 15px 15px 15px 50px;
  border: 2px solid #f0f0f0;
  border-radius: 10px;
  font-size: 16px;
  transition: all 0.3s;
  box-sizing: border-box;
  background: #fafafa;
  position: relative;
  z-index: 1;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input.has-value {
  background: white;
}

.password-toggle {
  position: absolute;
  right: 15px;
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  z-index: 2;
  padding: 5px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.password-toggle:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

/* 表单选项 */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  font-size: 14px;
}

.remember-me {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #666;
}

.remember-me input {
  display: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid #ddd;
  border-radius: 4px;
  margin-right: 8px;
  position: relative;
  transition: all 0.3s;
}

.remember-me input:checked + .checkmark {
  background-color: #667eea;
  border-color: #667eea;
}

.remember-me input:checked + .checkmark::after {
  content: '✓';
  position: absolute;
  color: white;
  font-size: 12px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.forgot-password {
  color: #667eea;
  text-decoration: none;
  transition: color 0.3s;
}

.forgot-password:hover {
  color: #5a6fd8;
  text-decoration: underline;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.login-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.login-btn.loading {
  pointer-events: none;
}

.btn-text {
  position: relative;
  z-index: 1;
}

.btn-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid transparent;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-left: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 其他登录方式 */
.other-login {
  margin-bottom: 25px;
}

.divider {
  position: relative;
  text-align: center;
  margin: 25px 0;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e0e0e0;
}

.divider-text {
  display: inline-block;
  background: white;
  padding: 0 15px;
  color: #999;
  font-size: 14px;
  position: relative;
}

.login-methods {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-bottom: 20px;
}

.login-method {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.login-method:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.login-method.wechat:hover {
  border-color: #07c160;
  background: rgba(7, 193, 96, 0.05);
}

.login-method.qq:hover {
  border-color: #12b7f5;
  background: rgba(18, 183, 245, 0.05);
}

.method-icon {
  font-size: 18px;
  margin-right: 8px;
}

.method-text {
  font-size: 14px;
  color: #333;
}

/* 二维码区域 */
.qrcode-section {
  margin-top: 20px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 10px;
  text-align: center;
}

.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode {
  width: 180px;
  height: 180px;
  background: white;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 15px;
  position: relative;
  overflow: hidden;
}

.qr-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-line {
  width: 80%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #667eea, transparent);
  animation: scan 2s infinite;
}

@keyframes scan {
  0% { transform: translateY(-40px); }
  50% { transform: translateY(40px); }
  100% { transform: translateY(-40px); }
}

.qr-text {
  font-size: 14px;
  color: #666;
}

.qrcode-tips p {
  margin: 5px 0;
  font-size: 12px;
  color: #999;
}

/* 隐私协议 */
.privacy-agreement {
  text-align: center;
  font-size: 12px;
  color: #999;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.privacy-link {
  color: #667eea;
  text-decoration: none;
}

.privacy-link:hover {
  text-decoration: underline;
}

/* 页脚 */
.footer {
  margin-top: 40px;
  text-align: center;
}

.footer p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-content {
    padding: 20px 15px;
  }

  .login-card {
    padding: 30px 20px;
  }

  .logo {
    font-size: 28px;
  }

  .logo-icon {
    font-size: 32px;
  }

  .login-methods {
    flex-direction: column;
  }

  .login-method {
    justify-content: center;
  }

  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .forgot-password {
    align-self: flex-end;
  }
}
</style>