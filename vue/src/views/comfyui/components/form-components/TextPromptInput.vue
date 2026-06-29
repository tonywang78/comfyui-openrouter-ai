<template>
  <div class="text-prompt-input">
    <el-form-item
      :prop="fieldKey"
      :required="formItem.required"
      class="custom-form-item"
    >
      <template #label>
        <div class="label-with-tooltip">
          <span>{{ formItem.tips }}</span>
          <el-tooltip v-if="formItem.template" :content="formItem.template" placement="top">
            <el-icon class="tooltip-icon">
              <QuestionFilled />
            </el-icon>
          </el-tooltip>
        </div>
      </template>
      <div class="textarea-wrapper">
        <el-input
          :model-value="modelValue"
          @update:model-value="handleInput"
          type="textarea"
          :rows="4"
          :placeholder="getPlaceholder()"
          resize="vertical"
          :maxlength="formItem.size || 2000"
          show-word-limit
          class="prompt-textarea"
        />
        <el-popover
          v-if="assistEnabled"
          v-model:visible="popoverVisible"
          placement="bottom-end"
          :width="420"
          trigger="click"
          popper-class="prompt-assist-popover"
        >
          <template #reference>
            <el-button
              class="assist-btn"
              type="primary"
              link
              :icon="MagicStick"
            >
              {{ t('comfyui.promptAssist.title') }}
            </el-button>
          </template>

          <div class="assist-panel">
            <div class="assist-header">
              <span class="assist-title">{{ t('comfyui.promptAssist.title') }}</span>
              <el-tag size="small" type="info">{{ styleLabel }}</el-tag>
            </div>

            <p class="assist-hint">{{ t('comfyui.promptAssist.privacyHint') }}</p>

            <el-input
              v-model="draftText"
              type="textarea"
              :rows="3"
              :placeholder="t('comfyui.promptAssist.draftPlaceholder')"
            />

            <div v-if="allImageSlots.length" class="image-section">
              <div class="image-section-header">
                <span>{{ t('comfyui.promptAssist.referenceImages') }}</span>
                <div class="image-section-actions">
                  <span class="image-count">{{ selectedCount }}/{{ uploadedCount }}</span>
                  <el-button
                    v-if="uploadedCount > 1"
                    link
                    type="primary"
                    size="small"
                    :disabled="allUploadedSelected"
                    @click="selectAllImages"
                  >
                    {{ t('comfyui.promptAssist.selectAll') }}
                  </el-button>
                </div>
              </div>
              <div class="image-strip">
                <div
                  v-for="img in allImageSlots"
                  :key="img.key"
                  class="image-chip"
                  :class="{
                    selected: isImageSelected(img.key),
                    empty: !img.url
                  }"
                >
                  <div
                    class="thumb-wrap"
                    :class="{ clickable: img.url && !isImageSelected(img.key) }"
                    @click="img.url && !isImageSelected(img.key) && addImage(img.key)"
                  >
                    <img v-if="img.url" :src="img.url" :alt="img.label" />
                    <div v-else class="empty-thumb">{{ t('comfyui.promptAssist.notUploaded') }}</div>
                    <button
                      v-if="img.url && isImageSelected(img.key)"
                      type="button"
                      class="remove-btn"
                      :title="t('comfyui.promptAssist.removeImage')"
                      @click.stop="removeImage(img.key)"
                    >
                      ×
                    </button>
                  </div>
                  <span class="chip-label">{{ img.label }}</span>
                  <el-button
                    v-if="img.url && !isImageSelected(img.key)"
                    type="primary"
                    link
                    size="small"
                    class="add-btn"
                    @click="addImage(img.key)"
                  >
                    {{ t('comfyui.promptAssist.addImage') }}
                  </el-button>
                </div>
              </div>
            </div>
            <p v-else class="no-image-hint">{{ t('comfyui.promptAssist.noImageHint') }}</p>

            <div class="assist-actions">
              <el-button
                type="primary"
                :loading="generating"
                @click="handleGenerate"
              >
                {{ t('comfyui.promptAssist.generate') }}
              </el-button>
            </div>

            <div v-if="resultPrompt" class="assist-result">
              <div class="result-block">
                <div class="result-label">{{ t('comfyui.promptAssist.suggestion') }}</div>
                <div class="result-text">{{ resultPrompt }}</div>
                <div v-if="resultExplanation" class="result-explanation">{{ resultExplanation }}</div>
              </div>
              <div class="result-actions">
                <el-button type="primary" size="small" @click="applyReplace">
                  {{ t('comfyui.promptAssist.replace') }}
                </el-button>
                <el-button size="small" @click="applyAppend">
                  {{ t('comfyui.promptAssist.append') }}
                </el-button>
                <el-button size="small" link @click="handleGenerate">
                  {{ t('comfyui.promptAssist.regenerate') }}
                </el-button>
              </div>
            </div>
          </div>
        </el-popover>
      </div>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElFormItem, ElInput, ElTooltip, ElIcon, ElButton, ElPopover, ElTag, ElNotification } from 'element-plus'
import { QuestionFilled, MagicStick } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { isPromptAssistEnabled } from '@/enums/promptStyle'
import { promptAssistApi } from '@/api/prompt-assist/prompt-assist'

interface ImageCandidate {
  key: string
  label: string
  url: string
}

interface Props {
  formItem: {
    inputs: string
    nodeKey: string
    tips: string
    type: string
    required: boolean
    options: string
    template?: string | null
    size?: number
    promptStyle?: string | null
    promptImageRefs?: string[] | null
  }
  modelValue: string
  workflowId?: number | null
  fieldKey: string
  imageCandidates?: ImageCandidate[]
  allImageSlots?: ImageCandidate[]
  resolveImageUrls?: () => string[]
}

const props = withDefaults(defineProps<Props>(), {
  workflowId: null,
  imageCandidates: () => [],
  allImageSlots: () => [],
  resolveImageUrls: () => []
})

const emit = defineEmits<{
  'update:model-value': [value: string]
}>()

const { t } = useI18n()

const popoverVisible = ref(false)
const draftText = ref('')
const generating = ref(false)
const resultPrompt = ref('')
const resultExplanation = ref('')
const enabledImageKeys = ref<string[]>([])
const userDisabledKeys = ref<Set<string>>(new Set())

const assistEnabled = computed(() => isPromptAssistEnabled(props.formItem.promptStyle))

const styleLabel = computed(() => {
  const style = props.formItem.promptStyle || 'NONE'
  return t('system.workflow.promptStyles.' + style)
})

const allImageSlots = computed(() => {
  if (props.allImageSlots?.length) return props.allImageSlots
  return props.imageCandidates || []
})

const uploadedCount = computed(() => allImageSlots.value.filter(img => !!img.url).length)

const selectedCount = computed(() =>
  allImageSlots.value.filter(img => img.url && enabledImageKeys.value.includes(img.key)).length
)

const allUploadedSelected = computed(() => {
  const uploaded = allImageSlots.value.filter(img => img.url)
  return uploaded.length > 0 && uploaded.every(img => enabledImageKeys.value.includes(img.key))
})

const isImageSelected = (key: string) => enabledImageKeys.value.includes(key)

const defaultEnabledKeys = () =>
  allImageSlots.value.filter(img => img.url).map(img => img.key)

watch(popoverVisible, (visible) => {
  if (visible) {
    draftText.value = props.modelValue || ''
    resultPrompt.value = ''
    resultExplanation.value = ''
    userDisabledKeys.value = new Set()
    enabledImageKeys.value = defaultEnabledKeys()
  }
})

watch(allImageSlots, (list, oldList) => {
  if (!popoverVisible.value) return

  const oldUrlMap = new Map((oldList || []).map(item => [item.key, item.url]))
  const nextKeys = [...enabledImageKeys.value]

  for (const img of list) {
    if (!img.url) continue
    const hadUrl = !!oldUrlMap.get(img.key)
    const shouldAutoEnable = !hadUrl && !userDisabledKeys.value.has(img.key)
    if (shouldAutoEnable && !nextKeys.includes(img.key)) {
      nextKeys.push(img.key)
    }
  }

  enabledImageKeys.value = nextKeys.filter(key => {
    const slot = list.find(img => img.key === key)
    return !!slot?.url
  })
}, { deep: true })

const handleInput = (value: string) => {
  emit('update:model-value', value)
}

const getPlaceholder = () => {
  if (props.formItem.tips) {
    return `${props.formItem.tips}`
  }
  return t('comfyui.promptAssist.defaultPlaceholder')
}

const removeImage = (key: string) => {
  enabledImageKeys.value = enabledImageKeys.value.filter(k => k !== key)
  userDisabledKeys.value.add(key)
}

const addImage = (key: string) => {
  if (!enabledImageKeys.value.includes(key)) {
    enabledImageKeys.value = [...enabledImageKeys.value, key]
  }
  userDisabledKeys.value.delete(key)
}

const selectAllImages = () => {
  enabledImageKeys.value = allImageSlots.value.filter(img => img.url).map(img => img.key)
  userDisabledKeys.value.clear()
}

const collectImageUrls = (): string[] => {
  const keys = new Set(enabledImageKeys.value)
  return allImageSlots.value
    .filter(img => img.url && keys.has(img.key))
    .map(img => img.url)
}

const handleGenerate = async () => {
  if (!props.workflowId) {
    ElNotification.error(t('comfyui.promptAssist.workflowMissing'))
    return
  }
  const draft = draftText.value.trim()
  const imageUrls = collectImageUrls()
  if (!draft && imageUrls.length === 0) {
    ElNotification.warning(t('comfyui.promptAssist.emptyInput'))
    return
  }

  try {
    generating.value = true
    const result = await promptAssistApi.enhance({
      workflowId: props.workflowId,
      fieldKey: props.fieldKey,
      draftText: draft || undefined,
      imageUrls: imageUrls.length ? imageUrls : undefined,
      promptStyle: props.formItem.promptStyle!
    })
    resultPrompt.value = result.prompt
    resultExplanation.value = result.explanation || ''
  } catch (e: any) {
    ElNotification.error(e?.message || t('comfyui.promptAssist.generateFailed'))
  } finally {
    generating.value = false
  }
}

const clampText = (text: string) => {
  const max = props.formItem.size || 2000
  if (text.length <= max) return text
  ElNotification.warning(t('comfyui.promptAssist.truncated', { max }))
  return text.slice(0, max)
}

const applyReplace = () => {
  if (!resultPrompt.value) return
  emit('update:model-value', clampText(resultPrompt.value))
  popoverVisible.value = false
}

const applyAppend = () => {
  if (!resultPrompt.value) return
  const base = props.modelValue?.trim()
  const merged = base ? `${base}, ${resultPrompt.value}` : resultPrompt.value
  emit('update:model-value', clampText(merged))
  popoverVisible.value = false
}
</script>

<style scoped>
.text-prompt-input {
  width: 100% !important;
  display: block;
}

.custom-form-item {
  margin-bottom: 0;
  width: 100% !important;
}

.custom-form-item :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
  font-size: 14px;
}

.label-with-tooltip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tooltip-icon {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  cursor: help;
  transition: color 0.3s ease;
}

.tooltip-icon:hover {
  color: var(--el-color-primary);
}

.custom-form-item :deep(.el-form-item__content) {
  width: 100% !important;
}

.textarea-wrapper {
  position: relative;
  width: 100% !important;
}

.assist-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
}

.prompt-textarea {
  width: 100% !important;
  display: block;
}

.prompt-textarea :deep(.el-textarea) {
  width: 100% !important;
  display: block;
}

.prompt-textarea :deep(.el-textarea__inner) {
  font-family: inherit;
  line-height: 1.6;
  resize: vertical;
  min-height: 100px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
  transition: all 0.3s ease;
  padding: 12px;
  padding-right: 88px;
  font-size: 14px;
  width: 100% !important;
  box-sizing: border-box;
  display: block;
}

.prompt-textarea :deep(.el-textarea__inner):focus {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
  outline: none;
}

.prompt-textarea :deep(.el-textarea__inner):hover {
  border-color: var(--el-border-color-hover);
}

.prompt-textarea :deep(.el-textarea__inner)::placeholder {
  color: var(--el-text-color-placeholder);
  font-style: italic;
}

.prompt-textarea :deep(.el-input__count) {
  background-color: var(--el-bg-color);
  color: var(--el-text-color-regular);
  border-radius: 4px 0 8px 0;
  padding: 2px 6px;
  font-size: 12px;
}

.assist-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assist-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.assist-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.assist-hint,
.no-image-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.image-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.image-section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.image-count {
  color: var(--el-text-color-placeholder);
}

.thumb-wrap.clickable {
  cursor: pointer;
}

.thumb-wrap.clickable:hover img {
  border-color: var(--el-color-primary-light-3);
}

.image-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 80px;
}

.thumb-wrap {
  position: relative;
  width: 64px;
  height: 64px;
}

.image-chip img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  border: 2px solid var(--el-border-color);
  display: block;
}

.image-chip.selected img {
  border-color: var(--el-color-primary);
}

.image-chip.empty img,
.empty-thumb {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  border: 2px dashed var(--el-border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color-lighter);
  text-align: center;
  padding: 4px;
  box-sizing: border-box;
}

.remove-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: var(--el-color-danger);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.chip-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  text-align: center;
  line-height: 1.2;
  word-break: break-all;
}

.add-btn {
  padding: 0;
  height: auto;
  font-size: 11px;
}

.assist-actions,
.result-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.assist-result {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.result-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.result-explanation {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (prefers-color-scheme: dark) {
  .prompt-textarea :deep(.el-textarea__inner) {
    background-color: var(--el-bg-color-page);
    border-color: var(--el-border-color-dark);
  }

  .prompt-textarea :deep(.el-textarea__inner):focus {
    box-shadow: 0 0 0 2px var(--el-color-primary-dark-2);
  }
}

.dark .prompt-textarea :deep(.el-textarea__inner) {
  background-color: var(--el-bg-color-page);
  border-color: var(--el-border-color-dark);
  color: var(--el-text-color-primary);
}

.dark .prompt-textarea :deep(.el-textarea__inner):focus {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-dark-2);
}
</style>
