<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'
import { sendChangePasswordCode } from '@/api/user'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const showOld = ref(false)
const showNew = ref(false)
const isLoading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const code = ref('')
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

async function handleSendCode() {
  errorMsg.value = ''
  if (!userStore.email) { errorMsg.value = '无法获取邮箱，请重新登录'; return }
  try {
    const { data } = await sendChangePasswordCode(userStore.email)
    if (data.code === 200) {
      codeCountdown.value = 60
      countdownTimer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0 && countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
      }, 1000)
    } else {
      errorMsg.value = data.message || '发送失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '发送失败，请稍后再试'
  }
}

const strength = ref(0)
const strengthLabel = ref('')
const strengthColor = ref('')

function checkStrength() {
  const pwd = form.value.newPassword
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/[0-9]/.test(pwd)) score++
  if (/[^A-Za-z0-9]/.test(pwd)) score++
  strength.value = score
  if (score <= 1) { strengthLabel.value = '弱'; strengthColor.value = '#e53935' }
  else if (score <= 3) { strengthLabel.value = '中'; strengthColor.value = '#FFB347' }
  else { strengthLabel.value = '强'; strengthColor.value = '#4caf50' }
}

async function handleChange() {
  errorMsg.value = ''
  successMsg.value = ''
  if (!form.value.oldPassword) { errorMsg.value = '请输入当前密码'; return }
  if (!form.value.newPassword) { errorMsg.value = '请输入新密码'; return }
  if (form.value.newPassword.length < 6) { errorMsg.value = '新密码长度至少6位'; return }
  if (form.value.newPassword === form.value.oldPassword) { errorMsg.value = '新密码不能与旧密码相同'; return }
  if (form.value.newPassword !== form.value.confirmPassword) { errorMsg.value = '两次密码输入不一致'; return }
  if (!code.value.trim()) { errorMsg.value = '请输入验证码'; return }

  isLoading.value = true
  try {
    const { data } = await api.post('/api/change-password', {
      userId: userStore.userId,
      username: userStore.username,
      email: userStore.email,
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword,
      code: code.value
    })
    if (data.code === 200) {
      successMsg.value = '密码修改成功！'
      form.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      strength.value = 0
    } else {
      errorMsg.value = data.message || '修改失败，请检查当前密码'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '网络错误，请稍后再试'
  } finally {
    isLoading.value = false
  }
}

function goBack() { router.push('/') }
</script>

<template>
  <div class="change-pwd-page">
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="clouds"><div class="cloud cloud-1"></div><div class="cloud cloud-2"></div></div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <div class="decor">
      <span class="float-icon fi-1">🔑</span>
      <span class="float-icon fi-2">✨</span>
      <span class="float-icon fi-3">🌙</span>
    </div>

    <nav class="page-nav">
      <button class="back-btn" @click="goBack"><span>←</span><span>返回首页</span></button>
      <h1 class="page-title">🔑 修改密码</h1>
      <div class="nav-placeholder"></div>
    </nav>

    <div class="content-wrapper">
      <div class="pwd-card glass">
        <div class="card-header">
          <div class="card-icon">🔐</div>
          <h2 class="card-title">修改密码</h2>
          <p class="card-subtitle">定期更换密码，保护账户安全</p>
        </div>

        <form class="pwd-form" @submit.prevent="handleChange">
          <div class="form-group">
            <label for="oldPwd"><span class="label-icon">🔒</span>当前密码</label>
            <div class="input-wrapper">
              <input id="oldPwd" v-model="form.oldPassword" :type="showOld ? 'text' : 'password'" placeholder="输入当前密码" maxlength="50" :disabled="isLoading" />
              <button type="button" class="toggle-pwd" @click="showOld = !showOld">{{ showOld ? '🙈' : '👁️' }}</button>
            </div>
          </div>

          <div class="form-group">
            <label for="newPwd"><span class="label-icon">🔑</span>新密码</label>
            <div class="input-wrapper">
              <input id="newPwd" v-model="form.newPassword" :type="showNew ? 'text' : 'password'" placeholder="输入新密码（至少6位）" maxlength="50" :disabled="isLoading" @input="checkStrength" />
              <button type="button" class="toggle-pwd" @click="showNew = !showNew">{{ showNew ? '🙈' : '👁️' }}</button>
            </div>
            <div v-if="form.newPassword" class="strength-bar">
              <div class="strength-track">
                <div class="strength-fill" :style="{ width: (strength / 5 * 100) + '%', background: strengthColor }"></div>
              </div>
              <span class="strength-label" :style="{ color: strengthColor }">密码强度：{{ strengthLabel }}</span>
            </div>
          </div>

          <div class="form-group">
            <label for="confirmPwd"><span class="label-icon">🔑</span>确认新密码</label>
            <div class="input-wrapper">
              <input id="confirmPwd" v-model="form.confirmPassword" :type="showNew ? 'text' : 'password'" placeholder="再次输入新密码" maxlength="50" :disabled="isLoading" />
            </div>
          </div>

          <div class="form-group">
            <label for="code"><span class="label-icon">📧</span>邮箱验证码</label>
            <div class="code-row">
              <div class="input-wrapper code-input">
                <input id="code" v-model="code" type="text" placeholder="输入6位验证码" maxlength="6" :disabled="isLoading" />
              </div>
              <button type="button" class="send-code-btn" :disabled="codeCountdown > 0" @click="handleSendCode">
                {{ codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码' }}
              </button>
            </div>
          </div>

          <Transition name="msg">
            <div v-if="errorMsg" class="message error-msg"><span>⚠️</span>{{ errorMsg }}</div>
          </Transition>
          <Transition name="msg">
            <div v-if="successMsg" class="message success-msg"><span>✅</span>{{ successMsg }}</div>
          </Transition>

          <button type="submit" class="submit-btn" :disabled="isLoading">
            <span v-if="!isLoading" class="btn-content"><span>确认修改</span><span class="btn-sparkle">🌙</span></span>
            <span v-else class="btn-loading"><span class="spinner"></span><span>提交中...</span></span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.change-pwd-page {
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

.decor { position: fixed; inset: 0; pointer-events: none; z-index: 1; }
.float-icon { position: absolute; font-size: 1.5rem; animation: float 6s ease-in-out infinite; opacity: 0.7; }
.fi-1 { top: 12%; left: 8%; animation-delay: 0s; }
.fi-2 { top: 18%; right: 10%; animation-delay: 1.5s; font-size: 2rem; }
.fi-3 { bottom: 20%; left: 15%; animation-delay: 3s; }

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

.content-wrapper { position: relative; z-index: 10; max-width: 460px; margin: 0 auto; padding: 0 1.5rem; }

.glass {
  background: rgba(255,255,255,0.35); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.2) inset;
}

.pwd-card { padding: 2.5rem; border-radius: 24px; animation: card-enter 0.6s cubic-bezier(0.16,1,0.3,1); }
.card-header { text-align: center; margin-bottom: 2rem; }
.card-icon { font-size: 2.5rem; margin-bottom: 0.75rem; animation: pulse-icon 3s ease-in-out infinite; }
.card-title { font-size: 1.5rem; font-weight: 700; color: var(--text-dark); margin-bottom: 0.4rem; }
.card-subtitle { font-size: 0.9rem; color: var(--text-light); }

.pwd-form { display: flex; flex-direction: column; gap: 1.25rem; }
.form-group { display: flex; flex-direction: column; gap: 0.4rem; }
.form-group label { font-size: 0.85rem; font-weight: 500; color: var(--text-dark); display: flex; align-items: center; gap: 0.35rem; }
.label-icon { font-size: 0.9rem; }
.input-wrapper { position: relative; display: flex; align-items: center; }
.input-wrapper input {
  width: 100%; padding: 0.75rem 1rem; padding-right: 2.5rem;
  background: rgba(255,255,255,0.5); border: 1.5px solid rgba(124,111,224,0.15); border-radius: 12px;
  font-size: 0.95rem; color: var(--text-dark); outline: none;
  transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
}
.input-wrapper input::placeholder { color: rgba(107,104,153,0.5); }
.input-wrapper input:focus { border-color: var(--primary); background: rgba(255,255,255,0.7); box-shadow: 0 0 0 4px rgba(124,111,224,0.1); }
.input-wrapper input:disabled { opacity: 0.6; cursor: not-allowed; }
.toggle-pwd {
  position: absolute; right: 0.75rem; background: none; border: none;
  cursor: pointer; font-size: 1rem; padding: 0.25rem; opacity: 0.6; transition: opacity 0.2s;
}
.toggle-pwd:hover { opacity: 1; }

.strength-bar { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.2rem; }
.strength-track { flex: 1; height: 4px; background: rgba(124,111,224,0.1); border-radius: 2px; overflow: hidden; }
.strength-fill { height: 100%; border-radius: 2px; transition: all 0.3s ease; }
.strength-label { font-size: 0.75rem; font-weight: 500; }

.message { display: flex; align-items: center; gap: 0.5rem; padding: 0.7rem 1rem; border-radius: 10px; font-size: 0.85rem; font-weight: 500; }
.error-msg { background: rgba(255,82,82,0.1); color: #e53935; border: 1px solid rgba(255,82,82,0.2); }
.success-msg { background: rgba(76,175,80,0.1); color: #2e7d32; border: 1px solid rgba(76,175,80,0.2); }
.msg-enter-active, .msg-leave-active { transition: all 0.3s ease; }
.msg-enter-from, .msg-leave-to { opacity: 0; transform: translateY(-8px); }

.submit-btn {
  width: 100%; padding: 0.85rem; border: none; border-radius: 12px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: white; font-size: 1rem; font-weight: 600; cursor: pointer;
  transition: all 0.3s ease; font-family: 'Noto Sans SC', sans-serif;
  margin-top: 0.5rem; box-shadow: 0 4px 20px rgba(124,111,224,0.35);
  position: relative; overflow: hidden;
}
.submit-btn::before {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(255,255,255,0.15) 50%, transparent 100%);
  transform: translateX(-100%); transition: transform 0.5s ease;
}
.submit-btn:hover::before { transform: translateX(100%); }
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(124,111,224,0.45); }
.submit-btn:disabled { cursor: not-allowed; opacity: 0.8; }
.btn-content { display: flex; align-items: center; justify-content: center; gap: 0.5rem; }
.btn-sparkle { animation: pulse-icon 2.5s ease-in-out infinite; }
.btn-loading { display: flex; align-items: center; justify-content: center; gap: 0.5rem; }
.spinner { width: 18px; height: 18px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite; }

@keyframes twinkle { 0% { opacity: 0.5; } 100% { opacity: 1; } }
@keyframes float-cloud { 0%,100% { transform: translateX(0) translateY(0); } 50% { transform: translateX(50px) translateY(-20px); } }
@keyframes float { 0%,100% { transform: translateY(0) rotate(0deg); } 50% { transform: translateY(-15px) rotate(8deg); } }
@keyframes pulse { 0%,100% { opacity: 0.4; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.08); } }
@keyframes pulse-icon { 0%,100% { transform: scale(1); } 50% { transform: scale(1.12); } }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes card-enter { from { opacity: 0; transform: translateY(30px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }

.code-row { display: flex; gap: 0.75rem; align-items: stretch; }
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

@media (max-width: 768px) {
  .page-nav { padding: 1rem; }
  .content-wrapper { padding: 0 1rem; }
  .pwd-card { padding: 1.75rem; }
  .page-title { font-size: 1.1rem; }
  .nav-placeholder { display: none; }
  .code-row { flex-wrap: wrap; }
  .send-code-btn { flex-shrink: 1; min-width: 0; white-space: normal; font-size: 0.8rem; padding: 0 0.8rem; }
}

@media (max-width: 480px) {
  .pwd-card { padding: 1.5rem; }
  .card-title { font-size: 1.3rem; }
  .card-subtitle { font-size: 0.82rem; }
  .form-group label { font-size: 0.8rem; }
  .input-wrapper input { font-size: 0.9rem; padding: 0.7rem 0.9rem; }
  .submit-btn { font-size: 0.92rem; padding: 0.75rem; }
}

@media (min-width: 1024px) {
  .page-nav { padding: 1.5rem 3rem; }
  .content-wrapper { max-width: 560px; padding: 0 2rem; }
  .pwd-card { padding: 3rem; }
  .card-icon { font-size: 3rem; }
  .card-title { font-size: 1.75rem; }
  .card-subtitle { font-size: 1rem; }
  .form-group label { font-size: 0.92rem; }
  .input-wrapper input { padding: 0.85rem 1.1rem; font-size: 1rem; }
  .submit-btn { padding: 0.95rem; font-size: 1.05rem; }
}
</style>
