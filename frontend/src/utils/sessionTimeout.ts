import type { Router } from 'vue-router'
import { useUserStore } from '@/stores'
import { logout as logoutApi } from '@/api/user'

const CHECK_INTERVAL_MS = 30 * 1000
const ACTIVITY_REFRESH_GAP_MS = 1000
const ACTIVITY_EVENTS = ['click', 'keydown', 'pointerdown', 'scroll', 'touchstart'] as const

let installed = false
let lastRefreshAt = 0
let logoutInFlight = false

function redirectToLogin(router: Router) {
  const route = router.currentRoute.value
  if (route.path === '/login') return

  if (route.meta.requiresAuth) {
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  router.replace('/login')
}

export function installSessionTimeout(router: Router) {
  if (installed) return
  installed = true

  const userStore = useUserStore()

  function expireIfNeeded() {
    if (!userStore.isLoggedIn) return
    if (!userStore.isSessionExpired()) return

    if (!logoutInFlight) {
      logoutInFlight = true
      logoutApi().finally(() => {
        logoutInFlight = false
      })
    }
    userStore.logout()
    redirectToLogin(router)
  }

  function refreshOnActivity() {
    if (!userStore.isLoggedIn) return

    if (userStore.isSessionExpired()) {
      expireIfNeeded()
      return
    }

    const now = Date.now()
    if (now - lastRefreshAt < ACTIVITY_REFRESH_GAP_MS) return

    lastRefreshAt = now
    userStore.refreshSession()
  }

  for (const eventName of ACTIVITY_EVENTS) {
    window.addEventListener(eventName, refreshOnActivity, { capture: true, passive: true })
  }

  document.addEventListener('visibilitychange', expireIfNeeded)
  window.setInterval(expireIfNeeded, CHECK_INTERVAL_MS)
  expireIfNeeded()
}
