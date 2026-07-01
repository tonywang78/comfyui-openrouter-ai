<template>
  <div class="cover-media-root">
    <img
      v-if="displaySrc && mediaType !== 'video'"
      :key="displaySrc"
      :src="displaySrc"
      :alt="alt"
      :class="['cover-media-el', mediaClass]"
      :loading="imgLoading"
      referrerpolicy="no-referrer"
      @load="handleLoad"
      @error="handleError"
    />
    <video
      v-else-if="displaySrc && mediaType === 'video'"
      :key="displaySrc"
      :src="displaySrc"
      :class="['cover-media-el', mediaClass]"
      autoplay
      muted
      loop
      playsinline
      preload="auto"
      @loadeddata="handleLoad"
      @error="handleError"
    />
    <div v-else-if="loading" class="cover-media-loading">
      <slot name="loading" />
    </div>
    <div v-if="hasError" class="cover-media-error">
      <slot name="error">{{ errorText }}</slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import {
  fetchOssVideoBlobUrl,
  getCoverMediaType,
  isOwnOssMediaUrl
} from '@/utils/coverMedia'

interface Props {
  src?: string
  alt?: string
  fit?: 'cover' | 'contain'
  mediaClass?: string
  errorText?: string
  imgLoading?: 'lazy' | 'eager'
}

const props = withDefaults(defineProps<Props>(), {
  alt: '',
  fit: 'cover',
  mediaClass: '',
  errorText: '',
  imgLoading: 'eager'
})

const emit = defineEmits<{
  load: []
  error: [event: Event]
}>()

const displaySrc = ref('')
const hasError = ref(false)
const loading = ref(false)
let blobUrl: string | null = null

const mediaType = computed(() => getCoverMediaType(props.src))

function revokeBlobUrl() {
  if (blobUrl) {
    URL.revokeObjectURL(blobUrl)
    blobUrl = null
  }
}

async function resolveDisplaySrc(src?: string) {
  revokeBlobUrl()
  hasError.value = false
  loading.value = false
  displaySrc.value = ''

  if (!src) return

  if (getCoverMediaType(src) === 'video' && isOwnOssMediaUrl(src)) {
    loading.value = true
    try {
      blobUrl = await fetchOssVideoBlobUrl(src)
      displaySrc.value = blobUrl
    } catch (error) {
      console.warn('视频代理加载失败，回退直连 OSS', error)
      displaySrc.value = src
    } finally {
      loading.value = false
    }
    return
  }

  displaySrc.value = src
}

watch(
  () => props.src,
  (src) => {
    void resolveDisplaySrc(src)
  },
  { immediate: true }
)

onUnmounted(() => {
  revokeBlobUrl()
})

const handleLoad = () => {
  hasError.value = false
  emit('load')
}

const handleError = (event: Event) => {
  hasError.value = true
  emit('error', event)
}
</script>

<style scoped>
.cover-media-root {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--el-fill-color, #f5f7fa);
}

.cover-media-el {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: v-bind(fit);
}

.cover-media-loading,
.cover-media-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
  color: var(--el-text-color-secondary, #909399);
  background: var(--el-fill-color, #f5f7fa);
}
</style>
