import { watch, onMounted } from 'vue'
import { useGenerationStore } from '@/stores/modules/generation'
import { useTaskWebSocketStore } from '@/stores/modules/taskWebsocket'
import { comfyuiTaskApi } from '@/api/workflow-task/workflow-task'
import { WorkflowTaskStatusEnum } from '@/enums/workflow'
import type { TaskUpdate } from '../types'

export function useSessionTasks() {
  const generationStore = useGenerationStore()
  const taskStore = useTaskWebSocketStore()

  const syncTaskToMessages = (task: TaskUpdate) => {
    const sid = generationStore.activeSessionId
    if (!sid) return
    const session = generationStore.activeSession
    if (!session?.taskIds.includes(task.taskId)) return
    generationStore.upsertTaskStatus(sid, task)
  }

  const hydrateRunningTasks = async () => {
    const sid = generationStore.activeSessionId
    if (!sid) return
    const session = generationStore.activeSession
    if (!session?.taskIds.length) return

    try {
      const res = await comfyuiTaskApi.reqGetComfyuiTaskProgressPage({ page: 1 })
      for (const item of res.items || []) {
        if (session.taskIds.includes(item.taskId)) {
          generationStore.upsertTaskStatus(sid, {
            taskId: item.taskId,
            workflowName: item.workflowName,
            status: item.status,
            progress: item.progress ?? 0,
            location: item.location,
            workflowResultModel: item.workflowResultModel
          })
        }
      }
    } catch {
      // ignore
    }
  }

  watch(
    () => taskStore.tasks,
    (tasks) => {
      const sid = generationStore.activeSessionId
      if (!sid) return
      const session = generationStore.activeSession
      if (!session) return
      for (const task of tasks) {
        if (session.taskIds.includes(task.taskId)) {
          syncTaskToMessages({
            taskId: task.taskId,
            workflowName: task.workflowName,
            status: task.status,
            progress: task.progress ?? 0,
            location: task.location,
            workflowResultModel: task.workflowResultModel
          })
        }
      }
    },
    { deep: true }
  )

  watch(
    () => generationStore.activeSessionId,
    () => {
      hydrateRunningTasks()
    }
  )

  onMounted(() => {
    hydrateRunningTasks()
  })

  const registerTask = (taskId: string, workflowName: string) => {
    const sid = generationStore.activeSessionId
    if (!sid) return
    generationStore.addTaskId(sid, taskId)
    generationStore.upsertTaskStatus(sid, {
      taskId,
      workflowName,
      status: WorkflowTaskStatusEnum.WAIT,
      progress: 0
    })
  }

  return { registerTask, hydrateRunningTasks, syncTaskToMessages }
}
