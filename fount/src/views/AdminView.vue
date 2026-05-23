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
const editingUser = ref<AdminUserSummary | null>(null)
const userForm = ref<{
  id?: number
  username: string
  email: string
  password: string
  role: 'USER' | 'ADMIN'
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
  const ok = window.confirm(isBanned ? `确认解封账号 ${user.username}？` : `确认封禁账号 ${user.username}？`)
  if (!ok) return
  await performUserAction({ action, id: user.id }, isBanned ? '账号已解封' : '账号已封禁')
}

async function deleteUser(user: AdminUserSummary) {
  resetActionMessage()
  if (isCurrentUser(user)) {
    actionError.value = '不能删除自己的账号'
    return
  }

  const ok = window.confirm(`确认删除账号 ${user.username}？数据库记录会保留软删除标记。`)
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
    <header class="admin-header">
      <div>
        <p class="eyebrow">DreamArchive Admin</p>
        <h1>管理员控制台</h1>
      </div>
      <div class="header-actions">
        <button class="ghost-btn" type="button" @click="goHome">返回首页</button>
        <button class="primary-btn" type="button" :disabled="isLoading" @click="loadOverview">
          {{ isLoading ? '刷新中' : '刷新' }}
        </button>
      </div>
    </header>

    <main class="admin-main">
      <section v-if="isLoading" class="state-section">
        <div class="spinner"></div>
        <p>正在加载管理员数据...</p>
      </section>

      <section v-else-if="errorMsg" class="state-section error-section">
        <strong>{{ errorMsg }}</strong>
        <button class="primary-btn" type="button" @click="loadOverview">重试</button>
      </section>

      <template v-else-if="overview">
        <div v-if="actionMsg || actionError" class="action-message" :class="{ error: actionError }">
          {{ actionError || actionMsg }}
        </div>

        <section class="metrics-grid" aria-label="关键指标">
          <article class="metric-card">
            <span class="metric-label">用户总数</span>
            <strong>{{ overview.totalUsers }}</strong>
          </article>
          <article class="metric-card accent-blue">
            <span class="metric-label">梦境总数</span>
            <strong>{{ overview.totalDreams }}</strong>
          </article>
          <article class="metric-card accent-green">
            <span class="metric-label">今日新增梦境</span>
            <strong>{{ overview.todayDreams }}</strong>
          </article>
          <article class="metric-card accent-amber">
            <span class="metric-label">管理员账号</span>
            <strong>{{ overview.adminUsers }}</strong>
          </article>
        </section>

        <div class="admin-workspace">
          <aside class="admin-sidebar" aria-label="管理员模块">
            <button
              class="side-tab"
              :class="{ active: activePanel === 'users' }"
              type="button"
              @click="switchPanel('users')"
            >
              <span>用户管理</span>
              <strong>{{ userResultTotal }}</strong>
            </button>
            <button
              class="side-tab"
              :class="{ active: activePanel === 'dreams' }"
              type="button"
              @click="switchPanel('dreams')"
            >
              <span>最近梦境</span>
              <strong>{{ dreamResultTotal }}</strong>
            </button>
          </aside>

          <section v-if="activePanel === 'users'" class="panel-section">
            <div class="section-heading">
              <div>
                <h2>用户管理</h2>
                <p>
                  第 {{ pageStart(userResultTotal, userPage, userPageSize) }}-{{ pageEnd(userResultTotal, userPage, userPageSize) }}
                  条 / 共 {{ userResultTotal }} 个账号
                </p>
              </div>
              <div class="user-tools">
                <label class="search-box">
                  <span>搜索</span>
                  <input v-model="userSearch" type="search" placeholder="用户名、邮箱、角色、状态" />
                </label>
                <button class="primary-btn" type="button" @click="openCreateUser">新增账号</button>
              </div>
            </div>

            <div class="table-wrap">
              <table>
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
                    <td>#{{ user.id }}</td>
                    <td class="strong-cell">{{ user.username }}</td>
                    <td>{{ user.email }}</td>
                    <td>
                      <span class="role-badge" :class="{ admin: user.role === 'ADMIN' }">
                        {{ user.role === 'ADMIN' ? '管理员' : '用户' }}
                      </span>
                    </td>
                    <td>
                      <span class="status-badge" :class="{ banned: user.status === 'BANNED' }">
                        {{ statusLabel(user.status) }}
                      </span>
                    </td>
                    <td>{{ user.dreamCount }}</td>
                    <td>{{ formatDate(user.createdAt) }}</td>
                    <td class="actions-cell">
                      <button class="mini-btn" type="button" :disabled="isSubmitting" @click="openEditUser(user)">
                        编辑
                      </button>
                      <button
                        class="mini-btn warning"
                        type="button"
                        :disabled="isSubmitting || isCurrentUser(user)"
                        @click="toggleUserBan(user)"
                      >
                        {{ user.status === 'BANNED' ? '解封' : '封禁' }}
                      </button>
                      <button
                        class="mini-btn danger"
                        type="button"
                        :disabled="isSubmitting || isCurrentUser(user)"
                        @click="deleteUser(user)"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                  <tr v-if="filteredUsers.length === 0">
                    <td colspan="8" class="empty-cell">没有匹配的用户</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="pagination-bar">
              <span>第 {{ userPage }} / {{ userTotalPages }} 页</span>
              <div class="pager-actions">
                <label class="page-size-box">
                  <span>每页</span>
                  <select v-model.number="userPageSize" :disabled="isLoading" @change="changeUserPageSize">
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                    <option :value="50">50</option>
                  </select>
                </label>
                <button class="mini-btn" type="button" :disabled="userPage <= 1 || isLoading" @click="changeUserPage(1)">首页</button>
                <button class="mini-btn" type="button" :disabled="userPage <= 1 || isLoading" @click="changeUserPage(userPage - 1)">上一页</button>
                <button class="mini-btn" type="button" :disabled="userPage >= userTotalPages || isLoading" @click="changeUserPage(userPage + 1)">下一页</button>
                <button class="mini-btn" type="button" :disabled="userPage >= userTotalPages || isLoading" @click="changeUserPage(userTotalPages)">末页</button>
              </div>
            </div>
          </section>

          <section v-else class="panel-section">
            <div class="section-heading">
              <div>
                <h2>最近梦境</h2>
                <p>
                  第 {{ pageStart(dreamResultTotal, dreamPage, dreamPageSize) }}-{{ pageEnd(dreamResultTotal, dreamPage, dreamPageSize) }}
                  条 / 共 {{ dreamResultTotal }} 条记录
                </p>
              </div>
              <div class="user-tools">
                <label class="search-box">
                  <span>搜索</span>
                  <input v-model="dreamSearch" type="search" placeholder="标题、内容、用户、地点" />
                </label>
              </div>
            </div>

            <div class="dream-list">
              <article
                v-for="dream in filteredDreams"
                :key="dream.id"
                class="dream-row dream-row-button"
                role="button"
                tabindex="0"
                @click="openDreamDetail(dream)"
                @keydown.enter="openDreamDetail(dream)"
              >
                <div class="dream-main">
                  <div class="dream-title-row">
                    <strong>{{ dream.title || '未命名梦境' }}</strong>
                    <span>{{ emotionLabel(dream.emotion) }}</span>
                  </div>
                  <p>{{ preview(dream.content) }}</p>
                </div>
                <div class="dream-meta">
                  <span>{{ dream.username || `用户 ${dream.userId}` }}</span>
                  <span>{{ dream.place || '未知地点' }}</span>
                  <span>{{ formatDate(dream.createdAt) }}</span>
                </div>
              </article>
              <div v-if="filteredDreams.length === 0" class="empty-card">没有匹配的梦境记录</div>
            </div>

            <div class="pagination-bar">
              <span>第 {{ dreamPage }} / {{ dreamTotalPages }} 页</span>
              <div class="pager-actions">
                <label class="page-size-box">
                  <span>每页</span>
                  <select v-model.number="dreamPageSize" :disabled="isLoading" @change="changeDreamPageSize">
                    <option :value="5">5</option>
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                  </select>
                </label>
                <button class="mini-btn" type="button" :disabled="dreamPage <= 1 || isLoading" @click="changeDreamPage(1)">首页</button>
                <button class="mini-btn" type="button" :disabled="dreamPage <= 1 || isLoading" @click="changeDreamPage(dreamPage - 1)">上一页</button>
                <button class="mini-btn" type="button" :disabled="dreamPage >= dreamTotalPages || isLoading" @click="changeDreamPage(dreamPage + 1)">下一页</button>
                <button class="mini-btn" type="button" :disabled="dreamPage >= dreamTotalPages || isLoading" @click="changeDreamPage(dreamTotalPages)">末页</button>
              </div>
            </div>
          </section>
        </div>
      </template>
    </main>

    <div v-if="isUserModalOpen" class="modal-mask" @click.self="closeUserModal">
      <section class="user-modal" aria-label="账号表单">
        <header class="modal-header">
          <h2>{{ editingUser ? '编辑账号' : '新增账号' }}</h2>
          <button class="icon-btn" type="button" :disabled="isSubmitting" @click="closeUserModal">×</button>
        </header>

        <form class="user-form" @submit.prevent="submitUserForm">
          <label>
            <span>用户名</span>
            <input v-model="userForm.username" type="text" maxlength="20" :disabled="isSubmitting" />
          </label>

          <label>
            <span>邮箱</span>
            <input v-model="userForm.email" type="email" maxlength="100" :disabled="isSubmitting" />
          </label>

          <label>
            <span>{{ editingUser ? '新密码' : '密码' }}</span>
            <input
              v-model="userForm.password"
              type="password"
              maxlength="50"
              :placeholder="editingUser ? '留空表示不修改' : '至少 6 位'"
              :disabled="isSubmitting"
            />
          </label>

          <div class="form-row">
            <label>
              <span>角色</span>
              <select v-model="userForm.role" :disabled="isSubmitting">
                <option value="USER">用户</option>
                <option value="ADMIN">管理员</option>
              </select>
            </label>
            <label>
              <span>状态</span>
              <select v-model="userForm.status" :disabled="isSubmitting">
                <option value="ACTIVE">正常</option>
                <option value="BANNED">封禁</option>
              </select>
            </label>
          </div>

          <p v-if="actionError" class="form-error">{{ actionError }}</p>

          <div class="modal-actions">
            <button class="ghost-btn" type="button" :disabled="isSubmitting" @click="closeUserModal">取消</button>
            <button class="primary-btn" type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? '保存中' : '保存' }}
            </button>
          </div>
        </form>
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
.admin-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(245, 247, 251, 0.96), rgba(235, 241, 248, 0.96)),
    linear-gradient(135deg, #eef2ff, #f0fdf4 55%, #fff7ed);
  color: #1f2937;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem 2rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  position: sticky;
  top: 0;
  z-index: 20;
}

.eyebrow {
  color: #4f46e5;
  font-size: 0.76rem;
  font-weight: 700;
  margin-bottom: 0.2rem;
  text-transform: uppercase;
}

.admin-header h1 {
  font-size: 1.45rem;
  line-height: 1.2;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.ghost-btn,
.primary-btn {
  min-height: 2.25rem;
  padding: 0 0.95rem;
  border-radius: 8px;
  border: 1px solid transparent;
  font-family: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.ghost-btn {
  background: white;
  border-color: rgba(148, 163, 184, 0.42);
  color: #334155;
}

.primary-btn {
  background: #4f46e5;
  color: white;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.18);
}

.ghost-btn:hover,
.primary-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.primary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.ghost-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-main {
  max-width: 1540px;
  margin: 0 auto;
  padding: 1.5rem 2rem 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.action-message {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  background: #ecfdf5;
  border: 1px solid rgba(22, 163, 74, 0.24);
  color: #166534;
  font-size: 0.88rem;
  font-weight: 700;
}

.action-message.error {
  background: #fef2f2;
  border-color: rgba(220, 38, 38, 0.24);
  color: #b91c1c;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.metric-card {
  min-height: 6.25rem;
  padding: 1rem;
  border-radius: 8px;
  background: white;
  border: 1px solid rgba(148, 163, 184, 0.24);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-top: 3px solid #4f46e5;
}

.metric-card strong {
  font-size: 1.75rem;
  color: #111827;
}

.metric-label {
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 700;
}

.accent-blue {
  border-top-color: #0284c7;
}

.accent-green {
  border-top-color: #16a34a;
}

.accent-amber {
  border-top-color: #d97706;
}

.admin-workspace {
  display: grid;
  grid-template-columns: 13rem minmax(0, 1fr);
  align-items: start;
  gap: 1.25rem;
}

.admin-sidebar {
  position: sticky;
  top: 6rem;
  display: grid;
  gap: 0.65rem;
  min-width: 0;
}

.side-tab {
  width: 100%;
  min-height: 4.75rem;
  padding: 0.9rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.82);
  color: #475569;
  font: inherit;
  cursor: pointer;
  display: grid;
  gap: 0.35rem;
  text-align: left;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.side-tab span {
  font-size: 0.86rem;
  font-weight: 800;
}

.side-tab strong {
  font-size: 1.45rem;
  color: #111827;
}

.side-tab.active {
  border-color: rgba(79, 70, 229, 0.36);
  background: #eef2ff;
  color: #3730a3;
  box-shadow: 0 12px 26px rgba(79, 70, 229, 0.12);
}

.panel-section {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
  overflow: hidden;
  min-width: 0;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
}

.section-heading h2 {
  font-size: 1rem;
  margin-bottom: 0.15rem;
}

.section-heading p {
  color: #64748b;
  font-size: 0.82rem;
}

.user-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.65rem;
}

.search-box {
  width: min(20rem, 100%);
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.35rem 0.55rem;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 700;
}

.search-box input {
  min-width: 0;
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #1f2937;
  font: inherit;
  font-weight: 500;
}

.page-size-box {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 1.9rem;
  padding: 0 0.55rem;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 0.76rem;
  font-weight: 700;
}

.page-size-box select {
  min-height: 1.8rem;
  border: none;
  outline: none;
  background: transparent;
  color: #1f2937;
  font: inherit;
  font-weight: 700;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 980px;
}

th,
td {
  padding: 0.82rem 1rem;
  text-align: left;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  font-size: 0.88rem;
  white-space: nowrap;
}

th {
  color: #64748b;
  background: #f8fafc;
  font-size: 0.76rem;
  text-transform: uppercase;
}

.strong-cell {
  font-weight: 800;
  color: #111827;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  min-height: 1.55rem;
  padding: 0 0.55rem;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  font-size: 0.76rem;
  font-weight: 800;
}

.role-badge.admin {
  background: #e0e7ff;
  color: #3730a3;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 1.55rem;
  padding: 0 0.55rem;
  border-radius: 999px;
  background: #dcfce7;
  color: #166534;
  font-size: 0.76rem;
  font-weight: 800;
}

.status-badge.banned {
  background: #fee2e2;
  color: #991b1b;
}

.actions-cell {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.mini-btn {
  min-height: 1.9rem;
  padding: 0 0.55rem;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.36);
  background: white;
  color: #334155;
  font: inherit;
  font-size: 0.76rem;
  font-weight: 800;
  cursor: pointer;
}

.mini-btn.warning {
  border-color: rgba(217, 119, 6, 0.28);
  color: #92400e;
  background: #fffbeb;
}

.mini-btn.danger {
  border-color: rgba(220, 38, 38, 0.26);
  color: #b91c1c;
  background: #fef2f2;
}

.mini-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1rem;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  background: #f8fafc;
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 700;
}

.pager-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.42);
}

.user-modal {
  width: min(30rem, 100%);
  border-radius: 8px;
  background: white;
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.22);
}

.dream-modal {
  width: min(52rem, 100%);
  max-height: min(82vh, 48rem);
  border-radius: 8px;
  background: white;
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
}

.modal-header h2 {
  font-size: 1rem;
}

.modal-subtitle {
  margin-top: 0.18rem;
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 700;
}

.icon-btn {
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
  font-size: 1.2rem;
  line-height: 1;
  cursor: pointer;
}

.user-form {
  padding: 1rem;
  display: grid;
  gap: 0.9rem;
}

.user-form label {
  display: grid;
  gap: 0.35rem;
  color: #475569;
  font-size: 0.78rem;
  font-weight: 800;
}

.user-form input,
.user-form select {
  width: 100%;
  min-height: 2.45rem;
  border: 1px solid rgba(148, 163, 184, 0.42);
  border-radius: 8px;
  background: #f8fafc;
  color: #111827;
  font: inherit;
  font-weight: 600;
  padding: 0 0.7rem;
  outline: none;
}

.user-form input:focus,
.user-form select:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.form-error {
  padding: 0.65rem 0.75rem;
  border-radius: 8px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 0.82rem;
  font-weight: 700;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  padding-top: 0.25rem;
}

.dream-list {
  display: flex;
  flex-direction: column;
}

.dream-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 17rem;
  gap: 1rem;
  padding: 1rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.dream-row-button {
  cursor: pointer;
  transition: background 0.2s ease;
}

.dream-row-button:hover,
.dream-row-button:focus {
  background: #f8fafc;
  outline: none;
}

.dream-row:last-child {
  border-bottom: none;
}

.dream-title-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.35rem;
}

.dream-title-row strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dream-title-row span {
  flex-shrink: 0;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  background: #ecfeff;
  color: #0e7490;
  font-size: 0.74rem;
  font-weight: 800;
}

.dream-main p {
  color: #64748b;
  font-size: 0.88rem;
  line-height: 1.6;
}

.dream-meta {
  display: grid;
  gap: 0.2rem;
  color: #475569;
  font-size: 0.82rem;
  justify-content: end;
  text-align: right;
}

.dream-detail-body {
  padding: 1rem;
  overflow: auto;
  display: grid;
  gap: 1rem;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.55rem;
  padding: 0.85rem;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 700;
}

.detail-section {
  display: grid;
  gap: 0.5rem;
}

.detail-section h3 {
  font-size: 0.92rem;
  color: #111827;
}

.detail-section p {
  white-space: pre-wrap;
  line-height: 1.75;
  color: #334155;
  font-size: 0.9rem;
}

.ai-section {
  padding: 0.85rem;
  border-radius: 8px;
  background: #eef2ff;
  border: 1px solid rgba(79, 70, 229, 0.18);
}

.detail-state {
  padding: 2rem;
  text-align: center;
  color: #64748b;
  font-weight: 700;
}

.detail-state.error {
  color: #b91c1c;
}

.state-section,
.empty-card,
.empty-cell {
  padding: 2rem;
  text-align: center;
  color: #64748b;
}

.state-section {
  min-height: 18rem;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 0.9rem;
  border-radius: 8px;
  background: white;
  border: 1px solid rgba(148, 163, 184, 0.28);
}

.error-section strong {
  color: #b91c1c;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid rgba(79, 70, 229, 0.18);
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .admin-workspace {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dream-row {
    grid-template-columns: 1fr;
  }

  .dream-meta {
    justify-content: start;
    text-align: left;
  }
}

@media (max-width: 640px) {
  .admin-header,
  .section-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .user-tools,
  .modal-actions,
  .pagination-bar,
  .pager-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-header {
    padding: 1rem;
  }

  .header-actions {
    width: 100%;
  }

  .ghost-btn,
  .primary-btn {
    flex: 1;
  }

  .admin-main {
    padding: 1rem;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .detail-meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
