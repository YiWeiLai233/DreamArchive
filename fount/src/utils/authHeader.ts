import { AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'

export function attachAuthHeader(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const headers = AxiosHeaders.from(config.headers)
  const token = localStorage.getItem('authToken')

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  } else {
    headers.delete('Authorization')
  }

  config.headers = headers
  return config
}
