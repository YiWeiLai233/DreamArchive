<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { getDreamById, saveAndAnalyzeDream, saveDream } from '@/api/dream'
import type { DreamContent } from '@/api/dream'
import { formatDreamInterpretation } from '@/utils/dreamInterpretation'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  title: '',
  content: '',
  emotion: '',
  place: '',
  time: ''
})

const writingTips = [
  '梦中有哪些人或动物？',
  '场景是什么颜色、什么光线？',
  '你当时有什么感觉？',
  '有没有重复出现的符号？',
  '梦境的转折点是什么？'
]

const inspirationPrompts = [
  '我在一个从未去过的地方醒来',
  '天空下着彩色的雨',
  '我发现自己能飞',
  '镜子里的不是我',
  '时间在倒流',
  '一扇通往另一个世界的门'
]

const emotions = [
  { key: 'happy', label: '开心', icon: '😊', color: '#FFD93D' },
  { key: 'sad', label: '悲伤', icon: '😢', color: '#6B9BFF' },
  { key: 'scary', label: '恐惧', icon: '😰', color: '#FF6B6B' },
  { key: 'mysterious', label: '神秘', icon: '🔮', color: '#9B8FFF' },
  { key: 'peaceful', label: '平静', icon: '😌', color: '#4ECDC4' },
  { key: 'excited', label: '兴奋', icon: '🤩', color: '#FFB347' }
]

const timeOptions = [
  '凌晨 00:00-02:00', '凌晨 02:00-04:00', '凌晨 04:00-06:00',
  '清晨 06:00-08:00', '上午 08:00-10:00', '上午 10:00-12:00',
  '下午 12:00-14:00', '下午 14:00-16:00', '傍晚 16:00-18:00',
  '晚上 18:00-20:00', '晚上 20:00-22:00', '深夜 22:00-24:00'
]

const commonPlaces = [
  '家中', '学校', '办公室', '公园', '海边',
  '森林', '城市', '天空', '水下', '星空',
  '城堡', '迷宫', '花园', '图书馆', '其他'
]

const isSubmitting = ref(false)
const submitMode = ref<'save' | 'analyze' | null>(null)
const isPollingAnalysis = ref(false)
const step = ref<'form' | 'result'>('form')
const result = ref<DreamContent | null>(null)
let analysisPollTimer: ReturnType<typeof window.setInterval> | null = null
let analysisPollAttempts = 0

const isFormValid = computed(() => form.value.content.trim().length > 0 && form.value.emotion.length > 0)
const isAnalysisPending = computed(() => isPendingInterpretation(result.value?.interpretation))
const formattedInterpretation = computed(() => formatDreamInterpretation(result.value?.interpretation))

function selectEmotion(key: string) {
  form.value.emotion = key
}

function getEmotionIcon(emotion: string) {
  return emotions.find(e => e.key === emotion)?.icon || '🌙'
}

function isPendingInterpretation(interpretation?: string | null) {
  if (!interpretation) return false
  return interpretation.includes('后台解析') || interpretation.includes('解析中')
}

function clearAnalysisPolling() {
  if (analysisPollTimer) {
    window.clearInterval(analysisPollTimer)
    analysisPollTimer = null
  }
  isPollingAnalysis.value = false
  analysisPollAttempts = 0
}

function startAnalysisPolling(dreamId: string) {
  clearAnalysisPolling()
  isPollingAnalysis.value = true
  void refreshDreamUntilAnalyzed(dreamId)
  analysisPollTimer = window.setInterval(() => {
    void refreshDreamUntilAnalyzed(dreamId)
  }, 3000)
}

async function refreshDreamUntilAnalyzed(dreamId: string) {
  analysisPollAttempts += 1
  try {
    const res = await getDreamById(dreamId)
    if (res.data.code === 200 && res.data.data) {
      result.value = res.data.data
      if (!isPendingInterpretation(res.data.data.interpretation)) {
        clearAnalysisPolling()
      }
    }
  } catch (error) {
    console.error('刷新 AI 解析结果失败:', error)
  }

  if (analysisPollAttempts >= 40) {
    clearAnalysisPolling()
  }
}

async function handleSubmit(mode: 'save' | 'analyze') {
  if (!isFormValid.value || isSubmitting.value) return

  if (!userStore.isLoggedIn) {
    alert('请先登录')
    router.push('/login')
    return
  }

  isSubmitting.value = true
  submitMode.value = mode

  try {
    const dreamData = {
      userId: parseInt(userStore.userId) || 0,
      title: form.value.title,
      content: form.value.content,
      emotion: form.value.emotion,
      place: form.value.place || '未知',
      time: form.value.time || new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }

    const res = mode === 'analyze'
      ? await saveAndAnalyzeDream(dreamData)
      : await saveDream(dreamData)
    if (res.data.code === 200) {
      result.value = res.data.data
      step.value = 'result'
      if (mode === 'analyze' && res.data.data?.id && isPendingInterpretation(res.data.data.interpretation)) {
        startAnalysisPolling(res.data.data.id)
      }
    } else {
      alert(res.data.message || '保存失败')
    }
  } catch (error) {
    console.error('保存梦境失败:', error)
    alert('保存失败，请重试')
  } finally {
    isSubmitting.value = false
    submitMode.value = null
  }
}

function goBack() { router.push('/') }
function goDreams() { router.push('/dreams') }
function recordAnother() {
  clearAnalysisPolling()
  form.value = { title: '', content: '', emotion: '', place: '', time: '' }
  result.value = null
  step.value = 'form'
}

onBeforeUnmount(clearAnalysisPolling)
</script>

<template>
  <div class="record-page">
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds">
        <div class="cloud cloud-1"></div>
        <div class="cloud cloud-2"></div>
      </div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <div class="decor">
      <span class="float-icon fi-1">💭</span>
      <span class="float-icon fi-2">🌙</span>
      <span class="float-icon fi-3">✨</span>
      <span class="float-icon fi-4">☁️</span>
    </div>

    <!-- 顶部导航 -->
    <nav class="page-nav">
      <button class="back-btn" @click="goBack">
        <span>←</span>
        <span>返回首页</span>
      </button>
      <h1 class="page-title">📝 记录梦境</h1>
      <div class="nav-placeholder"></div>
    </nav>

    <!-- 第一步：填写表单 -->
    <div v-if="step === 'form'" class="page-body">
      <div class="form-layout">
        <!-- 左侧：情绪 + 地点/时间 -->
        <aside class="side-panel glass">
          <div class="form-section">
            <label class="section-label">
              <span class="label-icon">🎭</span>
              <span>梦中的情绪</span>
            </label>
            <div class="emotion-grid">
              <button
                v-for="e in emotions"
                :key="e.key"
                class="emotion-btn"
                :class="{ active: form.emotion === e.key }"
                :style="form.emotion === e.key ? { borderColor: e.color, background: e.color + '20' } : {}"
                @click="selectEmotion(e.key)"
              >
                <span class="emotion-icon">{{ e.icon }}</span>
                <span class="emotion-label">{{ e.label }}</span>
              </button>
            </div>
          </div>

          <div class="form-section">
            <label class="section-label">
              <span class="label-icon">🕐</span>
              <span>梦境时间</span>
            </label>
            <select v-model="form.time" class="form-select">
              <option value="">选择时间段</option>
              <option v-for="t in timeOptions" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>

          <div class="form-section">
            <label class="section-label">
              <span class="label-icon">📍</span>
              <span>梦境地点</span>
            </label>
            <input v-model="form.place" type="text" class="form-input" placeholder="输入或选择地点" />
            <div class="place-tags">
              <button
                v-for="p in commonPlaces"
                :key="p"
                class="place-tag"
                :class="{ active: form.place === p }"
                @click="form.place = p"
              >
                {{ p }}
              </button>
            </div>
          </div>
        </aside>

        <!-- 右侧：梦境内容 + 提交 -->
        <section class="main-panel glass">
          <div class="title-row">
            <label class="section-label" style="margin-bottom: 0;">
              <span class="label-icon">📖</span>
              <span>梦境内容</span>
              <span class="required">*</span>
            </label>
            <input
              v-model="form.title"
              type="text"
              class="title-input"
              placeholder="给这个梦取个名字（可选）"
            />
          </div>

          <textarea
            v-model="form.content"
            class="dream-textarea"
            placeholder="描述你梦到了什么...&#10;&#10;例如：我梦见自己站在一片星空下，周围漂浮着无数发光的气泡..."
          ></textarea>

          <!-- 写作提示 -->
          <div class="tips-bar">
            <span class="tips-label">写作提示：</span>
            <span v-for="tip in writingTips" :key="tip" class="tip-chip">{{ tip }}</span>
          </div>

          <!-- 灵感模板 -->
          <div class="inspiration-bar">
            <span class="tips-label">灵感片段：</span>
            <button
              v-for="p in inspirationPrompts"
              :key="p"
              class="inspiration-chip"
              @click="form.content += (form.content ? '\n' : '') + p"
            >
              {{ p }}
            </button>
          </div>

          <div class="bottom-bar">
            <span class="char-count">{{ form.content.length }} / 2000</span>
            <div class="submit-actions">
              <button
                class="submit-btn secondary"
                :class="{ disabled: !isFormValid || isSubmitting }"
                :disabled="!isFormValid || isSubmitting"
                @click="handleSubmit('save')"
              >
                <span v-if="isSubmitting && submitMode === 'save'" class="loading-spinner"></span>
                <span v-else class="btn-icon">💾</span>
                <span>{{ isSubmitting && submitMode === 'save' ? '正在保存...' : '单独保存梦境' }}</span>
              </button>
              <button
                class="submit-btn"
                :class="{ disabled: !isFormValid || isSubmitting }"
                :disabled="!isFormValid || isSubmitting"
                @click="handleSubmit('analyze')"
              >
                <span v-if="isSubmitting && submitMode === 'analyze'" class="loading-spinner"></span>
                <span v-else class="btn-icon">🔮</span>
                <span>{{ isSubmitting ? '正在解析...' : '保存并解析梦境' }}</span>
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- 第二步：显示 AI 解析结果 -->
    <div v-else class="page-body">
      <div class="result-layout">
        <div class="result-card glass">
          <div class="result-header">
            <span class="result-emotion">{{ getEmotionIcon(result?.emotion || '') }}</span>
            <h2 class="result-title">梦境已记录</h2>
            <div class="result-meta">
              <span v-if="result?.place">📍 {{ result.place }}</span>
              <span v-if="result?.time">🕐 {{ result.time }}</span>
            </div>
          </div>

          <div class="result-body">
            <div class="result-section">
              <h3 class="result-section-title">📖 梦境内容</h3>
              <p class="result-section-text">{{ result?.content }}</p>
            </div>

            <div class="result-section">
              <h3 class="result-section-title">🔮 AI 解析</h3>
              <div v-if="isAnalysisPending || isPollingAnalysis" class="analysis-refresh-status">
                <span class="analysis-spinner"></span>
                <span>AI 正在后台解析，页面会自动刷新结果</span>
              </div>
              <div class="interpretation-box">
                <div v-if="formattedInterpretation.length" class="interpretation-content">
                  <template
                    v-for="(block, index) in formattedInterpretation"
                    :key="`${block.type}-${index}`"
                  >
                    <h4 v-if="block.type === 'heading'" class="interpretation-heading">
                      {{ block.content }}
                    </h4>
                    <div v-else-if="block.type === 'item'" class="interpretation-item">
                      <span class="interpretation-number">{{ block.marker || index + 1 }}</span>
                      <p>{{ block.content }}</p>
                    </div>
                    <p v-else class="interpretation-paragraph">{{ block.content }}</p>
                  </template>
                </div>
                <p class="result-section-text placeholder-text" v-else>AI 暂未生成解析，你可以稍后在梦境列表中查看。</p>
              </div>
            </div>
          </div>

          <div class="result-actions">
            <button class="btn-action primary" @click="recordAnother">继续记录</button>
            <button class="btn-action secondary" @click="goDreams">查看梦境列表</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.record-page {
  height: 100vh;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, var(--bg-start) 0%, var(--bg-end) 100%);
  display: flex;
  flex-direction: column;
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
.decor { position: fixed; inset: 0; pointer-events: none; z-index: 1; }
.float-icon { position: absolute; font-size: 1.5rem; animation: float 6s ease-in-out infinite; opacity: 0.6; }
.fi-1 { top: 8%; left: 6%; font-size: 2rem; animation-delay: 0s; }
.fi-2 { top: 22%; right: 8%; font-size: 1.6rem; animation-delay: 1.5s; }
.fi-3 { bottom: 28%; left: 10%; font-size: 1.2rem; animation-delay: 3s; }
.fi-4 { top: 58%; right: 5%; font-size: 2.2rem; animation-delay: 4.5s; opacity: 0.4; }
.glass {
  background: rgba(255,255,255,0.35); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.2) inset;
}

/* 导航 */
.page-nav {
  position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.75rem 1.5rem;
  flex-shrink: 0;
}
.back-btn {
  display: flex; align-items: center; gap: 0.4rem;
  padding: 0.45rem 1rem;
  background: var(--glass-bg); backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border); border-radius: 50px;
  color: var(--text-dark); font-size: 0.85rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.back-btn:hover { background: rgba(255,255,255,0.45); transform: translateX(-3px); }
.page-title { font-size: 1.15rem; color: var(--text-dark); }
.nav-placeholder { width: 80px; }

/* 页面主体 */
.page-body {
  position: relative; z-index: 10;
  flex: 1;
  min-height: 0;
  padding: 0 1.5rem 1rem;
}

/* === 表单布局：左右分栏 === */
.form-layout {
  display: flex;
  gap: 1rem;
  height: 100%;
}
.side-panel {
  width: 300px;
  flex-shrink: 0;
  padding: 1rem;
  border-radius: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.main-panel {
  flex: 1;
  min-width: 0;
  padding: 1rem 1.25rem;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
}

/* 表单元素 */
.form-section { }
.section-label {
  display: flex; align-items: center; gap: 0.4rem;
  font-size: 0.85rem; font-weight: 600; color: var(--text-dark);
  margin-bottom: 0.5rem;
}
.label-icon { font-size: 1rem; }
.required { color: #FF6B6B; font-size: 0.8rem; }

/* 情绪选择 - 3列2行紧凑 */
.emotion-grid {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 0.4rem;
}
.emotion-btn {
  display: flex; align-items: center; justify-content: center; gap: 0.3rem;
  padding: 0.5rem 0.3rem; border: 2px solid var(--glass-border); border-radius: 10px;
  background: var(--glass-bg); backdrop-filter: blur(10px);
  cursor: pointer; transition: all 0.2s ease; font-family: 'Noto Sans SC', sans-serif;
}
.emotion-btn:hover { border-color: var(--primary); }
.emotion-btn.active { box-shadow: 0 2px 10px rgba(124,111,224,0.3); }
.emotion-icon { font-size: 1.1rem; }
.emotion-label { font-size: 0.75rem; color: var(--text-dark); }

/* 输入框 */
.form-select {
  width: 100%; padding: 0.5rem 0.75rem; border: 2px solid var(--glass-border); border-radius: 8px;
  background: rgba(255,255,255,0.5); backdrop-filter: blur(10px);
  font-size: 0.82rem; color: var(--text-dark); cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif; transition: all 0.3s ease;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236B6899' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 0.6rem center;
}
.form-select:focus { outline: none; border-color: var(--primary); }
.form-input {
  width: 100%; padding: 0.5rem 0.75rem; border: 2px solid var(--glass-border); border-radius: 8px;
  background: rgba(255,255,255,0.5); backdrop-filter: blur(10px);
  font-size: 0.82rem; color: var(--text-dark);
  font-family: 'Noto Sans SC', sans-serif; transition: all 0.3s ease;
}
.form-input:focus { outline: none; border-color: var(--primary); }
.form-input::placeholder { color: var(--text-light); }

/* 地点标签 */
.place-tags { display: flex; flex-wrap: wrap; gap: 0.3rem; margin-top: 0.4rem; }
.place-tag {
  padding: 0.2rem 0.55rem; border: 1.5px solid var(--glass-border); border-radius: 14px;
  background: var(--glass-bg); font-size: 0.7rem; color: var(--text-light);
  cursor: pointer; transition: all 0.2s ease; font-family: 'Noto Sans SC', sans-serif;
}
.place-tag:hover { border-color: var(--primary); color: var(--primary); }
.place-tag.active { background: var(--primary); border-color: var(--primary); color: white; }

/* 标题行 */
.title-row {
  display: flex; align-items: center; gap: 0.75rem;
  margin-bottom: 0.5rem; flex-shrink: 0;
}
.title-input {
  flex: 1; padding: 0.4rem 0.75rem; border: 1.5px solid var(--glass-border); border-radius: 8px;
  background: rgba(255,255,255,0.4);
  font-size: 0.85rem; color: var(--text-dark);
  font-family: 'Noto Sans SC', sans-serif; transition: all 0.3s ease;
}
.title-input:focus { outline: none; border-color: var(--primary); }
.title-input::placeholder { color: var(--text-light); }

/* 文本区域 - 填满剩余空间 */
.dream-textarea {
  flex: 1; width: 100%; padding: 0.75rem 1rem; border: 2px solid var(--glass-border); border-radius: 12px;
  background: rgba(255,255,255,0.5); backdrop-filter: blur(10px);
  font-size: 0.9rem; line-height: 1.6; color: var(--text-dark);
  font-family: 'Noto Sans SC', sans-serif; resize: none;
  transition: all 0.3s ease;
  min-height: 0;
}
.dream-textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px rgba(124,111,224,0.1); }
.dream-textarea::placeholder { color: var(--text-light); }

/* 写作提示 */
.tips-bar {
  display: flex; align-items: center; flex-wrap: wrap; gap: 0.3rem;
  padding: 0.35rem 0; flex-shrink: 0;
}
.tips-label { font-size: 0.72rem; color: var(--text-light); white-space: nowrap; }
.tip-chip {
  font-size: 0.68rem; color: var(--primary); background: rgba(124,111,224,0.06);
  padding: 0.15rem 0.5rem; border-radius: 10px;
  white-space: nowrap;
}

/* 灵感模板 */
.inspiration-bar {
  display: flex; align-items: center; flex-wrap: wrap; gap: 0.3rem;
  padding-bottom: 0.35rem; flex-shrink: 0;
}
.inspiration-chip {
  font-size: 0.68rem; color: var(--text-dark); background: rgba(255,179,71,0.12);
  border: 1px solid rgba(255,179,71,0.3); border-radius: 10px;
  padding: 0.15rem 0.5rem; cursor: pointer;
  transition: all 0.2s; font-family: 'Noto Sans SC', sans-serif;
}
.inspiration-chip:hover { background: rgba(255,179,71,0.25); transform: translateY(-1px); }

/* 底部栏：字数 + 按钮 */
.bottom-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 0.6rem;
  flex-shrink: 0;
}
.char-count { font-size: 0.75rem; color: var(--text-light); }
.submit-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
}
.submit-btn {
  display: inline-flex; align-items: center; gap: 0.5rem;
  padding: 0.6rem 1.8rem; border: none; border-radius: 50px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white; font-size: 0.9rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  box-shadow: 0 4px 15px rgba(124,111,224,0.4);
}
.submit-btn.secondary {
  background: rgba(255,255,255,0.72);
  color: var(--primary);
  border: 1px solid rgba(124,111,224,0.28);
  box-shadow: none;
}
.submit-btn:hover:not(.disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(124,111,224,0.5); }
.submit-btn.secondary:hover:not(.disabled) {
  background: rgba(124,111,224,0.12);
  box-shadow: 0 4px 14px rgba(124,111,224,0.16);
}
.submit-btn.disabled { opacity: 0.6; cursor: not-allowed; }
.btn-icon { font-size: 1.1rem; }
.loading-spinner {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white; border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* === 结果页面 === */
.result-layout {
  display: flex; justify-content: center; align-items: center;
  height: 100%;
}
.result-card {
  width: 100%; max-width: 640px;
  padding: 2rem; border-radius: 20px;
}
.result-header { text-align: center; margin-bottom: 1.5rem; }
.result-emotion { font-size: 2.5rem; display: block; margin-bottom: 0.3rem; }
.result-title { font-size: 1.4rem; font-weight: 700; color: var(--text-dark); margin-bottom: 0.5rem; }
.result-meta { display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; }
.result-meta span { font-size: 0.82rem; color: var(--text-light); }

.result-body { display: flex; flex-direction: column; gap: 1.25rem; margin-bottom: 1.5rem; }
.result-section-title { font-size: 0.9rem; font-weight: 600; color: var(--text-dark); margin-bottom: 0.4rem; }
.result-section-text { font-size: 0.88rem; color: #4A4678; line-height: 1.7; }
.analysis-refresh-status {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.6rem;
  padding: 0.38rem 0.72rem;
  border-radius: 999px;
  background: rgba(124,111,224,0.12);
  color: var(--primary);
  font-size: 0.82rem;
  font-weight: 600;
}
.analysis-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(124,111,224,0.22);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.interpretation-box {
  background: rgba(255,255,255,0.42);
  padding: 1rem; border-radius: 14px;
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
  font-size: 0.9rem;
  line-height: 1.9;
}
.interpretation-item {
  display: grid;
  grid-template-columns: 1.7rem 1fr;
  gap: 0.7rem;
  align-items: flex-start;
  padding: 0.78rem 0.85rem;
  border-radius: 12px;
  background: rgba(124,111,224,0.08);
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
  font-size: 0.9rem;
  line-height: 1.8;
}
.placeholder-text { color: var(--text-light); font-style: italic; }

.result-actions { display: flex; gap: 1rem; justify-content: center; }
.btn-action {
  padding: 0.6rem 1.5rem; border-radius: 50px; border: none;
  font-size: 0.88rem; font-weight: 500; cursor: pointer;
  transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.btn-action.primary {
  background: var(--primary); color: white;
  box-shadow: 0 4px 12px rgba(124,111,224,0.4);
}
.btn-action.primary:hover { background: var(--primary-light); transform: translateY(-2px); }
.btn-action.secondary {
  background: var(--glass-bg); color: var(--text-dark);
  border: 1px solid var(--glass-border); backdrop-filter: blur(10px);
}
.btn-action.secondary:hover { background: rgba(255,255,255,0.4); transform: translateY(-2px); }

/* 动画 */
@keyframes twinkle { 0% { opacity: 0.5; } 100% { opacity: 1; } }
@keyframes float-cloud { 0%,100% { transform: translateX(0) translateY(0); } 50% { transform: translateX(50px) translateY(-20px); } }
@keyframes float { 0%,100% { transform: translateY(0) rotate(0deg); } 50% { transform: translateY(-20px) rotate(10deg); } }
@keyframes pulse { 0%,100% { opacity: 0.4; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.08); } }
@keyframes spin { to { transform: rotate(360deg); } }

/* 手机端：上下布局 */
@media (max-width: 768px) {
  .page-nav { padding: 0.6rem 1rem; }
  .page-body { padding: 0 0.75rem 0.75rem; }
  .form-layout { flex-direction: column; }
  .side-panel { width: 100%; flex-shrink: 0; padding: 0.75rem; gap: 0.5rem; }
  .main-panel { flex: 1; min-height: 0; }
  .page-title { font-size: 1rem; }
  .nav-placeholder { display: none; }
  .emotion-grid { grid-template-columns: repeat(6, 1fr); }
  .emotion-btn { flex-direction: column; gap: 0.15rem; padding: 0.4rem 0.2rem; }
  .emotion-icon { font-size: 1rem; }
  .emotion-label { font-size: 0.65rem; }
  .bottom-bar { align-items: stretch; flex-direction: column; gap: 0.75rem; }
  .submit-actions { justify-content: stretch; }
  .submit-btn { flex: 1; justify-content: center; padding: 0.6rem 0.9rem; }
  .result-card { padding: 1.5rem; }
}

/* PC 标准屏 */
@media (min-width: 1024px) {
  .page-nav { padding: 0.75rem 2.5rem; }
  .page-body { padding: 0 2.5rem 1rem; }
  .side-panel { width: 340px; }
  .emotion-grid { gap: 0.5rem; }
}

/* 超宽屏 */
@media (min-width: 1440px) {
  .side-panel { width: 380px; }
  .page-nav { padding: 0.75rem 3rem; }
  .page-body { padding: 0 3rem 1rem; }
}
</style>
