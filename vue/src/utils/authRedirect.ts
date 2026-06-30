import router from '@/router'

export function redirectToLogin(redirect?: string) {
  if (router.currentRoute.value.path === '/login') return
  const target = redirect ?? router.currentRoute.value.fullPath
  return router.replace({
    path: '/login',
    query: target && target !== '/login' ? { redirect: target } : undefined
  })
}
