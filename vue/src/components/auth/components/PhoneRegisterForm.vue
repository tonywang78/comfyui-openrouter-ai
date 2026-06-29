<script setup lang="ts">
import { ElForm, ElFormItem, ElInput, ElButton, ElNotification, type FormRules } from 'element-plus'
import { reactive, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import TermsAgreement from './TermsAgreement.vue'

defineOptions({
  name: 'PhoneRegisterForm'
})

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const { t } = useI18n()

const registerForm = reactive({
  phone: '',
  code: '',
  password: ''
})

const agreementChecked = ref(false)
const formRef = ref()
const codeSending = ref(false)

const emit = defineEmits(['register', 'send-code'])

const rules = computed<FormRules>(() => ({
  phone: [
    { required: true, message: t('auth.pleaseEnterPhone'), trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: t('auth.pleaseEnterValidPhone'), trigger: 'blur' }
  ],
  code: [
    { required: true, message: t('auth.pleaseEnterCode'), trigger: 'blur' }
  ],
  password: [
    { min: 6, message: t('auth.passwordMinLength'), trigger: 'blur' }
  ]
}))

const handleRegister = () => {
  if (!agreementChecked.value) {
    ElNotification.warning({
      title: t('common.warning'),
      message: t('auth.pleaseAgreeTerms')
    })
    return
  }

  formRef.value.validate((valid: boolean) => {
    if (valid) {
      emit('register', registerForm)
    }
  })
}

const sendCode = () => {
  formRef.value.validateField('phone', (valid: boolean) => {
    if (valid) {
      codeSending.value = true
      emit('send-code', registerForm.phone)
      setTimeout(() => {
        codeSending.value = false
      }, 2000)
    }
  })
}
</script>

<template>
  <div class="phone-register-form-container">
    <el-form
      ref="formRef"
      :model="registerForm"
      :rules="rules"
      @submit.prevent
      autocomplete="off"
    >
      <el-form-item prop="phone">
        <el-input v-model="registerForm.phone" :placeholder="t('auth.phone')" size="large" autocomplete="off" maxlength="11" />
      </el-form-item>
      <el-form-item prop="code">
        <div class="verification-code-wrapper">
          <el-input v-model="registerForm.code" :placeholder="t('auth.verificationCode')" size="large" autocomplete="off" maxlength="6" />
          <div class="custom-send-btn" @click="sendCode" :class="{ 'is-loading': codeSending }">
            {{ codeSending ? t('auth.sendingCode') : t('auth.getCode') }}
          </div>
        </div>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="registerForm.password"
          type="password"
          :placeholder="t('auth.passwordOptional')"
          show-password
          size="large"
          autocomplete="new-password"
        />
      </el-form-item>
      <TermsAgreement v-model="agreementChecked" />
      <el-form-item>
        <el-button class="auth-button" type="primary" @click="handleRegister" size="large" :loading="props.loading">
          {{ t('auth.register') }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.phone-register-form-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
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
