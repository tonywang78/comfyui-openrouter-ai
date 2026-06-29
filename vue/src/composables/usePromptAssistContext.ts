import { computed, type ComputedRef, type Ref } from 'vue'
import { WorkflowFormTypeEnum } from '@/enums'
import type { GetWorkflowInterfaceApi } from '@/api/workflow-task/types'

const IMAGE_FIELD_TYPES = [
  WorkflowFormTypeEnum.IMAGE_UPLOAD,
  WorkflowFormTypeEnum.IMAGE_SCRIBBLE
] as const

export function getFieldKey(formItem: { nodeKey: string; inputs: string }) {
  return `${formItem.nodeKey}_${formItem.inputs}`
}

export function usePromptAssistContext(
  formData: Ref<GetWorkflowInterfaceApi.Result | null>,
  formValues: ComputedRef<Record<string, unknown>>
) {
  const imageFieldKeys = computed(() => {
    if (!formData.value) return [] as string[]
    return formData.value.formContainer
      .filter(item => IMAGE_FIELD_TYPES.includes(item.type as typeof IMAGE_FIELD_TYPES[number]))
      .map(item => getFieldKey(item))
  })

  const resolveImageUrls = (
    promptImageRefs?: string[] | null,
    enabledRefs?: string[] | null
  ): string[] => {
    const refs = (promptImageRefs?.length ? promptImageRefs : imageFieldKeys.value)
    const activeRefs = enabledRefs?.length ? enabledRefs : refs
    const urls: string[] = []
    for (const key of activeRefs) {
      const value = formValues.value[key]
      if (typeof value === 'string' && value.trim()) {
        urls.push(value.trim())
      }
    }
    return urls
  }

  const listImageCandidates = (_promptImageRefs?: string[] | null) => {
    if (!formData.value) return [] as { key: string; label: string; url: string }[]

    const labelMap = new Map(
      formData.value.formContainer.map(item => [getFieldKey(item), item.tips || item.nodeKey])
    )

    return imageFieldKeys.value.map(key => {
      const value = formValues.value[key]
      const url = typeof value === 'string' ? value.trim() : ''
      return {
        key,
        label: labelMap.get(key) || key,
        url
      }
    })
  }

  return {
    imageFieldKeys,
    resolveImageUrls,
    listImageCandidates,
    getFieldKey
  }
}
