<template>
  <el-dialog
    v-model="dialogVisible"
    :title="t('mediaLibrary.createTaskTitle')"
    width="480px"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item :label="t('mediaLibrary.selectWorkflow')">
        <el-select v-model="workflowId" filterable :loading="loadingWorkflows" style="width: 100%">
          <el-option
            v-for="wf in workflows"
            :key="wf.workflowId"
            :label="wf.name"
            :value="wf.workflowId"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!workflowId" @click="submit">
        {{ t('mediaLibrary.submitTask') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElNotification } from 'element-plus'
import { comfyuiTaskApi } from '@/api/workflow-task/workflow-task'
import { mediaApi } from '@/api/media/media'
import type { MediaApi } from '@/api/media/types'
import { useTaskWebSocketStore } from '@/stores'

const props = defineProps<{
  visible: boolean
  media: MediaApi.UserMediaVo
  variantId?: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  submitted: []
}>()

const { t } = useI18n()
const taskStore = useTaskWebSocketStore()

const dialogVisible = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v)
})

const workflows = ref<{ workflowId: number; name: string }[]>([])
const workflowId = ref<number>()
const loadingWorkflows = ref(false)
const submitting = ref(false)

watch(() => props.visible, (v) => {
  if (v) loadWorkflows()
})

async function loadWorkflows() {
  loadingWorkflows.value = true
  try {
    const res = await comfyuiTaskApi.reqGetWorkflowResultModelflowsPage({ page: '1' })
    workflows.value = (res.items || []).map(w => ({
      workflowId: w.workflowId,
      name: w.name
    }))
  } finally {
    loadingWorkflows.value = false
  }
}

async function submit() {
  if (!workflowId.value) return
  submitting.value = true
  try {
    await mediaApi.createTask({
      mediaId: props.media.mediaId,
      variantId: props.variantId,
      workflowId: workflowId.value
    })
    ElNotification.success({ message: t('mediaLibrary.taskSubmitted') })
    taskStore.refreshTasks()
    dialogVisible.value = false
    emit('submitted')
  } catch (e: unknown) {
    ElNotification.error({ message: e instanceof Error ? e.message : t('common.error') })
  } finally {
    submitting.value = false
  }
}
</script>
