<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, loginByCode, sendLoginCode, setupAccount } from '@/api/user'
import { useUserStore } from '@/stores'
import { syncGuestDreams } from '@/utils/guestDreams'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginMode = ref<'password' | 'code'>('password')
const form = ref({ username: '', password: '', email: '', code: '' })
const isLoading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const showPassword = ref(false)
const isCodeSending = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

function getRedirectPath() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return '/'
}

const showSetup = ref(false)
const setupForm = ref({ username: '', password: '', confirmPassword: '' })
const setupLoading = ref(false)
const setupError = ref('')

function afterLogin(data: any) {
  userStore.login(data.username, data.email, data.createdAt || '', data.id, data.role)
  if (data.avatarUrl) {
    userStore.updateAvatar(data.avatarUrl)
  }
  if (data.needsSetup) {
    showSetup.value = true
  } else {
    successMsg.value = '登录成功！正在同步数据...'
    syncGuestDreams(data.id).finally(() => {
      setTimeout(() => router.replace(getRedirectPath()), 1000)
    })
  }
}

async function handleSetup() {
  setupError.value = ''
  if (setupForm.value.username.trim().length < 3) { setupError.value = '用户名至少3个字符'; return }
  if (setupForm.value.password.length < 6) { setupError.value = '密码至少6位'; return }
  if (setupForm.value.password !== setupForm.value.confirmPassword) { setupError.value = '两次密码不一致'; return }
  setupLoading.value = true
  try {
    const { data } = await setupAccount(setupForm.value.username, setupForm.value.password)
    if (data.code === 200) {
      userStore.updateProfile(setupForm.value.username, userStore.email)
      showSetup.value = false
      successMsg.value = '设置成功！正在同步数据...'
      syncGuestDreams(Number(userStore.userId)).finally(() => {
        setTimeout(() => router.replace(getRedirectPath()), 1000)
      })
    } else {
      setupError.value = data.message || '设置失败'
    }
  } catch (e: any) {
    setupError.value = e.response?.data?.message || '设置失败'
  } finally {
    setupLoading.value = false
  }
}

async function handleSendCode() {
  if (codeCountdown.value > 0 || isCodeSending.value) return
  errorMsg.value = ''
  if (!form.value.email.trim()) { errorMsg.value = '请输入邮箱'; return }
  isCodeSending.value = true
  try {
    const { data } = await sendLoginCode(form.value.email)
    if (data.code === 200) {
      startCodeCountdown()
    } else {
      errorMsg.value = data.message || '发送失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '发送失败，请稍后再试'
  } finally {
    isCodeSending.value = false
  }
}

function startCodeCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleLogin() {
  errorMsg.value = ''
  successMsg.value = ''

  if (loginMode.value === 'password') {
    if (!form.value.username.trim()) { errorMsg.value = '请输入用户名或邮箱'; return }
    if (!form.value.password) { errorMsg.value = '请输入密码'; return }
    isLoading.value = true
    try {
      const { data } = await login({ username: form.value.username, password: form.value.password })
      if (data.code === 200) { afterLogin(data.data) }
      else { errorMsg.value = data.message || '登录失败' }
    } catch (e: any) {
      errorMsg.value = e.response?.data?.message || '网络错误'
    } finally { isLoading.value = false }
  } else {
    if (!form.value.email.trim()) { errorMsg.value = '请输入邮箱'; return }
    if (!form.value.code.trim()) { errorMsg.value = '请输入验证码'; return }
    isLoading.value = true
    try {
      const { data } = await loginByCode(form.value.email, form.value.code)
      if (data.code === 200) { afterLogin(data.data) }
      else { errorMsg.value = data.message || '登录失败' }
    } catch (e: any) {
      errorMsg.value = e.response?.data?.message || '网络错误'
    } finally { isLoading.value = false }
  }
}

function switchMode(mode: 'password' | 'code') {
  loginMode.value = mode
  errorMsg.value = ''
  successMsg.value = ''
}

function goHome() { router.push('/') }
function goRegister() { router.push('/register') }
function goResetPassword() { router.push('/reset-password') }
</script>

<template>
  <div class="login-page">
    <!-- 梦幻背景 -->
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds">
        <div class="cloud cloud-1"></div>
        <div class="cloud cloud-2"></div>
        <div class="cloud cloud-3"></div>
      </div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <div class="glow glow-3"></div>
    </div>

    <!-- 浮动装饰 -->
    <div class="decor">
      <span class="float-icon fi-1">✨</span>
      <span class="float-icon fi-2">🌙</span>
      <span class="float-icon fi-3">⭐</span>
      <span class="float-icon fi-4">💫</span>
      <span class="float-icon fi-5">🌟</span>
    </div>

    <!-- 返回首页 -->
    <button class="back-btn" @click="goHome">
      <span class="back-arrow">←</span>
      <span>返回首页</span>
    </button>

    <!-- 登录卡片 -->
    <div class="login-card glass">
      <!-- 顶部装饰 -->
      <div class="card-header">
        <div class="card-icon">🔮</div>
        <h1 class="card-title">欢迎回来</h1>
        <p class="card-subtitle">继续你的梦境探索之旅</p>
      </div>

      <!-- 登录模式切换 -->
      <div class="mode-tabs">
        <button type="button" class="mode-tab" :class="{ active: loginMode === 'password' }" @click="switchMode('password')">密码登录</button>
        <button type="button" class="mode-tab" :class="{ active: loginMode === 'code' }" @click="switchMode('code')">验证码登录</button>
      </div>

      <!-- 表单 -->
      <form class="login-form" @submit.prevent="handleLogin">
        <!-- 密码登录 -->
        <template v-if="loginMode === 'password'">
          <div class="form-group">
            <label for="username"><span class="label-icon">👤</span>用户名 / 邮箱</label>
            <div class="input-wrapper">
              <input id="username" v-model="form.username" type="text" placeholder="输入用户名或邮箱" maxlength="50" :disabled="isLoading" />
            </div>
          </div>
          <div class="form-group">
            <label for="password"><span class="label-icon">🔒</span>密码</label>
            <div class="input-wrapper">
              <input id="password" v-model="form.password" :type="showPassword ? 'text' : 'password'" placeholder="输入你的密码" maxlength="50" :disabled="isLoading" />
              <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">{{ showPassword ? '🙈' : '👁️' }}</button>
            </div>
          </div>
        </template>

        <!-- 验证码登录 -->
        <template v-else>
          <div class="form-group">
            <label for="email"><span class="label-icon">📧</span>邮箱</label>
            <div class="input-wrapper">
              <input id="email" v-model="form.email" type="email" placeholder="输入注册邮箱" :disabled="isLoading" />
            </div>
          </div>
          <div class="form-group">
            <label for="code"><span class="label-icon">🔑</span>验证码</label>
            <div class="code-row">
              <div class="input-wrapper code-input">
                <input id="code" v-model="form.code" type="text" placeholder="输入6位验证码" maxlength="6" :disabled="isLoading" />
              </div>
              <button type="button" class="send-code-btn" :disabled="codeCountdown > 0 || isCodeSending" @click="handleSendCode">
                {{ codeCountdown > 0 ? `${codeCountdown}s` : (isCodeSending ? '发送中...' : '发送验证码') }}
              </button>
            </div>
          </div>
        </template>

        <Transition name="msg">
          <div v-if="errorMsg" class="message error-msg"><span>⚠️</span>{{ errorMsg }}</div>
        </Transition>
        <Transition name="msg">
          <div v-if="successMsg" class="message success-msg"><span>✅</span>{{ successMsg }}</div>
        </Transition>

        <button type="submit" class="submit-btn" :class="{ loading: isLoading }" :disabled="isLoading">
          <span v-if="!isLoading" class="btn-content"><span>进入梦境</span><span class="btn-sparkle">🌙</span></span>
          <span v-else class="btn-loading"><span class="spinner"></span><span>登录中...</span></span>
        </button>
      </form>

      <div class="card-footer">
        <p class="footer-row"><span class="link" @click="goResetPassword">忘记密码？</span></p>
        <p>还没有账号？<span class="link" @click="goRegister">立即注册</span></p>
      </div>
    </div>

    <!-- 设置用户名密码弹窗 -->
    <Teleport to="body">
      <div v-if="showSetup" class="modal-overlay">
        <div class="modal-card glass">
          <div class="card-header">
            <div class="card-icon">🎉</div>
            <h2 class="card-title">欢迎加入梦境档案</h2>
            <p class="card-subtitle">请设置你的用户名和密码，下次可直接密码登录</p>
          </div>
          <form class="login-form" @submit.prevent="handleSetup">
            <div class="form-group">
              <label><span class="label-icon">👤</span>用户名</label>
              <div class="input-wrapper">
                <input v-model="setupForm.username" type="text" placeholder="给自己起个名字" maxlength="20" :disabled="setupLoading" />
              </div>
            </div>
            <div class="form-group">
              <label><span class="label-icon">🔒</span>密码</label>
              <div class="input-wrapper">
                <input v-model="setupForm.password" type="password" placeholder="至少6位" maxlength="50" :disabled="setupLoading" />
              </div>
            </div>
            <div class="form-group">
              <label><span class="label-icon">🔑</span>确认密码</label>
              <div class="input-wrapper">
                <input v-model="setupForm.confirmPassword" type="password" placeholder="再输入一次" maxlength="50" :disabled="setupLoading" />
              </div>
            </div>
            <Transition name="msg">
              <div v-if="setupError" class="message error-msg"><span>⚠️</span>{{ setupError }}</div>
            </Transition>
            <button type="submit" class="submit-btn" :disabled="setupLoading">
              <span v-if="!setupLoading" class="btn-content"><span>完成设置</span><span class="btn-sparkle">✨</span></span>
              <span v-else class="btn-loading"><span class="spinner"></span><span>设置中...</span></span>
            </button>
            <p class="skip-link" @click="showSetup = false; router.replace(getRedirectPath())">暂时跳过</p>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--bg-start) 0%, var(--bg-end) 100%);
  padding: 2rem;
}

/* 梦幻背景 */
.dream-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.stars {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(2px 2px at 20px 30px, rgba(255,255,255,0.8), transparent),
    radial-gradient(2px 2px at 40px 70px, rgba(255,255,255,0.6), transparent),
    radial-gradient(1px 1px at 90px 40px, rgba(255,255,255,0.7), transparent),
    radial-gradient(1px 1px at 130px 80px, rgba(255,255,255,0.5), transparent),
    radial-gradient(2px 2px at 160px 30px, rgba(255,255,255,0.8), transparent),
    radial-gradient(1px 1px at 200px 60px, rgba(255,255,255,0.6), transparent),
    radial-gradient(2px 2px at 80px 100px, rgba(255,255,255,0.7), transparent);
  background-repeat: repeat;
  background-size: 220px 120px;
  animation: twinkle 4s ease-in-out infinite alternate;
}

.cloud {
  position: absolute;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  filter: blur(50px);
}

.cloud-1 {
  width: 500px;
  height: 180px;
  top: 15%;
  left: -15%;
  animation: float-cloud 25s ease-in-out infinite;
}

.cloud-2 {
  width: 350px;
  height: 140px;
  top: 50%;
  right: -10%;
  animation: float-cloud 30s ease-in-out infinite reverse;
}

.cloud-3 {
  width: 400px;
  height: 160px;
  bottom: 10%;
  left: 20%;
  animation: float-cloud 28s ease-in-out infinite 5s;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}

.glow-1 {
  width: 400px;
  height: 400px;
  top: -5%;
  right: 10%;
  background: rgba(124, 111, 224, 0.25);
  animation: pulse 8s ease-in-out infinite;
}

.glow-2 {
  width: 300px;
  height: 300px;
  bottom: 5%;
  left: 10%;
  background: rgba(255, 179, 71, 0.15);
  animation: pulse 10s ease-in-out infinite 3s;
}

.glow-3 {
  width: 250px;
  height: 250px;
  top: 40%;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(155, 143, 255, 0.15);
  animation: pulse 12s ease-in-out infinite 6s;
}

/* 浮动装饰 */
.decor {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.float-icon {
  position: absolute;
  font-size: 1.5rem;
  animation: float 6s ease-in-out infinite;
  opacity: 0.7;
}

.fi-1 { top: 10%; left: 8%; animation-delay: 0s; }
.fi-2 { top: 15%; right: 12%; animation-delay: 1.5s; font-size: 2rem; }
.fi-3 { bottom: 20%; left: 15%; animation-delay: 3s; }
.fi-4 { top: 60%; right: 8%; animation-delay: 4.5s; }
.fi-5 { bottom: 15%; right: 20%; animation-delay: 2s; font-size: 1.2rem; }

/* 返回按钮 */
.back-btn {
  position: fixed;
  top: 1.5rem;
  left: 1.5rem;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 50px;
  color: var(--text-dark);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.45);
  transform: translateX(-3px);
}

.back-arrow {
  font-size: 1.1rem;
  transition: transform 0.3s ease;
}

.back-btn:hover .back-arrow {
  transform: translateX(-3px);
}

/* 登录卡片 */
.login-card {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
  padding: 2.5rem;
  border-radius: 24px;
  animation: card-enter 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.glass {
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.2) inset;
}

/* 卡片头部 */
.card-header {
  text-align: center;
  margin-bottom: 2rem;
}

.card-icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
  animation: pulse-icon 3s ease-in-out infinite;
}

.card-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-dark);
  margin-bottom: 0.4rem;
}

.card-subtitle {
  font-size: 0.9rem;
  color: var(--text-light);
}

/* 模式切换 */
.mode-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 1.5rem;
  background: rgba(124, 111, 224, 0.08);
  border-radius: 12px;
  padding: 4px;
}
.mode-tab {
  flex: 1;
  padding: 0.6rem;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--text-light);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}
.mode-tab.active {
  background: white;
  color: var(--primary);
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

/* 验证码行 */
.code-row { display: flex; gap: 0.75rem; align-items: stretch; min-width: 0; }
.code-input { flex: 1; }
.send-code-btn {
  flex-shrink: 0; padding: 0 1.2rem; height: auto;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white; border: none; border-radius: 12px; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; white-space: nowrap; transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}
.send-code-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 15px rgba(124,111,224,0.35); }
.send-code-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* 表单 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-group label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-dark);
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.label-icon {
  font-size: 0.9rem;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper input {
  width: 100%;
  padding: 0.75rem 1rem;
  padding-right: 2.5rem;
  background: rgba(255, 255, 255, 0.5);
  border: 1.5px solid rgba(124, 111, 224, 0.15);
  border-radius: 12px;
  font-size: 0.95rem;
  color: var(--text-dark);
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  outline: none;
}

.input-wrapper input::placeholder {
  color: rgba(107, 104, 153, 0.5);
}

.input-wrapper input:focus {
  border-color: var(--primary);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 0 0 4px rgba(124, 111, 224, 0.1);
}

.input-wrapper input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle-pwd {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  padding: 0.25rem;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.toggle-pwd:hover {
  opacity: 1;
}

/* 提示信息 */
.message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.7rem 1rem;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 500;
}

.error-msg {
  background: rgba(255, 82, 82, 0.1);
  color: #e53935;
  border: 1px solid rgba(255, 82, 82, 0.2);
}

.success-msg {
  background: rgba(76, 175, 80, 0.1);
  color: #2e7d32;
  border: 1px solid rgba(76, 175, 80, 0.2);
}

.msg-enter-active,
.msg-leave-active {
  transition: all 0.3s ease;
}

.msg-enter-from,
.msg-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  padding: 0.85rem;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  margin-top: 0.5rem;
  box-shadow: 0 4px 20px rgba(124, 111, 224, 0.35);
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(255, 255, 255, 0.15) 50%, transparent 100%);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
}

.submit-btn:hover::before {
  transform: translateX(100%);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(124, 111, 224, 0.45);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.8;
}

.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-sparkle {
  animation: pulse-icon 2.5s ease-in-out infinite;
}

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 底部 */
.card-footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.85rem;
  color: var(--text-light);
}

.link {
  color: var(--primary);
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.link:hover {
  color: var(--primary-light);
}

.footer-row {
  margin-bottom: 0.5rem;
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
  50% { transform: translateY(-15px) rotate(8deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.08); }
}

@keyframes pulse-icon {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.12); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .login-page {
    padding: 1rem;
    padding-top: 4rem;
  }

  .login-card {
    padding: 1.75rem;
  }

  .card-title {
    font-size: 1.4rem;
  }

  .back-btn {
    top: 1rem;
    left: 1rem;
    padding: 0.5rem 1rem;
    font-size: 0.8rem;
  }

  .float-icon {
    font-size: 1.2rem;
  }

  .code-row {
    flex-wrap: wrap;
  }

  .send-code-btn {
    flex-shrink: 1;
    min-width: 0;
    white-space: normal;
    font-size: 0.8rem;
    padding: 0 0.8rem;
  }
}

@media (max-width: 480px) {
  .card-subtitle { font-size: 0.82rem; }
  .mode-tab { font-size: 0.82rem; padding: 0.45rem 1rem; }
  .form-group label { font-size: 0.8rem; }
  .input-wrapper input { font-size: 0.9rem; padding: 0.7rem 0.9rem; }
  .submit-btn { font-size: 0.92rem; padding: 0.75rem; }
  .modal-overlay { padding: 1rem; }
}

@media (max-width: 400px) {
  .login-card {
    padding: 1.5rem;
    border-radius: 18px;
  }
}

.modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  padding: 2rem; animation: card-enter 0.3s ease;
}
.modal-card { max-width: 420px; width: 100%; padding: 2.5rem; border-radius: 24px; }
.skip-link {
  text-align: center; margin-top: 0.75rem; font-size: 0.85rem;
  color: var(--text-light); cursor: pointer; transition: color 0.2s;
}
.skip-link:hover { color: var(--primary); }

@media (min-width: 1024px) {
  .login-card { max-width: 500px; padding: 3rem; }
  .card-icon { font-size: 3rem; }
  .card-title { font-size: 2rem; }
  .card-subtitle { font-size: 1rem; }
  .form-group label { font-size: 0.92rem; }
  .input-wrapper input { padding: 0.85rem 1.1rem; font-size: 1rem; }
  .submit-btn { padding: 0.95rem; font-size: 1.05rem; }
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
html.dark .glow-3 { background: rgba(155, 143, 255, 0.1); }
html.dark .back-btn:hover { background: rgba(155, 143, 255, 0.15); }
html.dark .login-card {
  background: rgba(31, 27, 49, 0.9);
  border-color: rgba(184, 174, 255, 0.4);
  box-shadow: 0 22px 64px rgba(0, 0, 0, 0.48), 0 0 0 1px rgba(184, 174, 255, 0.08) inset;
}
html.dark .mode-tabs { background: rgba(155, 143, 255, 0.08); }
html.dark .mode-tab.active { background: rgba(155, 143, 255, 0.2); }
html.dark .send-code-btn {
  background: var(--primary);
  box-shadow: 0 2px 8px rgba(155, 143, 255, 0.25);
}
html.dark .input-wrapper input {
  background: rgba(30, 27, 46, 0.5);
  border-color: rgba(155, 143, 255, 0.15);
  color: #E8E4F0;
}
html.dark .input-wrapper input:focus {
  background: rgba(30, 27, 46, 0.7);
  box-shadow: 0 0 0 3px rgba(155, 143, 255, 0.15);
}
html.dark .input-wrapper input::placeholder { color: rgba(169, 163, 192, 0.5); }
html.dark .card-title { color: #E8E4F0; }
html.dark .card-subtitle { color: #A9A3C0; }
html.dark .form-group label { color: #A9A3C0; }
html.dark .error-msg {
  background: rgba(255, 82, 82, 0.12);
  color: #FF6B6B;
  border-color: rgba(255, 82, 82, 0.25);
}
html.dark .success-msg {
  background: rgba(76, 175, 80, 0.12);
  color: #81C784;
  border-color: rgba(76, 175, 80, 0.25);
}
html.dark .glass {
  background: rgba(28, 24, 45, 0.86);
  border-color: rgba(184, 174, 255, 0.42);
  box-shadow: 0 16px 46px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(184, 174, 255, 0.08) inset;
}
html.dark .submit-btn { box-shadow: 0 4px 15px rgba(155, 143, 255, 0.3); }
html.dark .submit-btn:hover { box-shadow: 0 8px 25px rgba(155, 143, 255, 0.4); }
html.dark .spinner { border-color: rgba(155, 143, 255, 0.2); border-top-color: #B8AEFF; }
html.dark .forgot-link { color: #A9A3C0; }
html.dark .forgot-link:hover { color: #B8AEFF; }
html.dark .divider-line { background: rgba(155, 143, 255, 0.15); }
html.dark .divider-text { color: #A9A3C0; }
</style>
