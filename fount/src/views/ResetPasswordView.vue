<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { resetPassword, sendResetPasswordCode } from '@/api/user'

const router = useRouter()

const step = ref<'input' | 'verify' | 'done'>('input')
const identifier = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const isLoading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const countdown = ref(0)

let timer: ReturnType<typeof setInterval> | null = null

const maskedIdentifier = computed(() => {
  if (!identifier.value) return ''
  if (identifier.value.includes('@')) {
    const [name, domain] = identifier.value.split('@')
    const masked = name.length > 2
      ? name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
      : name[0] + '*'
    return `${masked}@${domain}`
  }
  return identifier.value.slice(0, 3) + '****' + identifier.value.slice(-1)
})

async function handleSendCode() {
  errorMsg.value = ''
  if (!identifier.value.trim()) {
    errorMsg.value = '请输入用户名或邮箱'
    return
  }
  const target = identifier.value.trim()
  isLoading.value = true
  try {
    const { data } = await sendResetPasswordCode(target)
    if (data.code === 200) {
      identifier.value = target
      successMsg.value = '验证码已发送'
      step.value = 'verify'
      startCountdown()
    } else {
      errorMsg.value = data.message || '发送失败，请检查输入信息'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '网络错误，请稍后再试'
  } finally {
    isLoading.value = false
  }
}

async function handleReset() {
  errorMsg.value = ''
  if (!code.value.trim()) {
    errorMsg.value = '请输入验证码'
    return
  }
  if (!newPassword.value) {
    errorMsg.value = '请输入新密码'
    return
  }
  if (newPassword.value.length < 6) {
    errorMsg.value = '密码长度至少6位'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = '两次密码输入不一致'
    return
  }
  isLoading.value = true
  try {
    const { data } = await resetPassword(identifier.value.trim(), code.value.trim(), newPassword.value)
    if (data.code === 200) {
      successMsg.value = '密码重置成功！'
      step.value = 'done'
    } else {
      errorMsg.value = data.message || '重置失败，请检查验证码'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '网络错误，请稍后再试'
  } finally {
    isLoading.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer!)
      timer = null
    }
  }, 1000)
}

function handleResend() {
  if (countdown.value > 0) return
  successMsg.value = ''
  errorMsg.value = ''
  handleSendCode()
}

function goLogin() {
  router.push('/login')
}

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="reset-page">
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
    </div>

    <!-- 浮动装饰 -->
    <div class="decor">
      <span class="float-icon fi-1">🔑</span>
      <span class="float-icon fi-2">🌙</span>
      <span class="float-icon fi-3">⭐</span>
      <span class="float-icon fi-4">✨</span>
    </div>

    <!-- 返回首页 -->
    <button class="back-btn" @click="goHome">
      <span class="back-arrow">←</span>
      <span>返回首页</span>
    </button>

    <!-- 重置密码卡片 -->
    <div class="reset-card glass">
      <div class="card-header">
        <div class="card-icon">🔑</div>
        <h1 class="card-title">找回密码</h1>
        <p class="card-subtitle">
          <template v-if="step === 'input'">输入用户名或邮箱，我们将发送验证码</template>
          <template v-else-if="step === 'verify'">验证码已发送至 {{ maskedIdentifier }}</template>
          <template v-else>密码已成功重置</template>
        </p>
      </div>

      <!-- 步骤指示器 -->
      <div class="steps">
        <div class="step" :class="{ active: step === 'input', done: step === 'verify' || step === 'done' }">
          <div class="step-dot">1</div>
          <span>验证身份</span>
        </div>
        <div class="step-line" :class="{ active: step === 'verify' || step === 'done' }"></div>
        <div class="step" :class="{ active: step === 'verify', done: step === 'done' }">
          <div class="step-dot">2</div>
          <span>重置密码</span>
        </div>
        <div class="step-line" :class="{ active: step === 'done' }"></div>
        <div class="step" :class="{ active: step === 'done' }">
          <div class="step-dot">3</div>
          <span>完成</span>
        </div>
      </div>

      <!-- 步骤1：输入用户名/邮箱 -->
      <form v-if="step === 'input'" class="reset-form" @submit.prevent="handleSendCode">
        <div class="form-group">
          <label for="identifier">
            <span class="label-icon">👤</span>
            用户名 / 邮箱
          </label>
          <div class="input-wrapper">
            <input
              id="identifier"
              v-model="identifier"
              type="text"
              placeholder="输入用户名或邮箱"
              maxlength="50"
              :disabled="isLoading"
            />
          </div>
        </div>

        <Transition name="msg">
          <div v-if="errorMsg" class="message error-msg">
            <span>⚠️</span>{{ errorMsg }}
          </div>
        </Transition>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          <span v-if="!isLoading" class="btn-content">
            <span>发送验证码</span>
            <span class="btn-sparkle">📧</span>
          </span>
          <span v-else class="btn-loading">
            <span class="spinner"></span>
            <span>发送中...</span>
          </span>
        </button>
      </form>

      <!-- 步骤2：输入验证码和新密码 -->
      <form v-else-if="step === 'verify'" class="reset-form" @submit.prevent="handleReset">
        <div class="form-group">
          <label for="code">
            <span class="label-icon">📧</span>
            验证码
          </label>
          <div class="code-row">
            <div class="input-wrapper code-input">
              <input
                id="code"
                v-model="code"
                type="text"
                placeholder="输入验证码"
                maxlength="6"
                :disabled="isLoading"
              />
            </div>
            <button
              type="button"
              class="resend-btn"
              :disabled="countdown > 0 || isLoading"
              @click="handleResend"
            >
              {{ countdown > 0 ? `${countdown}s` : '重新发送' }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label for="newPassword">
            <span class="label-icon">🔒</span>
            新密码
          </label>
          <div class="input-wrapper">
            <input
              id="newPassword"
              v-model="newPassword"
              :type="showPassword ? 'text' : 'password'"
              placeholder="输入新密码（至少6位）"
              maxlength="50"
              :disabled="isLoading"
            />
            <button
              type="button"
              class="toggle-pwd"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '🙈' : '👁️' }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label for="confirmPassword">
            <span class="label-icon">🔒</span>
            确认密码
          </label>
          <div class="input-wrapper">
            <input
              id="confirmPassword"
              v-model="confirmPassword"
              :type="showPassword ? 'text' : 'password'"
              placeholder="再次输入新密码"
              maxlength="50"
              :disabled="isLoading"
            />
          </div>
        </div>

        <Transition name="msg">
          <div v-if="errorMsg" class="message error-msg">
            <span>⚠️</span>{{ errorMsg }}
          </div>
        </Transition>
        <Transition name="msg">
          <div v-if="successMsg" class="message success-msg">
            <span>✅</span>{{ successMsg }}
          </div>
        </Transition>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          <span v-if="!isLoading" class="btn-content">
            <span>重置密码</span>
            <span class="btn-sparkle">🌙</span>
          </span>
          <span v-else class="btn-loading">
            <span class="spinner"></span>
            <span>提交中...</span>
          </span>
        </button>
      </form>

      <!-- 步骤3：完成 -->
      <div v-else class="done-section">
        <div class="done-icon">🎉</div>
        <p class="done-text">密码已重置成功！</p>
        <button class="submit-btn" @click="goLogin">
          <span class="btn-content">
            <span>返回登录</span>
            <span class="btn-sparkle">🌙</span>
          </span>
        </button>
      </div>

      <div class="card-footer">
        <p>想起密码了？<span class="link" @click="goLogin">返回登录</span></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reset-page {
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

.cloud-1 { width: 500px; height: 180px; top: 15%; left: -15%; animation: float-cloud 25s ease-in-out infinite; }
.cloud-2 { width: 350px; height: 140px; top: 50%; right: -10%; animation: float-cloud 30s ease-in-out infinite reverse; }
.cloud-3 { width: 400px; height: 160px; bottom: 10%; left: 20%; animation: float-cloud 28s ease-in-out infinite 5s; }

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}

.glow-1 { width: 400px; height: 400px; top: -5%; right: 10%; background: rgba(124, 111, 224, 0.25); animation: pulse 8s ease-in-out infinite; }
.glow-2 { width: 300px; height: 300px; bottom: 5%; left: 10%; background: rgba(255, 179, 71, 0.15); animation: pulse 10s ease-in-out infinite 3s; }

/* 浮动装饰 */
.decor { position: fixed; inset: 0; pointer-events: none; z-index: 1; }

.float-icon {
  position: absolute;
  font-size: 1.5rem;
  animation: float 6s ease-in-out infinite;
  opacity: 0.7;
}

.fi-1 { top: 10%; left: 8%; animation-delay: 0s; }
.fi-2 { top: 15%; right: 12%; animation-delay: 1.5s; font-size: 2rem; }
.fi-3 { bottom: 20%; left: 15%; animation-delay: 3s; }
.fi-4 { bottom: 15%; right: 20%; animation-delay: 2s; font-size: 1.2rem; }

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

.back-arrow { font-size: 1.1rem; transition: transform 0.3s ease; }
.back-btn:hover .back-arrow { transform: translateX(-3px); }

/* 卡片 */
.reset-card {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 440px;
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

.card-header { text-align: center; margin-bottom: 1.5rem; }
.card-icon { font-size: 2.5rem; margin-bottom: 0.75rem; animation: pulse-icon 3s ease-in-out infinite; }
.card-title { font-size: 1.75rem; font-weight: 700; color: var(--text-dark); margin-bottom: 0.4rem; }
.card-subtitle { font-size: 0.9rem; color: var(--text-light); }

/* 步骤指示器 */
.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 1.75rem;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  color: var(--text-light);
  transition: color 0.3s;
}

.step.active { color: var(--primary); }
.step.done { color: var(--primary); }

.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
  background: rgba(124, 111, 224, 0.1);
  border: 2px solid rgba(124, 111, 224, 0.2);
  color: var(--text-light);
  transition: all 0.3s;
}

.step.active .step-dot {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
  box-shadow: 0 2px 12px rgba(124, 111, 224, 0.4);
}

.step.done .step-dot {
  background: var(--primary-light);
  border-color: var(--primary-light);
  color: white;
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(124, 111, 224, 0.15);
  margin: 0 0.25rem;
  margin-bottom: 1.2rem;
  transition: background 0.3s;
}

.step-line.active { background: var(--primary); }

/* 表单 */
.reset-form {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
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

.label-icon { font-size: 0.9rem; }

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper input {
  width: 100%;
  padding: 0.75rem 1rem;
  background: rgba(255, 255, 255, 0.5);
  border: 1.5px solid rgba(124, 111, 224, 0.15);
  border-radius: 12px;
  font-size: 0.95rem;
  color: var(--text-dark);
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  outline: none;
}

.input-wrapper input::placeholder { color: rgba(107, 104, 153, 0.5); }

.input-wrapper input:focus {
  border-color: var(--primary);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 0 0 4px rgba(124, 111, 224, 0.1);
}

.input-wrapper input:disabled { opacity: 0.6; cursor: not-allowed; }

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

.toggle-pwd:hover { opacity: 1; }

/* 验证码行 */
.code-row {
  display: flex;
  gap: 0.75rem;
}

.code-input { flex: 1; }
.code-input input { padding-right: 1rem; }

.resend-btn {
  white-space: nowrap;
  padding: 0 1rem;
  height: 46px;
  align-self: center;
  border: 1.5px solid rgba(124, 111, 224, 0.2);
  border-radius: 12px;
  background: rgba(124, 111, 224, 0.08);
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}

.resend-btn:hover:not(:disabled) {
  background: rgba(124, 111, 224, 0.15);
  border-color: var(--primary);
}

.resend-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.msg-enter-active, .msg-leave-active { transition: all 0.3s ease; }
.msg-enter-from, .msg-leave-to { opacity: 0; transform: translateY(-8px); }

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

.submit-btn:hover::before { transform: translateX(100%); }

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(124, 111, 224, 0.45);
}

.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { cursor: not-allowed; opacity: 0.8; }

.btn-content { display: flex; align-items: center; justify-content: center; gap: 0.5rem; }
.btn-sparkle { animation: pulse-icon 2.5s ease-in-out infinite; }
.btn-loading { display: flex; align-items: center; justify-content: center; gap: 0.5rem; }

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 完成区域 */
.done-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 1rem 0;
}

.done-icon {
  font-size: 3rem;
  animation: bounce-in 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.done-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-dark);
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

.link:hover { color: var(--primary-light); }

/* 动画 */
@keyframes twinkle { 0% { opacity: 0.5; } 100% { opacity: 1; } }
@keyframes float-cloud { 0%, 100% { transform: translateX(0) translateY(0); } 50% { transform: translateX(50px) translateY(-20px); } }
@keyframes float { 0%, 100% { transform: translateY(0) rotate(0deg); } 50% { transform: translateY(-15px) rotate(8deg); } }
@keyframes pulse { 0%, 100% { opacity: 0.4; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.08); } }
@keyframes pulse-icon { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.12); } }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes card-enter { from { opacity: 0; transform: translateY(30px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes bounce-in { 0% { opacity: 0; transform: scale(0.3); } 50% { transform: scale(1.1); } 100% { opacity: 1; transform: scale(1); } }

/* 响应式 */
@media (max-width: 768px) {
  .reset-page { padding: 1rem; padding-top: 4rem; }
  .reset-card { padding: 1.75rem; }
  .card-title { font-size: 1.4rem; }
  .back-btn { top: 1rem; left: 1rem; padding: 0.5rem 1rem; font-size: 0.8rem; }
  .float-icon { font-size: 1.2rem; }
  .step-line { width: 28px; }
}

@media (max-width: 400px) {
  .reset-card { padding: 1.5rem; border-radius: 18px; }
}

@media (min-width: 1024px) {
  .reset-card { max-width: 500px; padding: 3rem; }
  .card-icon { font-size: 3rem; }
  .card-title { font-size: 2rem; }
  .card-subtitle { font-size: 1rem; }
  .form-group label { font-size: 0.92rem; }
  .input-wrapper input { padding: 0.85rem 1.1rem; font-size: 1rem; }
  .submit-btn { padding: 0.95rem; font-size: 1.05rem; }
  .step-line { width: 56px; }
}
</style>
