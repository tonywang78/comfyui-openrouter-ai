<template>
  <div class="change-password">
    <div class="form-section">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item :label="t('profile.password.oldLabel')" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            :placeholder="t('profile.password.oldPlaceholder')"
            show-password
            size="large"
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item :label="t('profile.password.newLabel')" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            :placeholder="t('profile.password.newPlaceholder')"
            show-password
            size="large"
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item :label="t('profile.password.confirmLabel')" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            :placeholder="t('profile.password.confirmPlaceholder')"
            show-password
            size="large"
            autocomplete="new-password"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
      </el-form>
    </div>

    <div class="actions-section">
      <el-button size="large" class="cancel-btn" @click="handleCancel">
        {{ t('profile.password.cancel') }}
      </el-button>
      <el-button
        type="primary"
        size="large"
        class="submit-btn"
        :loading="submitting"
        @click="handleSubmit"
      >
        {{ submitting ? t('profile.password.saving') : t('profile.password.save') }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElNotification, ElForm } from 'element-plus'
import { userApi } from '@/api/user/user'

const { t } = useI18n()

const emit = defineEmits<{
  success: []
  cancel: []
}>()

const formRef = ref<InstanceType<typeof ElForm>>()
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = computed(() => ({
  oldPassword: [
    { required: true, message: t('profile.password.oldRequired'), trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: t('profile.password.newRequired'), trigger: 'blur' },
    { min: 6, message: t('profile.password.minLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('profile.password.confirmRequired'), trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error(t('profile.password.notMatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}))

const resetForm = () => {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

const handleCancel = () => {
  resetForm()
  emit('cancel')
}

defineExpose({ reset: resetForm })

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  submitting.value = true
  try {
    await userApi.reqChangePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElNotification.success(t('profile.password.updateSuccess'))
    resetForm()
    emit('success')
  } catch (error) {
    console.error('密码修改失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.change-password {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-section {
  padding: 0 4px;
}

.actions-section {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.cancel-btn,
.submit-btn {
  border-radius: 8px !important;
  font-weight: 500 !important;
}

@media (max-width: 640px) {
  .actions-section {
    flex-direction: column-reverse;
  }

  .cancel-btn,
  .submit-btn {
    width: 100% !important;
  }
}
</style>
