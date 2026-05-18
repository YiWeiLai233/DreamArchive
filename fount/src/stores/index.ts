import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const isLoggedIn = ref(localStorage.getItem('isLoggedIn') === 'true')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const email = ref(localStorage.getItem('email') || '')
  const createdAt = ref(localStorage.getItem('createdAt') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  function login(name: string, mail?: string, joinDate?: string, id?: string) {
    isLoggedIn.value = true
    username.value = name
    email.value = mail || ''
    createdAt.value = joinDate || ''
    userId.value = id || ''
    localStorage.setItem('isLoggedIn', 'true')
    localStorage.setItem('username', name)
    if (mail) localStorage.setItem('email', mail)
    if (joinDate) localStorage.setItem('createdAt', joinDate)
    if (id) localStorage.setItem('userId', id)
  }

  function logout() {
    isLoggedIn.value = false
    username.value = ''
    email.value = ''
    createdAt.value = ''
    userId.value = ''
    avatar.value = ''
    localStorage.removeItem('isLoggedIn')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
    localStorage.removeItem('createdAt')
    localStorage.removeItem('userId')
    localStorage.removeItem('avatar')
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

  return { isLoggedIn, userId, username, email, createdAt, avatar, login, logout, updateProfile, updateAvatar }
})
