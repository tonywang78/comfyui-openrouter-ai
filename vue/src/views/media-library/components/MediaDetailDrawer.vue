<template>
  <el-drawer
    :model-value="visible"
    :title="media?.name || t('mediaLibrary.detailTitle')"
    size="420px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="media" class="detail-body">
      <div class="preview">
        <img v-if="media.mediaType === 'IMAGE'" :src="previewUrl" alt="" />
        <video v-else-if="media.mediaType === 'VIDEO'" :src="previewUrl" controls />
      </div>

      <section class="section">
        <h4>{{ t('mediaLibrary.standardization') }}</h4>
        <div class="variant-list">
          <div
            v-for="variant in media.variants || []"
            :key="variant.variantId"
            class="variant-row"
            :class="{ active: selectedVariantId === variant.variantId }"
            @click="selectVariant(variant)"
          >
            <span>{{ variantLabel(variant.variantType) }}</span>
            <el-tag size="small" :type="statusTagType(variant.status)">{{ statusLabel(variant.status) }}</el-tag>
          </div>
        </div>
        <div class="variant-actions" v-if="media.mediaType === 'IMAGE'">
          <el-button
            size="small"
            :loading="builtinLoading"
            :disabled="hasSucceededVariant('HEADSHOT_BUILTIN')"
            @click="runBuiltin"
          >
            {{ t('mediaLibrary.builtinHeadshot') }}
          </el-button>
          <el-button
            size="small"
            type="primary"
            :loading="comfyuiLoading"
            :disabled="hasProcessingVariant('HEADSHOT_COMFYUI')"
            @click="runComfyui"
          >
            {{ t('mediaLibrary.comfyuiHeadshot') }}
          </el-button>
        </div>
      </section>

      <section class="section">
        <el-button type="primary" @click="showCreateTask = true">
          {{ t('mediaLibrary.createTask') }}
        </el-button>
      </section>
    </div>

    <CreateTaskDialog
      v-if="media"
      v-model:visible="showCreateTask"
      :media="media"
      :variant-id="selectedVariantId"
      @submitted="$emit('refresh')"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElNotification } from 'element-plus'
import { mediaApi } from '@/api/media/media'
import type { MediaApi } from '@/api/media/types'
import CreateTaskDialog from './CreateTaskDialog.vue'

const props = defineProps<{
  visible: boolean
  media: MediaApi.UserMediaVo | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  refresh: []
}>()

const { t } = useI18n()
const builtinLoading = ref(false)
const comfyuiLoading = ref(false)
const showCreateTask = ref(false)
const selectedVariantId = ref<number | undefined>()

const previewUrl = computed(() => {
  if (!props.media) return ''
  if (selectedVariantId.value) {
    const v = props.media.variants?.find(x => x.variantId === selectedVariantId.value)
    if (v?.url) return v.url
  }
  return props.media.url
})

watch(() => props.media, () => {
  selectedVariantId.value = undefined
})

function variantLabel(type: string) {
  if (type === 'HEADSHOT_BUILTIN') return t('mediaLibrary.builtinHeadshot')
  if (type === 'HEADSHOT_COMFYUI') return t('mediaLibrary.comfyuiHeadshot')
  return type
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: t('mediaLibrary.statusPending'),
    PROCESSING: t('mediaLibrary.statusProcessing'),
    SUCCEEDED: t('mediaLibrary.statusSucceeded'),
    FAILED: t('mediaLibrary.statusFailed')
  }
  return map[status] || status
}

function statusTagType(status: string) {
  if (status === 'SUCCEEDED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PROCESSING') return 'warning'
  return 'info'
}

function hasSucceededVariant(type: string) {
  return props.media?.variants?.some(v => v.variantType === type && v.status === 'SUCCEEDED')
}

function hasProcessingVariant(type: string) {
  return props.media?.variants?.some(v => v.variantType === type && v.status === 'PROCESSING')
}

function selectVariant(variant: MediaApi.MediaVariantVo) {
  if (variant.status === 'SUCCEEDED' && variant.url) {
    selectedVariantId.value = variant.variantId
  }
}

async function runBuiltin() {
  if (!props.media) return
  builtinLoading.value = true
  try {
    await mediaApi.createBuiltinVariant(props.media.mediaId)
    ElNotification.success({ message: t('mediaLibrary.builtinDone') })
    emit('refresh')
  } catch (e: unknown) {
    ElNotification.error({ message: e instanceof Error ? e.message : t('common.error') })
  } finally {
    builtinLoading.value = false
  }
}

async function runComfyui() {
  if (!props.media) return
  comfyuiLoading.value = true
  try {
    await mediaApi.createComfyuiVariant({
      mediaId: props.media.mediaId,
      variantId: selectedVariantId.value
    })
    ElNotification.success({ message: t('mediaLibrary.comfyuiStarted') })
    emit('refresh')
  } catch (e: unknown) {
    ElNotification.error({ message: e instanceof Error ? e.message : t('common.error') })
  } finally {
    comfyuiLoading.value = false
  }
}
</script>

<style scoped>
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview {
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-light);
}

.preview img,
.preview video {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  display: block;
}

.section h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.variant-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.variant-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
}

.variant-row.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.variant-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
