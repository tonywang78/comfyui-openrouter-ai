<script setup lang="ts">
import { ElLink, ElTabs, ElTabPane, ElNotification } from 'element-plus'
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import emitter, { LOGIN_SUCCESS } from '@/utils/eventBusUtil'
import LoginForm from './components/LoginForm.vue'
import RegisterForm from './components/RegisterForm.vue'
import ForgotPasswordForm from './components/ForgotPasswordForm.vue'
import PhoneLoginForm from './components/PhoneLoginForm.vue'
import PhoneRegisterForm from './components/PhoneRegisterForm.vue'
import WeChatLoginPanel from './components/WeChatLoginPanel.vue'
import lottie from 'lottie-web'
import logoAnimation from '@/assets/lottie/logo.json'
import { useAuthStore } from '@/stores'
import { useUserStore } from '@/stores/modules/user'

const props = withDefaults(defineProps<{
  redirect?: string
}>(), {
  redirect: '/comfyui'
})

const { t } = useI18n()
const authStore = useAuthStore()
const userStore = useUserStore()

const activeTab = ref('phone')
const viewState = ref<'login' | 'register' | 'phoneRegister' | 'forgotPassword'>('login')
const logoContainer = ref<HTMLElement | null>(null)

const passwordLoginLoading = ref(false)
const phoneLoginLoading = ref(false)
const registerLoading = ref(false)
const phoneRegisterLoading = ref(false)
const forgotPasswordLoading = ref(false)
const wechatBindLoading = ref(false)

const loginFormRef = ref<InstanceType<typeof LoginForm> | null>(null)
const phoneLoginFormRef = ref<InstanceType<typeof PhoneLoginForm> | null>(null)

let logoAnimationInstance: ReturnType<typeof lottie.loadAnimation> | null = null

const initAnimation = () => {
  if (logoAnimationInstance) {
    logoAnimationInstance.destroy()
    logoAnimationInstance = null
  }

  if (logoContainer.value) {
    try {
      logoAnimationInstance = lottie.loadAnimation({
        container: logoContainer.value,
        renderer: 'svg',
        loop: true,
        autoplay: true,
        animationData: logoAnimation
      })
    } catch (error) {
      console.error('Failed to load animation:', error)
    }
  }
}

const finishLogin = async (generation: number) => {
  const ok = await userStore.handleLoginSuccess(props.redirect)
  if (ok && authStore.isLoginAttemptActive(generation)) {
    ElNotification.success({
      title: t('common.success'),
      message: t('auth.loginSuccess')
    })
    emitter.emit(LOGIN_SUCCESS)
  }
}

const handleLogin = async (loginForm: {
  account: string
  password: string
  captchaKey: string
  captchaCode: string
}) => {
  const generation = authStore.beginLoginAttempt()
  passwordLoginLoading.value = true
  try {
    const success = await authStore.passwordLogin(loginForm)
    if (success) {
      await finishLogin(generation)
    } else {
      loginFormRef.value?.refreshCaptcha()
    }
  } finally {
    passwordLoginLoading.value = false
  }
}

const handleRegister = async (registerForm: { email: string; code: string; password: string }) => {
  registerLoading.value = true
  try {
    const success = await authStore.register(registerForm)
    if (success) {
      viewState.value = 'login'
      activeTab.value = 'password'
    }
  } finally {
    registerLoading.value = false
  }
}

const handlePhoneRegister = async (registerForm: { phone: string; code: string; password?: string }) => {
  phoneRegisterLoading.value = true
  try {
    const success = await authStore.phoneRegister(registerForm)
    if (success) {
      viewState.value = 'login'
      activeTab.value = 'phone'
    }
  } finally {
    phoneRegisterLoading.value = false
  }
}

const handlePhoneLogin = async (phoneLoginForm: {
  phone: string
  code: string
  captchaKey: string
  captchaCode: string
}) => {
  const generation = authStore.beginLoginAttempt()
  phoneLoginLoading.value = true
  try {
    const success = await authStore.phoneLogin(phoneLoginForm)
    if (success) {
      await finishLogin(generation)
    } else {
      phoneLoginFormRef.value?.refreshCaptcha()
    }
  } finally {
    phoneLoginLoading.value = false
  }
}

const handleWechatLoginSuccess = async (token: string) => {
  const generation = authStore.beginLoginAttempt()
  await authStore.loginWithToken(token)
  await finishLogin(generation)
}

const handleWechatBindPhone = async (payload: { bindTicket: string; phone: string; code: string }) => {
  const generation = authStore.beginLoginAttempt()
  wechatBindLoading.value = true
  try {
    const success = await authStore.bindWechatPhone(payload)
    if (success) {
      await finishLogin(generation)
    }
  } finally {
    wechatBindLoading.value = false
  }
}

const handlePasswordReset = async (data: { email: string; code: string; password: string }) => {
  forgotPasswordLoading.value = true
  try {
    const success = await authStore.forgotPassword(data)
    if (success) {
      viewState.value = 'login'
      activeTab.value = 'password'
    }
  } finally {
    forgotPasswordLoading.value = false
  }
}

const handleGetLoginCode = async (email: string) => {
  await authStore.getVerificationCode(email)
}

const handleGetPhoneCode = async (payload: {
  phone: string
  captchaKey: string
  captchaCode: string
}) => {
  const success = await authStore.getPhoneVerificationCode(payload)
  phoneLoginFormRef.value?.refreshCaptcha()
  return success
}

const goRegister = () => {
  viewState.value = activeTab.value === 'phone' ? 'phoneRegister' : 'register'
}

onMounted(() => {
  nextTick(() => initAnimation())
})

onUnmounted(() => {
  if (logoAnimationInstance) {
    logoAnimationInstance.destroy()
  }
})

watch(viewState, () => {
  nextTick(() => initAnimation())
})
</script>

<template>
  <div class="auth-panel">
    <div class="auth-panel-header">
      <div ref="logoContainer" class="lottie-container"></div>
      <h2 class="logo-text">慧心云创</h2>
    </div>

    <div class="auth-panel-content">
      <div v-if="viewState === 'login'">
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane :label="t('auth.phoneLogin')" name="phone">
            <PhoneLoginForm
              ref="phoneLoginFormRef"
              @login-with-code="handlePhoneLogin"
              @send-code="handleGetPhoneCode"
              :loading="phoneLoginLoading"
            />
          </el-tab-pane>
          <el-tab-pane :label="t('auth.passwordLogin')" name="password">
            <LoginForm ref="loginFormRef" @login="handleLogin" :loading="passwordLoginLoading" />
          </el-tab-pane>
          <el-tab-pane :label="t('auth.wechatLogin')" name="wechat">
            <WeChatLoginPanel
              :bind-loading="wechatBindLoading"
              @login-success="handleWechatLoginSuccess"
              @bind-phone="handleWechatBindPhone"
              @send-bind-code="handleGetPhoneCode"
            />
          </el-tab-pane>
        </el-tabs>
        <div v-if="activeTab !== 'wechat'" class="extra-actions">
          <el-link
            v-if="activeTab === 'password'"
            type="primary"
            :underline="false"
            style="font-size: 12px;"
            @click="viewState = 'forgotPassword'"
          >
            {{ t('auth.forgotPassword') }}
          </el-link>
        </div>
      </div>

      <RegisterForm
        v-else-if="viewState === 'register'"
        @register="handleRegister"
        @send-code="handleGetLoginCode"
        :loading="registerLoading"
      />
      <PhoneRegisterForm
        v-else-if="viewState === 'phoneRegister'"
        @register="handlePhoneRegister"
        @send-code="handleGetPhoneCode"
        :loading="phoneRegisterLoading"
      />
      <ForgotPasswordForm
        v-else-if="viewState === 'forgotPassword'"
        @reset-password="handlePasswordReset"
        @send-code="handleGetLoginCode"
        :loading="forgotPasswordLoading"
      />

      <div class="auth-toggle">
        <span v-if="viewState === 'login'">
          {{ t('auth.noAccount') }}
          <el-link type="primary" @click="goRegister">{{ t('auth.registerNow') }}</el-link>
        </span>
        <span v-if="viewState === 'register' || viewState === 'phoneRegister'">
          {{ t('auth.hasAccount') }}
          <el-link type="primary" @click="viewState = 'login'">{{ t('auth.loginNow') }}</el-link>
        </span>
        <span v-if="viewState === 'forgotPassword'">
          <el-link type="primary" @click="viewState = 'login'">{{ t('auth.backToLogin') }}</el-link>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-panel {
  width: 100%;
  max-width: 450px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-light);
  overflow: hidden;
}

.auth-panel-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 0 10px;
  background: var(--el-bg-color);
}

.lottie-container {
  width: 80px;
  height: 80px;
  margin-bottom: 8px;
}

.logo-text {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
  text-align: center;
}

.auth-panel-content {
  width: 100%;
  padding: 20px;
}

.login-tabs {
  width: 100%;
  :deep(.el-tabs__nav) {
    width: 100%;
    display: flex;
  }
  :deep(.el-tabs__item) {
    flex: 1;
    text-align: center;
    font-size: 14px;
    padding: 0 4px;
  }
}

.extra-actions {
  margin-top: 0;
  text-align: right;
  width: 100%;
}

.auth-toggle {
  margin-top: 54px;
  text-align: center;
  width: 100%;
  font-size: 14px;
}

.auth-toggle .el-link {
  font-size: 14px;
  vertical-align: baseline;
}
</style>
