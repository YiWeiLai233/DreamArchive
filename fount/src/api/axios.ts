import axios from 'axios'
import { onAxiosError } from '@/utils/errorHandler'
import { attachAuthHeader } from '@/utils/authHeader'

const api = axios.create({
  baseURL: '',
  timeout: 10000
})

api.interceptors.request.use(attachAuthHeader)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    onAxiosError(error)
    return Promise.reject(error)
  }
)

export default api
