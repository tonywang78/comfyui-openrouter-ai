import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth/auth'
import type { RegisterApi, ForgotPasswordApi, PhoneRegisterApi, WechatBindPhoneApi } from '@/api/auth/types'
import { ElNotification } from 'element-plus'
import router from '@/router'
import i18n from '@/i18n'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const isLoggedIn = computed(() => !!token.value)

  async function fetchUserInfoAfterLogin() {
    const { useUserStore } = await import('../modules/user')
    const userStore = useUserStore()
    await userStore.fetchUserInfo()
  }

  async function passwordLogin(email: string, password: string) {
    try {
      const tokenValue = await authApi.reqPasswordLogin({ email, password })
      setToken(tokenValue)
      await fetchUserInfoAfterLogin()
      return true
    } catch (err: any) {
      console.error('登录失败:', err)
      return false
    }
  }

  async function emailLogin(email: string, code: string) {
    try {
      const tokenValue = await authApi.reqEmailLogin({ email, code })
      setToken(tokenValue)
      await fetchUserInfoAfterLogin()
      return true
    } catch (err: any) {
      console.error('邮箱登录失败:', err)
      return false
    }
  }

  async function phoneLogin(phone: string, code: string) {
    try {
      const tokenValue = await authApi.reqPhoneLogin({ phone, code })
      setToken(tokenValue)
      await fetchUserInfoAfterLogin()
      return true
    } catch (err: any) {
      console.error('手机号登录失败:', err)
      return false
    }
  }

  async function getVerificationCode(email: string) {
    try {
      await authApi.reqGetVerificationCode({ email, password: '' })
      ElNotification.success({
        title: i18n.global.t('common.success'),
        message: i18n.global.t('auth.getCode')
      })
      return true
    } catch {
      return false
    }
  }

  async function getPhoneVerificationCode(phone: string) {
    try {
      await authApi.reqPhoneVerificationCode({ phone })
      ElNotification.success({
        title: i18n.global.t('common.success'),
        message: i18n.global.t('auth.phoneCodeSent')
      })
      return true
    } catch {
      return false
    }
  }

  async function register(params: RegisterApi.Params) {
    try {
      await authApi.reqRegister(params)
      ElNotification.success({
        title: i18n.global.t('common.success'),
        message: i18n.global.t('auth.registerSuccess')
      })
      return true
    } catch {
      return false
    }
  }

  async function phoneRegister(params: PhoneRegisterApi.Params) {
    try {
      await authApi.reqPhoneRegister(params)
      ElNotification.success({
        title: i18n.global.t('common.success'),
        message: i18n.global.t('auth.registerSuccess')
      })
      return true
    } catch {
      return false
    }
  }

  async function bindWechatPhone(params: WechatBindPhoneApi.Params) {
    try {
      const tokenValue = await authApi.reqWechatBindPhone(params)
      setToken(tokenValue)
      await fetchUserInfoAfterLogin()
      return true
    } catch (err: any) {
      console.error('微信绑定手机号失败:', err)
      return false
    }
  }

  async function loginWithToken(tokenValue: string) {
    setToken(tokenValue)
    await fetchUserInfoAfterLogin()
    return true
  }

  async function forgotPassword(params: ForgotPasswordApi.Params) {
    try {
      await authApi.reqForgotPassword(params)
      ElNotification.success({
        title: i18n.global.t('common.success'),
        message: i18n.global.t('auth.resetPasswordSuccess')
      })
      return true
    } catch {
      return false
    }
  }

  function setToken(tokenValue: string | any) {
    if (!tokenValue) {
      console.error('设置token失败: tokenValue为空')
      return
    }

    let actualToken = tokenValue
    if (tokenValue && typeof tokenValue === 'object' && tokenValue.data) {
      actualToken = tokenValue.data
    }

    token.value = actualToken
    localStorage.setItem('token', actualToken)
  }

  async function logout() {
    try {
      await authApi.reqLogout()

      try {
        const { useTaskWebSocketStore: useComfyuiTaskProgressWebSocketStore } = await import('./taskWebsocket')
        const webSocketStore = useComfyuiTaskProgressWebSocketStore()
        webSocketStore.disconnect()
      } catch (error) {
        console.error('断开WebSocket连接失败:', error)
      }

      token.value = ''
      localStorage.removeItem('token')

      const { useUserStore } = await import('../modules/user')
      const userStore = useUserStore()
      userStore.clearUserInfo()

      ElNotification.success({
        title: i18n.global.t('common.success'),
        message: i18n.global.t('auth.logout')
      })

      await router.replace('/')
      window.location.reload()
      return true
    } catch (error: any) {
      console.error('退出登录失败:', error)

      try {
        const { useTaskWebSocketStore: useComfyuiTaskProgressWebSocketStore } = await import('./taskWebsocket')
        const webSocketStore = useComfyuiTaskProgressWebSocketStore()
        webSocketStore.disconnect()
      } catch (disconnectError) {
        console.error('断开WebSocket连接失败:', disconnectError)
      }

      token.value = ''
      localStorage.removeItem('token')

      const { useUserStore } = await import('../modules/user')
      const userStore = useUserStore()
      userStore.clearUserInfo()

      ElNotification.warning({
        title: i18n.global.t('common.warning'),
        message: i18n.global.t('common.error')
      })

      await router.replace('/')
      window.location.reload()
      return false
    }
  }

  return {
    token,
    isLoggedIn,
    passwordLogin,
    emailLogin,
    phoneLogin,
    getVerificationCode,
    getPhoneVerificationCode,
    register,
    phoneRegister,
    bindWechatPhone,
    loginWithToken,
    forgotPassword,
    setToken,
    logout
  }
})
