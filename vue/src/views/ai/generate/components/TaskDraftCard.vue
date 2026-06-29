<template>
  <div class="task-draft-card" :class="{ expired: draft.status === 'expired', confirmed: draft.status === 'confirmed' }">
    <div class="card-header">
      <span class="workflow-name">{{ draft.workflowName }}</span>
      <span class="credits">{{ t('generate.draft.credits', { n: draft.creditsDeducted }) }}</span>
    </div>
    <p class="summary">{{ draft.summary }}</p>
    <div class="params">
      <div v-for="(node, idx) in draft.nodeContainer" :key="idx" class="param-row">
        <span class="param-label">{{ node.tips || node.inputs }}</span>
        <template v-if="node.isUpload && isImageUrl(node.nodeValue)">
          <img :src="node.nodeValue" class="thumb" alt="" />
        </template>
        <span v-else class="param-value">{{ truncate(node.nodeValue) }}</span>
      </div>
    </div>
    <div v-if="draft.status === 'pending'" class="actions">
      <button class="btn secondary" @click="$emit('modify', draft)">{{ t('generate.draft.modify') }}</button>
      <button class="btn primary" :disabled="confirming" @click="handleConfirm">
        {{ confirming ? t('common.loading') : t('generate.draft.confirm') }}
      </button>
    </div>
    <div v-else-if="draft.status === 'confirmed'" class="status-tag confirmed">{{ t('generate.draft.confirmed') }}</div>
    <div v-else class="status-tag expired">{{ t('generate.draft.expired') }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { generationApi } from '@/api/generation/generation'
import type { TaskDraftApi } from '@/api/generation/types'

const props = defineProps<{
  draft: TaskDraftApi.Draft
  sessionId: string
}>()

const emit = defineEmits<{
  confirm: [payload: { taskId: string; draftId: string; workflowName: string }]
  modify: [draft: TaskDraftApi.Draft]
}>()

const { t } = useI18n()
const confirming = ref(false)

const truncate = (v: string, max = 120) => {
  if (!v) return ''
  return v.length > max ? v.slice(0, max) + '…' : v
}

const isImageUrl = (url: string) => /\.(jpg|jpeg|png|webp|gif)(\?|$)/i.test(url)

const handleConfirm = async () => {
  confirming.value = true
  try {
    const res = await generationApi.reqConfirmDraft({
      sessionId: props.sessionId,
      draftId: props.draft.draftId
    })
    ElMessage.success(t('generate.draft.submitSuccess'))
    emit('confirm', {
      taskId: res.taskId,
      draftId: props.draft.draftId,
      workflowName: props.draft.workflowName
    })
  } catch (e: any) {
    ElMessage.error(e?.message || t('generate.draft.submitFailed'))
  } finally {
    confirming.value = false
  }
}
</script>

<style scoped>
.task-draft-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  padding: 18px;
  background: var(--el-fill-color-blank);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.workflow-name { font-weight: 600; }
.credits { font-size: 12px; color: var(--el-text-color-secondary); }
.summary { margin: 0 0 12px; color: var(--el-text-color-regular); }
.param-row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 6px;
}
.param-label { color: var(--el-text-color-secondary); min-width: 80px; flex-shrink: 0; }
.param-value { word-break: break-all; }
.thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 6px; }
.actions { display: flex; gap: 8px; margin-top: 12px; justify-content: flex-end; }
.btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}
.btn.primary {
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
  color: #fff;
}
.btn.secondary { background: var(--el-fill-color); color: var(--el-text-color-primary); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.status-tag { margin-top: 12px; font-size: 13px; }
.status-tag.confirmed { color: var(--el-color-success); }
.status-tag.expired { color: var(--el-text-color-secondary); }
.task-draft-card.confirmed { opacity: 0.85; }
.task-draft-card.expired { opacity: 0.6; }
</style>
