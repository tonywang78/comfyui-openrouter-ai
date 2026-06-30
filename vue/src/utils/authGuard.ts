import { useAuthStore } from '@/stores'
import { redirectToLogin } from '@/utils/authRedirect'

export function withAuth(callback: () => void | Promise<void>, message?: string): void {
  const authStore = useAuthStore()

  if (authStore.isLoggedIn) {
    callback()
  } else {
    if (message) {
      console.log(message)
    }
    redirectToLogin()
  }
}

export function checkAuth(): boolean {
  const authStore = useAuthStore()
  return authStore.isLoggedIn
}

export function requireAuth<T extends (...args: any[]) => any>(
  fn: T,
  options?: {
    message?: string;
    onUnauthorized?: () => void;
  }
): T {
  return ((...args: any[]) => {
    const authStore = useAuthStore()

    if (authStore.isLoggedIn) {
      return fn(...args)
    }

    if (options?.message) {
      console.log(options.message)
    }

    if (options?.onUnauthorized) {
      options.onUnauthorized()
    } else {
      redirectToLogin()
    }

    return undefined
  }) as T
}

export function ensureAuth(): Promise<void> {
  const authStore = useAuthStore()
  if (authStore.isLoggedIn) {
    return Promise.resolve()
  }
  redirectToLogin()
  return Promise.reject(new Error('未登录'))
}
