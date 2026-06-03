<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDreamStats, getEmotionDistribution, getPlaceDistribution, getRecentTrend, getUserByEmail } from '@/api/user'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()

const isLoading = ref(true)
const errorMsg = ref('')

// 统计数据
const totalDreams = ref(0)
const todayCount = ref(0)
const yesterdayCount = ref(0)
const emotionData = ref<{ label: string; icon: string; count: number; color: string }[]>([])
const placeData = ref<{ label: string; value: number }[]>([])
const weekData = ref<{ day: string; count: number }[]>([])

const trendDiff = computed(() => todayCount.value - yesterdayCount.value)

// 情绪图标映射（英文 key + 中文标签）
const emotionIcons: Record<string, string> = {
  'happy': '😊', '开心': '😊', '快乐': '😊', '高兴': '😊',
  'sad': '😢', '难过': '😢', '悲伤': '😢', '伤心': '😢',
  'scary': '😰', '恐惧': '😰', '害怕': '😰', '紧张': '😰',
  'angry': '😤', '愤怒': '😤', '生气': '😤',
  'peaceful': '😌', '平静': '😌', '安宁': '😌', '放松': '😌',
  'mysterious': '🔮', '神秘': '🔮',
  'excited': '🤩', '兴奋': '🤩'
}

// 情绪中文标签映射
const emotionLabels: Record<string, string> = {
  'happy': '开心', 'sad': '悲伤', 'scary': '恐惧',
  'angry': '愤怒', 'peaceful': '平静', 'mysterious': '神秘',
  'excited': '兴奋'
}

// 情绪颜色映射
const emotionColors: Record<string, string> = {
  'happy': '#4caf50', '开心': '#4caf50', '快乐': '#4caf50', '高兴': '#4caf50',
  'sad': '#64b5f6', '难过': '#64b5f6', '悲伤': '#64b5f6', '伤心': '#64b5f6',
  'scary': '#e53935', '恐惧': '#e53935', '害怕': '#e53935', '紧张': '#e53935',
  'angry': '#ff9800', '愤怒': '#ff9800', '生气': '#ff9800',
  'peaceful': '#7C6FE0', '平静': '#7C6FE0', '安宁': '#7C6FE0', '放松': '#7C6FE0',
  'mysterious': '#9c27b0', '神秘': '#9c27b0',
  'excited': '#FFB347', '兴奋': '#FFB347'
}

const maxEmotion = computed(() => Math.max(...emotionData.value.map(e => e.count), 1))
const maxWeek = computed(() => Math.max(...weekData.value.map(d => d.count), 1))
const maxPlace = computed(() => Math.max(...placeData.value.map(p => p.value), 1))

// 获取用户ID（如果localStorage中没有，则通过email查询）
async function getUserId(): Promise<string | number | null> {
  // 优先使用已保存的 userId
  if (userStore.userId) {
    return userStore.userId
  }

  // 如果没有 userId 但有 email，通过 email 查询
  if (userStore.email) {
    try {
      const res = await getUserByEmail(userStore.email)
      if (res.data.code === 200 && res.data.data) {
        const userId = res.data.data.id
        // 保存到 localStorage
        localStorage.setItem('userId', String(userId))
        userStore.$patch({ userId: String(userId) })
        return userId
      }
    } catch (e) {
      console.error('获取用户ID失败:', e)
    }
  }

  return null
}

// 加载统计数据
async function loadStats() {
  const userId = await getUserId()
  if (!userId) {
    errorMsg.value = '请先登录'
    isLoading.value = false
    return
  }

  try {
    isLoading.value = true
    errorMsg.value = ''

    // 并行请求数据
    const [statsRes, emotionRes, placeRes, trendRes] = await Promise.all([
      getDreamStats(userId),
      getEmotionDistribution(userId),
      getPlaceDistribution(userId),
      getRecentTrend(userId, 7)
    ])

    // 总数
    if (statsRes.data.code === 200) {
      totalDreams.value = statsRes.data.data.totalDreams
    }

    // 情绪分布
    if (emotionRes.data.code === 200) {
      emotionData.value = emotionRes.data.data.map(e => ({
        label: emotionLabels[e.label] || e.label,
        icon: emotionIcons[e.label] || '😊',
        count: e.value,
        color: emotionColors[e.label] || '#7C6FE0'
      }))
    }

    // 地点分布
    if (placeRes.data.code === 200) {
      placeData.value = placeRes.data.data
    }

    // 最近7天趋势
    if (trendRes.data.code === 200) {
      const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const today = new Date()
      const trend = trendRes.data.data

      // 构建最近7天的数据
      const weekDays = []
      for (let i = 6; i >= 0; i--) {
        const date = new Date(today)
        date.setDate(date.getDate() - i)
        const dateStr = date.toISOString().split('T')[0]
        const dayName = dayNames[date.getDay()]
        const found = trend.find(t => t.date === dateStr)
        const count = found ? found.count : 0
        weekDays.push({ day: dayName, count })

        if (i === 0) todayCount.value = count
        if (i === 1) yesterdayCount.value = count
      }
      weekData.value = weekDays
    }
  } catch (e: any) {
    errorMsg.value = e.message || '加载统计数据失败'
  } finally {
    isLoading.value = false
  }
}

function goBack() { router.push('/') }

onMounted(loadStats)
</script>

<template>
  <div class="stats-page">
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds"><div class="cloud cloud-1"></div><div class="cloud cloud-2"></div></div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <!-- 浮动装饰 -->
    <div class="decor">
      <span class="float-icon fi-1">🔮</span>
      <span class="float-icon fi-2">🌀</span>
      <span class="float-icon fi-3">🪐</span>
      <span class="float-icon fi-4">✨</span>
      <span class="float-icon fi-5">🎆</span>
    </div>

    <nav class="page-nav">
      <button class="back-btn" @click="goBack"><span>←</span><span>返回首页</span></button>
      <h1 class="page-title">📊 梦境统计</h1>
      <div class="nav-placeholder"></div>
    </nav>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>加载统计数据中...</p>
    </div>

    <!-- 错误提示 -->
    <div v-else-if="errorMsg" class="error-state">
      <p>{{ errorMsg }}</p>
      <button @click="loadStats" class="retry-btn">重试</button>
    </div>

    <!-- 统计内容 -->
    <div v-else class="content-wrapper">
      <!-- 概览卡片 -->
      <div class="overview-grid">
        <div class="overview-card glass card-enter" style="animation-delay: 0.1s">
          <span class="ov-icon">🌙</span>
          <span class="ov-value">{{ totalDreams }}</span>
          <span class="ov-label">梦境总数</span>
        </div>
        <div class="overview-card glass card-enter" style="animation-delay: 0.2s">
          <span class="ov-icon">📅</span>
          <div class="ov-value-row">
            <span class="ov-value">{{ todayCount }}</span>
            <span v-if="trendDiff !== 0" class="ov-trend" :class="trendDiff > 0 ? 'up' : 'down'">
              {{ trendDiff > 0 ? '↑' : '↓' }} {{ Math.abs(trendDiff) }}
            </span>
          </div>
          <span class="ov-label">今日梦境 <span class="ov-sub">较昨日</span></span>
        </div>
        <div class="overview-card glass card-enter" style="animation-delay: 0.3s">
          <span class="ov-icon">📊</span>
          <span class="ov-value">{{ weekData.reduce((s, d) => s + d.count, 0) }}</span>
          <span class="ov-label">本周梦境</span>
        </div>
        <div class="overview-card glass card-enter" style="animation-delay: 0.4s">
          <span class="ov-icon">{{ emotionData.length > 0 ? emotionData[0].icon : '🌙' }}</span>
          <span class="ov-value">{{ emotionData.length > 0 ? emotionData[0].label : '-' }}</span>
          <span class="ov-label">最常情绪</span>
        </div>
      </div>

      <!-- 双列区域 -->
      <div class="stats-grid">
        <!-- 情绪分布 -->
        <div v-if="emotionData.length > 0" class="section-card glass">
          <h2 class="section-title">😊 情绪分布</h2>
          <div class="emotion-bars">
            <div v-for="e in emotionData" :key="e.label" class="emotion-row">
              <span class="emotion-label">{{ e.icon }} {{ e.label }}</span>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: (e.count / maxEmotion * 100) + '%', background: e.color }"></div>
              </div>
              <span class="bar-count">{{ e.count }}</span>
            </div>
          </div>
        </div>

        <!-- 地点分布 -->
        <div v-if="placeData.length > 0" class="section-card glass">
          <h2 class="section-title">📍 梦境地点</h2>
          <div class="place-bars">
            <div v-for="p in placeData" :key="p.label" class="place-row">
              <span class="place-label">{{ p.label }}</span>
              <div class="bar-track">
                <div class="bar-fill place-fill" :style="{ width: (p.value / maxPlace * 100) + '%' }"></div>
              </div>
              <span class="bar-count">{{ p.value }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 本周梦境 -->
      <div v-if="weekData.length > 0" class="section-card glass">
        <h2 class="section-title">📅 最近7天</h2>
        <div class="week-chart">
          <div v-for="d in weekData" :key="d.day" class="week-col">
            <div class="week-bar-wrap">
              <div class="week-bar" :style="{ height: (d.count / maxWeek * 100) + '%' }"></div>
            </div>
            <span class="week-label">{{ d.day }}</span>
            <span class="week-count">{{ d.count }}</span>
          </div>
        </div>
      </div>

      <!-- 无数据提示 -->
      <div v-if="totalDreams === 0" class="empty-state">
        <span class="empty-icon">🌙</span>
        <p>还没有梦境记录</p>
        <button @click="router.push('/record-dream')" class="add-btn">记录第一个梦</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-page {
  min-height: 100vh; position: relative;
  background: linear-gradient(135deg, var(--bg-start) 0%, var(--bg-end) 100%);
  padding-bottom: 2rem;
}

.dream-bg { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.stars {
  position: absolute; inset: 0;
  background-image:
    radial-gradient(2px 2px at 20px 30px, rgba(255,255,255,0.8), transparent),
    radial-gradient(2px 2px at 40px 70px, rgba(255,255,255,0.6), transparent),
    radial-gradient(1px 1px at 90px 40px, rgba(255,255,255,0.7), transparent),
    radial-gradient(1px 1px at 130px 80px, rgba(255,255,255,0.5), transparent);
  background-repeat: repeat; background-size: 200px 100px;
  animation: twinkle 4s ease-in-out infinite alternate;
}
.cloud { position: absolute; background: rgba(255,255,255,0.5); border-radius: 50%; filter: blur(50px); }
.cloud-1 { width: 500px; height: 180px; top: 10%; left: -15%; animation: float-cloud 25s ease-in-out infinite; }
.cloud-2 { width: 350px; height: 140px; bottom: 15%; right: -10%; animation: float-cloud 30s ease-in-out infinite reverse; }
.glow { position: absolute; border-radius: 50%; filter: blur(100px); }
.glow-1 { width: 300px; height: 300px; top: 5%; right: 15%; background: rgba(124,111,224,0.2); animation: pulse 8s ease-in-out infinite; }
.glow-2 { width: 250px; height: 250px; bottom: 10%; left: 10%; background: rgba(255,179,71,0.12); animation: pulse 10s ease-in-out infinite 3s; }

/* 浮动装饰 */
.decor { position: fixed; inset: 0; pointer-events: none; z-index: 1; }
.float-icon { position: absolute; font-size: 1.5rem; animation: float 6s ease-in-out infinite; opacity: 0.6; }
.fi-1 { top: 10%; left: 5%; font-size: 1.6rem; animation-delay: 0s; }
.fi-2 { top: 25%; right: 6%; font-size: 2rem; animation-delay: 1.5s; }
.fi-3 { bottom: 22%; left: 12%; font-size: 1.8rem; animation-delay: 3s; }
.fi-4 { top: 60%; right: 10%; font-size: 1.2rem; animation-delay: 4.5s; opacity: 0.4; }
.fi-5 { bottom: 10%; right: 18%; font-size: 1.5rem; animation-delay: 2s; }

.page-nav {
  position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 2rem;
}
.back-btn {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.7rem 1.2rem; min-height: 44px;
  background: var(--glass-bg); backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border); border-radius: 50px;
  color: var(--text-dark); font-size: 0.9rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.back-btn:hover { background: rgba(255,255,255,0.45); transform: translateX(-3px); }
.page-title { font-size: 1.4rem; color: var(--text-dark); }
.nav-placeholder { width: 100px; }

.content-wrapper {
  position: relative; z-index: 10;
  max-width: 700px; margin: 0 auto; padding: 0 1.5rem;
  display: flex; flex-direction: column; gap: 1.25rem;
}

.stats-grid {
  display: flex; flex-direction: column; gap: 1.25rem;
}

.glass {
  background: rgba(255,255,255,0.35); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.2) inset;
}

/* 概览 */
.overview-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }
.overview-card {
  padding: 1.25rem; border-radius: 18px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 0.35rem;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative; overflow: hidden;
}
.overview-card:hover { transform: translateY(-5px); box-shadow: 0 8px 25px rgba(107,140,255,0.2); }
.ov-icon { font-size: 1.5rem; }
.ov-value-row { display: flex; align-items: baseline; gap: 0.4rem; }
.ov-value {
  font-size: 1.3rem; font-weight: 700;
  background: linear-gradient(135deg, #6B8CFF, #FF8FAB);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.ov-trend {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
}
.ov-trend.up {
  color: #16a34a;
  background: rgba(22, 163, 74, 0.12);
}
.ov-trend.down {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.12);
}
.ov-label { font-size: 0.78rem; color: var(--text-light); }
.ov-sub { font-size: 0.68rem; opacity: 0.7; }

/* 通用图表卡片 */
.section-card { padding: 1.75rem; border-radius: 20px; }
.section-title { font-size: 1.1rem; font-weight: 600; color: var(--text-dark); margin-bottom: 1.25rem; }

/* 情绪分布 */
.emotion-bars { display: flex; flex-direction: column; gap: 0.85rem; }
.emotion-row { display: flex; align-items: center; gap: 0.75rem; transition: background 0.2s; padding: 0.25rem 0.35rem; border-radius: 8px; }
.emotion-row:hover { background: rgba(124,111,224,0.04); }
.emotion-label { font-size: 0.85rem; color: var(--text-dark); min-width: 80px; }
.bar-track { flex: 1; height: 14px; background: rgba(124,111,224,0.08); border-radius: 7px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 7px; transition: width 0.8s cubic-bezier(0.16,1,0.3,1); }
.bar-count { font-size: 0.85rem; font-weight: 600; color: var(--text-dark); min-width: 24px; text-align: right; }

/* 本周柱状图 */
.week-chart { display: flex; justify-content: space-between; align-items: flex-end; height: 140px; gap: 0.5rem; }
.week-col { display: flex; flex-direction: column; align-items: center; gap: 0.3rem; flex: 1; }
.week-bar-wrap {
  width: 100%; height: 100px; display: flex; align-items: flex-end; justify-content: center;
}
.week-bar {
  width: 60%; max-width: 40px; min-height: 4px;
  background: linear-gradient(to top, var(--primary), var(--secondary));
  border-radius: 8px 8px 2px 2px; transition: height 0.8s cubic-bezier(0.16,1,0.3,1), box-shadow 0.3s;
  cursor: pointer;
}
.week-bar:hover { box-shadow: 0 0 12px rgba(124,111,224,0.35); }
.week-label { font-size: 0.75rem; color: var(--text-light); }
.week-count { font-size: 0.78rem; font-weight: 600; color: var(--text-dark); }

/* 月度趋势 */
.month-chart { display: flex; justify-content: space-around; align-items: flex-end; height: 140px; gap: 1rem; }
.month-col { display: flex; flex-direction: column; align-items: center; gap: 0.3rem; flex: 1; }
.month-count { font-size: 0.85rem; font-weight: 600; color: var(--text-dark); }
.month-bar-wrap { width: 100%; height: 90px; display: flex; align-items: flex-end; justify-content: center; }
.month-bar {
  width: 50%; max-width: 48px; min-height: 4px;
  background: linear-gradient(to top, var(--secondary), #FFD08A);
  border-radius: 6px 6px 2px 2px; transition: height 0.8s cubic-bezier(0.16,1,0.3,1);
}
.month-label { font-size: 0.8rem; color: var(--text-light); }

/* 关键词云 */
.keyword-cloud { display: flex; flex-wrap: wrap; gap: 0.75rem; justify-content: center; }
.keyword-tag {
  padding: 0.4rem 1rem; border-radius: 50px;
  background: rgba(124,111,224,0.08); color: var(--primary);
  font-weight: 500; transition: all 0.3s ease; cursor: default;
}
.keyword-tag:hover { background: rgba(124,111,224,0.15); transform: scale(1.05); }

/* 时间分布 */
.time-bars { display: flex; flex-direction: column; gap: 0.85rem; }
.time-row { display: flex; align-items: center; gap: 0.75rem; }
.time-label { font-size: 0.88rem; color: var(--text-dark); min-width: 140px; }
.time-fill { background: linear-gradient(90deg, var(--primary), var(--secondary)); }

@keyframes twinkle { 0% { opacity: 0.5; } 100% { opacity: 1; } }
@keyframes float-cloud { 0%,100% { transform: translateX(0) translateY(0); } 50% { transform: translateX(50px) translateY(-20px); } }
@keyframes pulse { 0%,100% { opacity: 0.4; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.08); } }

@media (max-width: 768px) {
  .page-nav { padding: 1rem; }
  .content-wrapper { padding: 0 1rem; max-width: 100%; }
  .page-title { font-size: 1.1rem; }
  .nav-placeholder { display: none; }
  .overview-grid { grid-template-columns: repeat(2, 1fr); }
  .section-card { padding: 1.25rem; }
  .time-label { min-width: 110px; font-size: 0.8rem; }
  .emotion-label { min-width: 60px; font-size: 0.8rem; }
  .place-label { min-width: 60px; font-size: 0.8rem; }
}

@media (max-width: 480px) {
  .overview-grid { grid-template-columns: repeat(2, 1fr); gap: 0.5rem; }
  .overview-card { padding: 0.75rem; }
  .ov-icon { font-size: 1.2rem; }
  .ov-value { font-size: 1rem; }
  .ov-label { font-size: 0.68rem; }
  .section-card { padding: 1rem; }
  .section-title { font-size: 0.95rem; margin-bottom: 0.75rem; }
  .emotion-label { min-width: 50px; font-size: 0.75rem; }
  .bar-count { font-size: 0.75rem; }
  .place-label { min-width: 50px; font-size: 0.75rem; }
  .week-chart { height: 100px; }
  .week-bar-wrap { height: 70px; }
  .week-label { font-size: 0.7rem; }
  .week-count { font-size: 0.72rem; }
  .time-label { min-width: 90px; font-size: 0.75rem; }
  .empty-state { padding: 2rem 1rem; }
}

@media (min-width: 1024px) {
  .page-nav { padding: 1.5rem 3rem; }
  .content-wrapper { max-width: 1000px; padding: 0 2rem; }
  .overview-grid { grid-template-columns: repeat(4, 1fr); }
  .overview-card { padding: 1.5rem; }
  .ov-icon { font-size: 2rem; }
  .ov-value { font-size: 1.6rem; }
  .ov-label { font-size: 0.88rem; }
  .stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
  .section-card { padding: 2rem; }
  .section-title { font-size: 1.2rem; margin-bottom: 1.5rem; }
  .emotion-label { font-size: 0.95rem; min-width: 90px; }
  .bar-count { font-size: 0.95rem; }
  .place-label { font-size: 0.95rem; min-width: 90px; }
  .week-chart { height: 200px; gap: 1rem; }
  .week-bar-wrap { height: 140px; }
  .week-bar { max-width: 48px; }
  .week-label { font-size: 0.85rem; }
  .week-count { font-size: 0.88rem; }
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  gap: 1rem;
  position: relative;
  z-index: 10;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(124, 111, 224, 0.2);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  color: var(--text-light);
  font-size: 0.95rem;
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  gap: 1rem;
  position: relative;
  z-index: 10;
}

.error-state p {
  color: #e53935;
  font-size: 0.95rem;
}

.retry-btn {
  padding: 0.6rem 1.5rem;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: var(--primary-light);
}

/* 地点分布 */
.place-bars {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.place-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  transition: background 0.2s;
  padding: 0.25rem 0.35rem;
  border-radius: 8px;
}
.place-row:hover { background: rgba(255,179,71,0.06); }

.place-label {
  font-size: 0.85rem;
  color: var(--text-dark);
  min-width: 80px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-fill {
  background: linear-gradient(90deg, #FFB347, #FFD08A);
  border-radius: 7px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 1rem;
  position: relative;
  z-index: 10;
}

.empty-icon {
  font-size: 3rem;
}

.empty-state p {
  color: var(--text-light);
  font-size: 1rem;
}

.add-btn {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(124, 111, 224, 0.3);
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(124, 111, 224, 0.4);
}

/* 深夜模式 */
html.dark .stars {
  background-image:
    radial-gradient(2px 2px at 20px 30px, rgba(155,143,255,0.6), transparent),
    radial-gradient(2px 2px at 40px 70px, rgba(155,143,255,0.5), transparent),
    radial-gradient(1px 1px at 90px 40px, rgba(155,143,255,0.4), transparent),
    radial-gradient(1px 1px at 130px 80px, rgba(155,143,255,0.4), transparent),
    radial-gradient(2px 2px at 160px 30px, rgba(155,143,255,0.5), transparent);
}
html.dark .cloud { background: rgba(155, 143, 255, 0.12); }
html.dark .glow-1 { background: rgba(124, 111, 224, 0.15); }
html.dark .glow-2 { background: rgba(255, 179, 71, 0.08); }
html.dark .back-btn:hover { background: rgba(155, 143, 255, 0.15); }
html.dark .page-title { color: #E8E4F0; }
html.dark .page-subtitle { color: #A9A3C0; }
html.dark .glass {
  background: rgba(31, 27, 49, 0.86);
  border-color: rgba(184, 174, 255, 0.38);
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.42), 0 0 0 1px rgba(184, 174, 255, 0.08) inset;
}
html.dark .section-title { color: #E8E4F0; }
html.dark .overview-card:hover {
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.38), 0 0 26px rgba(155, 143, 255, 0.14);
}
html.dark .ov-label { color: #A9A3C0; }
html.dark .ov-sub { color: #A9A3C0; }
html.dark .trend-up { color: #81C784; background: rgba(129, 199, 132, 0.12); }
html.dark .trend-down { color: #FF6B6B; background: rgba(255, 107, 107, 0.12); }
html.dark .emotion-row:hover { background: rgba(155, 143, 255, 0.06); }
html.dark .emotion-name { color: #E8E4F0; }
html.dark .emotion-count { color: #A9A3C0; }
html.dark .bar-track { background: rgba(155, 143, 255, 0.1); }
html.dark .week-bar { color: #A9A3C0; }
html.dark .week-bar:hover { box-shadow: 0 4px 15px rgba(155, 143, 255, 0.2); }
html.dark .week-label { color: #A9A3C0; }
html.dark .week-count { color: #E8E4F0; }
html.dark .keyword-tag {
  background: rgba(155, 143, 255, 0.1);
  color: #B8AEFF;
}
html.dark .keyword-tag:hover { background: rgba(155, 143, 255, 0.18); }
html.dark .spinner { border-color: rgba(155, 143, 255, 0.2); border-top-color: #B8AEFF; }
html.dark .error-text { color: #FF6B6B; }
html.dark .retry-btn { box-shadow: 0 4px 15px rgba(155, 143, 255, 0.25); }
html.dark .place-row:hover { background: rgba(255, 179, 71, 0.06); }
html.dark .place-name { color: #E8E4F0; }
html.dark .place-count { color: #A9A3C0; }
html.dark .back-link { color: #A9A3C0; }
html.dark .back-link:hover { color: #B8AEFF; }
html.dark .add-btn { box-shadow: 0 4px 15px rgba(155, 143, 255, 0.25); }
html.dark .add-btn:hover { box-shadow: 0 6px 20px rgba(155, 143, 255, 0.35); }
</style>
