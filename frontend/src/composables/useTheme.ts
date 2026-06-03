import { ref, watch, onMounted } from 'vue'

type ThemeMode = 'light' | 'dark'

const THEME_KEY = 'dreamArchiveTheme'

function getSystemDark(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function getSavedTheme(): ThemeMode | null {
  if (typeof window === 'undefined') return null
  try {
    const saved = window.localStorage.getItem(THEME_KEY)
    return saved === 'light' || saved === 'dark' ? saved : null
  } catch {
    return null
  }
}

const saved = getSavedTheme()
const theme = ref<ThemeMode>(saved || (getSystemDark() ? 'dark' : 'light'))

function applyTheme(dark: boolean) {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
}

applyTheme(theme.value === 'dark')

export function useTheme() {
  onMounted(() => {
    applyTheme(theme.value === 'dark')
  })

  watch(theme, (val) => {
    applyTheme(val === 'dark')
    try {
      window.localStorage.setItem(THEME_KEY, val)
    } catch {
      // Theme still applies if persistence is unavailable.
    }
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
