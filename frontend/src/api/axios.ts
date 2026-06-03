import axios from 'axios'
import { onAxiosError } from '@/utils/errorHandler'

const api = axios.create({
  baseURL: '',
  timeout: 10000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    onAxiosError(error)
    return Promise.reject(error)
  }
)

export default api
