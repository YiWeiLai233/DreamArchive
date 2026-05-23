import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const SESSION_TIMEOUT_MS = 15 * 60 * 1000

const SESSION_COOKIE_NAME = 'dreamArchiveSession'
const LAST_ACTIVITY_KEY = 'lastActivityAt'

function setSessionCookie() {
  const maxAgeSeconds = Math.floor(SESSION_TIMEOUT_MS / 1000)
  document.cookie = `${SESSION_COOKIE_NAME}=active; Max-Age=${maxAgeSeconds}; Path=/; SameSite=Lax`
}

function clearSessionCookie() {
  document.cookie = `${SESSION_COOKIE_NAME}=; Max-Age=0; Path=/; SameSite=Lax`
}

function hasSessionCookie() {
  return document.cookie
    .split(';')
    .map((item) => item.trim())
    .some((item) => item.startsWith(`${SESSION_COOKIE_NAME}=`))
}

export const useUserStore = defineStore('user', () => {
  const isLoggedIn = ref(localStorage.getItem('isLoggedIn') === 'true')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const email = ref(localStorage.getItem('email') || '')
  const createdAt = ref(localStorage.getItem('createdAt') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')
  const role = ref(localStorage.getItem('role') || 'USER')
  const token = ref(localStorage.getItem('authToken') || '')
  const isAdmin = computed(() => role.value === 'ADMIN')

  function login(name: string, mail?: string, joinDate?: string, id?: string | number, userRole?: string, authToken?: string) {
    isLoggedIn.value = true
    username.value = name
    email.value = mail || ''
    createdAt.value = joinDate || ''
    userId.value = id ? String(id) : ''
    role.value = userRole || 'USER'
    token.value = authToken || ''
    localStorage.setItem('isLoggedIn', 'true')
    localStorage.setItem('username', name)
    if (mail) localStorage.setItem('email', mail)
    if (joinDate) localStorage.setItem('createdAt', joinDate)
    if (id) localStorage.setItem('userId', String(id))
    localStorage.setItem('role', role.value)
    if (authToken) localStorage.setItem('authToken', authToken)
    refreshSession()
  }

  function logout() {
    isLoggedIn.value = false
    username.value = ''
    email.value = ''
    createdAt.value = ''
    userId.value = ''
    avatar.value = ''
    role.value = 'USER'
    token.value = ''
    localStorage.removeItem('isLoggedIn')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
    localStorage.removeItem('createdAt')
    localStorage.removeItem('userId')
    localStorage.removeItem('avatar')
    localStorage.removeItem('role')
    localStorage.removeItem('authToken')
    localStorage.removeItem(LAST_ACTIVITY_KEY)
    clearSessionCookie()
  }

  function refreshSession() {
    if (!isLoggedIn.value) return
    localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()))
    setSessionCookie()
  }

  function isSessionExpired() {
    if (!isLoggedIn.value) return false

    const lastActivityAt = Number(localStorage.getItem(LAST_ACTIVITY_KEY) || 0)
    if (!lastActivityAt) return false
    if (!hasSessionCookie()) return true

    return Date.now() - lastActivityAt > SESSION_TIMEOUT_MS
  }

  function updateProfile(name: string, mail: string) {
    username.value = name
    email.value = mail
    localStorage.setItem('username', name)
    if (mail) localStorage.setItem('email', mail)
  }

  function updateAvatar(value: string) {
    avatar.value = value
    if (value) {
      localStorage.setItem('avatar', value)
    } else {
      localStorage.removeItem('avatar')
    }
  }

  if (isLoggedIn.value) {
    if (isSessionExpired()) {
      logout()
    } else if (!localStorage.getItem(LAST_ACTIVITY_KEY)) {
      refreshSession()
    }
  }

  return {
    isLoggedIn,
    userId,
    username,
    email,
    createdAt,
    avatar,
    role,
    token,
    isAdmin,
    login,
    logout,
    refreshSession,
    isSessionExpired,
    updateProfile,
    updateAvatar
  }
})
