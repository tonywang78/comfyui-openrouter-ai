<script setup lang="ts">
import { ElFormItem, ElInput } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { authApi } from '@/api/auth/auth'

defineOptions({
  name: 'CaptchaField'
})

const { t } = useI18n()

const captchaCode = defineModel<string>('captchaCode', { default: '' })
const captchaKey = ref('')
const imageSrc = ref('')
const loading = ref(false)

const refresh = async () => {
  loading.value = true
  captchaCode.value = ''
  try {
    const data = await authApi.reqCaptcha()
    captchaKey.value = data.captchaKey
    const base64 = data.imageBase64
    imageSrc.value = base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`
  } catch (error) {
    console.error('获取图形验证码失败:', error)
    imageSrc.value = ''
    captchaKey.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refresh()
})

defineExpose({
  captchaKey,
  getCaptchaKey: () => captchaKey.value,
  refresh
})
</script>

<template>
  <el-form-item prop="captchaCode">
    <div class="captcha-wrapper">
      <el-input
        v-model="captchaCode"
        :placeholder="t('auth.imageCaptchaPlaceholder')"
        size="large"
        autocomplete="off"
        maxlength="6"
      />
      <button
        type="button"
        class="captcha-image-btn"
        :class="{ 'is-loading': loading }"
        :title="t('auth.clickToRefreshCaptcha')"
        :disabled="loading"
        @click="refresh"
      >
        <img v-if="imageSrc" :src="imageSrc" alt="captcha" class="captcha-image" />
        <span v-else class="captcha-placeholder">{{ t('auth.clickToRefreshCaptcha') }}</span>
      </button>
    </div>
  </el-form-item>
</template>

<style scoped>
.captcha-wrapper {
  display: flex;
  width: 100%;
  gap: 10px;
}

.captcha-wrapper :deep(.el-input) {
  flex: 1;
}

.captcha-image-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-bg-color);
  cursor: pointer;
  overflow: hidden;
}

.captcha-image-btn.is-loading {
  opacity: 0.7;
  cursor: wait;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding: 0 6px;
  text-align: center;
  line-height: 1.2;
}
</style>
