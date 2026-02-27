import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useRegister } from '@/api/auth'
import { FormField } from '@/components/common/FormField'

const registerSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  email: z.string().email('Valid email is required'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string(),
  department: z.string().min(1, 'Department is required'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type RegisterForm = z.infer<typeof registerSchema>

export default function RegisterPage() {
  const navigate = useNavigate()
  const registerMutation = useRegister()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = (data: RegisterForm) => {
    registerMutation.mutate(
      {
        name: data.name,
        email: data.email,
        password: data.password,
        department: data.department,
      },
      {
        onSuccess: () => {
          navigate('/login')
        },
      },
    )
  }

  return (
    <div className="min-h-screen bg-surface-secondary flex">
      <div className="hidden lg:flex lg:w-1/2 bg-primary-900 items-center justify-center p-12">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Universal MCP Gateway
          </h1>
          <p className="text-sm text-primary-400 mt-3 max-w-md leading-relaxed">
            Create your account to start managing MCP tool integrations
            with centralized access control and monitoring.
          </p>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-8">
            <h2 className="text-lg font-bold text-text">Create Account</h2>
            <p className="text-xs text-text-secondary mt-1">
              Register for a UMG account
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <FormField label="Full Name" error={errors.name?.message} required>
              <input
                {...register('name')}
                type="text"
                className="input-field w-full"
                placeholder="Jane Doe"
                autoComplete="name"
              />
            </FormField>

            <FormField label="Email" error={errors.email?.message} required>
              <input
                {...register('email')}
                type="email"
                className="input-field w-full"
                placeholder="you@company.com"
                autoComplete="email"
              />
            </FormField>

            <FormField label="Department" error={errors.department?.message} required>
              <input
                {...register('department')}
                type="text"
                className="input-field w-full"
                placeholder="Engineering"
              />
            </FormField>

            <FormField label="Password" error={errors.password?.message} required>
              <input
                {...register('password')}
                type="password"
                className="input-field w-full"
                placeholder="Minimum 8 characters"
                autoComplete="new-password"
              />
            </FormField>

            <FormField label="Confirm Password" error={errors.confirmPassword?.message} required>
              <input
                {...register('confirmPassword')}
                type="password"
                className="input-field w-full"
                placeholder="Re-enter password"
                autoComplete="new-password"
              />
            </FormField>

            <button
              type="submit"
              disabled={registerMutation.isPending}
              className="btn-primary w-full disabled:opacity-50"
            >
              {registerMutation.isPending ? 'Creating account...' : 'Create Account'}
            </button>
          </form>

          <p className="text-xs text-text-secondary mt-6 text-center">
            Already have an account?{' '}
            <Link to="/login" className="text-text font-medium hover:underline">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
