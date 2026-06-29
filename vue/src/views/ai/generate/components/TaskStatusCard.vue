<template>
  <div class="task-status-card">
    <div class="card-header">
      <TaskStatusIcon :status="status" />
      <span class="workflow-name">{{ workflowName }}</span>
      <span class="status-text" :class="status.toLowerCase()">{{ statusLabel }}</span>
    </div>

    <div v-if="status === WorkflowTaskStatusEnum.WAIT" class="queue-info">
      <template v-if="location && location > 0">{{ t('generate.task.queue', { n: location }) }}</template>
      <template v-else>{{ t('task.joining') }}</template>
    </div>

    <div v-if="status === WorkflowTaskStatusEnum.BUILD" class="progress-wrap">
      <div class="progress-bar">
        <div class="progress-inner" :style="{ width: `${progress}%` }"></div>
      </div>
      <span class="progress-text">{{ progress }}%</span>
    </div>

    <div v-if="status === WorkflowTaskStatusEnum.SUCCEED && workflowResultModel" class="result">
      <img
        v-if="workflowResultModel.type === WorkflowResultModelTypeEnum.IMAGE"
        :src="workflowResultModel.url"
        class="result-thumb"
        alt=""
      />
      <button class="btn primary" @click="openWorkDetail">{{ t('generate.task.viewWork') }}</button>
    </div>

    <div v-if="status === WorkflowTaskStatusEnum.FAILED || status === WorkflowTaskStatusEnum.CANCELED" class="actions">
      <button class="btn secondary" :disabled="remaking" @click="handleRemake">
        {{ remaking ? t('common.loading') : t('generate.task.remake') }}
      </button>
    </div>

    <WorkDetailDialog ref="workDetailRef" v-model:visible="showWork" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import TaskStatusIcon from '@/components/common/TaskStatusIcon.vue'
import WorkDetailDialog from '@/components/common/WorkDetailDialog.vue'
import { comfyuiTaskApi } from '@/api/workflow-task/workflow-task'
import { WorkflowTaskStatusEnum, WorkflowResultModelTypeEnum } from '@/enums/workflow'
import type { WorkflowResultModel } from '@/api/generation/types'

const props = defineProps<{
  taskId: string
  workflowName: string
  status: WorkflowTaskStatusEnum
  progress: number
  location?: number
  workflowResultModel?: WorkflowResultModel
}>()

const emit = defineEmits<{ remake: [taskId: string] }>()

const { t } = useI18n()
const showWork = ref(false)
const workDetailRef = ref<InstanceType<typeof WorkDetailDialog> | null>(null)
const remaking = ref(false)

const statusLabel = computed(() => {
  const map: Record<string, string> = {
    [WorkflowTaskStatusEnum.WAIT]: t('task.waiting'),
    [WorkflowTaskStatusEnum.BUILD]: t('task.building'),
    [WorkflowTaskStatusEnum.SUCCEED]: t('task.completed'),
    [WorkflowTaskStatusEnum.FAILED]: t('task.failed'),
    [WorkflowTaskStatusEnum.CANCELED]: t('task.canceled')
  }
  return map[props.status] || props.status
})

const openWorkDetail = () => {
  if (props.workflowResultModel?.workflowResultId && workDetailRef.value) {
    workDetailRef.value.fetchWorkDetail(props.workflowResultModel.workflowResultId)
    showWork.value = true
  }
}

const handleRemake = async () => {
  remaking.value = true
  try {
    await comfyuiTaskApi.reqRemakeComfyuiTask({ taskId: props.taskId })
    ElMessage.success(t('generate.task.remakeSuccess'))
    emit('remake', props.taskId)
  } catch (e: any) {
    ElMessage.error(e?.message || t('generate.task.remakeFailed'))
  } finally {
    remaking.value = false
  }
}
</script>

<style scoped>
.task-status-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  padding: 14px;
  background: var(--el-fill-color-blank);
  margin-top: 8px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.workflow-name { font-weight: 600; flex: 1; }
.status-text { font-size: 12px; }
.progress-wrap { display: flex; align-items: center; gap: 10px; }
.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--el-fill-color);
  border-radius: 3px;
  overflow: hidden;
}
.progress-inner {
  height: 100%;
  background: var(--el-color-primary);
  transition: width 0.3s;
}
.progress-text { font-size: 12px; min-width: 36px; }
.result { display: flex; align-items: center; gap: 12px; }
.result-thumb { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; }
.btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 13px;
}
.btn.primary { background: var(--el-color-primary); color: #fff; }
.btn.secondary { background: var(--el-fill-color); }
.queue-info { font-size: 13px; color: var(--el-text-color-secondary); }
</style>
