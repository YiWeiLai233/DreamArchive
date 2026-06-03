<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'

const { theme, toggleTheme } = useTheme()
</script>

<template>
  <button
    class="global-theme-toggle"
    type="button"
    :title="theme === 'light' ? '切换深色模式' : '切换浅色模式'"
    :aria-label="theme === 'light' ? '切换深色模式' : '切换浅色模式'"
    @click="toggleTheme"
  >
    <span v-if="theme === 'dark'">☀️</span>
    <span v-else>🌙</span>
  </button>
</template>

<style scoped>
.global-theme-toggle {
  position: fixed;
  right: max(1rem, env(safe-area-inset-right));
  bottom: max(1rem, env(safe-area-inset-bottom));
  z-index: 80;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border: 1.5px solid rgba(124, 111, 224, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.5);
  color: var(--text-dark);
  box-shadow: 0 12px 30px rgba(45, 43, 85, 0.16);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 1.35rem;
  line-height: 1;
  transition: transform 0.25s ease, box-shadow 0.25s ease, background 0.25s ease, border-color 0.25s ease;
}

.global-theme-toggle:hover {
  transform: translateY(-3px);
  background: rgba(255, 255, 255, 0.68);
  border-color: rgba(124, 111, 224, 0.42);
  box-shadow: 0 16px 40px rgba(124, 111, 224, 0.24);
}

html.dark .global-theme-toggle {
  background: rgba(32, 28, 52, 0.9);
  border-color: rgba(184, 174, 255, 0.46);
  box-shadow: 0 14px 36px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(184, 174, 255, 0.08) inset;
}

html.dark .global-theme-toggle:hover {
  background: rgba(42, 36, 68, 0.94);
  border-color: rgba(184, 174, 255, 0.62);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.5), 0 0 24px rgba(155, 143, 255, 0.18);
}

@media (max-width: 768px) {
  .global-theme-toggle {
    width: 2.55rem;
    height: 2.55rem;
    right: max(0.85rem, env(safe-area-inset-right));
    bottom: max(0.85rem, env(safe-area-inset-bottom));
    font-size: 1.12rem;
  }
}
</style>
