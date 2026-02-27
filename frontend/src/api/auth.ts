import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api, { extractApiError } from '@/lib/api'
import { useAuthStore } from '@/stores/authStore'
import { useUIStore } from '@/stores/uiStore'
import type { LoginRequest, LoginResponse, RegisterRequest, User } from '@/types'

export function useLogin() {
  const login = useAuthStore((s) => s.login)
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: LoginRequest) => {
      const response = await api.post<LoginResponse>('/auth/login', data)
      return response.data
    },
    onSuccess: (data) => {
      login(data.user, data.token)
      addNotification({ type: 'success', message: 'Login successful' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useRegister() {
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: RegisterRequest) => {
      const response = await api.post<User>('/auth/register', data)
      return response.data
    },
    onSuccess: () => {
      addNotification({ type: 'success', message: 'Registration successful. Please log in.' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useCurrentUser() {
  const setUser = useAuthStore((s) => s.setUser)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: async () => {
      const response = await api.get<User>('/auth/me')
      setUser(response.data)
      return response.data
    },
    enabled: isAuthenticated,
  })
}

export function useUsers() {
  return useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      const response = await api.get<User[]>('/users')
      return response.data
    },
  })
}

export function useUpdateUserRole() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async ({ userId, role }: { userId: string; role: string }) => {
      const response = await api.patch<User>(`/users/${userId}/role`, { role })
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      addNotification({ type: 'success', message: 'User role updated' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}
