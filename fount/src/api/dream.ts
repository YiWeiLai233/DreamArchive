import axios from 'axios'

export interface DreamForm {
  userId: number
  title?: string
  content: string
  emotion: string
  place?: string
  time?: string
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

// 保存梦境
export function saveDream(form: DreamForm) {
  return axios.post<ApiResult<DreamContent>>('/api/analysisDream', form)
}

// 根据 ID 获取梦境
export function getDreamById(id: string) {
  return axios.get<ApiResult<DreamContent>>(`/api/dream/${id}`)
}

// 获取用户的所有梦境
export function getUserDreams(userId: number) {
  return axios.get<ApiResult<DreamDetail[]>>(`/api/dreams/user/${userId}`)
}

// 删除梦境
export function deleteDream(id: string) {
  return axios.delete<ApiResult<void>>(`/api/dream/${id}`)
}
