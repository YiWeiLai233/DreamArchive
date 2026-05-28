import { ref, watch, onMounted } from 'vue'

type ThemeMode = 'light' | 'dark'

const THEME_KEY = 'dreamArchiveTheme'

function getSystemDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

const saved = localStorage.getItem(THEME_KEY) as ThemeMode | null
const theme = ref<ThemeMode>(saved || (getSystemDark() ? 'dark' : 'light'))

function applyTheme(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
}

export function useTheme() {
  onMounted(() => {
    applyTheme(theme.value === 'dark')
  })

  watch(theme, (val) => {
    applyTheme(val === 'dark')
    localStorage.setItem(THEME_KEY, val)
  })

  function setTheme(mode: ThemeMode) {
    theme.value = mode
  }

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
  }

  return {
    theme,
    resolvedTheme: theme,
    setTheme,
    toggleTheme
  }
}
