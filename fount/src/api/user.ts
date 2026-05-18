import axios from 'axios'
import { onAxiosError } from '@/utils/errorHandler'

// 创建 axios 实例并配置拦截器
const api = axios.create({
  baseURL: '',
  timeout: 10000
})

// 响应拦截器：处理非200状态码
api.interceptors.response.use(
  (response) => response,
  (error) => {
    onAxiosError(error)
    return Promise.reject(error)
  }
)

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

export function register(form: RegisterForm) {
  return api.post<ApiResult<RegisterForm>>('/api/register', form)
}

export function login(form: LoginForm) {
  return api.post<ApiResult<UserInfo>>('/api/login', form)
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
