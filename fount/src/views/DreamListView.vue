<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { deleteDream, getUserDreams } from '@/api/dream'
import { getUserByEmail } from '@/api/user'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()

const searchQuery = ref('')
const activeFilter = ref('all')
const showDetail = ref(false)
const selectedDream = ref<Dream | null>(null)
const isLoading = ref(true)
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

async function loadDreams() {
  const userId = await getUserId()
  if (!userId) {
    errorMsg.value = '请先登录'
    isLoading.value = false
    return
  }

  try {
    isLoading.value = true
    errorMsg.value = ''
    const res = await getUserDreams(Number(userId))
    if (res.data.code === 200) {
      dreams.value = (res.data.data || []).map(d => ({
        id: d.id,
        title: d.title || (d.content ? d.content.slice(0, 20) + (d.content.length > 20 ? '...' : '') : '未命名梦境'),
        content: d.content || '',
        emotion: d.emotion || 'mysterious',
        place: d.place || '未知',
        time: d.time || '',
        interpretation: d.interpretation || '暂无解析',
        createdAt: formatCreatedAt(d.createdAt)
      }))
    } else {
      errorMsg.value = res.data.message || '加载失败'
    }
  } catch (e: any) {
    errorMsg.value = e.message || '网络错误'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadDreams)

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

function getEmotionIcon(emotion: string) {
  const map: Record<string, string> = { happy: '😊', sad: '😢', scary: '😰', mysterious: '🔮' }
  return map[emotion] || '🌙'
}

function getEmotionLabel(emotion: string) {
  const map: Record<string, string> = { happy: '开心', sad: '悲伤', scary: '恐惧', mysterious: '神秘' }
  return map[emotion] || '未知'
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
        <button @click="loadDreams" class="retry-btn">重试</button>
      </div>

      <div v-else-if="filteredDreams.length === 0" class="empty-state glass">
        <div class="empty-icon">🌙</div>
        <p class="empty-text">暂无梦境记录</p>
        <p class="empty-hint">开始记录你的第一个梦吧</p>
      </div>

      <div v-else class="dreams-grid">
        <div
          v-for="dream in filteredDreams"
          :key="dream.id"
          class="dream-card glass"
          @click="openDetail(dream)"
        >
          <div class="card-top">
            <span class="emotion-badge">{{ getEmotionIcon(dream.emotion) }} {{ getEmotionLabel(dream.emotion) }}</span>
            <div class="card-actions">
              <span class="dream-date">{{ dream.createdAt }}</span>
              <button
                class="card-delete-btn"
                title="删除梦境"
                @click.stop="askDeleteDream(dream)"
              >
                <span>🗑</span>
                <span>删除</span>
              </button>
            </div>
          </div>
          <h3 class="dream-title">{{ dream.title }}</h3>
          <p class="dream-preview">{{ dream.content.slice(0, 80) }}...</p>
          <div class="card-bottom">
            <span class="dream-meta">📍 {{ dream.place }}</span>
            <span class="dream-meta">🕐 {{ dream.time }}</span>
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
            </div>
            <div class="section">
              <h3 class="section-title">🔮 AI 解析</h3>
              <p class="section-text interpretation">{{ selectedDream.interpretation }}</p>
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
  padding: 0.5rem 1rem; border: 1.5px solid var(--glass-border);
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
}
.dream-card:hover { transform: translateY(-5px); box-shadow: 0 12px 40px rgba(0,0,0,0.12); }
.glass {
  background: rgba(255,255,255,0.35); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.2) inset;
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.emotion-badge {
  font-size: 0.8rem; padding: 0.25rem 0.75rem; border-radius: 50px;
  background: rgba(124,111,224,0.1); color: var(--primary);
}
.card-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.dream-date { font-size: 0.8rem; color: var(--text-light); }
.card-delete-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.6rem;
  border: 1px solid rgba(229,57,53,0.22);
  border-radius: 999px;
  background: rgba(255,255,255,0.5);
  color: #c62828;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Sans SC', sans-serif;
}
.card-delete-btn:hover {
  background: rgba(229,57,53,0.12);
  border-color: rgba(229,57,53,0.45);
  transform: translateY(-1px);
}
.dream-title { font-size: 1.15rem; font-weight: 600; color: var(--text-dark); margin-bottom: 0.5rem; }
.dream-preview { font-size: 0.88rem; color: var(--text-light); line-height: 1.6; margin-bottom: 0.75rem; }
.card-bottom { display: flex; gap: 1rem; }
.dream-meta { font-size: 0.8rem; color: var(--text-light); }

/* 空状态 */
.empty-state { text-align: center; padding: 3rem; border-radius: 20px; }

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
  width: 100%; max-width: 560px; max-height: 80vh; overflow-y: auto;
  padding: 2rem; border-radius: 24px; position: relative;
  background: rgba(255,255,255,0.88);
  border: 1px solid rgba(255,255,255,0.7);
  box-shadow: 0 12px 48px rgba(0,0,0,0.15), 0 0 0 1px rgba(255,255,255,0.3) inset;
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
.interpretation {
  background: rgba(124,111,224,0.1); padding: 1rem; border-radius: 12px;
  border-left: 3px solid var(--primary);
}
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
  .card-top { align-items: flex-start; gap: 0.5rem; }
  .card-actions { align-items: flex-end; flex-direction: column; gap: 0.35rem; }
  .modal-card { padding: 1.5rem; }
  .confirm-card { padding: 1.5rem; }
  .confirm-actions { flex-direction: column; }
  .confirm-cancel,
  .confirm-delete { width: 100%; }
  .page-title { font-size: 1.1rem; }
  .nav-placeholder { display: none; }
}

@media (min-width: 1024px) {
  .page-nav { padding: 1.5rem 3rem; }
  .toolbar { padding: 0 3rem; }
  .search-box { max-width: 420px; }
  .dreams-container { padding: 0 3rem; }
  .dreams-grid { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 1.5rem; }
  .dream-card { padding: 1.75rem; }
  .dream-title { font-size: 1.25rem; }
  .dream-preview { font-size: 0.95rem; }
  .modal-card { max-width: 660px; padding: 2.5rem; }
  .modal-title { font-size: 1.7rem; }
  .section-title { font-size: 1.1rem; }
  .section-text { font-size: 1rem; }
}
</style>
