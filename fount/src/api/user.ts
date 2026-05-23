import api from './axios'

export interface RegisterForm {
  username: string
  password: string
  email: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  email: string
  role?: 'USER' | 'ADMIN'
  status?: 'ACTIVE' | 'BANNED'
  createdAt?: string
  token?: string
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface DreamStats {
  userId: number
  totalDreams: number
  emotionDistribution: { label: string; value: number }[]
  placeDistribution: { label: string; value: number }[]
  recentTrend: { date: string; count: number }[]
}

export function sendRegisterCode(email: string) {
  return api.post<ApiResult<string>>('/api/register/send-code', { email })
}

export function register(form: RegisterForm, code?: string) {
  const params = code ? `?code=${encodeURIComponent(code)}` : ''
  return api.post<ApiResult<UserInfo>>(`/api/register${params}`, form)
}

export function sendChangePasswordCode(email: string) {
  return api.post<ApiResult<string>>('/api/change-password/send-code', { email })
}

export function sendResetPasswordCode(identifier: string) {
  return api.post<ApiResult<string>>('/api/reset-password/send-code', { identifier })
}

export function resetPassword(identifier: string, code: string, newPassword: string) {
  return api.post<ApiResult<string>>('/api/reset-password', { identifier, code, newPassword })
}

export function setupAccount(username: string, password: string) {
  return api.post<ApiResult<string>>('/api/account/setup', { username, password })
}

export function login(form: LoginForm) {
  return api.post<ApiResult<UserInfo>>('/api/login', form)
}

export function sendLoginCode(email: string) {
  return api.post<ApiResult<string>>('/api/login/send-code', { email })
}

export function loginByCode(email: string, code: string) {
  return api.post<ApiResult<UserInfo>>('/api/login/code', { email, code })
}

// 梦境统计相关 API
export function getDreamStats(userId: string | number) {
  return api.get<ApiResult<DreamStats>>(`/api/stats/${userId}`)
}

export function getDreamTotal(userId: string | number) {
  return api.get<ApiResult<number>>(`/api/stats/${userId}/total`)
}

export function getEmotionDistribution(userId: string | number) {
  return api.get<ApiResult<{ label: string; value: number }[]>>(`/api/stats/${userId}/emotion`)
}

export function getPlaceDistribution(userId: string | number) {
  return api.get<ApiResult<{ label: string; value: number }[]>>(`/api/stats/${userId}/place`)
}

export function getRecentTrend(userId: string | number, days: number = 7) {
  return api.get<ApiResult<{ date: string; count: number }[]>>(`/api/stats/${userId}/trend?days=${days}`)
}

export function getStreak(userId: string | number) {
  return api.get<ApiResult<number>>(`/api/stats/${userId}/streak`)
}

export function getLongestDream(userId: string | number) {
  return api.get<ApiResult<number>>(`/api/stats/${userId}/longest`)
}

// 通过邮箱获取用户信息
export function getUserByEmail(email: string) {
  return api.get<ApiResult<UserInfo>>(`/api/user/by-email?email=${encodeURIComponent(email)}`)
}
