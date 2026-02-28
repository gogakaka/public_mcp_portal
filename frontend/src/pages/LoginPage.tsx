import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { FormField } from '@/components/common/FormField'

const loginSchema = z.object({
  email: z.string().email('Valid email is required'),
  password: z.string().min(1, 'Password is required'),
})

type LoginForm = z.infer<typeof loginSchema>

export default function LoginPage() {
  const navigate = useNavigate()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const loginMutation = useLogin()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  })

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true })
    }
  }, [isAuthenticated, navigate])

  const onSubmit = (data: LoginForm) => {
    loginMutation.mutate(data, {
      onSuccess: () => {
        navigate('/dashboard')
      },
    })
  }

  return (
    <div className="min-h-screen bg-surface-secondary flex">
      <div className="hidden lg:flex lg:w-1/2 bg-primary-900 items-center justify-center p-12">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Universal MCP Gateway
          </h1>
          <p className="text-sm text-primary-400 mt-3 max-w-md leading-relaxed">
            Centralized management for MCP tools. Register, discover, and orchestrate
            tool integrations with enterprise-grade security and audit capabilities.
          </p>
          <div className="mt-10 space-y-4">
            <div className="border-l-2 border-l-primary-500 pl-4">
              <p className="text-xs font-medium text-primary-300">Tool Registry</p>
              <p className="text-2xs text-primary-500 mt-0.5">N8N, Cube.js, AWS services and more</p>
            </div>
            <div className="border-l-2 border-l-primary-500 pl-4">
              <p className="text-xs font-medium text-primary-300">Access Control</p>
              <p className="text-2xs text-primary-500 mt-0.5">Role-based permissions with audit trail</p>
            </div>
            <div className="border-l-2 border-l-primary-500 pl-4">
              <p className="text-xs font-medium text-primary-300">API Gateway</p>
              <p className="text-2xs text-primary-500 mt-0.5">Rate limiting, key management, monitoring</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-8">
            <h2 className="text-lg font-bold text-text">Sign In</h2>
            <p className="text-xs text-text-secondary mt-1">
              Access your UMG dashboard
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <FormField label="Email" error={errors.email?.message} required>
              <input
                {...register('email')}
                type="email"
                className="input-field w-full"
                placeholder="you@company.com"
                autoComplete="email"
              />
            </FormField>

            <FormField label="Password" error={errors.password?.message} required>
              <input
                {...register('password')}
                type="password"
                className="input-field w-full"
                placeholder="Enter your password"
                autoComplete="current-password"
              />
            </FormField>

            <button
              type="submit"
              disabled={loginMutation.isPending}
              className="btn-primary w-full disabled:opacity-50"
            >
              {loginMutation.isPending ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <p className="text-xs text-text-secondary mt-6 text-center">
            No account?{' '}
            <Link to="/register" className="text-text font-medium hover:underline">
              Register
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
