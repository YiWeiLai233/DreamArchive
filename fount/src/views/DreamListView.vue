<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { deleteDream, getUserDreams, triggerAnalyze } from '@/api/dream'
import { getUserByEmail } from '@/api/user'
import { useUserStore } from '@/stores'
import { formatDreamInterpretation } from '@/utils/dreamInterpretation'
import { getAnalysisFields, isPendingAnalysis, needsAnalysis } from '@/utils/analysisStatus'

const router = useRouter()
const userStore = useUserStore()

const searchQuery = ref('')
const activeFilter = ref('all')
const showDetail = ref(false)
const selectedDream = ref<Dream | null>(null)
const isLoading = ref(true)
const isRefreshingPendingDreams = ref(false)
const isDeleting = ref(false)
const errorMsg = ref('')
const showDeleteConfirm = ref(false)
const dreamPendingDelete = ref<Dream | null>(null)

interface Dream {
  id: string
  title: string
  content: string
  emotion: string
  place: string
  time: string
  interpretation: string
  analysisStatus?: string
  analysisError?: string
  imageUrl?: string
  createdAt: string
}

const filters = [
  { key: 'all', label: '全部', icon: '🌙' },
  { key: 'happy', label: '开心', icon: '😊' },
  { key: 'sad', label: '悲伤', icon: '😢' },
  { key: 'scary', label: '恐惧', icon: '😰' },
  { key: 'mysterious', label: '神秘', icon: '🔮' }
]

const dreams = ref<Dream[]>([])
let pendingDreamsRefreshTimer: ReturnType<typeof window.setInterval> | null = null

const PENDING_DREAMS_REFRESH_INTERVAL_MS = 5000

async function getUserId(): Promise<string | number | null> {
  if (userStore.userId) return userStore.userId
  if (userStore.email) {
    try {
      const res = await getUserByEmail(userStore.email)
      if (res.data.code === 200 && res.data.data) {
        const userId = res.data.data.id
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

function formatCreatedAt(raw: string): string {
  if (!raw) return ''
  const d = new Date(raw)
  if (isNaN(d.getTime())) return raw
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function hasPendingDreams() {
  return dreams.value.some(dream => isPendingAnalysis(dream))
}

function stopPendingDreamsRefresh() {
  if (pendingDreamsRefreshTimer) {
    window.clearInterval(pendingDreamsRefreshTimer)
    pendingDreamsRefreshTimer = null
  }
}

function startPendingDreamsRefresh() {
  if (pendingDreamsRefreshTimer) return

  pendingDreamsRefreshTimer = window.setInterval(() => {
    if (document.visibilityState === 'hidden') return
    if (!hasPendingDreams()) {
      stopPendingDreamsRefresh()
      return
    }
    void refreshPendingDreams()
  }, PENDING_DREAMS_REFRESH_INTERVAL_MS)
}

function syncPendingDreamsRefresh() {
  if (hasPendingDreams()) {
    startPendingDreamsRefresh()
  } else {
    stopPendingDreamsRefresh()
  }
}

async function refreshPendingDreams() {
  if (isRefreshingPendingDreams.value || !hasPendingDreams()) return

  isRefreshingPendingDreams.value = true
  try {
    await loadDreams({ silent: true })
  } finally {
    isRefreshingPendingDreams.value = false
  }
}

function refreshPendingDreamsOnResume() {
  if (document.visibilityState === 'hidden') return
  if (hasPendingDreams()) {
    startPendingDreamsRefresh()
    void refreshPendingDreams()
  }
}

async function loadDreams(options: { silent?: boolean } = {}) {
  const userId = await getUserId()
  if (!userId) {
    if (!options.silent) {
      errorMsg.value = '请先登录'
      isLoading.value = false
    }
    return
  }

  try {
    if (!options.silent) {
      isLoading.value = true
      errorMsg.value = ''
    }
    const res = await getUserDreams(Number(userId))
    if (res.data.code === 200) {
      const nextDreams = (res.data.data || []).map(d => ({
        id: d.id,
        title: d.title || (d.content ? d.content.slice(0, 20) + (d.content.length > 20 ? '...' : '') : '未命名梦境'),
        content: d.content || '',
        emotion: d.emotion || 'mysterious',
        place: d.place || '未知',
        time: d.time || '',
        interpretation: d.interpretation || '暂无解析',
        ...getAnalysisFields(d),
        imageUrl: d.imageUrl || '',
        createdAt: formatCreatedAt(d.createdAt)
      }))
      dreams.value = nextDreams
      if (selectedDream.value) {
        const updatedSelectedDream = nextDreams.find(dream => dream.id === selectedDream.value?.id)
        if (updatedSelectedDream) {
          selectedDream.value = updatedSelectedDream
        }
      }
      syncPendingDreamsRefresh()
    } else {
      if (!options.silent) {
        errorMsg.value = res.data.message || '加载失败'
      }
    }
  } catch (e: any) {
    if (!options.silent) {
      errorMsg.value = e.message || '网络错误'
    }
  } finally {
    if (!options.silent) {
      isLoading.value = false
    }
  }
}

onMounted(() => {
  void loadDreams()
  document.addEventListener('visibilitychange', refreshPendingDreamsOnResume)
  window.addEventListener('focus', refreshPendingDreamsOnResume)
  window.addEventListener('pageshow', refreshPendingDreamsOnResume)
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', refreshPendingDreamsOnResume)
  window.removeEventListener('focus', refreshPendingDreamsOnResume)
  window.removeEventListener('pageshow', refreshPendingDreamsOnResume)
  stopPendingDreamsRefresh()
})

const filteredDreams = computed(() => {
  let result = dreams.value
  if (activeFilter.value !== 'all') {
    result = result.filter(d => d.emotion === activeFilter.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(d =>
      d.title.toLowerCase().includes(q) ||
      d.content.toLowerCase().includes(q) ||
      d.place.toLowerCase().includes(q)
    )
  }
  return result
})

const selectedInterpretationBlocks = computed(() => formatDreamInterpretation(selectedDream.value?.interpretation))

function getEmotionIcon(emotion: string) {
  const map: Record<string, string> = { happy: '😊', sad: '😢', scary: '😰', mysterious: '🔮', peaceful: '😌', excited: '🤩', angry: '😤' }
  return map[emotion] || '🌙'
}

function getEmotionLabel(emotion: string) {
  const map: Record<string, string> = { happy: '开心', sad: '悲伤', scary: '恐惧', mysterious: '神秘', peaceful: '平静', excited: '兴奋', angry: '愤怒' }
  return map[emotion] || '未知'
}

function getEmotionColor(emotion: string): string {
  const map: Record<string, string> = {
    happy: '#4caf50', sad: '#64b5f6', scary: '#e53935', mysterious: '#9c27b0',
    peaceful: '#7C6FE0', excited: '#FFB347', angry: '#ff9800'
  }
  return map[emotion] || '#7C6FE0'
}

async function handleAnalyze(dream: Dream) {
  try {
    const { data } = await triggerAnalyze(dream.id)
    if (data.code === 200) {
      dream.analysisStatus = 'PENDING'
      dream.analysisError = undefined
      syncPendingDreamsRefresh()
    }
  } catch {
    // 静默失败
  }
}

function openDetail(dream: Dream) {
  selectedDream.value = dream
  showDetail.value = true
}

function closeDetail() {
  showDetail.value = false
  selectedDream.value = null
}

function askDeleteDream(dream: Dream) {
  dreamPendingDelete.value = dream
  showDeleteConfirm.value = true
}

function closeDeleteConfirm() {
  if (isDeleting.value) return
  showDeleteConfirm.value = false
  dreamPendingDelete.value = null
}

async function confirmDeleteDream() {
  if (!dreamPendingDelete.value || isDeleting.value) return

  const dreamId = dreamPendingDelete.value.id
  try {
    isDeleting.value = true
    const res = await deleteDream(dreamId)
    if (res.data.code === 200) {
      dreams.value = dreams.value.filter(d => d.id !== dreamId)
      if (selectedDream.value?.id === dreamId) {
        closeDetail()
      }
      showDeleteConfirm.value = false
      dreamPendingDelete.value = null
    } else {
      alert(res.data.message || '删除失败，请重试')
    }
  } catch (e: any) {
    alert(e.message || '删除失败，请重试')
  } finally {
    isDeleting.value = false
  }
}

function goBack() {
  router.push('/')
}
</script>

<template>
  <div class="dream-list-page">
    <!-- 背景 -->
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds">
        <div class="cloud cloud-1"></div>
        <div class="cloud cloud-2"></div>
      </div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <!-- 浮动装饰 -->
    <div class="decor">
      <span class="float-icon fi-1">💭</span>
      <span class="float-icon fi-2">🌙</span>
      <span class="float-icon fi-3">🦋</span>
      <span class="float-icon fi-4">☁️</span>
      <span class="float-icon fi-5">💤</span>
    </div>

    <!-- 顶部导航 -->
    <nav class="page-nav">
      <button class="back-btn" @click="goBack">
        <span>←</span>
        <span>返回首页</span>
      </button>
      <h1 class="page-title">📖 我的梦境</h1>
      <div class="nav-placeholder"></div>
    </nav>

    <!-- 搜索和筛选 -->
    <div class="toolbar">
      <div class="search-box glass">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索梦境..."
        />
      </div>
      <div class="filter-tabs">
        <button
          v-for="f in filters"
          :key="f.key"
          class="filter-tab"
          :class="{ active: activeFilter === f.key }"
          @click="activeFilter = f.key"
        >
          <span>{{ f.icon }}</span>
          <span>{{ f.label }}</span>
        </button>
      </div>
    </div>

    <!-- 梦境列表 -->
    <div class="dreams-container">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>加载梦境中...</p>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="errorMsg" class="error-state">
        <p>{{ errorMsg }}</p>
        <button @click="loadDreams()" class="retry-btn">重试</button>
      </div>

      <div v-else-if="filteredDreams.length === 0" class="empty-state glass">
        <div class="empty-visual">
          <span class="empty-star s1">✨</span>
          <span class="empty-icon">🌙</span>
          <span class="empty-star s2">⭐</span>
          <span class="empty-star s3">💫</span>
        </div>
        <p class="empty-text">暂无梦境记录</p>
        <p class="empty-hint">开始记录你的第一个梦吧</p>
        <button class="empty-cta" @click="router.push('/record-dream')">✨ 开始记录</button>
      </div>

      <div v-else class="dreams-grid">
        <div
          v-for="(dream, index) in filteredDreams"
          :key="dream.id"
          class="dream-card glass card-enter"
          :style="{ animationDelay: Math.min(index * 0.06, 0.6) + 's' }"
          @click="openDetail(dream)"
        >
          <div class="card-top">
            <div class="card-badges">
              <span class="emotion-badge" :style="{ background: getEmotionColor(dream.emotion) + '18', color: getEmotionColor(dream.emotion) }">{{ getEmotionIcon(dream.emotion) }} {{ getEmotionLabel(dream.emotion) }}</span>
              <span v-if="isPendingAnalysis(dream)" class="analysis-badge">🔮 解析中</span>
              <span v-else-if="dream.analysisStatus === 'FAILED'" class="analysis-badge failed">⚠️ 解析失败</span>
            </div>
            <span class="dream-date">{{ dream.createdAt }}</span>
          </div>
          <h3 class="dream-title">{{ dream.title }}</h3>
          <img v-if="dream.imageUrl" :src="dream.imageUrl" class="dream-card-image" />
          <p v-else class="dream-preview">{{ dream.content }}</p>
          <div class="card-bottom">
            <span class="dream-meta">📍 {{ dream.place }}</span>
            <span class="dream-meta">🕐 {{ dream.time }}</span>
          </div>
          <div class="card-footer-actions">
            <button
              v-if="needsAnalysis(dream)"
              class="card-action-btn analyze"
              @click.stop="handleAnalyze(dream)"
            >🔮 AI 解析</button>
            <span v-else></span>
            <button
              class="card-action-btn delete"
              @click.stop="askDeleteDream(dream)"
            >🗑 删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <Transition name="modal">
      <div v-if="showDetail && selectedDream" class="modal-overlay" @click.self="closeDetail">
        <div class="modal-card glass">
          <button class="modal-close" @click="closeDetail">✕</button>
          <div class="modal-header">
            <span class="modal-emotion">{{ getEmotionIcon(selectedDream.emotion) }}</span>
            <h2 class="modal-title">{{ selectedDream.title }}</h2>
            <div class="modal-meta">
              <span>📍 {{ selectedDream.place }}</span>
              <span>🕐 {{ selectedDream.time }}</span>
              <span>📅 {{ selectedDream.createdAt }}</span>
            </div>
          </div>
          <div class="modal-body">
            <div class="section">
              <h3 class="section-title">梦境内容</h3>
              <p class="section-text">{{ selectedDream.content }}</p>
              <img v-if="selectedDream.imageUrl" :src="selectedDream.imageUrl" class="detail-image" />
            </div>
            <div class="section">
              <h3 class="section-title">🔮 AI 解析</h3>
              <div v-if="isPendingAnalysis(selectedDream)" class="analysis-refresh-status">
                <span class="analysis-spinner"></span>
                <span>正在解读梦境，稍等片刻...</span>
              </div>
              <div class="interpretation">
                <div v-if="selectedInterpretationBlocks.length" class="interpretation-content">
                  <template
                    v-for="(block, index) in selectedInterpretationBlocks"
                    :key="`${block.type}-${index}`"
                  >
                    <h4 v-if="block.type === 'heading'" class="interpretation-heading">
                      {{ block.content }}
                    </h4>
                    <div v-else-if="block.type === 'item'" class="interpretation-item">
                      <span class="interpretation-number">{{ block.marker }}</span>
                      <p>{{ block.content }}</p>
                    </div>
                    <p v-else class="interpretation-paragraph">{{ block.content }}</p>
                  </template>
                </div>
                <p v-else class="section-text">暂无解析内容</p>
                <div class="ai-disclaimer" v-if="selectedInterpretationBlocks.length">
                  <p>本解析仅用于梦境记录和自我观察，不构成医学、心理诊断或治疗建议。</p>
                  <p>如长期出现严重焦虑、抑郁、失眠或创伤相关梦境，请咨询专业人士。</p>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="delete-detail-btn" @click="askDeleteDream(selectedDream)">
              <span>🗑</span>
              <span>删除梦境</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal">
      <div
        v-if="showDeleteConfirm && dreamPendingDelete"
        class="modal-overlay confirm-overlay"
        @click.self="closeDeleteConfirm"
      >
        <div class="confirm-card glass">
          <div class="confirm-icon">🗑</div>
          <h2 class="confirm-title">确认删除这个梦境？</h2>
          <p class="confirm-text">
            《{{ dreamPendingDelete.title }}》删除后无法恢复，请确认后再继续。
          </p>
          <div class="confirm-actions">
            <button class="confirm-cancel" :disabled="isDeleting" @click="closeDeleteConfirm">
              取消
            </button>
            <button class="confirm-delete" :disabled="isDeleting" @click="confirmDeleteDream">
              {{ isDeleting ? '正在删除...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.dream-list-page {
  min-height: 100vh;
  position: relative;
  background: linear-gradient(135deg, var(--bg-start) 0%, var(--bg-end) 100%);
  padding-bottom: 2rem;
}

/* 背景 */
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
.fi-1 { top: 8%; left: 6%; font-size: 2rem; animation-delay: 0s; }
.fi-2 { top: 22%; right: 8%; font-size: 1.6rem; animation-delay: 1.5s; }
.fi-3 { bottom: 28%; left: 10%; font-size: 1.2rem; animation-delay: 3s; }
.fi-4 { top: 58%; right: 5%; font-size: 2.2rem; animation-delay: 4.5s; opacity: 0.4; }
.fi-5 { bottom: 10%; right: 18%; font-size: 1.4rem; animation-delay: 2s; }

/* 导航 */
.page-nav {
  position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 1.25rem 2rem;
}
.back-btn {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  background: var(--glass-bg); backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border); border-radius: 50px;
  color: var(--text-dark); font-size: 0.9rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.back-btn:hover { background: rgba(255,255,255,0.45); transform: translateX(-3px); }
.page-title { font-size: 1.4rem; color: var(--text-dark); }
.nav-placeholder { width: 100px; }

/* 工具栏 */
.toolbar {
  position: relative; z-index: 10;
  display: flex; align-items: center; gap: 1rem;
  padding: 0 2rem; margin-bottom: 1.5rem; flex-wrap: wrap;
}
.search-box {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.6rem 1rem; border-radius: 50px; flex: 1; min-width: 200px; max-width: 360px;
}
.search-icon { font-size: 1rem; }
.search-box input {
  flex: 1; border: none; background: transparent; outline: none;
  font-size: 0.9rem; color: var(--text-dark); font-family: 'Noto Sans SC', sans-serif;
}
.search-box input::placeholder { color: var(--text-light); }
.filter-tabs { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.filter-tab {
  display: flex; align-items: center; gap: 0.3rem;
  padding: 0.55rem 1rem; min-height: 44px; border: 1.5px solid var(--glass-border);
  border-radius: 50px; background: var(--glass-bg); backdrop-filter: blur(10px);
  color: var(--text-light); font-size: 0.85rem; cursor: pointer;
  transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.filter-tab:hover { border-color: var(--primary); color: var(--primary); }
.filter-tab.active {
  background: var(--primary); border-color: var(--primary); color: white;
  box-shadow: 0 2px 12px rgba(124,111,224,0.3);
}

/* 梦境网格 */
.dreams-container { position: relative; z-index: 10; padding: 0 2rem; }
.dreams-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.25rem;
}
.dream-card {
  padding: 1.5rem; border-radius: 18px; cursor: pointer;
  transition: all 0.3s ease;
  display: flex; flex-direction: column;
  position: relative;
}
.dream-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 50px rgba(107,140,255,0.2), 0 0 0 2px rgba(107,140,255,0.2);
  background: rgba(255,255,255,0.55);
}
.glass {
  background: rgba(255,255,255,0.35); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.2) inset;
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; gap: 0.75rem; }
.card-badges {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
  min-width: 0;
}
.emotion-badge {
  font-size: 0.8rem; padding: 0.25rem 0.75rem; border-radius: 50px;
  background: rgba(124,111,224,0.1); color: var(--primary);
}
.analysis-badge {
  font-size: 0.75rem;
  padding: 0.22rem 0.65rem;
  border-radius: 50px;
  background: rgba(124,111,224,0.13);
  color: var(--primary);
  font-weight: 600;
  animation: badge-pulse 2s ease-in-out infinite;
}
@keyframes badge-pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
.analysis-badge.failed {
  background: rgba(255,107,107,0.13);
  color: #e74c3c;
  animation: none;
}
html.dark .analysis-badge.failed { background: rgba(255,107,107,0.15); color: #ff8a80; }
.card-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.dream-date { font-size: 0.8rem; color: var(--text-light); }
.card-footer-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: auto;
  padding-top: 0.65rem;
  border-top: 1px solid rgba(124,111,224,0.08);
  justify-content: space-between;
  min-height: 38px;
  align-items: center;
}
.card-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-size: 0.78rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Sans SC', sans-serif;
}
.card-action-btn.analyze {
  border: 1px solid rgba(124,111,224,0.2);
  background: rgba(124,111,224,0.06);
  color: var(--primary);
}
.card-action-btn.analyze:hover {
  background: rgba(124,111,224,0.15);
  border-color: rgba(124,111,224,0.4);
}
.card-action-btn.delete {
  border: 1px solid rgba(229,57,53,0.18);
  background: rgba(229,57,53,0.04);
  color: #c62828;
}
.card-action-btn.delete:hover {
  background: rgba(229,57,53,0.1);
  border-color: rgba(229,57,53,0.35);
}
.dream-title { font-size: 1.15rem; font-weight: 600; color: var(--text-dark); margin-bottom: 0.5rem; }
.dream-preview {
  font-size: 0.88rem; color: var(--text-light); line-height: 1.6; margin-bottom: 0.75rem;
  flex: 1; min-height: 0;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.card-bottom { display: flex; gap: 1rem; }
.dream-meta { font-size: 0.8rem; color: var(--text-light); }

/* 空状态 */
.empty-state { text-align: center; padding: 3rem; border-radius: 20px; }
.empty-visual { position: relative; display: inline-block; margin-bottom: 1.25rem; }
.empty-visual .empty-icon { font-size: 4rem; display: block; animation: gentle-bob 3s ease-in-out infinite; }
.empty-star { position: absolute; font-size: 1.2rem; animation: twinkle-float 2.5s ease-in-out infinite; }
.empty-star.s1 { top: -8px; left: -28px; animation-delay: 0s; }
.empty-star.s2 { top: -4px; right: -24px; animation-delay: 0.8s; }
.empty-star.s3 { bottom: 2px; left: 50%; animation-delay: 1.6s; }
@keyframes gentle-bob { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
@keyframes twinkle-float { 0%,100% { opacity: 0.4; transform: scale(0.8) translateY(0); } 50% { opacity: 1; transform: scale(1.1) translateY(-6px); } }
.empty-cta {
  margin-top: 1.25rem; padding: 0.75rem 2rem;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white; border: none; border-radius: 50px; font-size: 0.95rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
  box-shadow: 0 4px 15px rgba(124,111,224,0.3);
}
.empty-cta:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(124,111,224,0.45); }

/* 加载状态 */
.loading-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 40vh; gap: 1rem; position: relative; z-index: 10;
}
.spinner {
  width: 40px; height: 40px;
  border: 3px solid rgba(124,111,224,0.2); border-top-color: var(--primary);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.loading-state p { color: var(--text-light); font-size: 0.95rem; }

/* 错误状态 */
.error-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 40vh; gap: 1rem; position: relative; z-index: 10;
}
.error-state p { color: #e53935; font-size: 0.95rem; }
.retry-btn {
  padding: 0.6rem 1.5rem; background: var(--primary); color: white;
  border: none; border-radius: 8px; font-size: 0.9rem; cursor: pointer;
  transition: background 0.2s; font-family: 'Noto Sans SC', sans-serif;
}
.retry-btn:hover { background: var(--primary-light); }
.empty-icon { font-size: 3rem; margin-bottom: 1rem; }
.empty-text { font-size: 1.1rem; font-weight: 600; color: var(--text-dark); margin-bottom: 0.5rem; }
.empty-hint { font-size: 0.9rem; color: var(--text-light); }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center; padding: 2rem;
}
.modal-card {
  width: 100%; max-width: 700px; max-height: 85vh; overflow-y: auto;
  padding: 2.5rem; border-radius: 24px; position: relative;
  background: rgba(255,255,255,0.92);
  border: 1px solid rgba(255,255,255,0.7);
  box-shadow: 0 16px 56px rgba(0,0,0,0.18), 0 0 0 1px rgba(255,255,255,0.3) inset;
}
.modal-close {
  position: absolute; top: 1rem; right: 1rem;
  width: 32px; height: 32px; border-radius: 50%; border: none;
  background: rgba(124,111,224,0.1); color: var(--text-dark);
  font-size: 1rem; cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.modal-close:hover { background: rgba(124,111,224,0.2); }
.modal-header { text-align: center; margin-bottom: 1.5rem; }
.modal-emotion { font-size: 2.5rem; display: block; margin-bottom: 0.5rem; }
.modal-title { font-size: 1.5rem; font-weight: 700; color: var(--text-dark); margin-bottom: 0.75rem; }
.modal-meta { display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; }
.modal-meta span { font-size: 0.85rem; color: #5A5788; }
.modal-body { display: flex; flex-direction: column; gap: 1.5rem; }
.section-title { font-size: 1rem; font-weight: 600; color: var(--text-dark); margin-bottom: 0.5rem; }
.section-text { font-size: 0.92rem; color: #4A4678; line-height: 1.8; }
.analysis-refresh-status {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.65rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: rgba(124,111,224,0.12);
  color: var(--primary);
  font-size: 0.82rem;
  font-weight: 600;
}
.analysis-spinner {
  width: 0.8rem;
  height: 0.8rem;
  border: 2px solid rgba(124,111,224,0.22);
  border-top-color: var(--primary);
  border-radius: 999px;
  animation: spin 0.8s linear infinite;
}
.interpretation {
  background: rgba(124,111,224,0.08); padding: 1rem; border-radius: 14px;
  border-left: 3px solid var(--primary);
}
.interpretation-content {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}
.interpretation-heading {
  align-self: flex-start;
  margin: 0.2rem 0 0;
  padding: 0.24rem 0.72rem;
  border-radius: 999px;
  background: rgba(124,111,224,0.14);
  color: var(--primary);
  font-size: 0.86rem;
  font-weight: 700;
}
.interpretation-paragraph {
  margin: 0;
  color: #4A4678;
  font-size: 0.92rem;
  line-height: 1.9;
}
.interpretation-item {
  display: grid;
  grid-template-columns: 1.7rem 1fr;
  gap: 0.7rem;
  align-items: flex-start;
  padding: 0.78rem 0.85rem;
  border-radius: 12px;
  background: rgba(255,255,255,0.46);
}
.interpretation-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.7rem;
  height: 1.7rem;
  border-radius: 999px;
  background: var(--primary);
  color: white;
  font-size: 0.78rem;
  font-weight: 700;
}
.interpretation-item p {
  margin: 0;
  color: #4A4678;
  font-size: 0.92rem;
  line-height: 1.8;
}
.ai-disclaimer {
  margin-top: 1rem;
  padding-top: 0.8rem;
  border-top: 1px solid rgba(124, 111, 224, 0.15);
  font-size: 0.75rem;
  color: #9994B8;
  text-align: center;
  line-height: 1.6;
}
.ai-disclaimer p { margin: 0; }
.ai-disclaimer p + p { margin-top: 0.2rem; }
.modal-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.5rem;
}
.delete-detail-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.6rem 1rem;
  border: 1px solid rgba(229,57,53,0.3);
  border-radius: 999px;
  background: rgba(229,57,53,0.08);
  color: #c62828;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Sans SC', sans-serif;
}
.delete-detail-btn:hover {
  background: rgba(229,57,53,0.15);
  transform: translateY(-1px);
}
.confirm-overlay { z-index: 140; }
.confirm-card {
  width: 100%;
  max-width: 420px;
  padding: 2rem;
  border-radius: 20px;
  text-align: center;
  background: rgba(255,255,255,0.92);
}
.confirm-icon { font-size: 2rem; margin-bottom: 0.75rem; }
.confirm-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-dark);
  margin-bottom: 0.65rem;
}
.confirm-text {
  color: #5A5788;
  font-size: 0.92rem;
  line-height: 1.7;
  margin-bottom: 1.5rem;
}
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
}
.confirm-cancel,
.confirm-delete {
  min-width: 108px;
  padding: 0.65rem 1.1rem;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Sans SC', sans-serif;
}
.confirm-cancel {
  border: 1px solid rgba(124,111,224,0.18);
  background: rgba(255,255,255,0.75);
  color: var(--text-dark);
}
.confirm-delete {
  border: 1px solid rgba(229,57,53,0.32);
  background: #e53935;
  color: white;
}
.confirm-cancel:hover:not(:disabled),
.confirm-delete:hover:not(:disabled) {
  transform: translateY(-1px);
}
.confirm-cancel:disabled,
.confirm-delete:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* 动画 */
@keyframes twinkle { 0% { opacity: 0.5; } 100% { opacity: 1; } }
@keyframes float-cloud { 0%,100% { transform: translateX(0) translateY(0); } 50% { transform: translateX(50px) translateY(-20px); } }
@keyframes pulse { 0%,100% { opacity: 0.4; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.08); } }
.modal-enter-active { transition: all 0.3s ease; }
.modal-leave-active { transition: all 0.2s ease; }
.modal-enter-from { opacity: 0; }
.modal-leave-to { opacity: 0; }
.modal-enter-from .modal-card { transform: translateY(20px) scale(0.95); }
.modal-leave-to .modal-card { transform: translateY(20px) scale(0.95); }

/* 响应式 */
@media (max-width: 768px) {
  .page-nav { padding: 1rem; }
  .toolbar { padding: 0 1rem; flex-direction: column; align-items: stretch; }
  .search-box { max-width: 100%; }
  .dreams-container { padding: 0 1rem; }
  .dreams-grid { grid-template-columns: 1fr; }
  .card-top { gap: 0.35rem; margin-bottom: 0.5rem; }
  .card-badges { flex: 1; min-width: 0; }
  .card-footer-actions { gap: 0.4rem; }
  .card-action-btn { padding: 0.35rem 0.6rem; font-size: 0.72rem; }
  .modal-card { padding: 1.5rem; }
  .confirm-card { padding: 1.5rem; }
  .confirm-actions { flex-direction: column; }
  .confirm-cancel,
  .confirm-delete { width: 100%; }
  .page-title { font-size: 1.1rem; }
  .nav-placeholder { display: none; }
}

@media (max-width: 480px) {
  .page-nav { padding: 0.75rem; }
  .toolbar { padding: 0 0.75rem; }
  .dreams-container { padding: 0 0.75rem; }
  .dream-card { padding: 1rem; }
  .dream-title { font-size: 1rem; }
  .dream-preview { font-size: 0.82rem; -webkit-line-clamp: 2; }
  .filter-tab { padding: 0.45rem 0.8rem; font-size: 0.78rem; }
  .search-box input { font-size: 0.88rem; padding: 0.6rem 0.8rem; }
  .empty-state { padding: 2rem 1rem; }
  .empty-icon { font-size: 2.5rem; }
  .empty-title { font-size: 1.1rem; }
  .empty-desc { font-size: 0.82rem; }
  .modal-card { padding: 1.25rem; }
  .confirm-card { padding: 1.25rem; }
}

@media (min-width: 1024px) {
  .page-nav { padding: 1.5rem 2rem; }
  .toolbar { padding: 0 2rem; }
  .search-box { max-width: 480px; }
  .dreams-container { padding: 0 2rem; }
  .dreams-grid { grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 1.5rem; }
  .dream-card { padding: 1.75rem; }
  .dream-title { font-size: 1.25rem; }
  .dream-preview { font-size: 0.95rem; -webkit-line-clamp: 4; }
  .modal-card { max-width: 1100px; padding: 3rem 4rem; }
  .modal-overlay { padding: 1.5rem; }
  .modal-close { top: 1.25rem; right: 1.25rem; width: 36px; height: 36px; font-size: 1.1rem; }
  .modal-header { margin-bottom: 2rem; }
  .modal-emotion { font-size: 3rem; }
  .modal-title { font-size: 1.8rem; }
  .modal-meta span { font-size: 0.92rem; }
  .modal-body { gap: 2rem; }
  .section-title { font-size: 1.15rem; }
  .section-text { font-size: 1.05rem; line-height: 1.9; }
  .interpretation { padding: 1.5rem; }
  .interpretation-heading { font-size: 0.95rem; }
  .interpretation-paragraph { font-size: 1.05rem; line-height: 2; }
  .interpretation-item { padding: 1rem 1.2rem; }
  .interpretation-item p { font-size: 1.05rem; line-height: 1.9; }
  .interpretation-number { width: 2rem; height: 2rem; font-size: 0.85rem; }
  .modal-actions { margin-top: 2rem; }
  .delete-detail-btn { font-size: 0.95rem; padding: 0.7rem 1.2rem; }
}

/* 梦境图片 */
.dream-card-image {
  width: 100%; height: 140px; object-fit: cover;
  border-radius: 8px; margin: 0.5rem 0;
  border: 1px solid rgba(124,111,224,0.12);
  flex-shrink: 0;
}
.detail-image {
  width: 100%; max-height: 400px; object-fit: contain;
  border-radius: 12px; margin-top: 0.8rem;
  border: 1px solid rgba(124,111,224,0.15);
}

/* 深夜模式 */
html.dark .glass {
  background: rgba(28, 24, 45, 0.86);
  border-color: rgba(184, 174, 255, 0.42);
  box-shadow: 0 16px 46px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(184, 174, 255, 0.08) inset;
}
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
html.dark .search-box input {
  background: rgba(34, 29, 54, 0.7);
  border-color: rgba(184, 174, 255, 0.24);
  color: #E8E4F0;
}
html.dark .search-box:focus-within {
  background: rgba(39, 33, 62, 0.9);
  border-color: var(--primary);
}
html.dark .search-box input::placeholder { color: rgba(169, 163, 192, 0.58); }
html.dark .filter-tab { border-color: rgba(184, 174, 255, 0.28); color: #C6C0DA; }
html.dark .filter-tab:hover { background: rgba(155, 143, 255, 0.16); }
html.dark .filter-tab.active {
  background: var(--primary);
  box-shadow: 0 2px 10px rgba(155, 143, 255, 0.25);
}
html.dark .dream-card {
  background: rgba(31, 27, 49, 0.88);
  border-color: rgba(184, 174, 255, 0.34);
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.36), 0 0 0 1px rgba(184, 174, 255, 0.06) inset;
}
html.dark .dream-card:hover {
  background: rgba(39, 33, 62, 0.94);
  border-color: rgba(184, 174, 255, 0.52);
  box-shadow: 0 20px 52px rgba(0, 0, 0, 0.44), 0 0 28px rgba(155, 143, 255, 0.12);
}
html.dark .dream-title { color: #F1EEFA; }
html.dark .dream-date { color: #BFB8D5; }
html.dark .dream-preview { color: #C6C0DA; }
html.dark .dream-meta { color: #BFB8D5; }
html.dark .emotion-badge { background: rgba(155, 143, 255, 0.12); }
html.dark .analysis-badge { background: rgba(155, 143, 255, 0.15); color: #B8AEFF; }
html.dark .card-footer-actions { border-color: rgba(184, 174, 255, 0.16); }
html.dark .card-action-btn.analyze {
  border-color: rgba(155, 143, 255, 0.2);
  background: rgba(155, 143, 255, 0.06);
  color: #B8AEFF;
}
html.dark .card-action-btn.analyze:hover {
  background: rgba(155, 143, 255, 0.15);
  border-color: rgba(155, 143, 255, 0.35);
}
html.dark .card-action-btn.delete {
  border-color: rgba(255, 82, 82, 0.2);
  background: rgba(255, 82, 82, 0.06);
  color: #FF6B6B;
}
html.dark .card-action-btn.delete:hover {
  background: rgba(255, 82, 82, 0.12);
  border-color: rgba(255, 82, 82, 0.3);
}
html.dark .empty-state {
  background: rgba(31, 27, 49, 0.86);
  border-color: rgba(184, 174, 255, 0.32);
}
html.dark .empty-icon { opacity: 0.6; }
html.dark .empty-text { color: #A9A3C0; }
html.dark .empty-hint { color: #A9A3C0; }
html.dark .empty-cta { box-shadow: 0 4px 15px rgba(155, 143, 255, 0.25); }
html.dark .spinner { border-color: rgba(155, 143, 255, 0.2); border-top-color: #B8AEFF; }
html.dark .error-state p { color: #FF6B6B; }
html.dark .retry-btn { box-shadow: 0 4px 15px rgba(155, 143, 255, 0.25); }
html.dark .modal-card {
  background: rgba(31, 27, 49, 0.96);
  border-color: rgba(184, 174, 255, 0.42);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(184, 174, 255, 0.08) inset;
}
html.dark .modal-close { background: rgba(155, 143, 255, 0.12); }
html.dark .modal-close:hover { background: rgba(155, 143, 255, 0.22); }
html.dark .modal-title { color: #F1EEFA; }
html.dark .modal-meta span { color: #BFB8D5; }
html.dark .section-text { color: #C6C0DA; }
html.dark .analysis-refresh-status { background: rgba(155, 143, 255, 0.14); }
html.dark .analysis-spinner { border-color: rgba(155, 143, 255, 0.2); border-top-color: #B8AEFF; }
html.dark .interpretation { background: rgba(155, 143, 255, 0.1); }
html.dark .interpretation-heading { background: rgba(155, 143, 255, 0.12); }
html.dark .interpretation-item { background: rgba(24, 20, 39, 0.62); }
html.dark .interpretation-paragraph { color: #C6C0DA; }
html.dark .interpretation-item p { color: #C6C0DA; }
html.dark .ai-disclaimer { color: #8A84A8; border-top-color: rgba(155, 143, 255, 0.15); }
html.dark .delete-detail-btn {
  border-color: rgba(255, 82, 82, 0.25);
  background: rgba(255, 82, 82, 0.08);
  color: #FF6B6B;
}
html.dark .delete-detail-btn:hover { background: rgba(255, 82, 82, 0.15); }
html.dark .confirm-card {
  background: rgba(31, 27, 49, 0.97);
  border-color: rgba(184, 174, 255, 0.36);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.55);
}
html.dark .confirm-text { color: #A9A3C0; }
html.dark .confirm-cancel {
  background: rgba(30, 27, 46, 0.6);
  color: #E8E4F0;
  border-color: rgba(155, 143, 255, 0.15);
}
html.dark .confirm-cancel:hover { background: rgba(30, 27, 46, 0.8); }
html.dark .dream-card-image { border-color: rgba(184, 174, 255, 0.22); }
html.dark .detail-image { border-color: rgba(155, 143, 255, 0.15); }
</style>
