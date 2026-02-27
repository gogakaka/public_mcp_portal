import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { format, formatDistanceToNow, isValid, parseISO } from 'date-fns'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatDate(date: string | Date | null | undefined): string {
  if (!date) return '--'
  const parsed = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(parsed)) return '--'
  return format(parsed, 'MMM d, yyyy')
}

export function formatDateTime(date: string | Date | null | undefined): string {
  if (!date) return '--'
  const parsed = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(parsed)) return '--'
  return format(parsed, 'MMM d, yyyy HH:mm:ss')
}

export function formatRelativeTime(date: string | Date | null | undefined): string {
  if (!date) return '--'
  const parsed = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(parsed)) return '--'
  return formatDistanceToNow(parsed, { addSuffix: true })
}

export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

export function maskSensitiveValue(value: string): string {
  if (value.length <= 8) return '********'
  return value.slice(0, 4) + '****' + value.slice(-4)
}

export function copyToClipboard(text: string): Promise<void> {
  return navigator.clipboard.writeText(text)
}

export function formatExecutionTime(ms: number | null | undefined): string {
  if (ms === null || ms === undefined) return '--'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

export function formatNumber(num: number | null | undefined): string {
  if (num === null || num === undefined) return '0'
  if (num >= 1_000_000) return `${(num / 1_000_000).toFixed(1)}M`
  if (num >= 1_000) return `${(num / 1_000).toFixed(1)}K`
  return num.toString()
}

export function generateId(): string {
  return Math.random().toString(36).substring(2, 11)
}
