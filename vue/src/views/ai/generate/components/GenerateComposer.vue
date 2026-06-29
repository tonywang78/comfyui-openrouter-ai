<template>
  <div class="generate-composer">
    <div class="composer-card">
      <!-- 附件区：在输入卡片内部，避免被挤出视口 -->
      <div v-if="uploading || attachedMeta.length" class="attachments-panel">
        <div class="attachments-list">
          <div
            v-for="(item, i) in attachedMeta"
            :key="item.url + i"
            class="attachment-item"
            :class="item.kind"
          >
            <img v-if="item.kind === 'image'" :src="item.url" class="thumb" alt="" />
            <div v-else class="file-badge">
              <span class="ext">{{ (item.kind || 'file').toUpperCase() }}</span>
              <span class="name">{{ item.filename || item.kind }}</span>
            </div>
            <button class="remove" type="button" @click="removeAttachment(i)" aria-label="移除">×</button>
          </div>
        </div>
        <div v-if="uploading" class="upload-hint">{{ t('common.loading') }}</div>
      </div>

      <textarea
        ref="inputRef"
        v-model="text"
        class="message-input"
        :placeholder="t('generate.composer.placeholder')"
        rows="1"
        :disabled="disabled"
        @input="autoResize"
        @keydown="onKeydown"
      />

      <div class="composer-toolbar">
        <button
          type="button"
          class="tool-btn attach"
          :disabled="disabled || uploading"
          @click="pickFile"
          :title="t('generate.composer.attach')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
          </svg>
        </button>

        <label class="web-toggle" :class="{ active: webSearchLocal }">
          <input type="checkbox" v-model="webSearchLocal" :disabled="disabled" />
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
          </svg>
          <span>{{ t('generate.composer.webSearch') }}</span>
        </label>

        <div class="toolbar-spacer" />

        <button
          type="button"
          class="send-btn"
          :class="{ stopping: isTyping }"
          :disabled="!isTyping && disabled || (!isTyping && !text.trim() && !attachedMeta.length)"
          @click="isTyping ? $emit('stop') : send()"
        >
          <span v-if="isTyping" class="stop-icon">■</span>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M12 19V5M5 12l7-7 7 7"/>
          </svg>
        </button>
      </div>

      <input ref="fileRef" type="file" accept="image/*,video/*,audio/*,.pdf" multiple hidden @change="onFiles" />
    </div>
    <p class="composer-hint">{{ t('generate.composer.hint') }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElNotification } from 'element-plus'
import { ossApi } from '@/api/oss/oss'

const props = defineProps<{
  disabled?: boolean
  isTyping?: boolean
  webSearchEnabled?: boolean
}>()

const emit = defineEmits<{
  send: [payload: {
    content: string
    imageUrls?: string[]
    attachments?: { url: string; filename?: string; mime?: string; kind?: string }[]
  }]
  stop: []
  'websearch-change': [enabled: boolean]
}>()

const { t } = useI18n()
const text = ref('')
const attachedMeta = ref<{ url: string; filename?: string; mime?: string; kind?: string }[]>([])
const fileRef = ref<HTMLInputElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const uploading = ref(false)
const webSearchLocal = ref(props.webSearchEnabled ?? false)

watch(webSearchLocal, (v) => emit('websearch-change', v))
watch(() => props.webSearchEnabled, (v) => {
  if (v !== undefined) webSearchLocal.value = v
})

const autoResize = () => {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

const pickFile = () => fileRef.value?.click()

const kindFromFile = (file: File) => {
  if (file.type.startsWith('image/')) return 'image'
  if (file.type.startsWith('video/')) return 'video'
  if (file.type.startsWith('audio/')) return 'audio'
  if (file.type === 'application/pdf') return 'pdf'
  return 'file'
}

const onFiles = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files?.length) return
  uploading.value = true
  try {
    for (const file of Array.from(files)) {
      if (attachedMeta.value.length >= 8) {
        ElNotification.warning({ message: t('generate.composer.maxAttachments') })
        break
      }
      const url = await ossApi.uploadFile({ file })
      attachedMeta.value.push({
        url,
        filename: file.name,
        mime: file.type,
        kind: kindFromFile(file)
      })
    }
  } catch {
    ElNotification.error({ message: t('generate.composer.uploadFailed') })
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const removeAttachment = (idx: number) => {
  attachedMeta.value.splice(idx, 1)
}

const send = () => {
  const content = text.value.trim()
  if (!content && !attachedMeta.value.length) return
  emit('send', {
    content: content || t('generate.composer.attachmentOnly'),
    imageUrls: attachedMeta.value.filter(a => a.kind === 'image').map(a => a.url),
    attachments: [...attachedMeta.value]
  })
  text.value = ''
  attachedMeta.value = []
  nextTick(autoResize)
}

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    if (!props.isTyping) send()
  }
}

const fillText = (value: string) => {
  text.value = value
  nextTick(autoResize)
}

defineExpose({ fillText })
</script>

<style scoped>
.generate-composer {
  width: 100%;
}

.composer-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 20px;
  padding: 12px 14px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.composer-card:focus-within {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 4px 28px rgba(64, 158, 255, 0.12), 0 2px 8px rgba(0, 0, 0, 0.04);
}

.attachments-panel {
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.attachments-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 120px;
  overflow-y: auto;
}

.attachment-item {
  position: relative;
  flex-shrink: 0;
}

.attachment-item .thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
}

.file-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 64px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  padding: 4px;
  gap: 2px;
}

.file-badge .ext { font-size: 10px; font-weight: 700; color: var(--el-color-primary); }
.file-badge .name {
  font-size: 9px;
  color: var(--el-text-color-secondary);
  max-width: 64px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove {
  position: absolute;
  top: -5px;
  right: -5px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: var(--el-text-color-primary);
  color: var(--el-bg-color);
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}

.message-input {
  width: 100%;
  min-height: 24px;
  max-height: 160px;
  resize: none;
  border: none;
  outline: none;
  padding: 4px 4px 8px;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.5;
  background: transparent;
  color: var(--el-text-color-primary);
}

.message-input::placeholder { color: var(--el-text-color-placeholder); }
.message-input:disabled { opacity: 0.6; cursor: not-allowed; }

.composer-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.tool-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-regular);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.tool-btn:hover:not(:disabled) {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.tool-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.web-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  font-size: 13px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
}

.web-toggle input { display: none; }

.web-toggle.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.toolbar-spacer { flex: 1; }

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.send-btn:hover:not(:disabled) { transform: scale(1.04); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; transform: none; }
.send-btn.stopping { background: var(--el-color-danger); }
.stop-icon { font-size: 14px; line-height: 1; }

.composer-hint {
  margin: 8px 0 0;
  text-align: center;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
</style>
