<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { getDreamStats, getEmotionDistribution, getPlaceDistribution } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

// 统计数据
const totalDreams = ref(0)
const topEmotion = ref<{ icon: string; label: string; count: number } | null>(null)
const topPlace = ref<{ label: string; count: number } | null>(null)
const statsLoaded = ref(false)

const emotionIcons: Record<string, string> = {
  'happy': '😊', '开心': '😊', '快乐': '😊',
  'sad': '😢', '难过': '😢', '悲伤': '😢',
  'scary': '😰', '恐惧': '😰', '害怕': '😰',
  'angry': '😤', '愤怒': '😤', '生气': '😤',
  'peaceful': '😌', '平静': '😌', '安宁': '😌',
  'mysterious': '🔮', '神秘': '🔮',
  'excited': '🤩', '兴奋': '🤩'
}
const emotionLabels: Record<string, string> = {
  'happy': '开心', 'sad': '悲伤', 'scary': '恐惧',
  'angry': '愤怒', 'peaceful': '平静', 'mysterious': '神秘', 'excited': '兴奋'
}

async function loadStats() {
  if (!userStore.isLoggedIn || !userStore.userId) return
  try {
    const [statsRes, emotionRes, placeRes] = await Promise.all([
      getDreamStats(userStore.userId),
      getEmotionDistribution(userStore.userId),
      getPlaceDistribution(userStore.userId)
    ])
    if (statsRes.data.code === 200) {
      totalDreams.value = statsRes.data.data.totalDreams
    }
    if (emotionRes.data.code === 200 && emotionRes.data.data.length > 0) {
      const top = emotionRes.data.data[0]
      topEmotion.value = {
        icon: emotionIcons[top.label] || '😊',
        label: emotionLabels[top.label] || top.label,
        count: top.value
      }
    }
    if (placeRes.data.code === 200 && placeRes.data.data.length > 0) {
      topPlace.value = { label: placeRes.data.data[0].label, count: placeRes.data.data[0].value }
    }
    statsLoaded.value = true
  } catch {
    // 静默失败，不展示错误
  }
}

onMounted(loadStats)

function goRegister() {
  router.push('/register')
}

function goLogin() {
  router.push('/login')
}

function handleLogout() {
  userStore.logout()
}

function goDreamList() {
  router.push('/dreams')
}

function goProfile() {
  router.push('/profile')
}

function goChangePassword() {
  router.push('/change-password')
}

function goDreamStats() {
  router.push('/dream-stats')
}

function goAdmin() {
  router.push('/admin')
}

function goDreamNew() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  router.push('/record-dream')
}

function goStats() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  router.push('/dream-stats')
}

function goLearnMore() {
  router.push('/learn-more')
}
</script>

<template>
  <div class="home">
    <!-- 梦幻背景层 -->
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds">
        <div class="cloud cloud-1"></div>
        <div class="cloud cloud-2"></div>
        <div class="cloud cloud-3"></div>
      </div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <!-- 两侧装饰 -->
    <div class="side-decor">
      <span class="sd sd-1">🌙</span>
      <span class="sd sd-2">✨</span>
      <span class="sd sd-3">🔮</span>
      <span class="sd sd-4">💫</span>
      <span class="sd sd-5">⭐</span>
      <span class="sd sd-6">💜</span>
      <span class="sd sd-7">🌀</span>
      <span class="sd sd-8">🪐</span>
      <span class="sd sd-9">☁️</span>
      <span class="sd sd-10">🌟</span>
      <span class="sd sd-11">🎆</span>
      <span class="sd sd-12">🦋</span>
    </div>

    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="nav-brand">
        <span class="brand-icon">🌙</span>
        <span class="brand-text">梦境档案</span>
      </div>
      <div class="nav-auth">
        <!-- 未登录：显示登录注册按钮 -->
        <template v-if="!userStore.isLoggedIn">
          <button class="btn btn-login" @click="goLogin">登录</button>
          <button class="btn btn-register" @click="goRegister">注册</button>
        </template>
        <!-- 已登录：显示用户头像 -->
        <div v-else class="user-avatar-wrapper">
          <div class="user-avatar" :title="userStore.username">
            <img v-if="userStore.avatar" :src="userStore.avatar" alt="头像" class="avatar-img" />
            <span v-else class="avatar-text">{{ userStore.username.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="user-dropdown">
            <div class="dropdown-header">
              <div class="dropdown-avatar">
                <img v-if="userStore.avatar" :src="userStore.avatar" alt="头像" class="avatar-img" />
                <span v-else>{{ userStore.username.charAt(0).toUpperCase() }}</span>
              </div>
              <div class="dropdown-info">
                <span class="dropdown-username">{{ userStore.username }}</span>
              </div>
            </div>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item" @click="goDreamList">
              <span>📖</span>
              <span>我的梦境</span>
            </button>
            <button class="dropdown-item" @click="goProfile">
              <span>👤</span>
              <span>个人资料</span>
            </button>
            <button class="dropdown-item" @click="goChangePassword">
              <span>🔑</span>
              <span>修改密码</span>
            </button>
            <button class="dropdown-item" @click="goDreamStats">
              <span>📊</span>
              <span>梦境统计</span>
            </button>
            <button v-if="userStore.isAdmin" class="dropdown-item admin-item" @click="goAdmin">
              <span>🛡️</span>
              <span>管理员控制台</span>
            </button>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item logout-item" @click="handleLogout">
              <span>🚪</span>
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- 主横幅 -->
    <main class="hero">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="title-line">记录每一个</span>
          <span class="title-line title-highlight">奇妙梦境</span>
        </h1>
        <p class="hero-subtitle">
          在星光与云层之间，捕捉转瞬即逝的梦中奇遇
        </p>
        <div class="hero-actions">
          <button class="btn btn-primary" @click="goDreamNew">开始记录</button>
        </div>
      </div>

      <!-- 装饰元素 -->
      <div class="hero-decor">
        <div class="floating star-1">✨</div>
        <div class="floating star-2">⭐</div>
        <div class="floating star-3">💫</div>
        <div class="floating moon">🌙</div>
      </div>
    </main>

    <!-- 底部功能区（已登录时展示） -->
    <section v-if="userStore.isLoggedIn" class="dashboard">
      <!-- 快捷入口 -->
      <div class="quick-cards">
        <div class="quick-card glass" @click="goDreamList">
          <span class="quick-icon">📖</span>
          <span class="quick-label">我的梦境</span>
        </div>
        <div class="quick-card glass" @click="goStats">
          <span class="quick-icon">📊</span>
          <span class="quick-label">梦境统计</span>
        </div>
      </div>

      <!-- 统计概览 -->
      <div v-if="statsLoaded" class="stats-cards">
        <div class="stat-card glass" @click="goDreamList">
          <span class="stat-icon">🌙</span>
          <span class="stat-value">{{ totalDreams }}</span>
          <span class="stat-label">梦境总数</span>
        </div>
        <div class="stat-card glass" @click="goStats">
          <span class="stat-icon">{{ topEmotion?.icon || '😊' }}</span>
          <span class="stat-value">{{ topEmotion?.label || '—' }}</span>
          <span class="stat-label">最常情绪</span>
        </div>
        <div class="stat-card glass" @click="goStats">
          <span class="stat-icon">📍</span>
          <span class="stat-value">{{ topPlace?.label || '—' }}</span>
          <span class="stat-label">最常地点</span>
        </div>
      </div>

      <!-- 了解更多 -->
      <div class="learn-more-card glass" @click="goLearnMore">
        <div class="learn-more-content">
          <div class="learn-more-left">
            <span class="learn-more-icon">✨</span>
            <div class="learn-more-text">
              <h3>探索梦境的奥秘</h3>
              <p>了解梦境解析的原理，发现潜意识中的秘密</p>
            </div>
          </div>
          <span class="learn-more-arrow">→</span>
        </div>
      </div>
    </section>

    <!-- 未登录时只展示了解更多 -->
    <section v-else class="dashboard">
      <div class="learn-more-card glass" @click="goLearnMore">
        <div class="learn-more-content">
          <div class="learn-more-left">
            <span class="learn-more-icon">✨</span>
            <div class="learn-more-text">
              <h3>探索梦境的奥秘</h3>
              <p>了解梦境解析的原理，发现潜意识中的秘密</p>
            </div>
          </div>
          <span class="learn-more-arrow">→</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
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

.cloud-3 {
  width: 350px;
  height: 130px;
  bottom: 20%;
  left: 30%;
  animation: float-cloud 22s ease-in-out infinite 5s;
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

/* 用户头像 */
.user-avatar-wrapper {
  position: relative;
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(124, 111, 224, 0.4);
  border: 2px solid rgba(255, 255, 255, 0.5);
  overflow: hidden;
}

.user-avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(124, 111, 224, 0.5);
}

.avatar-text {
  color: white;
  font-size: 1.2rem;
  font-weight: 600;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 下拉菜单 */
.user-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 220px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.3s ease;
  z-index: 100;
}

.user-avatar-wrapper:hover .user-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
}

.dropdown-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1rem;
  font-weight: 600;
  overflow: hidden;
}

.dropdown-info {
  display: flex;
  flex-direction: column;
}

.dropdown-username {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-dark);
}

.dropdown-divider {
  height: 1px;
  background: var(--glass-border);
  margin: 0 0.5rem;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.75rem 1rem;
  border: none;
  background: none;
  color: var(--text-dark);
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s ease;
  font-family: 'Noto Sans SC', sans-serif;
}

.dropdown-item:hover {
  background: rgba(124, 111, 224, 0.1);
}

.admin-item {
  color: #4b3fc9;
}

.admin-item:hover {
  background: rgba(75, 63, 201, 0.12);
}

.logout-item:hover {
  background: rgba(255, 82, 82, 0.1);
  color: #e53935;
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

/* 主横幅 */
.hero {
  position: relative;
  z-index: 5;
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  min-height: 0;
  padding: 1rem 3rem 2rem;
  text-align: center;
}

.hero-content {
  max-width: 700px;
}

.hero-title {
  font-size: 3.5rem;
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
  font-size: 4.5rem;
  background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.3rem;
  color: var(--text-light);
  margin-bottom: 2rem;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 1.5rem;
  justify-content: center;
}

/* 浮动装饰 */
.hero-decor {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.floating {
  position: absolute;
  font-size: 2rem;
  animation: float 6s ease-in-out infinite;
}

.star-1 {
  top: 15%;
  left: 10%;
  animation-delay: 0s;
}

.star-2 {
  top: 25%;
  right: 15%;
  animation-delay: 2s;
  font-size: 1.5rem;
}

.star-3 {
  bottom: 25%;
  left: 20%;
  animation-delay: 4s;
}

.moon {
  top: 10%;
  right: 10%;
  font-size: 3rem;
  animation-delay: 1s;
}

/* 特性介绍 */
.features {
  position: relative;
  z-index: 5;
  display: flex;
  justify-content: center;
  gap: 2rem;
  padding: 0 3rem 2rem;
  flex-shrink: 0;
}

.feature-card {
  padding: 1.25rem;
  border-radius: 16px;
  text-align: center;
  width: 200px;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.feature-card:active {
  transform: translateY(-5px) scale(0.98);
}

.feature-card:hover {
  transform: translateY(-10px);
}

.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.feature-card h3 {
  font-size: 1.1rem;
  margin-bottom: 0.4rem;
  color: var(--text-dark);
}

.feature-card p {
  font-size: 0.85rem;
  color: var(--text-light);
  line-height: 1.4;
}

/* 底部功能区 */
.dashboard {
  position: relative;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
  padding: 0 3rem 2.5rem;
  flex-shrink: 0;
  max-width: 700px;
  margin: 0 auto;
  width: 100%;
}

/* 快捷入口 */
.quick-cards {
  display: flex;
  gap: 1rem;
  width: 100%;
}

.quick-card {
  flex: 1;
  padding: 1rem;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.quick-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 36px rgba(124, 111, 224, 0.18);
}

.quick-icon {
  font-size: 1.4rem;
}

.quick-label {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-dark);
}

/* 统计概览 */
.stats-cards {
  display: flex;
  gap: 1rem;
  width: 100%;
}

.stat-card {
  flex: 1;
  padding: 1rem;
  border-radius: 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  font-size: 1.3rem;
}

.stat-value {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-dark);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.stat-label {
  font-size: 0.72rem;
  color: var(--text-light);
}

/* 了解更多 */
.learn-more-card {
  width: 100%;
  padding: 1.1rem 1.5rem;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(124, 111, 224, 0.2);
  background: linear-gradient(135deg, rgba(124, 111, 224, 0.08) 0%, rgba(255, 179, 71, 0.06) 100%);
  backdrop-filter: blur(20px);
}

.learn-more-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(124, 111, 224, 0.15);
  border-color: rgba(124, 111, 224, 0.35);
}

.learn-more-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.learn-more-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.learn-more-icon {
  font-size: 1.8rem;
  flex-shrink: 0;
}

.learn-more-text h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 0.15rem;
}

.learn-more-text p {
  font-size: 0.8rem;
  color: var(--text-light);
}

.learn-more-arrow {
  font-size: 1.2rem;
  color: var(--primary);
  font-weight: 600;
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.learn-more-card:hover .learn-more-arrow {
  transform: translateX(6px);
}


/* 两侧装饰 */
.side-decor {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
  display: none;
}

.sd {
  position: absolute;
  opacity: 0.28;
  animation: drift 8s ease-in-out infinite;
}

.sd-1  { left: 3%;  top: 8%;   font-size: 2.4rem; animation-delay: 0s;    animation-duration: 9s; }
.sd-2  { left: 7%;  top: 28%;  font-size: 1.2rem; animation-delay: 1.5s;  animation-duration: 7s; }
.sd-3  { left: 2%;  top: 52%;  font-size: 2rem;   animation-delay: 3.2s;  animation-duration: 11s; }
.sd-4  { left: 9%;  top: 72%;  font-size: 1.5rem; animation-delay: 0.8s;  animation-duration: 8s; }
.sd-5  { left: 4%;  top: 88%;  font-size: 1.8rem; animation-delay: 4.5s;  animation-duration: 10s; }
.sd-6  { right: 3%; top: 12%;  font-size: 1.6rem; animation-delay: 2.1s;  animation-duration: 9s; }
.sd-7  { right: 8%; top: 32%;  font-size: 2.2rem; animation-delay: 0.4s;  animation-duration: 12s; }
.sd-8  { right: 2%; top: 55%;  font-size: 2.8rem; animation-delay: 3.8s;  animation-duration: 10s; }
.sd-9  { right: 6%; top: 70%;  font-size: 1.3rem; animation-delay: 1.2s;  animation-duration: 7s; }
.sd-10 { right: 4%; top: 85%;  font-size: 1.9rem; animation-delay: 5s;    animation-duration: 9s; }
.sd-11 { left: 5%;  top: 40%;  font-size: 1.1rem; animation-delay: 2.8s;  animation-duration: 8s; }
.sd-12 { right: 5%; top: 45%;  font-size: 1.4rem; animation-delay: 4.2s;  animation-duration: 11s; }

@keyframes drift {
  0%, 100% { transform: translate(0, 0) rotate(0deg) scale(1); opacity: 0.28; }
  25%      { transform: translate(8px, -12px) rotate(8deg) scale(1.08); opacity: 0.38; }
  50%      { transform: translate(-5px, -20px) rotate(-5deg) scale(0.95); opacity: 0.22; }
  75%      { transform: translate(12px, -8px) rotate(12deg) scale(1.05); opacity: 0.35; }
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

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

/* 响应式 */
@media (max-width: 768px) {
  .navbar {
    padding: 0.75rem 1.5rem;
  }

  .brand-text {
    font-size: 1.2rem;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .title-highlight {
    font-size: 3.2rem;
  }

  .hero-subtitle {
    font-size: 1rem;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
  }

  .features {
    gap: 1rem;
    padding: 0 1.5rem 1.5rem;
  }

  .feature-card {
    width: 100%;
    max-width: 300px;
  }

  .dashboard { padding: 0 1.5rem 2rem; gap: 1rem; }
  .quick-cards { gap: 0.75rem; }
  .quick-card { padding: 0.85rem; }
  .quick-icon { font-size: 1.2rem; }
  .quick-label { font-size: 0.85rem; }
  .stats-cards { gap: 0.75rem; }
  .stat-card { padding: 0.8rem 0.5rem; }
  .stat-icon { font-size: 1.1rem; }
  .stat-value { font-size: 0.95rem; }
  .stat-label { font-size: 0.68rem; }
  .learn-more-card { padding: 0.9rem 1.2rem; }
  .learn-more-icon { font-size: 1.4rem; }
  .learn-more-text h3 { font-size: 0.9rem; }
  .learn-more-text p { font-size: 0.75rem; }
}

@media (min-width: 1024px) {
  .navbar { padding: 1.25rem 4rem; }
  .hero { padding: 1rem 4rem 2rem; }
  .hero-content { max-width: 900px; }
  .hero-title { font-size: 4rem; }
  .title-highlight { font-size: 5rem; }
  .hero-subtitle { font-size: 1.4rem; }
  .btn-primary { padding: 1.1rem 3rem; font-size: 1.15rem; }

  .side-decor { display: block; }
  .sd { opacity: 0.22; }

  .dashboard {
    max-width: 860px;
    padding: 0 4rem 3rem;
    gap: 1.5rem;
  }
  .quick-cards { gap: 1.25rem; }
  .quick-card { padding: 1.15rem; border-radius: 18px; }
  .quick-icon { font-size: 1.6rem; }
  .quick-label { font-size: 1rem; }
  .stats-cards { gap: 1.25rem; }
  .stat-card { padding: 1.15rem; border-radius: 18px; }
  .stat-icon { font-size: 1.5rem; }
  .stat-value { font-size: 1.2rem; }
  .stat-label { font-size: 0.8rem; }
  .learn-more-card { padding: 1.25rem 1.75rem; }
  .learn-more-icon { font-size: 2rem; }
  .learn-more-text h3 { font-size: 1.05rem; }
  .learn-more-text p { font-size: 0.85rem; }
}

@media (min-width: 1440px) {
  .sd-1  { left: 5%;  font-size: 2.8rem; }
  .sd-3  { left: 3%;  font-size: 2.4rem; }
  .sd-7  { right: 10%; font-size: 2.6rem; }
  .sd-8  { right: 4%; font-size: 3.2rem; }
  .dashboard { max-width: 960px; }
}
</style>
