import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth/auth'
import type { RegisterApi, ForgotPasswordApi, PhoneRegisterApi, WechatBindPhoneApi, PasswordLoginApi, PhoneLoginApi, PhoneVerificationCodeApi } from '@/api/auth/types'
import { ElNotification } from 'element-plus'
import i18n from '@/i18n'
import { redirectToLogin } from '@/utils/authRedirect'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const isLoggedIn = computed(() => !!token.value)
  let loggingOut = false
  let loginGeneration = 0

  function beginLoginAttempt() {
    loginGeneration += 1
    return loginGeneration
  }

  function isLoginAttemptActive(generation: number) {
    return !loggingOut && generation === loginGeneration && !!token.value
  }

  function isAuthTransitioning() {
    return loggingOut
  }

  async function fetchUserInfoAfterLogin() {
    const { useUserStore } = await import('../modules/user')
    const userStore = useUserStore()
    await userStore.fetchUserInfo()
  }

  async function passwordLogin(params: PasswordLoginApi.Params) {
    try {
      const tokenValue = await authApi.reqPasswordLogin(params)
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

  async function phoneLogin(params: PhoneLoginApi.Params) {
    try {
      const tokenValue = await authApi.reqPhoneLogin(params)
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

  async function getPhoneVerificationCode(params: PhoneVerificationCodeApi.Params) {
    try {
      await authApi.reqPhoneVerificationCode(params)
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

  async function disconnectTaskWebSocket() {
    try {
      const { useTaskWebSocketStore: useComfyuiTaskProgressWebSocketStore } = await import('./taskWebsocket')
      useComfyuiTaskProgressWebSocketStore().disconnect()
    } catch (error) {
      console.error('断开WebSocket连接失败:', error)
    }
  }

  async function clearSession() {
    loginGeneration += 1
    await disconnectTaskWebSocket()
    token.value = ''
    localStorage.removeItem('token')

    const { useUserStore } = await import('../modules/user')
    useUserStore().clearUserInfo()
  }

  /** 401 拦截器调用：仅清本地会话，不重复请求 logout API、不 reload */
  async function handleUnauthorized() {
    if (loggingOut || !token.value) return
    loggingOut = true
    try {
      ElNotification.closeAll()
      await clearSession()
      await redirectToLogin()
    } finally {
      loggingOut = false
    }
  }

  async function logout() {
    if (loggingOut) return true
    loggingOut = true
    ElNotification.closeAll()
    try {
      const hadToken = !!token.value
      if (hadToken) {
        try {
          await authApi.reqLogout()
        } catch {
          // 会话已失效时忽略
        }
      }

      await clearSession()
      await redirectToLogin()
      return true
    } catch (error: any) {
      console.error('退出登录失败:', error)
      await clearSession()
      await redirectToLogin()
      return false
    } finally {
      loggingOut = false
    }
  }

  return {
    token,
    isLoggedIn,
    beginLoginAttempt,
    isLoginAttemptActive,
    isAuthTransitioning,
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
    clearSession,
    handleUnauthorized,
    logout
  }
})
