<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const props = defineProps<{ code?: string }>()
const errorCode = computed(() => props.code || route.params.code as string || '404')

const errorInfo: Record<string, { icon: string; title: string; desc: string }> = {
  '400': { icon: '⚠️', title: '请求错误', desc: '服务器无法理解您的请求，请检查输入是否正确' },
  '401': { icon: '🔒', title: '未登录', desc: '您需要登录才能访问此页面' },
  '403': { icon: '🚫', title: '无权访问', desc: '您没有权限访问此页面' },
  '404': { icon: '🔍', title: '页面未找到', desc: '您访问的页面不存在或已被移除' },
  '500': { icon: '💥', title: '服务器错误', desc: '服务器出了点问题，请稍后再试' },
  '502': { icon: '🔌', title: '服务不可用', desc: '服务器暂时无法响应，请稍后再试' },
  '503': { icon: '⏳', title: '服务维护中', desc: '服务器正在维护，请稍后再试' }
}

const currentError = computed(() => errorInfo[errorCode.value] || errorInfo['404'])

function goHome() { router.push('/') }
function goBack() { router.go(-1) }
</script>

<template>
  <div class="error-page">
    <div class="dream-bg">
      <div class="stars"></div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <div class="decor">
      <span class="float-icon fi-1">🌙</span>
      <span class="float-icon fi-2">✨</span>
      <span class="float-icon fi-3">💤</span>
    </div>

    <div class="error-content">
      <span class="error-icon">{{ currentError.icon }}</span>
      <h1 class="error-code">{{ errorCode }}</h1>
      <h2 class="error-title">{{ currentError.title }}</h2>
      <p class="error-desc">{{ currentError.desc }}</p>

      <div class="error-actions">
        <button class="btn-back" @click="goBack">返回上一页</button>
        <button class="btn-home" @click="goHome">回到首页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, var(--bg-start) 0%, var(--bg-end) 100%);
}

.dream-bg { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.stars {
  position: absolute; inset: 0;
  background-image:
    radial-gradient(2px 2px at 20px 30px, rgba(255,255,255,0.8), transparent),
    radial-gradient(2px 2px at 40px 70px, rgba(255,255,255,0.6), transparent),
    radial-gradient(1px 1px at 90px 40px, rgba(255,255,255,0.7), transparent);
  background-repeat: repeat; background-size: 200px 100px;
}
.glow { position: absolute; border-radius: 50%; filter: blur(100px); }
.glow-1 { width: 300px; height: 300px; top: 10%; right: 20%; background: rgba(124,111,224,0.2); }
.glow-2 { width: 250px; height: 250px; bottom: 15%; left: 15%; background: rgba(255,179,71,0.12); }

.decor { position: fixed; inset: 0; pointer-events: none; z-index: 1; }
.float-icon { position: absolute; font-size: 1.5rem; animation: float 6s ease-in-out infinite; opacity: 0.6; }
.fi-1 { top: 15%; left: 10%; }
.fi-2 { top: 30%; right: 12%; animation-delay: 2s; }
.fi-3 { bottom: 20%; left: 20%; animation-delay: 4s; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.error-content {
  position: relative; z-index: 10;
  text-align: center;
  padding: 2rem;
}

.error-icon {
  font-size: 4rem;
  display: block;
  margin-bottom: 1rem;
}

.error-code {
  font-size: 6rem;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
  margin: 0;
  text-shadow: 0 4px 20px rgba(124,111,224,0.3);
}

.error-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-dark);
  margin: 1rem 0 0.5rem;
}

.error-desc {
  font-size: 1rem;
  color: var(--text-light);
  margin: 0 0 2rem;
  max-width: 400px;
}

.error-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn-back, .btn-home {
  padding: 0.75rem 1.5rem;
  border-radius: 50px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
}

.btn-back {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  color: var(--text-dark);
}

.btn-back:hover {
  background: rgba(255,255,255,0.45);
  transform: translateY(-2px);
}

.btn-home {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  border: none;
  color: white;
  box-shadow: 0 4px 15px rgba(124,111,224,0.3);
}

.btn-home:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(124,111,224,0.4);
}

@media (max-width: 768px) {
  .error-code { font-size: 4rem; }
  .error-title { font-size: 1.2rem; }
  .error-actions { flex-direction: column; align-items: center; }
  .btn-back, .btn-home { width: 100%; max-width: 260px; justify-content: center; }
}

@media (max-width: 480px) {
  .error-page { padding: 1.5rem; }
  .error-icon { font-size: 3rem; }
  .error-code { font-size: 3rem; }
  .error-title { font-size: 1.1rem; }
  .error-desc { font-size: 0.88rem; }
}

@media (min-width: 1024px) {
  .error-code { font-size: 8rem; }
  .error-title { font-size: 2rem; }
  .error-desc { font-size: 1.1rem; }
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
html.dark .glow-1 { background: rgba(124, 111, 224, 0.15); }
html.dark .glow-2 { background: rgba(255, 179, 71, 0.08); }
html.dark .error-code { text-shadow: 0 0 40px rgba(155, 143, 255, 0.2); }
html.dark .error-title { color: #E8E4F0; }
html.dark .error-desc { color: #A9A3C0; }
html.dark .btn-back {
  color: #E8E4F0;
  border-color: rgba(155, 143, 255, 0.25);
}
html.dark .btn-back:hover { background: rgba(155, 143, 255, 0.15); }
html.dark .btn-home { box-shadow: 0 4px 15px rgba(155, 143, 255, 0.25); }
html.dark .btn-home:hover { box-shadow: 0 6px 20px rgba(155, 143, 255, 0.35); }
</style>
