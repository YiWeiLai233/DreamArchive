import router from '@/router'

/**
 * 处理 API 错误并跳转到对应错误页面
 * @param status HTTP 状态码
 * @param message 错误信息（可选）
 */
export function handleApiError(status: number, message?: string) {
  const errorPages: Record<number, string> = {
    400: '请求参数错误',
    401: '未登录或登录已过期',
    403: '没有访问权限',
    404: '请求的资源不存在',
    500: '服务器内部错误',
    502: '服务暂时不可用',
    503: '服务维护中'
  }

  const errorMsg = message || errorPages[status] || '未知错误'
  console.error(`API Error ${status}: ${errorMsg}`)

  if (status === 401) {
    localStorage.removeItem('isLoggedIn')
    localStorage.removeItem('authToken')
    router.push('/login')
    return
  }

  if (status === 403) {
    router.push('/error/403')
    return
  }

  router.push({
    name: 'error',
    params: { code: String(status) }
  })
}

/**
 * Axios 响应拦截器错误处理
 * 可在 axios 实例中使用
 */
export function onAxiosError(error: any) {
  if (error.response) {
    // 服务器返回了错误状态码
    handleApiError(error.response.status, error.response.data?.message)
  } else if (error.request) {
    // 请求已发送但没有收到响应
    handleApiError(502, '无法连接到服务器')
  } else {
    // 请求配置出错
    console.error('请求配置错误:', error.message)
  }
}
