import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('auth_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

api.interceptors.response.use(
  (response) => {
    return response
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export interface ApiError {
  message: string
  code: string
  status: number
}

export function extractApiError(error: unknown): ApiError {
  if (error instanceof AxiosError && error.response?.data) {
    const data = error.response.data as Record<string, unknown>
    return {
      message: (data.message as string) || (data.error as string) || 'An unexpected error occurred',
      code: (data.code as string) || 'UNKNOWN_ERROR',
      status: error.response.status,
    }
  }
  if (error instanceof Error) {
    return {
      message: error.message,
      code: 'CLIENT_ERROR',
      status: 0,
    }
  }
  return {
    message: 'An unexpected error occurred',
    code: 'UNKNOWN_ERROR',
    status: 0,
  }
}

export default api
