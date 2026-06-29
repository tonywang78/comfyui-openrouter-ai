<template>
  <div class="workflow-picker">
    <div class="chips-row">
      <span
        v-for="item in modelValue"
        :key="item.workflowId"
        class="chip selected"
      >
        <span class="icon">⚙</span>
        <span class="name">{{ item.name }}</span>
        <span v-if="item.creditsDeducted != null" class="credits">
          {{ item.creditsDeducted }} {{ t('generate.workflow.creditsUnit') }}
        </span>
        <button
          type="button"
          class="remove"
          :aria-label="t('generate.workflow.remove')"
          @click.stop="remove(item.workflowId)"
        >
          ×
        </button>
      </span>

      <button type="button" class="chip add" @click="openPicker">
        <span class="icon">{{ modelValue.length ? '+' : '⚙' }}</span>
        <span>{{ modelValue.length ? t('generate.workflow.addMore') : t('generate.workflow.select') }}</span>
      </button>
    </div>

    <el-dialog
      v-model="showPicker"
      :title="t('generate.workflow.selectTitle')"
      width="520px"
      append-to-body
      align-center
      destroy-on-close
      :z-index="3000"
      class="workflow-picker-dialog"
      @opened="loadWorkflows"
    >
      <el-input
        v-model="keyword"
        :placeholder="t('generate.workflow.searchPlaceholder')"
        clearable
        class="search-input"
        @input="searchWorkflows"
      />
      <p v-if="modelValue.length" class="pinned-hint">
        {{ t('generate.workflow.pinnedCount', { n: modelValue.length }) }}
      </p>
      <div v-loading="loading" class="workflow-list">
        <button
          v-for="item in workflows"
          :key="item.workflowId"
          type="button"
          class="workflow-item"
          :class="{ active: isPinned(item.workflowId) }"
          @click="toggle(item)"
        >
          <div class="item-main">
            <div class="name-row">
              <span class="name">{{ item.name }}</span>
              <span v-if="isPinned(item.workflowId)" class="check">✓</span>
            </div>
            <div v-if="item.description" class="desc">{{ item.description }}</div>
          </div>
          <div class="meta">
            <span v-if="item.categoryName" class="tag">{{ item.categoryName }}</span>
          </div>
        </button>
        <div v-if="!loading && !workflows.length" class="empty">{{ t('generate.workflow.empty') }}</div>
        <div v-if="loadError" class="error">{{ loadError }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { comfyuiTaskApi } from '@/api/workflow-task/workflow-task'
import type { GenerationWorkflowApi } from '@/api/generation/types'
import type { PinnedWorkflow } from '../types'

const MAX_PINNED = 5

const props = defineProps<{
  modelValue: PinnedWorkflow[]
}>()

const emit = defineEmits<{
  'update:modelValue': [workflows: PinnedWorkflow[]]
}>()

const { t } = useI18n()
const showPicker = ref(false)
const keyword = ref('')
const workflows = ref<GenerationWorkflowApi.Item[]>([])
const loading = ref(false)
const loadError = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const isPinned = (workflowId: number) =>
  props.modelValue.some(w => w.workflowId === workflowId)

const openPicker = () => {
  showPicker.value = true
}

const loadWorkflows = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await comfyuiTaskApi.reqGetWorkflowResultModelflowsPage({
      page: '1',
      prompt: keyword.value || undefined
    })
    workflows.value = (res.items || []).map(item => ({
      workflowId: item.workflowId,
      name: item.name,
      description: item.description,
      categoryName: item.categoryName
    }))
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : t('generate.workflow.loadFailed')
    loadError.value = msg
    workflows.value = []
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const searchWorkflows = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(loadWorkflows, 300)
}

const enrichItem = async (item: GenerationWorkflowApi.Item): Promise<PinnedWorkflow> => {
  try {
    const detail = await comfyuiTaskApi.reqGetWorkflowResultModelflowsInterface({
      workflowId: item.workflowId
    })
    return {
      workflowId: item.workflowId,
      name: item.name,
      creditsDeducted: detail.creditsDeducted
    }
  } catch {
    return { workflowId: item.workflowId, name: item.name }
  }
}

const toggle = async (item: GenerationWorkflowApi.Item) => {
  if (isPinned(item.workflowId)) {
    emit('update:modelValue', props.modelValue.filter(w => w.workflowId !== item.workflowId))
    return
  }
  if (props.modelValue.length >= MAX_PINNED) {
    ElMessage.warning({ message: t('generate.workflow.maxPinned', { n: MAX_PINNED }) })
    return
  }
  const pinned = await enrichItem(item)
  emit('update:modelValue', [...props.modelValue, pinned])
}

const remove = (workflowId: number) => {
  emit('update:modelValue', props.modelValue.filter(w => w.workflowId !== workflowId))
}
</script>

<style scoped>
.chips-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.15s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  max-width: 240px;
}

.chip.selected {
  border-color: var(--el-color-primary-light-3);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.chip.add {
  cursor: pointer;
  color: var(--el-text-color-secondary);
}

.chip.add:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.chip .name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credits {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.remove {
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  opacity: 0.65;
  flex-shrink: 0;
}

.remove:hover { opacity: 1; }

.search-input { margin-bottom: 4px; }

.pinned-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.workflow-list {
  max-height: 400px;
  overflow-y: auto;
  margin-top: 12px;
  min-height: 120px;
}

.workflow-item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 8px;
  background: var(--el-fill-color-blank);
  text-align: left;
  transition: all 0.15s ease;
}

.workflow-item:hover,
.workflow-item.active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.item-main { flex: 1; min-width: 0; }

.name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.name { font-weight: 600; font-size: 14px; color: var(--el-text-color-primary); }

.check {
  color: var(--el-color-primary);
  font-weight: 700;
  flex-shrink: 0;
}

.desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta { flex-shrink: 0; }

.tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
}

.empty, .error {
  text-align: center;
  padding: 32px 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.error { color: var(--el-color-danger); }
</style>
