<script setup lang="ts">
import { ElDialog, ElLink, ElTabs, ElTabPane, ElNotification } from 'element-plus'
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import emitter, { OPEN_AUTH_DIALOG, LOGIN_SUCCESS } from '@/utils/eventBusUtil'
import LoginForm from './components/LoginForm.vue'
import RegisterForm from './components/RegisterForm.vue'
import CodeLoginForm from './components/CodeLoginForm.vue'
import ForgotPasswordForm from './components/ForgotPasswordForm.vue'
import PhoneLoginForm from './components/PhoneLoginForm.vue'
import PhoneRegisterForm from './components/PhoneRegisterForm.vue'
import WeChatLoginPanel from './components/WeChatLoginPanel.vue'
import lottie from 'lottie-web'
import logoAnimation from '@/assets/lottie/logo.json'
import { useAuthStore } from '@/stores'
import { useUserStore } from '@/stores/modules/user'

const { t } = useI18n()
const authStore = useAuthStore()
const userStore = useUserStore()

const visible = ref(false)
const activeTab = ref('phone')
const viewState = ref<'login' | 'register' | 'phoneRegister' | 'forgotPassword'>('login')
const logoContainer = ref<HTMLElement | null>(null)

const passwordLoginLoading = ref(false)
const codeLoginLoading = ref(false)
const phoneLoginLoading = ref(false)
const registerLoading = ref(false)
const phoneRegisterLoading = ref(false)
const forgotPasswordLoading = ref(false)
const wechatBindLoading = ref(false)

let logoAnimation_instance: any = null

const initAnimation = () => {
  if (logoAnimation_instance) {
    logoAnimation_instance.destroy()
    logoAnimation_instance = null
  }

  if (logoContainer.value) {
    try {
      logoAnimation_instance = lottie.loadAnimation({
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

const handleClose = () => {
  visible.value = false
}

const openDialog = () => {
  visible.value = true
  activeTab.value = 'phone'
  viewState.value = 'login'
  nextTick(() => {
    initAnimation()
  })
}

const finishLogin = async () => {
  ElNotification.success({
    title: t('common.success'),
    message: t('auth.loginSuccess')
  })
  handleClose()
  await userStore.handleLoginSuccess()
  emitter.emit(LOGIN_SUCCESS)
}

const handleLogin = async (loginForm: { account: string; password: string }) => {
  passwordLoginLoading.value = true
  try {
    const success = await authStore.passwordLogin(loginForm.account, loginForm.password)
    if (success) {
      await finishLogin()
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

const handleCodeLogin = async (codeLoginForm: { email: string; code: string }) => {
  codeLoginLoading.value = true
  try {
    const success = await authStore.emailLogin(codeLoginForm.email, codeLoginForm.code)
    if (success) {
      await finishLogin()
    }
  } finally {
    codeLoginLoading.value = false
  }
}

const handlePhoneLogin = async (phoneLoginForm: { phone: string; code: string }) => {
  phoneLoginLoading.value = true
  try {
    const success = await authStore.phoneLogin(phoneLoginForm.phone, phoneLoginForm.code)
    if (success) {
      await finishLogin()
    }
  } finally {
    phoneLoginLoading.value = false
  }
}

const handleWechatLoginSuccess = async (token: string) => {
  await authStore.loginWithToken(token)
  await finishLogin()
}

const handleWechatBindPhone = async (payload: { bindTicket: string; phone: string; code: string }) => {
  wechatBindLoading.value = true
  try {
    const success = await authStore.bindWechatPhone(payload)
    if (success) {
      await finishLogin()
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

const handleGetPhoneCode = async (phone: string) => {
  await authStore.getPhoneVerificationCode(phone)
}

const goRegister = () => {
  viewState.value = activeTab.value === 'phone' ? 'phoneRegister' : 'register'
}

onMounted(() => {
  emitter.on(OPEN_AUTH_DIALOG, openDialog)
})

onUnmounted(() => {
  emitter.off(OPEN_AUTH_DIALOG, openDialog)
  if (logoAnimation_instance) {
    logoAnimation_instance.destroy()
  }
})

watch(visible, (newVal) => {
  if (newVal) {
    nextTick(() => {
      initAnimation()
    })
  } else if (logoAnimation_instance) {
    logoAnimation_instance.destroy()
    logoAnimation_instance = null
  }
})
</script>

<template>
  <el-dialog
    v-model="visible"
    width="450px"
    :before-close="handleClose"
    align-center
    class="auth-dialog"
    :append-to-body="true"
    :lock-scroll="true"
    :close-on-click-modal="false"
  >
    <div class="auth-dialog-header">
      <div ref="logoContainer" class="lottie-container"></div>
      <h2 class="logo-text">慧心云创</h2>
    </div>

    <div class="auth-dialog-content">
      <div v-if="viewState === 'login'">
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane :label="t('auth.phoneLogin')" name="phone">
            <PhoneLoginForm
              @login-with-code="handlePhoneLogin"
              @send-code="handleGetPhoneCode"
              :loading="phoneLoginLoading"
            />
          </el-tab-pane>
          <el-tab-pane :label="t('auth.passwordLogin')" name="password">
            <LoginForm @login="handleLogin" :loading="passwordLoginLoading" />
          </el-tab-pane>
          <el-tab-pane :label="t('auth.codeLogin')" name="code">
            <CodeLoginForm
              @login-with-code="handleCodeLogin"
              @send-code="handleGetLoginCode"
              :loading="codeLoginLoading"
            />
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
  </el-dialog>
</template>

<style scoped>
.auth-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.auth-dialog-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 0;
  padding-bottom: 10px;
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

.auth-dialog-content {
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
