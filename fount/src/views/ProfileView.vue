<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { getDreamTotal, getStreak, getLongestDream } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const isEditing = ref(false)
const isLoading = ref(false)
const successMsg = ref('')
const errorMsg = ref('')
const avatarInput = ref<HTMLInputElement | null>(null)

const profileUsername = computed(() => userStore.username || '梦境旅人')
const profileEmail = computed(() => userStore.email || '未绑定邮箱')
const profileJoinDate = computed(() => {
  const raw = userStore.createdAt
  if (!raw) return '未知'
  // 后端返回 LocalDateTime 格式如 "2024-01-01T10:30:00"
  const d = new Date(raw)
  if (isNaN(d.getTime())) return raw
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})
const avatarUrl = computed(() => userStore.avatar)

const profile = reactive({
  bio: '记录每一个奇妙的梦境'
})

const editForm = reactive({
  username: profileUsername.value,
  email: profileEmail.value,
  bio: profile.bio
})

const stats = ref([
  { label: '梦境总数', value: '-', icon: '🌙' },
  { label: '连续记录', value: '-天', icon: '🔥' },
  { label: '最长梦境', value: '-字', icon: '📝' }
])

async function loadStats() {
  const userId = userStore.userId
  if (!userId) return
  try {
    const [totalRes, streakRes, longestRes] = await Promise.all([
      getDreamTotal(userId),
      getStreak(userId),
      getLongestDream(userId)
    ])
    stats.value[0].value = String(totalRes.data.data ?? 0)
    stats.value[1].value = `${streakRes.data.data ?? 0}天`
    stats.value[2].value = `${longestRes.data.data ?? 0}字`
  } catch {
    // 静默失败，保持默认值
  }
}

onMounted(loadStats)

function startEdit() {
  editForm.username = profileUsername.value
  editForm.email = profileEmail.value
  editForm.bio = profile.bio
  isEditing.value = true
  errorMsg.value = ''
  successMsg.value = ''
}

function cancelEdit() {
  isEditing.value = false
}

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    errorMsg.value = '请选择图片文件'
    input.value = ''
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    errorMsg.value = '头像图片不能超过 2MB'
    input.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    userStore.updateAvatar(String(reader.result || ''))
    successMsg.value = '头像已更新'
    errorMsg.value = ''
    setTimeout(() => { successMsg.value = '' }, 2500)
  }
  reader.onerror = () => {
    errorMsg.value = '头像读取失败，请换一张图片'
  }
  reader.readAsDataURL(file)
  input.value = ''
}


async function saveProfile() {
  errorMsg.value = ''
  if (!editForm.username.trim()) {
    errorMsg.value = '用户名不能为空'
    return
  }
  isLoading.value = true
  try {
    // 模拟保存
    await new Promise(r => setTimeout(r, 800))
    userStore.updateProfile(editForm.username, editForm.email)
    profile.bio = editForm.bio
    isEditing.value = false
    successMsg.value = '资料更新成功！'
    setTimeout(() => { successMsg.value = '' }, 3000)
  } catch {
    errorMsg.value = '保存失败，请稍后再试'
  } finally {
    isLoading.value = false
  }
}

function goBack() { router.push('/') }
</script>

<template>
  <div class="profile-page">
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds"><div class="cloud cloud-1"></div><div class="cloud cloud-2"></div></div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <!-- 浮动装饰 -->
    <div class="decor">
      <span class="float-icon fi-1">💎</span>
      <span class="float-icon fi-2">🌸</span>
      <span class="float-icon fi-3">🪄</span>
      <span class="float-icon fi-4">🫧</span>
    </div>

    <nav class="page-nav">
      <button class="back-btn" @click="goBack"><span>←</span><span>返回首页</span></button>
      <h1 class="page-title">👤 个人资料</h1>
      <div class="nav-placeholder"></div>
    </nav>

    <div class="content-wrapper">
      <!-- 资料卡片 -->
      <div class="profile-card glass">
        <div class="profile-header">
          <div class="avatar-block">
            <button type="button" class="avatar-large avatar-button" @click="triggerAvatarUpload" title="更换头像">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
              <span v-else>{{ profileUsername.charAt(0).toUpperCase() }}</span>
            </button>
            <input ref="avatarInput" class="avatar-input" type="file" accept="image/*" @change="handleAvatarChange" />
            <div class="avatar-actions">
              <button type="button" class="avatar-action" @click="triggerAvatarUpload">更换头像</button>
            </div>
          </div>
          <div v-if="!isEditing" class="profile-info">
            <h2 class="profile-name">{{ profileUsername }}</h2>
            <p class="profile-email">{{ profileEmail }}</p>
            <p class="profile-bio">{{ profile.bio }}</p>
          </div>
        </div>

        <!-- 查看模式 -->
        <div v-if="!isEditing" class="profile-details">
          <div class="detail-row">
            <span class="detail-label">用户名</span>
            <span class="detail-value">{{ profileUsername }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">邮箱</span>
            <span class="detail-value">{{ profileEmail }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">个人简介</span>
            <span class="detail-value">{{ profile.bio }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">注册时间</span>
            <span class="detail-value">{{ profileJoinDate }}</span>
          </div>

          <button class="edit-btn" @click="startEdit">✏️ 编辑资料</button>
        </div>

        <!-- 编辑模式 -->
        <form v-else class="edit-form" @submit.prevent="saveProfile">
          <div class="form-group">
            <label>用户名</label>
            <div class="input-wrapper">
              <input v-model="editForm.username" type="text" maxlength="20" />
            </div>
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <div class="input-wrapper">
              <input v-model="editForm.email" type="email" />
            </div>
          </div>
          <div class="form-group">
            <label>个人简介</label>
            <div class="input-wrapper">
              <textarea v-model="editForm.bio" rows="3" maxlength="100"></textarea>
            </div>
          </div>

          <Transition name="msg">
            <div v-if="errorMsg" class="message error-msg"><span>⚠️</span>{{ errorMsg }}</div>
          </Transition>
          <Transition name="msg">
            <div v-if="successMsg" class="message success-msg"><span>✅</span>{{ successMsg }}</div>
          </Transition>

          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="cancelEdit">取消</button>
            <button type="submit" class="save-btn" :disabled="isLoading">
              {{ isLoading ? '保存中...' : '💾 保存' }}
            </button>
          </div>
        </form>

        <Transition name="msg">
          <div v-if="successMsg && !isEditing" class="message success-msg" style="margin-top:1rem"><span>✅</span>{{ successMsg }}</div>
        </Transition>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-row">
        <div v-for="s in stats" :key="s.label" class="stat-card glass">
          <span class="stat-icon">{{ s.icon }}</span>
          <span class="stat-value">{{ s.value }}</span>
          <span class="stat-label">{{ s.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
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
.fi-1 { top: 12%; left: 10%; font-size: 1.3rem; animation-delay: 0s; }
.fi-2 { top: 68%; right: 8%; font-size: 1.8rem; animation-delay: 1.5s; }
.fi-3 { bottom: 18%; left: 5%; font-size: 1.5rem; animation-delay: 3s; }
.fi-4 { top: 28%; right: 12%; font-size: 2rem; animation-delay: 4.5s; opacity: 0.4; }

.page-nav {
  position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 2rem;
}
.back-btn {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.6rem 1.2rem; background: var(--glass-bg); backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border); border-radius: 50px;
  color: var(--text-dark); font-size: 0.9rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.back-btn:hover { background: rgba(255,255,255,0.45); transform: translateX(-3px); }
.page-title { font-size: 1.4rem; color: var(--text-dark); }
.nav-placeholder { width: 100px; }

.content-wrapper {
  position: relative; z-index: 10;
  max-width: 600px; margin: 0 auto; padding: 0 1.5rem;
}

.glass {
  background: rgba(255,255,255,0.35); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.2) inset;
}

.profile-card { padding: 2rem; border-radius: 24px; margin-bottom: 1.5rem; }
.profile-header { display: flex; align-items: center; gap: 1.5rem; margin-bottom: 1.5rem; }
.avatar-block { display: flex; flex-direction: column; align-items: center; gap: 0.55rem; flex-shrink: 0; }
.avatar-large {
  width: 72px; height: 72px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 2rem; font-weight: 700;
  box-shadow: 0 4px 20px rgba(124,111,224,0.4); border: 3px solid rgba(255,255,255,0.5);
  overflow: hidden;
}
.avatar-button { padding: 0; cursor: pointer; transition: all 0.3s ease; }
.avatar-button:hover { transform: scale(1.04); box-shadow: 0 8px 28px rgba(124,111,224,0.45); }
.avatar-large img { width: 100%; height: 100%; object-fit: cover; display: block; }
.avatar-input { display: none; }
.avatar-actions { display: flex; align-items: center; gap: 0.4rem; }
.avatar-action {
  border: none; background: rgba(124,111,224,0.1); color: var(--primary);
  border-radius: 999px; padding: 0.28rem 0.6rem; font-size: 0.75rem;
  cursor: pointer; font-family: 'Noto Sans SC', sans-serif;
}
.avatar-action:hover { background: rgba(124,111,224,0.16); }
.profile-name { font-size: 1.4rem; font-weight: 700; color: var(--text-dark); }
.profile-email { font-size: 0.9rem; color: var(--text-light); margin-top: 0.2rem; }
.profile-bio { font-size: 0.88rem; color: var(--text-light); margin-top: 0.3rem; }

.profile-details { display: flex; flex-direction: column; gap: 0; }
.detail-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0.85rem 0; border-bottom: 1px solid rgba(124,111,224,0.08);
}
.detail-row:last-of-type { border-bottom: none; }
.detail-label { font-size: 0.9rem; color: var(--text-light); }
.detail-value { font-size: 0.9rem; font-weight: 500; color: var(--text-dark); }

.edit-btn {
  margin-top: 1rem; width: 100%; padding: 0.75rem;
  border: 1.5px solid var(--primary); border-radius: 12px;
  background: rgba(124,111,224,0.06); color: var(--primary);
  font-size: 0.95rem; font-weight: 500; cursor: pointer;
  transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.edit-btn:hover { background: rgba(124,111,224,0.12); }

.edit-form { display: flex; flex-direction: column; gap: 1rem; }
.form-group { display: flex; flex-direction: column; gap: 0.35rem; }
.form-group label { font-size: 0.85rem; font-weight: 500; color: var(--text-dark); }
.input-wrapper input, .input-wrapper textarea {
  width: 100%; padding: 0.7rem 1rem; background: rgba(255,255,255,0.5);
  border: 1.5px solid rgba(124,111,224,0.15); border-radius: 12px;
  font-size: 0.92rem; color: var(--text-dark); outline: none;
  transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif; resize: vertical;
}
.input-wrapper input:focus, .input-wrapper textarea:focus {
  border-color: var(--primary); background: rgba(255,255,255,0.7);
  box-shadow: 0 0 0 4px rgba(124,111,224,0.1);
}

.form-actions { display: flex; gap: 0.75rem; margin-top: 0.5rem; }
.cancel-btn {
  flex: 1; padding: 0.75rem; border: 1.5px solid var(--glass-border); border-radius: 12px;
  background: var(--glass-bg); color: var(--text-dark); font-size: 0.95rem;
  cursor: pointer; transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.cancel-btn:hover { background: rgba(255,255,255,0.45); }
.save-btn {
  flex: 1; padding: 0.75rem; border: none; border-radius: 12px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white; font-size: 0.95rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
  box-shadow: 0 4px 20px rgba(124,111,224,0.35);
}
.save-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(124,111,224,0.45); }
.save-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.message {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.7rem 1rem; border-radius: 10px; font-size: 0.85rem; font-weight: 500;
}
.error-msg { background: rgba(255,82,82,0.1); color: #e53935; border: 1px solid rgba(255,82,82,0.2); }
.success-msg { background: rgba(76,175,80,0.1); color: #2e7d32; border: 1px solid rgba(76,175,80,0.2); }
.msg-enter-active, .msg-leave-active { transition: all 0.3s ease; }
.msg-enter-from, .msg-leave-to { opacity: 0; transform: translateY(-8px); }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
.stat-card {
  padding: 1.25rem; border-radius: 18px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 0.35rem;
  transition: transform 0.3s ease;
}
.stat-card:hover { transform: translateY(-5px); }
.stat-icon { font-size: 1.5rem; }
.stat-value { font-size: 1.4rem; font-weight: 700; color: var(--text-dark); }
.stat-label { font-size: 0.8rem; color: var(--text-light); }

@keyframes twinkle { 0% { opacity: 0.5; } 100% { opacity: 1; } }
@keyframes float-cloud { 0%,100% { transform: translateX(0) translateY(0); } 50% { transform: translateX(50px) translateY(-20px); } }
@keyframes pulse { 0%,100% { opacity: 0.4; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.08); } }

@media (max-width: 768px) {
  .page-nav { padding: 1rem; }
  .content-wrapper { padding: 0 1rem; }
  .page-title { font-size: 1.1rem; }
  .nav-placeholder { display: none; }
  .stats-row { grid-template-columns: repeat(3, 1fr); gap: 0.5rem; }
  .stat-card { padding: 0.75rem; }
  .stat-value { font-size: 1.1rem; }
}

@media (min-width: 1024px) {
  .page-nav { padding: 1.5rem 3rem; }
  .content-wrapper { max-width: 800px; padding: 0 2rem; }
  .profile-card { padding: 2.5rem; }
  .profile-name { font-size: 1.6rem; }
  .detail-label { font-size: 1rem; }
  .detail-value { font-size: 1rem; }
  .stat-card { padding: 1.5rem; }
  .stat-value { font-size: 1.6rem; }
  .stat-label { font-size: 0.88rem; }
}
</style>
