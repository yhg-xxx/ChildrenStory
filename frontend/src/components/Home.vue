<template>
  <div class="home-container">
    <!-- 头部导航 -->
    <header class="main-header">
      <div class="header-content">
        <div class="logo-container">
          <div class="logo">
            <span class="logo-icon">📚</span>
            童话王国
          </div>
        </div>
        <nav class="main-nav">
          <ul>
            <li><a href="#features" class="nav-link">功能特点</a></li>
            <li><a href="#about" class="nav-link">关于我们</a></li>
            <li><a href="#contact" class="nav-link">联系我们</a></li>
          </ul>
        </nav>
        <div class="header-actions">
          <button class="login-btn" @click="checkLoginStatus">
            <span class="btn-icon">🔑</span>
            登录
          </button>
        </div>
        <div class="mobile-menu-toggle" @click="toggleMobileMenu">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>

      <!-- 移动端菜单 -->
      <div class="mobile-menu" :class="{ 'active': mobileMenuActive }">
        <ul>
          <li><a href="#features" @click="closeMobileMenu">功能特点</a></li>
          <li><a href="#about" @click="closeMobileMenu">关于我们</a></li>
          <li><a href="#contact" @click="closeMobileMenu">联系我们</a></li>
          <li><button @click="closeMobileMenu; checkLoginStatus()">登录</button></li>
        </ul>
      </div>
    </header>

    <!-- 主要内容区 -->
    <main class="main-content">
      <!-- 英雄区 -->
      <section class="hero-section">
        <div class="hero-background">
          <div class="floating-shape shape-1"></div>
          <div class="floating-shape shape-2"></div>
          <div class="floating-shape shape-3"></div>
        </div>
        <div class="hero-content">
          <h1 class="hero-title">
            <span class="title-word">童话</span>
            <span class="title-word">王国</span>
          </h1>
          <p class="subtitle">探索未至之境</p>
          <p class="description">集故事生成、插图创作、语音合成于一体<br/>为您的孩子打造专属童话故事体验</p>
          <div class="action-buttons">
              <button class="primary-btn" @click="checkLoginStatus">
                <span class="btn-sparkle">✨</span>
                开始创作
              </button>
            <button class="secondary-btn" @click="scrollToFeatures">
              了解更多
              <span class="btn-arrow">→</span>
            </button>
          </div>
        </div>

        <!-- 滚动指示器 -->
        <div class="scroll-indicator" @click="scrollToFeatures">
          <span>探索更多</span>
          <div class="indicator-arrow"></div>
        </div>
      </section>

      <!-- 功能特点 -->
      <section id="features" class="features-section">
        <div class="section-header">
          <h2>核心功能</h2>
          <p class="section-subtitle">专为儿童设计的智能故事创作平台</p>
        </div>
        <div class="features-grid">
          <div class="feature-card" v-for="(feature, index) in features" :key="index">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">{{ feature.icon }}</div>
            </div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
            <div class="feature-highlight"></div>
          </div>
        </div>
      </section>

      <!-- 新增：用户评价部分 -->
      <section class="testimonials-section">
        <div class="section-header">
          <h2>用户评价</h2>
          <p class="section-subtitle">听听家长们怎么说</p>
        </div>
        <div class="testimonials-grid">
          <div class="testimonial-card" v-for="(testimonial, index) in testimonials" :key="index">
            <div class="testimonial-content">
              <p>"{{ testimonial.content }}"</p>
            </div>
            <div class="testimonial-author">
              <div class="author-avatar">{{ testimonial.avatar }}</div>
              <div class="author-info">
                <h4>{{ testimonial.name }}</h4>
                <p>{{ testimonial.role }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="main-footer">
      <div class="footer-content">
        <div class="footer-brand">
          <div class="footer-logo">童话王国</div>
          <p class="footer-description">为孩子们创造美好的童话世界</p>
          <div class="social-links">
            <a href="#" class="social-link">微</a>
            <a href="#" class="social-link">Q</a>
            <a href="#" class="social-link">抖</a>
          </div>
        </div>
        <div class="footer-links">
          <div class="link-group">
            <h4>产品</h4>
            <ul>
              <li><a href="#">故事生成</a></li>
              <li><a href="#">插图创作</a></li>
              <li><a href="#">语音合成</a></li>
            </ul>
          </div>
          <div class="link-group">
            <h4>关于我们</h4>
            <ul>
              <li><a href="#">公司介绍</a></li>
              <li><a href="#">联系我们</a></li>
              <li><a href="#">加入我们</a></li>
            </ul>
          </div>
          <div class="link-group">
            <h4>法律</h4>
            <ul>
              <li><a href="#">隐私政策</a></li>
              <li><a href="#">用户协议</a></li>
              <li><a href="#">版权声明</a></li>
            </ul>
          </div>
        </div>
      </div>
      <div class="footer-copyright">
        <p>© 2025 童话王国 儿童故事管理平台. 保留所有权利.</p>
      </div>
    </footer>
  </div>
</template>

<script>
import { BASE_URL } from '../main.js'
export default {
  name: 'Home',
  data() {
    return {
      mobileMenuActive: false,
      features: [
        {
          icon: '📝',
          title: '智能故事生成',
          description: '通过关键词自动生成富有想象力的儿童故事，激发孩子的阅读兴趣'
        },
        {
          icon: '🎨',
          title: '精美插图创作',
          description: '为故事自动生成生动有趣的插图，让故事情节更加直观形象'
        },
        {
          icon: '🔊',
          title: '语音朗读合成',
          description: '将故事内容转换为流畅自然的语音朗读，方便孩子随时收听'
        }
      ],
      testimonials: [
        {
          content: '我的孩子现在每天晚上都要听童话王国生成的故事，这大大激发了他的想象力和阅读兴趣！',
          name: '张妈妈',
          role: '5岁孩子的家长',
          avatar: '👩'
        },
        {
          content: '作为幼儿园老师，我发现童话王国的故事非常适合课堂使用，孩子们都非常喜欢！',
          name: '李老师',
          role: '幼儿园教师',
          avatar: '👨‍🏫'
        },
        {
          content: '语音朗读功能太棒了，孩子可以在睡前自己听故事，解放了我们的时间！',
          name: '王爸爸',
          role: '7岁孩子的家长',
          avatar: '👨'
        }
      ]
    };
  },
  mounted() {
    // 添加滚动动画效果
    this.initScrollAnimations();
  },
  methods: {
    // 检查登录状态
    async checkLoginStatus() {
      try {
        const response = await fetch(`${BASE_URL}/api/users/checkSession`, {
          credentials: 'include'
        });

        if (response.ok) {
          // 用户已登录，在新标签页打开对话页（使用hash路由格式）
          window.open('/#/chat', '_blank');
        } else {
          // 用户未登录，在新标签页打开登录页（使用hash路由格式）
          window.open('/#/login', '_blank');
        }
      } catch (error) {
        console.log('未登录或Session已过期');
        // 出错时也跳转到登录页
        window.open('/#/login', '_blank');
      }
    },
    toggleMobileMenu() {
      this.mobileMenuActive = !this.mobileMenuActive;
    },
    closeMobileMenu() {
      this.mobileMenuActive = false;
    },
    scrollToFeatures() {
      const featuresSection = document.getElementById('features');
      if (featuresSection) {
        featuresSection.scrollIntoView({ behavior: 'smooth' });
      }
    },
    initScrollAnimations() {
      // 简单的滚动动画实现
      const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
      };

      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            entry.target.classList.add('animate-in');
          }
        });
      }, observerOptions);

      // 观察所有功能卡片和评价卡片
      document.querySelectorAll('.feature-card, .testimonial-card').forEach(el => {
        observer.observe(el);
      });
    }
  }
};
</script>

<style scoped>
/* 全局样式重置和基础设置 */
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  overflow-x: hidden;
}

/* 头部导航 */
.main-header {
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 1000;
  transition: all 0.3s ease;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 24px;
  font-weight: bold;
  color: #1976d2;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.05);
}

.logo-icon {
  margin-right: 8px;
  font-size: 28px;
}

.main-nav ul {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
}

.main-nav li {
  margin: 0 20px;
}

.nav-link {
  text-decoration: none;
  color: #333;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
  position: relative;
  padding: 8px 0;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background-color: #1976d2;
  transition: width 0.3s ease;
}

.nav-link:hover {
  color: #1976d2;
}

.nav-link:hover::after {
  width: 100%;
}

.login-btn {
  display: flex;
  align-items: center;
  background-color: #1976d2;
  color: white;
  padding: 10px 24px;
  border-radius: 50px;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.3);
}

.login-btn:hover {
  background-color: #1565c0;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(25, 118, 210, 0.4);
}

.btn-icon {
  margin-right: 8px;
}

.mobile-menu-toggle {
  display: none;
  flex-direction: column;
  cursor: pointer;
  padding: 5px;
}

.mobile-menu-toggle span {
  width: 25px;
  height: 3px;
  background-color: #333;
  margin: 3px 0;
  transition: 0.3s;
  border-radius: 2px;
}

.mobile-menu {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background-color: white;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  transform: translateY(-10px);
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
}

.mobile-menu.active {
  transform: translateY(0);
  opacity: 1;
  visibility: visible;
}

.mobile-menu ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.mobile-menu li {
  border-bottom: 1px solid #f0f0f0;
}

.mobile-menu a {
  display: block;
  padding: 15px 24px;
  text-decoration: none;
  color: #333;
  font-weight: 500;
  transition: all 0.3s;
}

.mobile-menu a:hover {
  background-color: #f5f5f5;
  color: #1976d2;
}

/* 主要内容区 */
.main-content {
  flex: 1;
  position: relative;
}

/* 英雄区 */
.hero-section {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  padding: 140px 0 100px;
  position: relative;
  overflow: hidden;
  min-height: 80vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
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

.hero-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  position: relative;
  z-index: 1;
  padding: 0 20px;
}

.hero-title {
  font-size: 64px;
  font-weight: 800;
  color: #1976d2;
  margin: 0 0 16px;
  line-height: 1.1;
}

.title-word {
  display: inline-block;
  animation: titleFloat 6s ease-in-out infinite;
}

.title-word:nth-child(2) {
  animation-delay: 2s;
}

@keyframes titleFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.hero-content .subtitle {
  font-size: 24px;
  color: #42a5f5;
  margin: 0 0 24px;
  font-weight: 500;
}

.hero-content .description {
  font-size: 18px;
  color: #333;
  line-height: 1.6;
  margin: 0 0 40px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 60px;
}

.primary-btn {
  display: flex;
  align-items: center;
  background-color: #1976d2;
  color: white;
  padding: 16px 40px;
  border-radius: 50px;
  text-decoration: none;
  font-size: 18px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 6px 20px rgba(25, 118, 210, 0.4);
  position: relative;
  overflow: hidden;
}

.primary-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s;
}

.primary-btn:hover::before {
  left: 100%;
}

.primary-btn:hover {
  background-color: #1565c0;
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(25, 118, 210, 0.5);
}

.btn-sparkle {
  margin-right: 8px;
  animation: sparkle 2s infinite;
}

@keyframes sparkle {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

.secondary-btn {
  display: flex;
  align-items: center;
  background-color: white;
  color: #1976d2;
  padding: 16px 40px;
  border-radius: 50px;
  text-decoration: none;
  font-size: 18px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 2px solid #1976d2;
}

.secondary-btn:hover {
  background-color: #f5f5f5;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  padding-right: 36px;
}

.btn-arrow {
  margin-left: 8px;
  transition: transform 0.3s;
}

.secondary-btn:hover .btn-arrow {
  transform: translateX(5px);
}

.scroll-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
}

.scroll-indicator:hover {
  color: #1976d2;
}

.indicator-arrow {
  width: 20px;
  height: 20px;
  border-right: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: rotate(45deg);
  margin-top: 5px;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: rotate(45deg) translateY(0); }
  40% { transform: rotate(45deg) translateY(-10px); }
  60% { transform: rotate(45deg) translateY(-5px); }
}

/* 功能特点 */
.features-section {
  padding: 100px 0;
  background-color: #fafafa;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-header h2 {
  font-size: 36px;
  color: #333;
  margin: 0 0 16px;
}

.section-subtitle {
  font-size: 18px;
  color: #666;
  max-width: 600px;
  margin: 0 auto;
}

.features-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 40px;
  padding: 0 20px;
}

.feature-card {
  background-color: white;
  padding: 40px 30px;
  border-radius: 16px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  text-align: center;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
  opacity: 0;
  transform: translateY(20px);
}

.feature-card.animate-in {
  opacity: 1;
  transform: translateY(0);
  transition: all 0.5s ease;
}

.feature-card:nth-child(1).animate-in { transition-delay: 0.1s; }
.feature-card:nth-child(2).animate-in { transition-delay: 0.2s; }
.feature-card:nth-child(3).animate-in { transition-delay: 0.3s; }

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.12);
}

.feature-icon-wrapper {
  margin-bottom: 20px;
}

.feature-icon {
  font-size: 64px;
  display: inline-block;
  transition: transform 0.3s;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1) rotate(5deg);
}

.feature-card h3 {
  font-size: 20px;
  color: #333;
  margin: 0 0 16px;
}

.feature-card p {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.feature-highlight {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #1976d2, #42a5f5);
  transform: scaleX(0);
  transition: transform 0.3s;
}

.feature-card:hover .feature-highlight {
  transform: scaleX(1);
}

/* 用户评价部分 */
.testimonials-section {
  padding: 100px 0;
  background-color: white;
}

.testimonials-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  padding: 0 20px;
}

.testimonial-card {
  background-color: #f9f9f9;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  opacity: 0;
  transform: translateY(20px);
}

.testimonial-card.animate-in {
  opacity: 1;
  transform: translateY(0);
  transition: all 0.5s ease;
}

.testimonial-card:nth-child(1).animate-in { transition-delay: 0.1s; }
.testimonial-card:nth-child(2).animate-in { transition-delay: 0.2s; }
.testimonial-card:nth-child(3).animate-in { transition-delay: 0.3s; }

.testimonial-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.testimonial-content {
  margin-bottom: 20px;
}

.testimonial-content p {
  font-style: italic;
  color: #555;
  line-height: 1.6;
  margin: 0;
}

.testimonial-author {
  display: flex;
  align-items: center;
}

.author-avatar {
  font-size: 40px;
  margin-right: 15px;
}

.author-info h4 {
  margin: 0 0 5px;
  font-size: 16px;
  color: #333;
}

.author-info p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

/* 页脚 */
.main-footer {
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  color: white;
  padding: 60px 0 20px;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 60px;
  margin-bottom: 40px;
}

.footer-brand {
  display: flex;
  flex-direction: column;
}

.footer-logo {
  font-size: 24px;
  font-weight: bold;
  color: white;
  margin-bottom: 16px;
}

.footer-description {
  color: #bdc3c7;
  margin-bottom: 24px;
  line-height: 1.6;
}

.social-links {
  display: flex;
  gap: 15px;
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: white;
  text-decoration: none;
  transition: all 0.3s;
}

.social-link:hover {
  background-color: #1976d2;
  transform: translateY(-3px);
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
}

.link-group h4 {
  font-size: 18px;
  color: white;
  margin: 0 0 20px;
  font-weight: 600;
}

.link-group ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.link-group li {
  margin-bottom: 12px;
}

.link-group a {
  text-decoration: none;
  color: #bdc3c7;
  transition: color 0.3s;
  font-size: 14px;
}

.link-group a:hover {
  color: #1976d2;
}

.footer-copyright {
  text-align: center;
  color: #95a5a6;
  font-size: 14px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .footer-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 16px;
  }

  .main-nav {
    display: none;
  }

  .header-actions {
    display: none;
  }

  .mobile-menu-toggle {
    display: flex;
  }

  .mobile-menu {
    display: block;
  }

  .hero-title {
    font-size: 48px;
  }

  .hero-content .subtitle {
    font-size: 20px;
  }

  .hero-content .description {
    font-size: 16px;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
    gap: 15px;
  }

  .primary-btn, .secondary-btn {
    width: 280px;
    text-align: center;
    justify-content: center;
  }

  .features-section, .testimonials-section {
    padding: 60px 0;
  }

  .section-header h2 {
    font-size: 28px;
  }

  .feature-card, .testimonial-card {
    padding: 30px 20px;
  }

  .footer-links {
    grid-template-columns: 1fr;
    gap: 30px;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 36px;
  }

  .hero-section {
    padding: 100px 0 80px;
  }

  .features-grid, .testimonials-grid {
    grid-template-columns: 1fr;
  }
}
</style>