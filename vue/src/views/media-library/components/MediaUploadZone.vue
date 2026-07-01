<template>
  <div class="upload-zone" @click="triggerUpload" @dragover.prevent @drop.prevent="onDrop">
    <input ref="fileInput" type="file" hidden :accept="accept" @change="onFileChange" />
    <el-icon size="28" class="upload-icon"><UploadFilled /></el-icon>
    <p class="upload-title">{{ t('mediaLibrary.uploadTitle') }}</p>
    <p class="upload-hint">{{ t('mediaLibrary.uploadHint') }}</p>
    <el-progress v-if="uploading" :percentage="progress" :stroke-width="4" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { mediaApi } from '@/api/media/media'

const { t } = useI18n()
const emit = defineEmits<{ uploaded: [] }>()

const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const progress = ref(0)
const accept = 'image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,audio/mpeg,audio/wav'

function triggerUpload() {
  if (!uploading.value) fileInput.value?.click()
}

async function uploadFile(file: File) {
  uploading.value = true
  progress.value = 10
  try {
    progress.value = 60
    await mediaApi.upload(file)
    progress.value = 100
    ElNotification.success({ title: t('common.success'), message: t('mediaLibrary.uploadSuccess') })
    emit('uploaded')
  } catch (e: unknown) {
    ElNotification.error({
      title: t('common.error'),
      message: e instanceof Error ? e.message : t('mediaLibrary.uploadFailed')
    })
  } finally {
    uploading.value = false
    progress.value = 0
    if (fileInput.value) fileInput.value.value = ''
  }
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) uploadFile(file)
}

function onDrop(e: DragEvent) {
  const file = e.dataTransfer?.files?.[0]
  if (file) uploadFile(file)
}
</script>

<style scoped>
.upload-zone {
  border: 2px dashed var(--el-border-color);
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  background: var(--el-bg-color);
}

.upload-zone:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.upload-icon {
  color: var(--el-color-primary);
  margin-bottom: 6px;
}

.upload-title {
  margin: 0 0 2px;
  font-weight: 600;
  font-size: 14px;
}

.upload-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.el-progress {
  margin-top: 12px;
}
</style>
