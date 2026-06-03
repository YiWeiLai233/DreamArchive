import api from './axios'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface AdminUserSummary {
  id: number
  username: string
  email: string
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN'
  status: 'ACTIVE' | 'BANNED'
  deleted: boolean
  createdAt: string
  dreamCount: number
}

export interface AdminDreamSummary {
  id: string
  userId: number
  username: string
  title: string
  content: string
  emotion: string
  place: string
  time: string
  createdAt: string
}

export interface AdminDreamDetail {
  id: string
  userId: number
  title: string
  content: string
  emotion: string
  place: string
  time: string
  interpretation: string
  createdAt: string
}

export interface AdminOverview {
  totalUsers: number
  adminUsers: number
  totalDreams: number
  todayDreams: number
  users: AdminUserSummary[]
  recentDreams: AdminDreamSummary[]
  userPage: number
  userPageSize: number
  userResultTotal: number
  userTotalPages: number
  dreamPage: number
  dreamPageSize: number
  dreamResultTotal: number
  dreamTotalPages: number
}

export interface AdminOverviewRequest {
  userPage: number
  userPageSize: number
  userKeyword?: string
  dreamPage: number
  dreamPageSize: number
  dreamKeyword?: string
}

export interface AdminUserActionRequest {
  action: 'CREATE' | 'UPDATE' | 'BAN' | 'UNBAN'
  id?: number
  username?: string
  password?: string
  email?: string
  role?: 'USER' | 'ADMIN' | 'SUPER_ADMIN'
  status?: 'ACTIVE' | 'BANNED'
}

export function getAdminOverview(payload: AdminOverviewRequest) {
  return api.post<ApiResult<AdminOverview>>('/api/admin/overview', payload)
}

export function runAdminUserAction(payload: AdminUserActionRequest) {
  return api.post<ApiResult<AdminOverview>>('/api/admin/user-action', payload)
}

export function deleteAdminUser(id: number) {
  return api.post<ApiResult<AdminOverview>>('/api/admin/delete-user', { id })
}

export function getAdminDreamDetail(id: string) {
  return api.post<ApiResult<AdminDreamDetail>>('/api/admin/dream-detail', { id })
}
