<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  deleteAdminUser,
  getAdminDreamDetail,
  getAdminOverview,
  runAdminUserAction,
  type AdminDreamDetail,
  type AdminDreamSummary,
  type AdminOverview,
  type AdminUserSummary
} from '@/api/admin'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()

const overview = ref<AdminOverview | null>(null)
const isLoading = ref(true)
const errorMsg = ref('')
const userSearch = ref('')
const dreamSearch = ref('')
const userPage = ref(1)
const userPageSize = ref(10)
const dreamPage = ref(1)
const dreamPageSize = ref(5)
const activePanel = ref<'users' | 'dreams'>('users')
const actionMsg = ref('')
const actionError = ref('')
const isSubmitting = ref(false)
const isUserModalOpen = ref(false)
const isDreamModalOpen = ref(false)
const isDreamDetailLoading = ref(false)
const dreamDetailError = ref('')
const selectedDreamSummary = ref<AdminDreamSummary | null>(null)
const selectedDreamDetail = ref<AdminDreamDetail | null>(null)
const isConfirmOpen = ref(false)
const confirmTitle = ref('')
const confirmMessage = ref('')
const confirmType = ref<'danger' | 'warning'>('warning')
let confirmResolve: ((value: boolean) => void) | null = null
const editingUser = ref<AdminUserSummary | null>(null)
const userForm = ref<{
  id?: number
  username: string
  email: string
  password: string
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN'
  status: 'ACTIVE' | 'BANNED'
}>({
  username: '',
  email: '',
  password: '',
  role: 'USER',
  status: 'ACTIVE'
})

const filteredUsers = computed(() => {
  return overview.value?.users || []
})

const filteredDreams = computed(() => {
  return overview.value?.recentDreams || []
})

const userTotalPages = computed(() => overview.value?.userTotalPages || 1)
const dreamTotalPages = computed(() => overview.value?.dreamTotalPages || 1)
const userResultTotal = computed(() => overview.value?.userResultTotal || 0)
const dreamResultTotal = computed(() => overview.value?.dreamResultTotal || 0)
let userSearchTimer: number | undefined
let dreamSearchTimer: number | undefined

async function loadOverview() {
  if (!userStore.token) {
    errorMsg.value = '登录状态已失效，请重新登录'
    isLoading.value = false
    return
  }

  try {
    isLoading.value = true
    errorMsg.value = ''
    const res = await getAdminOverview(userStore.token, {
      userPage: userPage.value,
      userPageSize: userPageSize.value,
      userKeyword: userSearch.value.trim() || undefined,
      dreamPage: dreamPage.value,
      dreamPageSize: dreamPageSize.value,
      dreamKeyword: dreamSearch.value.trim() || undefined
    })
    if (res.data.code === 200) {
      overview.value = res.data.data
      userPage.value = res.data.data.userPage || userPage.value
      userPageSize.value = res.data.data.userPageSize || userPageSize.value
      dreamPage.value = res.data.data.dreamPage || dreamPage.value
      dreamPageSize.value = res.data.data.dreamPageSize || dreamPageSize.value
    } else {
      errorMsg.value = res.data.message || '加载管理员数据失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || e.message || '加载管理员数据失败'
  } finally {
    isLoading.value = false
  }
}

function pageStart(total: number, page: number, size: number) {
  return total <= 0 ? 0 : (page - 1) * size + 1
}

function pageEnd(total: number, page: number, size: number) {
  return Math.min(total, page * size)
}

function changeUserPage(nextPage: number) {
  const next = Math.min(Math.max(nextPage, 1), userTotalPages.value)
  if (next === userPage.value) return
  userPage.value = next
  loadOverview()
}

function changeDreamPage(nextPage: number) {
  const next = Math.min(Math.max(nextPage, 1), dreamTotalPages.value)
  if (next === dreamPage.value) return
  dreamPage.value = next
  loadOverview()
}

function getPageNumbers(current: number, total: number): (number | string)[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages: (number | string)[] = [1]
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  if (start > 2) pages.push('...')
  for (let i = start; i <= end; i++) pages.push(i)
  if (end < total - 1) pages.push('...')
  pages.push(total)
  return pages
}

function changeUserPageSize() {
  userPage.value = 1
  loadOverview()
}

function changeDreamPageSize() {
  dreamPage.value = 1
  loadOverview()
}

function switchPanel(panel: 'users' | 'dreams') {
  activePanel.value = panel
}

watch(userSearch, () => {
  window.clearTimeout(userSearchTimer)
  userSearchTimer = window.setTimeout(() => {
    userPage.value = 1
    loadOverview()
  }, 300)
})

watch(dreamSearch, () => {
  window.clearTimeout(dreamSearchTimer)
  dreamSearchTimer = window.setTimeout(() => {
    dreamPage.value = 1
    loadOverview()
  }, 300)
})

function formatDate(raw?: string) {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function emotionLabel(emotion?: string) {
  const map: Record<string, string> = {
    happy: '开心',
    sad: '难过',
    scary: '恐惧',
    angry: '愤怒',
    peaceful: '平静',
    mysterious: '神秘'
  }
  return emotion ? map[emotion] || emotion : '-'
}

function preview(text?: string) {
  if (!text) return '暂无内容'
  return text.length > 72 ? `${text.slice(0, 72)}...` : text
}

function statusLabel(status?: string) {
  return status === 'BANNED' ? '已封禁' : '正常'
}

function resetActionMessage() {
  actionMsg.value = ''
  actionError.value = ''
}

function openCreateUser() {
  resetActionMessage()
  editingUser.value = null
  userForm.value = {
    username: '',
    email: '',
    password: '',
    role: 'USER',
    status: 'ACTIVE'
  }
  isUserModalOpen.value = true
}

function openEditUser(user: AdminUserSummary) {
  resetActionMessage()
  editingUser.value = user
  userForm.value = {
    id: user.id,
    username: user.username,
    email: user.email,
    password: '',
    role: user.role,
    status: user.status || 'ACTIVE'
  }
  isUserModalOpen.value = true
}

function closeUserModal() {
  if (!isSubmitting.value) {
    isUserModalOpen.value = false
  }
}

function closeDreamModal() {
  if (!isDreamDetailLoading.value) {
    isDreamModalOpen.value = false
  }
}

function showConfirm(title: string, message: string, type: 'danger' | 'warning' = 'warning'): Promise<boolean> {
  confirmTitle.value = title
  confirmMessage.value = message
  confirmType.value = type
  isConfirmOpen.value = true
  return new Promise(resolve => {
    confirmResolve = resolve
  })
}

function handleConfirm(result: boolean) {
  isConfirmOpen.value = false
  if (confirmResolve) {
    confirmResolve(result)
    confirmResolve = null
  }
}

async function openDreamDetail(dream: AdminDreamSummary) {
  selectedDreamSummary.value = dream
  selectedDreamDetail.value = null
  dreamDetailError.value = ''
  isDreamModalOpen.value = true

  if (!userStore.token) {
    dreamDetailError.value = '登录状态已失效，请重新登录'
    return
  }

  try {
    isDreamDetailLoading.value = true
    const res = await getAdminDreamDetail(userStore.token, dream.id)
    if (res.data.code === 200) {
      selectedDreamDetail.value = res.data.data
    } else {
      dreamDetailError.value = res.data.message || '加载梦境详情失败'
    }
  } catch (e: any) {
    dreamDetailError.value = e.response?.data?.message || e.message || '加载梦境详情失败'
  } finally {
    isDreamDetailLoading.value = false
  }
}

function isCurrentUser(user: AdminUserSummary) {
  return String(user.id) === userStore.userId
}

async function submitUserForm() {
  resetActionMessage()
  const username = userForm.value.username.trim()
  const email = userForm.value.email.trim()
  const password = userForm.value.password

  if (!username || !email) {
    actionError.value = '请填写用户名和邮箱'
    return
  }
  if (!editingUser.value && password.length < 6) {
    actionError.value = '新增账号时密码至少 6 位'
    return
  }
  if (password && password.length < 6) {
    actionError.value = '密码至少 6 位'
    return
  }

  const payload = {
    action: editingUser.value ? 'UPDATE' as const : 'CREATE' as const,
    id: editingUser.value?.id,
    username,
    email,
    password: password || undefined,
    role: userForm.value.role,
    status: userForm.value.status
  }

  const ok = await performUserAction(payload, editingUser.value ? '账号已更新' : '账号已新增')
  if (ok) {
    closeUserModal()
  }
}

async function toggleUserBan(user: AdminUserSummary) {
  resetActionMessage()
  if (isCurrentUser(user)) {
    actionError.value = '不能封禁自己的账号'
    return
  }

  const isBanned = user.status === 'BANNED'
  const action = isBanned ? 'UNBAN' as const : 'BAN' as const
  const title = isBanned ? '解封账号' : '封禁账号'
  const message = isBanned
    ? `确定要解封「${user.username}」吗？解封后该用户可以正常登录和使用系统。`
    : `确定要封禁「${user.username}」吗？封禁后该用户将无法登录，但数据会保留。`
  const ok = await showConfirm(title, message, 'warning')
  if (!ok) return
  await performUserAction({ action, id: user.id }, isBanned ? '账号已解封' : '账号已封禁')
}

async function deleteUser(user: AdminUserSummary) {
  resetActionMessage()
  if (isCurrentUser(user)) {
    actionError.value = '不能删除自己的账号'
    return
  }

  const title = '删除账号'
  const message = `确定要删除「${user.username}」吗？删除后该用户将无法登录，数据库记录会保留软删除标记。`
  const ok = await showConfirm(title, message, 'danger')
  if (!ok) return
  await performDeleteUser(user.id)
}

async function performUserAction(
  payload: Parameters<typeof runAdminUserAction>[1],
  successMessage: string
) {
  if (!userStore.token) {
    actionError.value = '登录状态已失效，请重新登录'
    return false
  }

  try {
    isSubmitting.value = true
    const res = await runAdminUserAction(userStore.token, payload)
    if (res.data.code === 200) {
      if (payload.action === 'CREATE') {
        userPage.value = 1
      }
      actionMsg.value = successMessage
      await loadOverview()
      return true
    }
    actionError.value = res.data.message || '操作失败'
    return false
  } catch (e: any) {
    actionError.value = e.response?.data?.message || e.message || '操作失败'
    return false
  } finally {
    isSubmitting.value = false
  }
}

async function performDeleteUser(id: number) {
  if (!userStore.token) {
    actionError.value = '登录状态已失效，请重新登录'
    return false
  }

  try {
    isSubmitting.value = true
    const res = await deleteAdminUser(userStore.token, id)
    if (res.data.code === 200) {
      actionMsg.value = '账号已删除'
      await loadOverview()
      return true
    }
    actionError.value = res.data.message || '删除失败'
    return false
  } catch (e: any) {
    actionError.value = e.response?.data?.message || e.message || '删除失败'
    return false
  } finally {
    isSubmitting.value = false
  }
}

function goHome() {
  router.push('/')
}

onMounted(loadOverview)
</script>

<template>
  <div class="admin-page">
    <!-- 顶部导航栏 -->
    <header class="admin-topbar">
      <div class="topbar-left">
        <div class="topbar-brand">
          <span class="brand-icon">D</span>
          <div>
            <span class="brand-name">DreamArchive</span>
            <span class="brand-tag">Admin</span>
          </div>
        </div>
      </div>
      <div class="topbar-right">
        <button class="topbar-btn" type="button" :disabled="isLoading" @click="loadOverview">
          <span class="topbar-btn-icon" :class="{ spinning: isLoading }">&#x21bb;</span>
          {{ isLoading ? '刷新中' : '刷新数据' }}
        </button>
        <div class="topbar-divider"></div>
        <button class="topbar-btn home" type="button" @click="goHome">
          <span class="topbar-btn-icon">&#x2190;</span>
          返回前台
        </button>
      </div>
    </header>

    <div class="admin-layout">
      <!-- 左侧导航 -->
      <aside class="admin-sidebar">
        <div class="sidebar-section">
          <span class="sidebar-label">数据概览</span>
        </div>
        <button
          class="sidebar-item"
          :class="{ active: activePanel === 'users' }"
          type="button"
          @click="switchPanel('users')"
        >
          <span class="sidebar-item-icon users-icon">&#x1F465;</span>
          <div class="sidebar-item-text">
            <span>用户管理</span>
            <span class="sidebar-count">{{ userResultTotal }}</span>
          </div>
        </button>
        <button
          class="sidebar-item"
          :class="{ active: activePanel === 'dreams' }"
          type="button"
          @click="switchPanel('dreams')"
        >
          <span class="sidebar-item-icon dreams-icon">&#x1F319;</span>
          <div class="sidebar-item-text">
            <span>最近梦境</span>
            <span class="sidebar-count">{{ dreamResultTotal }}</span>
          </div>
        </button>
      </aside>

      <!-- 右侧内容区 -->
      <main class="admin-content">
        <!-- 加载状态 -->
        <section v-if="isLoading" class="state-section">
          <div class="state-spinner"></div>
          <p>正在加载管理员数据...</p>
        </section>

        <!-- 错误状态 -->
        <section v-else-if="errorMsg" class="state-section error">
          <div class="state-icon">&#x26A0;</div>
          <strong>{{ errorMsg }}</strong>
          <button class="action-btn primary" type="button" @click="loadOverview">重试</button>
        </section>

        <template v-else-if="overview">
          <!-- 消息提示 -->
          <div v-if="actionMsg || actionError" class="toast" :class="{ error: actionError }">
            <span class="toast-icon">{{ actionError ? '&#x2716;' : '&#x2714;' }}</span>
            {{ actionError || actionMsg }}
          </div>

          <!-- 指标卡片 -->
          <section class="metrics-row" aria-label="关键指标">
            <article class="metric-card">
              <div class="metric-info">
                <span class="metric-label">用户总数</span>
                <strong class="metric-value">{{ overview.totalUsers }}</strong>
              </div>
              <div class="metric-icon-wrap indigo">&#x1F465;</div>
            </article>
            <article class="metric-card">
              <div class="metric-info">
                <span class="metric-label">梦境总数</span>
                <strong class="metric-value">{{ overview.totalDreams }}</strong>
              </div>
              <div class="metric-icon-wrap blue">&#x1F4AD;</div>
            </article>
            <article class="metric-card">
              <div class="metric-info">
                <span class="metric-label">今日新增</span>
                <strong class="metric-value">{{ overview.todayDreams }}</strong>
              </div>
              <div class="metric-icon-wrap green">&#x1F4C8;</div>
            </article>
            <article class="metric-card">
              <div class="metric-info">
                <span class="metric-label">管理员</span>
                <strong class="metric-value">{{ overview.adminUsers }}</strong>
              </div>
              <div class="metric-icon-wrap amber">&#x1F6E1;</div>
            </article>
          </section>

          <!-- 用户管理面板 -->
          <section v-if="activePanel === 'users'" class="content-card">
            <div class="card-header">
              <div class="card-title-group">
                <h2>用户管理</h2>
                <span class="card-subtitle">
                  第 {{ pageStart(userResultTotal, userPage, userPageSize) }}-{{ pageEnd(userResultTotal, userPage, userPageSize) }}
                  条 / 共 {{ userResultTotal }} 个账号
                </span>
              </div>
              <div class="card-tools">
                <div class="search-input">
                  <span class="search-icon">&#x1F50D;</span>
                  <input v-model="userSearch" type="search" placeholder="搜索用户名、邮箱..." />
                </div>
                <button class="action-btn primary" type="button" @click="openCreateUser">
                  <span>&#x2B;</span> 新增账号
                </button>
              </div>
            </div>

            <div class="table-container">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>用户</th>
                    <th>邮箱</th>
                    <th>角色</th>
                    <th>状态</th>
                    <th>梦境数</th>
                    <th>注册时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="user in filteredUsers" :key="user.id">
                    <td class="id-cell">#{{ user.id }}</td>
                    <td class="name-cell">{{ user.username }}</td>
                    <td class="email-cell">{{ user.email }}</td>
                    <td>
                      <span class="badge" :class="user.role === 'SUPER_ADMIN' ? 'badge-gold' : user.role === 'ADMIN' ? 'badge-purple' : 'badge-gray'">
                        {{ user.role === 'SUPER_ADMIN' ? '超级管理员' : user.role === 'ADMIN' ? '管理员' : '用户' }}
                      </span>
                    </td>
                    <td>
                      <span class="badge" :class="user.status === 'BANNED' ? 'badge-red' : 'badge-green'">
                        {{ statusLabel(user.status) }}
                      </span>
                    </td>
                    <td class="count-cell">{{ user.dreamCount }}</td>
                    <td class="date-cell">{{ formatDate(user.createdAt) }}</td>
                    <td class="action-cell">
                      <button class="action-btn small" type="button" :disabled="isSubmitting || user.role === 'SUPER_ADMIN'" @click="openEditUser(user)">
                        编辑
                      </button>
                      <button
                        class="action-btn small warning"
                        type="button"
                        :disabled="isSubmitting || isCurrentUser(user) || user.role === 'SUPER_ADMIN'"
                        @click="toggleUserBan(user)"
                      >
                        {{ user.status === 'BANNED' ? '解封' : '封禁' }}
                      </button>
                      <button
                        class="action-btn small danger"
                        type="button"
                        :disabled="isSubmitting || isCurrentUser(user) || user.role === 'SUPER_ADMIN'"
                        @click="deleteUser(user)"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                  <tr v-if="filteredUsers.length === 0">
                    <td colspan="8" class="empty-row">没有匹配的用户</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="card-footer">
              <span class="footer-info">第 {{ userPage }} / {{ userTotalPages }} 页</span>
              <div class="footer-actions">
                <div class="size-select">
                  <span>每页</span>
                  <select v-model.number="userPageSize" :disabled="isLoading" @change="changeUserPageSize">
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                    <option :value="50">50</option>
                  </select>
                </div>
                <div class="page-btns">
                  <button class="page-btn" type="button" :disabled="userPage <= 1 || isLoading" @click="changeUserPage(userPage - 1)">&#x276E;</button>
                  <template v-for="(p, i) in getPageNumbers(userPage, userTotalPages)" :key="i">
                    <span v-if="p === '...'" class="page-ellipsis">...</span>
                    <button v-else class="page-btn" :class="{ active: p === userPage }" type="button" :disabled="isLoading" @click="changeUserPage(p as number)">{{ p }}</button>
                  </template>
                  <button class="page-btn" type="button" :disabled="userPage >= userTotalPages || isLoading" @click="changeUserPage(userPage + 1)">&#x276F;</button>
                </div>
              </div>
            </div>
          </section>

          <!-- 最近梦境面板 -->
          <section v-else class="content-card">
            <div class="card-header">
              <div class="card-title-group">
                <h2>最近梦境</h2>
                <span class="card-subtitle">
                  第 {{ pageStart(dreamResultTotal, dreamPage, dreamPageSize) }}-{{ pageEnd(dreamResultTotal, dreamPage, dreamPageSize) }}
                  条 / 共 {{ dreamResultTotal }} 条记录
                </span>
              </div>
              <div class="card-tools">
                <div class="search-input">
                  <span class="search-icon">&#x1F50D;</span>
                  <input v-model="dreamSearch" type="search" placeholder="搜索标题、内容、用户..." />
                </div>
              </div>
            </div>

            <div class="dream-list">
              <article
                v-for="dream in filteredDreams"
                :key="dream.id"
                class="dream-item"
                role="button"
                tabindex="0"
                @click="openDreamDetail(dream)"
                @keydown.enter="openDreamDetail(dream)"
              >
                <div class="dream-item-left">
                  <div class="dream-item-title">
                    <strong>{{ dream.title || '未命名梦境' }}</strong>
                    <span class="badge badge-blue">{{ emotionLabel(dream.emotion) }}</span>
                  </div>
                  <p class="dream-item-preview">{{ preview(dream.content) }}</p>
                </div>
                <div class="dream-item-right">
                  <span>{{ dream.username || `用户 ${dream.userId}` }}</span>
                  <span>{{ dream.place || '未知地点' }}</span>
                  <span>{{ formatDate(dream.createdAt) }}</span>
                </div>
              </article>
              <div v-if="filteredDreams.length === 0" class="empty-row">没有匹配的梦境记录</div>
            </div>

            <div class="card-footer">
              <span class="footer-info">第 {{ dreamPage }} / {{ dreamTotalPages }} 页</span>
              <div class="footer-actions">
                <div class="size-select">
                  <span>每页</span>
                  <select v-model.number="dreamPageSize" :disabled="isLoading" @change="changeDreamPageSize">
                    <option :value="5">5</option>
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                  </select>
                </div>
                <div class="page-btns">
                  <button class="page-btn" type="button" :disabled="dreamPage <= 1 || isLoading" @click="changeDreamPage(dreamPage - 1)">&#x276E;</button>
                  <template v-for="(p, i) in getPageNumbers(dreamPage, dreamTotalPages)" :key="i">
                    <span v-if="p === '...'" class="page-ellipsis">...</span>
                    <button v-else class="page-btn" :class="{ active: p === dreamPage }" type="button" :disabled="isLoading" @click="changeDreamPage(p as number)">{{ p }}</button>
                  </template>
                  <button class="page-btn" type="button" :disabled="dreamPage >= dreamTotalPages || isLoading" @click="changeDreamPage(dreamPage + 1)">&#x276F;</button>
                </div>
              </div>
            </div>
          </section>
        </template>
      </main>
    </div>

    <div v-if="isUserModalOpen" class="modal-mask" @click.self="closeUserModal">
      <section class="user-modal" aria-label="账号表单">
        <div class="modal-hero" :class="{ edit: editingUser }">
          <span class="hero-icon">{{ editingUser ? '✏️' : '👤' }}</span>
          <h2>{{ editingUser ? '编辑账号' : '新增账号' }}</h2>
          <p>{{ editingUser ? '修改用户信息和权限' : '创建一个新的用户账号' }}</p>
          <button class="hero-close" type="button" :disabled="isSubmitting" @click="closeUserModal">×</button>
        </div>

        <form class="user-form" @submit.prevent="submitUserForm">
          <div class="form-group">
            <label class="form-label">
              <span class="label-dot"></span>
              用户名
            </label>
            <div class="input-wrap">
              <span class="input-icon">👤</span>
              <input v-model="userForm.username" type="text" maxlength="20" placeholder="输入用户名" :disabled="isSubmitting" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="label-dot"></span>
              邮箱
            </label>
            <div class="input-wrap">
              <span class="input-icon">📧</span>
              <input v-model="userForm.email" type="email" maxlength="100" placeholder="输入邮箱地址" :disabled="isSubmitting" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="label-dot"></span>
              {{ editingUser ? '新密码' : '密码' }}
            </label>
            <div class="input-wrap">
              <span class="input-icon">🔒</span>
              <input
                v-model="userForm.password"
                type="password"
                maxlength="50"
                :placeholder="editingUser ? '留空表示不修改' : '至少 6 位'"
                :disabled="isSubmitting"
              />
            </div>
          </div>

          <div class="form-group-row">
            <div class="form-group">
              <label class="form-label">角色</label>
              <div class="select-wrap">
                <select v-model="userForm.role" :disabled="isSubmitting || editingUser?.role === 'SUPER_ADMIN'">
                  <option value="USER">普通用户</option>
                  <option value="ADMIN">管理员</option>
                  <option v-if="editingUser?.role === 'SUPER_ADMIN'" value="SUPER_ADMIN">超级管理员</option>
                </select>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">状态</label>
              <div class="select-wrap">
                <select v-model="userForm.status" :disabled="isSubmitting">
                  <option value="ACTIVE">正常</option>
                  <option value="BANNED">封禁</option>
                </select>
              </div>
            </div>
          </div>

          <p v-if="actionError" class="form-error">
            <span class="error-icon">!</span>
            {{ actionError }}
          </p>

          <div class="modal-actions">
            <button class="cancel-btn" type="button" :disabled="isSubmitting" @click="closeUserModal">取消</button>
            <button class="save-btn" :class="{ edit: editingUser }" type="submit" :disabled="isSubmitting">
              <span v-if="isSubmitting" class="btn-spinner"></span>
              <span v-else class="btn-icon">{{ editingUser ? '✓' : '+' }}</span>
              {{ isSubmitting ? '保存中...' : (editingUser ? '保存修改' : '创建账号') }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="isConfirmOpen" class="modal-mask confirm-mask" @click.self="handleConfirm(false)">
      <section class="confirm-modal" :class="confirmType" aria-label="确认操作">
        <div class="confirm-icon-wrap">
          <span v-if="confirmType === 'danger'" class="confirm-icon">!</span>
          <span v-else class="confirm-icon">?</span>
        </div>
        <h2 class="confirm-title">{{ confirmTitle }}</h2>
        <p class="confirm-message">{{ confirmMessage }}</p>
        <div class="confirm-actions">
          <button class="ghost-btn" type="button" @click="handleConfirm(false)">取消</button>
          <button
            class="confirm-btn"
            :class="confirmType"
            type="button"
            @click="handleConfirm(true)"
          >
            {{ confirmType === 'danger' ? '确认删除' : '确认' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="isDreamModalOpen" class="modal-mask" @click.self="closeDreamModal">
      <section class="dream-modal" aria-label="梦境详情">
        <header class="modal-header">
          <div>
            <h2>{{ selectedDreamDetail?.title || selectedDreamSummary?.title || '未命名梦境' }}</h2>
            <p class="modal-subtitle">
              {{ selectedDreamSummary?.username || `用户 ${selectedDreamDetail?.userId || selectedDreamSummary?.userId || '-'}` }}
              · {{ formatDate(selectedDreamDetail?.createdAt || selectedDreamSummary?.createdAt) }}
            </p>
          </div>
          <button class="icon-btn" type="button" :disabled="isDreamDetailLoading" @click="closeDreamModal">×</button>
        </header>

        <div class="dream-detail-body">
          <div v-if="isDreamDetailLoading" class="detail-state">正在加载梦境详情...</div>
          <div v-else-if="dreamDetailError" class="detail-state error">{{ dreamDetailError }}</div>
          <template v-else-if="selectedDreamDetail">
            <div class="detail-meta-grid">
              <span>情绪：{{ emotionLabel(selectedDreamDetail.emotion) }}</span>
              <span>地点：{{ selectedDreamDetail.place || '未知地点' }}</span>
              <span>时间：{{ selectedDreamDetail.time || '-' }}</span>
              <span>ID：{{ selectedDreamDetail.id }}</span>
            </div>

            <section class="detail-section">
              <h3>梦境内容</h3>
              <p>{{ selectedDreamDetail.content || '暂无内容' }}</p>
            </section>

            <section class="detail-section ai-section">
              <h3>AI 解析</h3>
              <p>{{ selectedDreamDetail.interpretation || '暂无 AI 解析内容' }}</p>
            </section>
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* ===== Admin Professional Theme ===== */
.admin-page {
  min-height: 100vh;
  background: #f1f5f9;
  color: #1e293b;
  font-family: 'Inter', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
  display: flex;
  flex-direction: column;
}

/* ===== Top Bar ===== */
.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 3.5rem;
  padding: 0 1.5rem;
  background: #1e293b;
  color: white;
  flex-shrink: 0;
  z-index: 30;
}

.topbar-left {
  display: flex;
  align-items: center;
}

.topbar-brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.brand-icon {
  width: 2rem;
  height: 2rem;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 900;
}

.brand-name {
  font-size: 0.9rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.brand-tag {
  display: inline-block;
  margin-left: 0.4rem;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  background: rgba(99, 102, 241, 0.3);
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.topbar-divider {
  width: 1px;
  height: 1.5rem;
  background: rgba(255, 255, 255, 0.15);
  margin: 0 0.25rem;
}

.topbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  height: 2rem;
  padding: 0 0.8rem;
  border-radius: 6px;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
  font-family: inherit;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.topbar-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.topbar-btn.home {
  background: rgba(99, 102, 241, 0.25);
  color: white;
}

.topbar-btn.home:hover {
  background: rgba(99, 102, 241, 0.4);
}

.topbar-btn-icon {
  font-size: 0.9rem;
}

.topbar-btn-icon.spinning {
  display: inline-block;
  animation: spin 1s linear infinite;
}

/* ===== Layout ===== */
.admin-layout {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* ===== Sidebar ===== */
.admin-sidebar {
  width: 14rem;
  background: #1e293b;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  padding: 1rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-section {
  padding: 0.5rem 0.6rem 0.4rem;
}

.sidebar-label {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.35);
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  padding: 0.7rem 0.6rem;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  font: inherit;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.sidebar-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.85);
}

.sidebar-item.active {
  background: rgba(99, 102, 241, 0.2);
  color: white;
}

.sidebar-item-icon {
  width: 2rem;
  height: 2rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
}

.sidebar-item:not(.active) .sidebar-item-icon {
  opacity: 0.5;
}

.sidebar-item.active .sidebar-item-icon {
  background: rgba(99, 102, 241, 0.3);
}

.sidebar-item-text {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.82rem;
  font-weight: 600;
}

.sidebar-count {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
}

.sidebar-item.active .sidebar-count {
  background: rgba(99, 102, 241, 0.4);
  color: white;
}

/* ===== Content Area ===== */
.admin-content {
  flex: 1;
  min-width: 0;
  padding: 1.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ===== Toast Messages ===== */
.toast {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.7rem 1rem;
  border-radius: 10px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
  font-size: 0.85rem;
  font-weight: 600;
}

.toast.error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}

.toast-icon {
  font-size: 0.9rem;
}

/* ===== Metrics ===== */
.metrics-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.metric-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.1rem 1.25rem;
  border-radius: 12px;
  background: white;
  border: 1px solid #e2e8f0;
  transition: box-shadow 0.2s, transform 0.2s;
}

.metric-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.metric-info {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.metric-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.metric-value {
  font-size: 1.6rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
}

.metric-icon-wrap {
  width: 2.8rem;
  height: 2.8rem;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.metric-icon-wrap.indigo { background: #eef2ff; }
.metric-icon-wrap.blue { background: #eff6ff; }
.metric-icon-wrap.green { background: #ecfdf5; }
.metric-icon-wrap.amber { background: #fffbeb; }

/* ===== Content Card ===== */
.content-card {
  border-radius: 12px;
  background: white;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}

.card-title-group h2 {
  font-size: 1rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.15rem;
}

.card-subtitle {
  font-size: 0.78rem;
  color: #94a3b8;
  font-weight: 600;
}

.card-tools {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

/* ===== Search Input ===== */
.search-input {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  height: 2.25rem;
  padding: 0 0.75rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  transition: border-color 0.2s, box-shadow 0.2s;
  min-width: 14rem;
}

.search-input:focus-within {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
  background: white;
}

.search-icon {
  font-size: 0.8rem;
  opacity: 0.4;
}

.search-input input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font: inherit;
  font-size: 0.82rem;
  font-weight: 500;
  color: #1e293b;
}

.search-input input::placeholder {
  color: #94a3b8;
}

/* ===== Action Buttons ===== */
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  height: 2.25rem;
  padding: 0 0.9rem;
  border-radius: 8px;
  border: none;
  font-family: inherit;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.action-btn.primary {
  background: #6366f1;
  color: white;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.action-btn.primary:hover {
  background: #4f46e5;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
  transform: translateY(-1px);
}

.action-btn.small {
  height: 1.85rem;
  padding: 0 0.6rem;
  font-size: 0.75rem;
  font-weight: 600;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.action-btn.small:hover {
  background: #e2e8f0;
}

.action-btn.small.warning {
  background: #fffbeb;
  color: #b45309;
  border-color: #fde68a;
}

.action-btn.small.warning:hover {
  background: #fef3c7;
}

.action-btn.small.danger {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;
}

.action-btn.small.danger:hover {
  background: #fee2e2;
}

.action-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

/* ===== Table ===== */
.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

.data-table th,
.data-table td {
  padding: 0.7rem 1rem;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.82rem;
}

.data-table th:nth-child(4),
.data-table td:nth-child(4),
.data-table th:nth-child(5),
.data-table td:nth-child(5) {
  white-space: nowrap;
}

.data-table th:nth-child(4),
.data-table td:nth-child(4) {
  min-width: 6.25rem;
}

.data-table th:nth-child(5),
.data-table td:nth-child(5) {
  min-width: 4.75rem;
}

.data-table th {
  color: #64748b;
  background: #f8fafc;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid #e2e8f0;
}

.data-table tbody tr:hover {
  background: #f8fafc;
}

.id-cell {
  color: #94a3b8;
  font-weight: 600;
  font-size: 0.78rem;
}

.name-cell {
  font-weight: 700;
  color: #0f172a;
}

.email-cell {
  color: #64748b;
}

.count-cell {
  font-weight: 700;
  color: #475569;
}

.date-cell {
  color: #94a3b8;
  font-size: 0.78rem;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

/* ===== Badges ===== */
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 1.5rem;
  min-width: max-content;
  padding: 0 0.5rem;
  border-radius: 6px;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  line-height: 1;
  white-space: nowrap;
  word-break: keep-all;
}

.badge-gray { background: #f1f5f9; color: #475569; }
.badge-purple { background: #eef2ff; color: #4338ca; }
.badge-gold { background: #fef9c3; color: #92400e; border: 1px solid #fbbf24; }
.badge-green { background: #ecfdf5; color: #065f46; }
.badge-red { background: #fef2f2; color: #991b1b; }
.badge-blue { background: #eff6ff; color: #1e40af; }

/* ===== Dream List ===== */
.dream-list {
  display: flex;
  flex-direction: column;
}

.dream-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s;
}

.dream-item:last-child {
  border-bottom: none;
}

.dream-item:hover {
  background: #f8fafc;
}

.dream-item-left {
  flex: 1;
  min-width: 0;
}

.dream-item-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.3rem;
}

.dream-item-title strong {
  font-size: 0.88rem;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dream-item-preview {
  color: #64748b;
  font-size: 0.8rem;
  line-height: 1.5;
}

.dream-item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
  color: #94a3b8;
  font-size: 0.75rem;
  font-weight: 500;
  flex-shrink: 0;
}

/* ===== Card Footer / Pagination ===== */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.65rem 1.25rem;
  border-top: 1px solid #f1f5f9;
  background: #fafbfc;
}

.footer-info {
  font-size: 0.78rem;
  color: #94a3b8;
  font-weight: 600;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.size-select {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 600;
}

.size-select select {
  height: 1.8rem;
  padding: 0 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  font: inherit;
  font-size: 0.75rem;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
}

.page-btns {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.page-btn {
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: white;
  color: #475569;
  font-size: 0.75rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-btn.active {
  background: #6366f1;
  border-color: #6366f1;
  color: white;
  font-weight: 700;
}

.page-ellipsis {
  min-width: 1.8rem;
  height: 1.8rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 0.75rem;
}

/* ===== Empty / State ===== */
.empty-row {
  padding: 2rem;
  text-align: center;
  color: #94a3b8;
  font-weight: 600;
  font-size: 0.85rem;
}

.state-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 20rem;
  color: #64748b;
  font-weight: 600;
}

.state-section.error {
  color: #991b1b;
}

.state-icon {
  font-size: 2rem;
  opacity: 0.5;
}

.state-spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid #e2e8f0;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* ===== Modals ===== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.5);
  animation: fadeIn 0.2s ease;
}

/* Confirm Modal */
.confirm-modal {
  width: min(26rem, 100%);
  border-radius: 16px;
  background: white;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.3);
  padding: 2rem;
  text-align: center;
  animation: scaleIn 0.25s ease;
}

.confirm-icon-wrap {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.confirm-modal.warning .confirm-icon-wrap {
  background: #fffbeb;
  border: 2px solid #f59e0b;
}

.confirm-modal.danger .confirm-icon-wrap {
  background: #fef2f2;
  border: 2px solid #ef4444;
}

.confirm-icon {
  font-size: 1.6rem;
  font-weight: 900;
  line-height: 1;
}

.confirm-modal.warning .confirm-icon { color: #d97706; }
.confirm-modal.danger .confirm-icon { color: #dc2626; }

.confirm-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.5rem;
}

.confirm-message {
  color: #64748b;
  font-size: 0.88rem;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.confirm-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.ghost-btn {
  height: 2.4rem;
  padding: 0 1.5rem;
  border-radius: 8px;
  border: 1.5px solid #e2e8f0;
  background: white;
  color: #64748b;
  font-family: inherit;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}
.ghost-btn:hover {
  border-color: #cbd5e1;
  color: #334155;
  background: #f8fafc;
}

.confirm-btn {
  height: 2.4rem;
  padding: 0 1.5rem;
  border-radius: 8px;
  border: none;
  font-family: inherit;
  font-weight: 700;
  font-size: 0.88rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.confirm-btn.warning {
  background: #f59e0b;
  color: white;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35);
}

.confirm-btn.warning:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(245, 158, 11, 0.45);
}

.confirm-btn.danger {
  background: #ef4444;
  color: white;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.35);
}

.confirm-btn.danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(239, 68, 68, 0.45);
}

/* User Modal */
.user-modal {
  width: min(30rem, 100%);
  border-radius: 16px;
  background: white;
  box-shadow: 0 32px 80px rgba(15, 23, 42, 0.3);
  overflow: hidden;
  animation: scaleIn 0.3s ease;
}

.modal-hero {
  position: relative;
  padding: 1.75rem 2rem 1.25rem;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  color: white;
  text-align: center;
}

.modal-hero.edit {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.hero-icon {
  font-size: 2rem;
  display: block;
  margin-bottom: 0.4rem;
}

.modal-hero h2 {
  font-size: 1.15rem;
  font-weight: 800;
  margin-bottom: 0.2rem;
}

.modal-hero p {
  font-size: 0.78rem;
  opacity: 0.65;
}

.hero-close {
  position: absolute;
  top: 0.65rem;
  right: 0.65rem;
  width: 1.8rem;
  height: 1.8rem;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 1.1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.hero-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.user-form {
  padding: 1.25rem 1.75rem 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.label-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #6366f1;
  flex-shrink: 0;
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 0.75rem;
  background: #f8fafb;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-wrap:focus-within {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
  background: white;
}

.input-icon {
  font-size: 0.85rem;
  flex-shrink: 0;
  opacity: 0.5;
}

.input-wrap input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  min-height: 2.4rem;
  color: #1e293b;
  font: inherit;
  font-weight: 600;
  font-size: 0.85rem;
}

.input-wrap input::placeholder {
  color: #94a3b8;
  font-weight: 500;
}

.select-wrap select {
  width: 100%;
  height: 2.4rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafb;
  color: #1e293b;
  font: inherit;
  font-weight: 600;
  font-size: 0.85rem;
  padding: 0 0.75rem;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
}

.select-wrap select:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
  background-color: white;
}

.form-group-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.form-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.8rem;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
  font-size: 0.8rem;
  font-weight: 600;
}

.error-icon {
  width: 1.2rem;
  height: 1.2rem;
  border-radius: 50%;
  background: #ef4444;
  color: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  font-weight: 900;
  flex-shrink: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  padding-top: 0.4rem;
}

.cancel-btn {
  height: 2.4rem;
  padding: 0 1.2rem;
  border-radius: 8px;
  border: 1.5px solid #e2e8f0;
  background: white;
  color: #64748b;
  font-family: inherit;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  border-color: #cbd5e1;
  color: #334155;
  background: #f8fafc;
}

.save-btn {
  height: 2.4rem;
  padding: 0 1.3rem;
  border-radius: 8px;
  border: none;
  background: #6366f1;
  color: white;
  font-family: inherit;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.save-btn.edit {
  background: #0ea5e9;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.4);
}

.save-btn.edit:hover:not(:disabled) {
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.4);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 0.9rem;
  font-weight: 900;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* Dream Detail Modal */
.dream-modal {
  width: min(48rem, 100%);
  max-height: min(80vh, 44rem);
  border-radius: 16px;
  background: white;
  box-shadow: 0 32px 80px rgba(15, 23, 42, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}

.modal-header h2 {
  font-size: 1rem;
  font-weight: 800;
  color: #0f172a;
}

.modal-subtitle {
  margin-top: 0.1rem;
  color: #94a3b8;
  font-size: 0.75rem;
  font-weight: 600;
}

.icon-btn {
  width: 1.8rem;
  height: 1.8rem;
  border: none;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 1.1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: #e2e8f0;
}

.dream-detail-body {
  padding: 1.25rem;
  overflow: auto;
  display: grid;
  gap: 1rem;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
  padding: 0.75rem;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
  font-size: 0.78rem;
  font-weight: 600;
}

.detail-section {
  display: grid;
  gap: 0.4rem;
}

.detail-section h3 {
  font-size: 0.85rem;
  font-weight: 700;
  color: #0f172a;
}

.detail-section p {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #334155;
  font-size: 0.85rem;
}

.ai-section {
  padding: 0.85rem;
  border-radius: 8px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
}

.detail-state {
  padding: 2rem;
  text-align: center;
  color: #94a3b8;
  font-weight: 600;
}

.detail-state.error {
  color: #991b1b;
}

/* ===== Animations ===== */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .admin-sidebar {
    width: 12rem;
  }

  .metrics-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .search-input {
    min-width: 10rem;
  }

  .data-table {
    min-width: 800px;
  }
}

@media (max-width: 768px) {
  .admin-layout {
    flex-direction: column;
  }

  .admin-sidebar {
    width: 100%;
    flex-direction: row;
    padding: 0.5rem;
    overflow-x: auto;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  .sidebar-section {
    display: none;
  }

  .sidebar-item {
    padding: 0.5rem 0.75rem;
    white-space: nowrap;
  }

  .sidebar-item-icon {
    width: 1.5rem;
    height: 1.5rem;
    font-size: 0.85rem;
  }

  .admin-content {
    padding: 1rem;
  }

  .metrics-row {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .card-tools {
    flex-direction: column;
  }

  .search-input {
    min-width: 0;
  }

  .dream-item {
    flex-direction: column;
  }

  .dream-item-right {
    align-items: flex-start;
    flex-direction: row;
    gap: 0.75rem;
  }

  .card-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .footer-actions {
    justify-content: space-between;
  }

  .form-group-row {
    grid-template-columns: 1fr;
  }

  .modal-actions {
    flex-direction: column;
  }

  .modal-actions .cancel-btn,
  .modal-actions .save-btn {
    flex: 1;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .topbar-brand .brand-name {
    display: none;
  }

  .topbar-btn span:not(.topbar-btn-icon) {
    display: none;
  }

  .data-table {
    min-width: 700px;
  }
}

/* 深夜模式 */
html.dark .admin-page {
  background: #0E0E14;
  color: #E0E0E8;
}
html.dark .admin-topbar {
  background: #161620;
}
html.dark .topbar-divider { background: rgba(155, 143, 255, 0.12); }
html.dark .topbar-btn {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.75);
}
html.dark .topbar-btn:hover { background: rgba(255, 255, 255, 0.12); }
html.dark .topbar-btn.home { background: rgba(124, 111, 224, 0.2); }
html.dark .topbar-btn.home:hover { background: rgba(124, 111, 224, 0.35); }
html.dark .admin-sidebar {
  background: #161620;
  border-color: rgba(255, 255, 255, 0.06);
}
html.dark .sidebar-label { color: rgba(255, 255, 255, 0.3); }
html.dark .sidebar-item { color: rgba(255, 255, 255, 0.55); }
html.dark .sidebar-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.8);
}
html.dark .sidebar-item.active { background: rgba(124, 111, 224, 0.2); }
html.dark .sidebar-item.active .sidebar-item-text { color: #B8AEFF; }
html.dark .sidebar-item.active .sidebar-item-icon { background: rgba(124, 111, 224, 0.35); }
html.dark .sidebar-count { background: rgba(255, 255, 255, 0.08); }
html.dark .sidebar-item.active .sidebar-count { background: rgba(124, 111, 224, 0.4); }
html.dark .toast {
  background: #0D3320;
  border-color: #155E3C;
  color: #81C784;
}
html.dark .toast.error {
  background: #3B1212;
  border-color: #6B2020;
  color: #FF6B6B;
}
html.dark .metric-card {
  background: #1A1A26;
  border-color: #32324A;
}
html.dark .metric-card:hover { box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3); }
html.dark .metric-label { color: #8B8BA0; }
html.dark .metric-value { color: #E8E4F0; }
html.dark .metric-icon-wrap.indigo { background: rgba(124, 111, 224, 0.15); }
html.dark .metric-icon-wrap.blue { background: rgba(96, 165, 250, 0.15); }
html.dark .metric-icon-wrap.green { background: rgba(129, 199, 132, 0.15); }
html.dark .metric-icon-wrap.amber { background: rgba(255, 183, 77, 0.15); }
html.dark .content-card {
  background: #1A1A26;
  border-color: #32324A;
}
html.dark .card-header { border-color: #32324A; }
html.dark .card-title-group h2 { color: #E8E4F0; }
html.dark .card-subtitle { color: #8B8BA0; }
html.dark .search-input {
  border-color: #32324A;
  background: #13131B;
  color: #E0E0E8;
}
html.dark .search-input:focus-within {
  border-color: #7C73E8;
  box-shadow: 0 0 0 3px rgba(124, 115, 232, 0.12);
  background: #1A1A26;
}
html.dark .search-input input::placeholder { color: #6B6B80; }
html.dark .action-btn.primary { box-shadow: 0 2px 8px rgba(124, 111, 224, 0.25); }
html.dark .action-btn.primary:hover { box-shadow: 0 4px 12px rgba(124, 111, 224, 0.35); }
html.dark .action-btn.small {
  background: #32324A;
  color: #A9A3C0;
  border-color: #3A3A52;
}
html.dark .action-btn.small:hover { background: #3A3A52; }
html.dark .action-btn.small.warning {
  background: rgba(255, 183, 77, 0.12);
  color: #FFB347;
  border-color: rgba(255, 183, 77, 0.25);
}
html.dark .action-btn.small.warning:hover { background: rgba(255, 183, 77, 0.2); }
html.dark .action-btn.small.danger {
  background: rgba(255, 82, 82, 0.12);
  color: #FF6B6B;
  border-color: rgba(255, 82, 82, 0.25);
}
html.dark .action-btn.small.danger:hover { background: rgba(255, 82, 82, 0.2); }
html.dark .data-table { border-color: #32324A; }
html.dark .data-table th {
  color: #8B8BA0;
  background: #13131B;
  border-color: #32324A;
}
html.dark .data-table tr:hover { background: rgba(155, 143, 255, 0.04); }
html.dark .data-table td { border-color: #32324A; }
html.dark .id-cell { color: #6B6B80; }
html.dark .name-cell { color: #E8E4F0; }
html.dark .email-cell { color: #8B8BA0; }
html.dark .count-cell { color: #A9A3C0; }
html.dark .date-cell { color: #6B6B80; }
html.dark .badge-gray { background: #32324A; color: #A9A3C0; }
html.dark .badge-purple { background: rgba(124, 111, 224, 0.2); color: #B8AEFF; }
html.dark .badge-gold {
  background: rgba(255, 183, 77, 0.15);
  color: #FFB347;
  border-color: rgba(255, 183, 77, 0.3);
}
html.dark .badge-green { background: rgba(129, 199, 132, 0.15); color: #81C784; }
html.dark .badge-red { background: rgba(255, 82, 82, 0.15); color: #FF6B6B; }
html.dark .badge-blue { background: rgba(96, 165, 250, 0.15); color: #93C5FD; }
html.dark .dream-item { border-color: #32324A; }
html.dark .dream-item:hover { background: rgba(155, 143, 255, 0.04); }
html.dark .dream-item-title strong { color: #E8E4F0; }
html.dark .dream-item-preview { color: #8B8BA0; }
html.dark .dream-item-right { color: #6B6B80; }
html.dark .card-footer {
  border-color: #32324A;
  background: #13131B;
}
html.dark .footer-info { color: #6B6B80; }
html.dark .size-select {
  border-color: #32324A;
  background: #1A1A26;
  color: #A9A3C0;
}
html.dark .page-btn {
  border-color: #32324A;
  background: #1A1A26;
  color: #A9A3C0;
}
html.dark .page-btn:hover {
  background: #32324A;
  border-color: #3A3A52;
}
html.dark .page-btn.active {
  background: #7C73E8;
  border-color: #7C73E8;
  color: white;
}
html.dark .page-ellipsis { color: #6B6B80; }
html.dark .empty-row { color: #6B6B80; }
html.dark .state-section { color: #8B8BA0; }
html.dark .state-section.error { color: #FF6B6B; }
html.dark .state-spinner {
  border-color: #32324A;
  border-top-color: #7C73E8;
}
html.dark .modal-mask { background: rgba(0, 0, 0, 0.6); }
html.dark .confirm-modal {
  background: #1A1A26;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}
html.dark .confirm-icon-wrap.warning {
  background: rgba(255, 183, 77, 0.12);
  border-color: rgba(255, 183, 77, 0.3);
}
html.dark .confirm-icon-wrap.danger {
  background: rgba(255, 82, 82, 0.12);
  border-color: rgba(255, 82, 82, 0.3);
}
html.dark .confirm-icon.warning { color: #FFB347; }
html.dark .confirm-icon.danger { color: #FF6B6B; }
html.dark .confirm-title { color: #E8E4F0; }
html.dark .confirm-message { color: #A9A3C0; }
html.dark .ghost-btn {
  background: #1A1A26;
  color: #A9A3C0;
  border-color: #32324A;
}
html.dark .ghost-btn:hover {
  border-color: #3A3A52;
  color: #E0E0E8;
  background: #32324A;
}
html.dark .warning-confirm { box-shadow: 0 4px 15px rgba(255, 183, 77, 0.25); }
html.dark .danger-confirm { box-shadow: 0 4px 15px rgba(255, 82, 82, 0.25); }
html.dark .user-modal {
  background: #1A1A26;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}
html.dark .modal-hero { background: linear-gradient(135deg, #161620, #2A2A3A); }
html.dark .modal-hero.edit { background: linear-gradient(135deg, #0E0E14, #161620); }
html.dark .form-label { color: #A9A3C0; }
html.dark .input-wrap {
  border-color: #32324A;
  background: #13131B;
}
html.dark .input-wrap:focus-within {
  border-color: #7C73E8;
  box-shadow: 0 0 0 3px rgba(124, 115, 232, 0.12);
  background: #1A1A26;
}
html.dark .input-wrap input,
html.dark .input-wrap select {
  color: #E8E4F0;
}
html.dark .input-wrap input::placeholder { color: #6B6B80; }
html.dark .input-wrap select {
  background: #13131B;
  color: #E8E4F0;
}
html.dark .form-error {
  background: rgba(255, 82, 82, 0.12);
  border-color: rgba(255, 82, 82, 0.25);
  color: #FF6B6B;
}
html.dark .cancel-btn {
  border-color: #32324A;
  background: #1A1A26;
  color: #A9A3C0;
}
html.dark .cancel-btn:hover { background: #32324A; }
html.dark .save-btn { box-shadow: 0 4px 15px rgba(124, 111, 224, 0.25); }
html.dark .save-btn:hover { box-shadow: 0 8px 20px rgba(124, 111, 224, 0.35); }
html.dark .save-btn.edit { box-shadow: 0 4px 15px rgba(14, 165, 233, 0.25); }
html.dark .save-btn.edit:hover { box-shadow: 0 8px 20px rgba(14, 165, 233, 0.35); }
html.dark .btn-spinner {
  border-color: rgba(255, 255, 255, 0.2);
  border-top-color: white;
}
html.dark .dream-modal {
  background: #1A1A26;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}
html.dark .modal-header { border-color: #32324A; }
html.dark .modal-header h2 { color: #E8E4F0; }
html.dark .modal-subtitle { color: #8B8BA0; }
html.dark .icon-btn {
  background: #32324A;
  color: #A9A3C0;
}
html.dark .icon-btn:hover { background: #3A3A52; }
html.dark .detail-meta-grid {
  background: #13131B;
  color: #A9A3C0;
}
html.dark .detail-section h3 { color: #E8E4F0; }
html.dark .detail-section p { color: #A9A3C0; }
html.dark .ai-section {
  background: rgba(96, 165, 250, 0.08);
  border-color: rgba(96, 165, 250, 0.2);
}
html.dark .detail-state { color: #6B6B80; }
html.dark .detail-state.error { color: #FF6B6B; }
</style>
