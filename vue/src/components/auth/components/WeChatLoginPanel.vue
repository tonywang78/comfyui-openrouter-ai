<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElNotification, ElButton } from 'element-plus'
import { authApi } from '@/api/auth/auth'
import { isMobileClient } from '@/config/runtime'
import { buildWechatOAuthUrl, openExternalUrl } from '@/utils/platformUtil'
import WeChatBindPhoneForm from './WeChatBindPhoneForm.vue'

declare global {
  interface Window {
    WxLogin?: new (options: {
      self_redirect?: boolean
      id: string
      appid: string
      scope: string
      redirect_uri: string
      state: string
      style?: string
      href?: string
    }) => void
  }
}

defineOptions({
  name: 'WeChatLoginPanel'
})

defineProps({
  bindLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['login-success', 'bind-phone', 'send-bind-code'])

const { t } = useI18n()

const loading = ref(true)
const bindTicket = ref('')
const pollTimer = ref<number | null>(null)
const currentState = ref('')
const isMobile = computed(() => isMobileClient())

const loadWxLoginScript = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (window.WxLogin) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = 'https://res.wx.qq.com/connect/zh_CN/htmledition/js/wxLogin.js'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('failed to load wxLogin.js'))
    document.body.appendChild(script)
  })
}

const stopPolling = () => {
  if (pollTimer.value !== null) {
    window.clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

const startPolling = (state: string) => {
  stopPolling()
  pollTimer.value = window.setInterval(async () => {
    try {
      const result = await authApi.reqWechatPoll(state)
      if (result.status === 'success' && result.token) {
        stopPolling()
        emit('login-success', result.token)
      } else if (result.status === 'need_bind' && result.bindTicket) {
        stopPolling()
        bindTicket.value = result.bindTicket
      }
    } catch (error) {
      console.error('WeChat poll failed:', error)
    }
  }, 2000)
}

const initWechatLogin = async () => {
  loading.value = true
  bindTicket.value = ''
  try {
    const loginState = await authApi.reqWechatLoginState()
    currentState.value = loginState.state

    if (isMobile.value) {
      return
    }

    await loadWxLoginScript()

    const container = document.getElementById('wechat_login_container')
    if (container) {
      container.innerHTML = ''
    }

    if (window.WxLogin) {
      new window.WxLogin({
        self_redirect: false,
        id: 'wechat_login_container',
        appid: loginState.appId,
        scope: 'snsapi_login',
        redirect_uri: encodeURIComponent(loginState.redirectUri),
        state: loginState.state,
        style: 'black',
        href: ''
      })
    }

    startPolling(loginState.state)
  } catch (error) {
    console.error('Init WeChat login failed:', error)
    ElNotification.error({
      title: t('common.error'),
      message: t('auth.wechatLoginInitFailed')
    })
  } finally {
    loading.value = false
  }
}

const openWechatInBrowser = async () => {
  try {
    const loginState = await authApi.reqWechatLoginState()
    currentState.value = loginState.state
    startPolling(loginState.state)
    await openExternalUrl(
      buildWechatOAuthUrl(loginState.appId, loginState.redirectUri, loginState.state)
    )
  } catch (error) {
    console.error('Open WeChat login in browser failed:', error)
    ElNotification.error({
      title: t('common.error'),
      message: t('auth.wechatLoginInitFailed')
    })
  }
}

const handleWechatCallbackMessage = (event: MessageEvent) => {
  if (event.data?.type === 'wechat-login-callback' && event.data.state === currentState.value) {
    authApi.reqWechatPoll(currentState.value).then((result) => {
      if (result.status === 'success' && result.token) {
        stopPolling()
        emit('login-success', result.token)
      } else if (result.status === 'need_bind' && result.bindTicket) {
        stopPolling()
        bindTicket.value = result.bindTicket
      }
    })
  }
}

onMounted(() => {
  window.addEventListener('message', handleWechatCallbackMessage)
  initWechatLogin()
})

onUnmounted(() => {
  stopPolling()
  window.removeEventListener('message', handleWechatCallbackMessage)
})
</script>

<template>
  <div class="wechat-login-panel">
    <WeChatBindPhoneForm
      v-if="bindTicket"
      :bind-ticket="bindTicket"
      :loading="bindLoading"
      @bind="(payload) => emit('bind-phone', payload)"
      @send-code="(phone) => emit('send-bind-code', phone)"
    />
    <template v-else-if="isMobile">
      <p class="wechat-tip">{{ t('auth.wechatMobileTip') }}</p>
      <el-button type="primary" :loading="loading" @click="openWechatInBrowser">
        {{ t('auth.wechatOpenInBrowser') }}
      </el-button>
      <el-button link type="primary" @click="initWechatLogin">{{ t('auth.refreshQrcode') }}</el-button>
    </template>
    <template v-else>
      <p class="wechat-tip">{{ t('auth.wechatScanTip') }}</p>
      <div id="wechat_login_container" class="wechat-qrcode" v-loading="loading"></div>
      <el-button link type="primary" @click="initWechatLogin">{{ t('auth.refreshQrcode') }}</el-button>
    </template>
  </div>
</template>

<style scoped>
.wechat-login-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  min-height: 280px;
}

.wechat-tip {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.wechat-qrcode {
  width: 300px;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
