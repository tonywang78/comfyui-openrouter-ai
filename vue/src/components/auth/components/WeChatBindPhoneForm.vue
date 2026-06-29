<script setup lang="ts">
import { ElForm, ElFormItem, ElInput, ElButton, type FormRules } from 'element-plus'
import { reactive, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({
  name: 'WeChatBindPhoneForm'
})

const props = defineProps({
  bindTicket: {
    type: String,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const { t } = useI18n()

const bindForm = reactive({
  phone: '',
  code: ''
})

const formRef = ref()
const codeSending = ref(false)

const emit = defineEmits(['bind', 'send-code'])

const rules = computed<FormRules>(() => ({
  phone: [
    { required: true, message: t('auth.pleaseEnterPhone'), trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: t('auth.pleaseEnterValidPhone'), trigger: 'blur' }
  ],
  code: [
    { required: true, message: t('auth.pleaseEnterCode'), trigger: 'blur' }
  ]
}))

const handleBind = () => {
  formRef.value.validate((valid: boolean) => {
    if (valid) {
      emit('bind', {
        bindTicket: props.bindTicket,
        phone: bindForm.phone,
        code: bindForm.code
      })
    }
  })
}

const sendCode = () => {
  formRef.value.validateField('phone', (valid: boolean) => {
    if (valid) {
      codeSending.value = true
      emit('send-code', bindForm.phone)
      setTimeout(() => {
        codeSending.value = false
      }, 2000)
    }
  })
}
</script>

<template>
  <div class="wechat-bind-form-container">
    <p class="bind-tip">{{ t('auth.wechatBindPhoneTip') }}</p>
    <el-form ref="formRef" :model="bindForm" :rules="rules" @submit.prevent autocomplete="off">
      <el-form-item prop="phone">
        <el-input v-model="bindForm.phone" :placeholder="t('auth.phone')" size="large" maxlength="11" />
      </el-form-item>
      <el-form-item prop="code">
        <div class="verification-code-wrapper">
          <el-input v-model="bindForm.code" :placeholder="t('auth.verificationCode')" size="large" maxlength="6" />
          <div class="custom-send-btn" @click="sendCode" :class="{ 'is-loading': codeSending }">
            {{ codeSending ? t('auth.sendingCode') : t('auth.getCode') }}
          </div>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button class="auth-button" type="primary" @click="handleBind" size="large" :loading="props.loading">
          {{ t('auth.bindAndLogin') }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.wechat-bind-form-container {
  width: 100%;
}

.bind-tip {
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.auth-button {
  width: 100%;
  border-radius: 182px;
  font-weight: 600;
}

.verification-code-wrapper {
  display: flex;
  width: 100%;
  gap: 10px;
}

.verification-code-wrapper :deep(.el-input) {
  flex: 1;
}

.custom-send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 100px;
  height: 40px;
  background-color: var(--el-color-primary);
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.custom-send-btn.is-loading {
  opacity: 0.8;
  cursor: not-allowed;
}

:deep(.el-form) {
  width: 100%;
}
</style>
