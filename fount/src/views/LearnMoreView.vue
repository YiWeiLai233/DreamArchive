<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()

function goBack() {
  router.push('/')
}

function goRegister() {
  router.push('/register')
}

function goLogin() {
  router.push('/login')
}

function goDreamList() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push('/dreams')
}
</script>

<template>
  <div class="learn-more">
    <!-- 梦幻背景 -->
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds">
        <div class="cloud cloud-1"></div>
        <div class="cloud cloud-2"></div>
      </div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="nav-brand" @click="goBack" style="cursor: pointer;">
        <span class="brand-icon">🌙</span>
        <span class="brand-text">梦境档案</span>
      </div>
      <div class="nav-auth">
        <template v-if="!userStore.isLoggedIn">
          <button class="btn btn-login" @click="goLogin">登录</button>
          <button class="btn btn-register" @click="goRegister">注册</button>
        </template>
        <template v-else>
          <button class="btn btn-login" @click="goDreamList">我的梦境</button>
        </template>
      </div>
    </nav>

    <!-- 浮动装饰 -->
    <div class="decor">
      <span class="float-icon fi-1">🌙</span>
      <span class="float-icon fi-2">✨</span>
      <span class="float-icon fi-3">🔮</span>
      <span class="float-icon fi-4">💫</span>
      <span class="float-icon fi-5">⭐</span>
    </div>

    <!-- 主要内容 -->
    <main class="content">
      <!-- 标题区域 -->
      <section class="hero-section">
        <div class="hero-content card-enter">
          <h1 class="hero-title">
            <span class="title-line">探索梦境的</span>
            <span class="title-line title-highlight">无限可能</span>
          </h1>
          <p class="hero-subtitle">
            梦境档案是一款智能梦境记录与分析应用，帮助你理解潜意识的奥秘
          </p>
        </div>
      </section>

      <!-- 功能介绍 -->
      <section class="features-section">
        <h2 class="section-title">核心功能</h2>
        <div class="features-grid">
          <div class="feature-card glass card-enter" style="animation-delay: 0.1s">
            <div class="feature-icon-wrap purple"><div class="feature-icon">📖</div></div>
            <h3>梦境记录</h3>
            <p>用文字详细记录你的梦境内容，包括时间、地点、情绪等元素，建立专属的梦境档案库</p>
          </div>
          <div class="feature-card glass card-enter" style="animation-delay: 0.2s">
            <div class="feature-icon-wrap pink"><div class="feature-icon">🔮</div></div>
            <h3>AI 智能解析</h3>
            <p>借助先进的人工智能技术，分析梦境背后的深层含义，解读潜意识想要传达的信息</p>
          </div>
          <div class="feature-card glass card-enter" style="animation-delay: 0.3s">
            <div class="feature-icon-wrap blue"><div class="feature-icon">📊</div></div>
            <h3>数据统计</h3>
            <p>可视化展示你的梦境频率、情绪变化、主题分布，发现梦境中的规律和模式</p>
          </div>
          <div class="feature-card glass card-enter" style="animation-delay: 0.4s">
            <div class="feature-icon-wrap mixed"><div class="feature-icon">🌙</div></div>
            <h3>梦境追踪</h3>
            <p>长期追踪梦境变化，记录情绪波动，帮助你更好地了解自己的内心世界</p>
          </div>
        </div>
      </section>

      <!-- 使用流程 -->
      <section class="how-it-works">
        <h2 class="section-title">如何使用</h2>
        <div class="steps-container">
          <div class="step glass card-enter" style="animation-delay: 0.1s">
            <div class="step-number">1</div>
            <div class="step-content">
              <h3>注册账号</h3>
              <p>创建你的专属梦境档案账户，开始记录奇妙旅程</p>
            </div>
          </div>
          <div class="step glass card-enter" style="animation-delay: 0.2s">
            <div class="step-number">2</div>
            <div class="step-content">
              <h3>记录梦境</h3>
              <p>醒来后立即记录梦中的场景、人物、情节和感受</p>
            </div>
          </div>
          <div class="step glass card-enter" style="animation-delay: 0.3s">
            <div class="step-number">3</div>
            <div class="step-content">
              <h3>AI 分析</h3>
              <p>让 AI 为你解读梦境含义，发现潜意识的秘密</p>
            </div>
          </div>
          <div class="step glass card-enter" style="animation-delay: 0.4s">
            <div class="step-number">4</div>
            <div class="step-content">
              <h3>洞察自我</h3>
              <p>通过数据统计，深入了解自己的内心世界和情感变化</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 关于项目 -->
      <section class="about-section">
        <div class="about-card glass card-enter">
          <h2>关于梦境档案</h2>
          <p>
            梦境档案是一个充满创意的个人项目，旨在探索人类潜意识的奥秘。
            我们相信，每一个梦境都是内心世界的一扇窗户，通过记录和分析梦境，
            我们可以更好地了解自己的情感、欲望和恐惧。
          </p>
          <p>
            这个项目结合了现代 Web 技术和人工智能，为用户提供一个安全、私密的空间来记录和探索梦境。
            无论是奇幻的冒险、温馨的回忆，还是令人困惑的场景，都值得被记录和理解。
          </p>
          <div class="tech-stack">
            <h3>技术栈</h3>
            <div class="tech-tags">
              <span class="tech-tag">Vue 3</span>
              <span class="tech-tag">TypeScript</span>
              <span class="tech-tag">Spring Boot</span>
              <span class="tech-tag">MyBatis</span>
              <span class="tech-tag">MySQL</span>
              <span class="tech-tag">AI API</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 号召行动 -->
      <section class="cta-section">
        <div class="cta-content glass card-enter">
          <h2>开始你的梦境之旅</h2>
          <p>加入我们，一起探索梦境的奇妙世界</p>
          <div class="cta-actions">
            <button class="btn btn-primary" @click="goRegister">立即注册</button>
            <button class="btn btn-secondary" @click="goLogin">已有账号？登录</button>
          </div>
        </div>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="footer-content">
        <p>&copy; 2026 梦境档案 - 记录每一个奇妙梦境</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.learn-more {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow-x: hidden;
  background: linear-gradient(135deg, var(--bg-start) 0%, var(--bg-end) 100%);
}

/* 梦幻背景 */
.dream-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.stars {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image:
    radial-gradient(2px 2px at 20px 30px, #fff, transparent),
    radial-gradient(2px 2px at 40px 70px, #fff, transparent),
    radial-gradient(1px 1px at 90px 40px, #fff, transparent),
    radial-gradient(1px 1px at 130px 80px, #fff, transparent),
    radial-gradient(2px 2px at 160px 30px, #fff, transparent);
  background-repeat: repeat;
  background-size: 200px 100px;
  animation: twinkle 4s ease-in-out infinite alternate;
}

.cloud {
  position: absolute;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  filter: blur(40px);
}

.cloud-1 {
  width: 400px;
  height: 150px;
  top: 20%;
  left: -10%;
  animation: float-cloud 20s ease-in-out infinite;
}

.cloud-2 {
  width: 300px;
  height: 120px;
  top: 40%;
  right: -5%;
  animation: float-cloud 25s ease-in-out infinite reverse;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.glow-1 {
  width: 300px;
  height: 300px;
  top: 10%;
  right: 20%;
  background: rgba(124, 111, 224, 0.3);
  animation: pulse 8s ease-in-out infinite;
}

.glow-2 {
  width: 250px;
  height: 250px;
  bottom: 10%;
  left: 15%;
  background: rgba(255, 179, 71, 0.2);
  animation: pulse 10s ease-in-out infinite 3s;
}

/* 浮动装饰 */
.decor { position: fixed; inset: 0; pointer-events: none; z-index: 1; }
.float-icon { position: absolute; font-size: 1.5rem; animation: float 6s ease-in-out infinite; opacity: 0.5; }
.fi-1 { top: 12%; left: 8%; animation-delay: 0s; }
.fi-2 { top: 35%; right: 6%; animation-delay: 1.2s; font-size: 1.2rem; }
.fi-3 { top: 55%; left: 5%; animation-delay: 2.4s; font-size: 1.8rem; }
.fi-4 { bottom: 25%; right: 10%; animation-delay: 3.6s; }
.fi-5 { bottom: 10%; left: 15%; animation-delay: 4.8s; font-size: 1.3rem; }
@keyframes float { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-15px); } }

/* 导航栏 */
.navbar {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 3rem;
  flex-shrink: 0;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.brand-icon {
  font-size: 1.75rem;
}

.brand-text {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-dark);
}

.nav-auth {
  display: flex;
  gap: 1rem;
  align-items: center;
}

/* 按钮 */
.btn {
  padding: 0.6rem 1.5rem;
  border: none;
  border-radius: 50px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}

.btn-login {
  background: transparent;
  color: var(--text-dark);
  border: 2px solid var(--glass-border);
  backdrop-filter: blur(10px);
}

.btn-login:hover {
  background: var(--glass-bg);
  transform: translateY(-2px);
}

.btn-register {
  background: var(--primary);
  color: white;
  box-shadow: 0 4px 15px rgba(124, 111, 224, 0.4);
}

.btn-register:hover {
  background: var(--primary-light);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(124, 111, 224, 0.5);
}

.btn-primary {
  background: var(--primary);
  color: white;
  padding: 1rem 2.5rem;
  font-size: 1.1rem;
  box-shadow: 0 4px 20px rgba(124, 111, 224, 0.4);
}

.btn-primary:hover {
  background: var(--primary-light);
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(124, 111, 224, 0.5);
}

.btn-secondary {
  background: var(--glass-bg);
  color: var(--text-dark);
  padding: 1rem 2.5rem;
  font-size: 1.1rem;
  backdrop-filter: blur(10px);
  border: 1px solid var(--glass-border);
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.4);
  transform: translateY(-3px);
}

/* 主要内容 */
.content {
  position: relative;
  z-index: 5;
  flex: 1;
  padding: 0 3rem;
}

/* 标题区域 */
.hero-section {
  text-align: center;
  padding: 4rem 0;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 3rem;
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: 1.5rem;
  color: var(--text-dark);
}

.title-line {
  display: block;
  color: var(--text-light);
}

.title-highlight {
  font-size: 4rem;
  background: linear-gradient(135deg, #6B8CFF 0%, #FF8FAB 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.3rem;
  color: var(--text-light);
  line-height: 1.6;
}

/* 通用部分标题 */
.section-title {
  text-align: center;
  font-size: 2.5rem;
  color: var(--text-dark);
  margin-bottom: 3rem;
  font-weight: 700;
}

/* 功能介绍 */
.features-section {
  padding: 4rem 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  padding: 2rem;
  border-radius: 20px;
  text-align: center;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  overflow: hidden;
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.feature-icon-wrap {
  width: 4rem; height: 4rem; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 1rem;
}
.feature-icon-wrap.purple { background: linear-gradient(135deg, rgba(107,140,255,0.2), rgba(107,140,255,0.06)); }
.feature-icon-wrap.pink { background: linear-gradient(135deg, rgba(255,143,171,0.2), rgba(255,143,171,0.06)); }
.feature-icon-wrap.blue { background: linear-gradient(135deg, rgba(107,180,255,0.2), rgba(107,180,255,0.06)); }
.feature-icon-wrap.mixed { background: linear-gradient(135deg, rgba(107,140,255,0.15), rgba(255,143,171,0.15)); }

.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 2rem;
}

.feature-card h3 {
  font-size: 1.4rem;
  margin-bottom: 0.75rem;
  color: var(--text-dark);
}

.feature-card p {
  font-size: 1rem;
  color: var(--text-light);
  line-height: 1.6;
}

/* 使用流程 */
.how-it-works {
  padding: 4rem 0;
}

.steps-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.step {
  padding: 2rem;
  border-radius: 20px;
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
  transition: transform 0.3s ease;
}

.step:hover {
  transform: translateY(-5px);
}

.step-number {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6B8CFF 0%, #FF8FAB 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  flex-shrink: 0;
}

.step-content h3 {
  font-size: 1.3rem;
  margin-bottom: 0.5rem;
  color: var(--text-dark);
}

.step-content p {
  font-size: 1rem;
  color: var(--text-light);
  line-height: 1.5;
}

/* 关于项目 */
.about-section {
  padding: 4rem 0;
}

.about-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 3rem;
  border-radius: 24px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.about-card h2 {
  font-size: 2rem;
  color: var(--text-dark);
  margin-bottom: 1.5rem;
}

.about-card p {
  font-size: 1.1rem;
  color: var(--text-light);
  line-height: 1.8;
  margin-bottom: 1.5rem;
}

.tech-stack {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid var(--glass-border);
}

.tech-stack h3 {
  font-size: 1.3rem;
  color: var(--text-dark);
  margin-bottom: 1rem;
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.75rem;
}

.tech-tag {
  padding: 0.5rem 1rem;
  background: rgba(124, 111, 224, 0.1);
  border: 1px solid rgba(124, 111, 224, 0.3);
  border-radius: 20px;
  color: var(--primary);
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s;
}
.tech-tag:hover {
  background: rgba(124, 111, 224, 0.2);
  transform: translateY(-2px);
}

/* 号召行动 */
.cta-section {
  padding: 4rem 0;
  text-align: center;
}

.cta-content {
  max-width: 600px;
  margin: 0 auto;
  padding: 3rem;
  border-radius: 24px;
  position: relative;
  overflow: hidden;
}
.cta-content h2 {
  font-size: 2rem;
  color: var(--text-dark);
  margin-bottom: 1rem;
}

.cta-content p {
  font-size: 1.1rem;
  color: var(--text-light);
  margin-bottom: 2rem;
}

.cta-actions {
  display: flex;
  gap: 1.5rem;
  justify-content: center;
  flex-wrap: wrap;
}

/* 页脚 */
.footer {
  position: relative;
  z-index: 5;
  padding: 2rem 3rem;
  text-align: center;
  margin-top: auto;
}

.footer-content p {
  color: var(--text-light);
  font-size: 0.9rem;
}

/* 动画 */
@keyframes twinkle {
  0% { opacity: 0.5; }
  100% { opacity: 1; }
}

@keyframes float-cloud {
  0%, 100% { transform: translateX(0) translateY(0); }
  50% { transform: translateX(50px) translateY(-20px); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

/* 响应式 */
@media (max-width: 1024px) {
  .navbar {
    padding: 1rem 2rem;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 0.75rem 1.5rem;
  }

  .brand-text {
    font-size: 1.2rem;
  }

  .content {
    padding: 0 1.5rem;
  }

  .hero-title {
    font-size: 2rem;
  }

  .title-highlight {
    font-size: 2.8rem;
  }

  .hero-subtitle {
    font-size: 1rem;
  }

  .section-title {
    font-size: 1.8rem;
    margin-bottom: 2rem;
  }

  .features-grid {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .steps-container {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .step {
    flex-direction: column;
    text-align: center;
    align-items: center;
  }

  .cta-actions {
    flex-direction: column;
    align-items: center;
  }

  .hero-section { padding: 2.5rem 0; }
  .feature-card { padding: 1.5rem; }
  .feature-icon { font-size: 2.5rem; }
  .feature-card h3 { font-size: 1.2rem; }
  .feature-card p { font-size: 0.9rem; }
  .about-card { padding: 2rem; }
  .about-card h2 { font-size: 1.6rem; }
  .about-card p { font-size: 0.95rem; }
  .cta-content { padding: 2rem; }
  .cta-content h2 { font-size: 1.6rem; }
  .step-number { width: 40px; height: 40px; font-size: 1rem; }
  .footer { padding: 1.5rem; }
}

@media (max-width: 480px) {
  .hero-title { font-size: 1.8rem; }
  .title-highlight { font-size: 2.4rem; }
  .hero-subtitle { font-size: 0.9rem; }
  .section-title { font-size: 1.5rem; }
  .feature-card { padding: 1.25rem; }
  .feature-icon { font-size: 2rem; }
  .feature-card h3 { font-size: 1.1rem; }
  .about-card { padding: 1.5rem; }
  .about-card h2 { font-size: 1.4rem; }
  .cta-content { padding: 1.5rem; }
  .cta-content h2 { font-size: 1.4rem; }
  .step-number { width: 36px; height: 36px; font-size: 0.9rem; }
}

@media (min-width: 1024px) {
  .hero-title { font-size: 3.5rem; }
  .title-highlight { font-size: 4.5rem; }
  .section-title { font-size: 2.8rem; }
  .feature-card { padding: 2.5rem; }
  .feature-icon { font-size: 3.5rem; }
  .feature-card h3 { font-size: 1.5rem; }
  .steps-container { grid-template-columns: repeat(4, 1fr); gap: 1.5rem; }
  .step { padding: 1.75rem; gap: 1rem; }
  .step-number { width: 44px; height: 44px; font-size: 1.3rem; }
  .step-content h3 { font-size: 1.15rem; }
  .step-content p { font-size: 0.92rem; line-height: 1.7; }
}
</style>
