import api from './axios'

export interface DreamForm {
  userId: number
  title?: string
  content: string
  emotion: string
  place?: string
  time?: string
  analyze?: boolean
  interpretation?: string
}

export interface DreamContent {
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

export interface DreamDetail {
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

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface DreamAnalysisResult {
  interpretation: string
}

export function saveDream(form: DreamForm) {
  return api.post<ApiResult<DreamContent>>('/api/analysisDream', { ...form, analyze: false })
}

export function saveAndAnalyzeDream(form: DreamForm) {
  return api.post<ApiResult<DreamContent>>('/api/dreams/save-and-analyze', form)
}

export function analyzeDreamContent(content: string) {
  return api.post<ApiResult<DreamAnalysisResult>>('/api/dream/analyze', { content })
}

export function guestAnalyzeDream(content: string, deviceId: string) {
  return api.post<ApiResult<DreamAnalysisResult>>('/api/guest/analyze', { content, deviceId }, { timeout: 120000 })
}

export function getDreamById(id: string) {
  return api.get<ApiResult<DreamContent>>(`/api/dream/${id}`)
}

export function getUserDreams(userId: number) {
  return api.get<ApiResult<DreamDetail[]>>(`/api/dreams/user/${userId}`)
}

export function deleteDream(id: string) {
  return api.post<ApiResult<void>>(`/api/dream/${id}/delete`)
}

export function triggerAnalyze(id: string) {
  return api.post<ApiResult<void>>(`/api/dream/${id}/analyze`)
}
