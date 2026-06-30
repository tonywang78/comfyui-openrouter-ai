import type { Router } from 'vue-router'
import { useAuthStore, useUserStore } from '@/stores'
import { Role } from '@/enums/user'

const LOGIN_PATH = '/login'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const authStore = useAuthStore()
    const isLoggedIn = authStore.isLoggedIn

    if (to.path === LOGIN_PATH) {
      if (isLoggedIn) {
        const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/comfyui'
        next(redirect.startsWith('/') ? redirect : '/comfyui')
      } else {
        next()
      }
      return
    }

    if (!isLoggedIn) {
      next({ path: LOGIN_PATH, query: { redirect: to.fullPath } })
      return
    }

    if (to.meta.requiresAdmin) {
      const userStore = useUserStore()
      if (!userStore.userInfo) {
        await userStore.fetchUserInfo()
      }

      if (userStore.userInfo?.role !== Role.ADMIN) {
        next('/comfyui')
        return
      }
    }

    next()
  })
}
