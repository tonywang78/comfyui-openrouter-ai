<template>
  <div
    class="media-card"
    @click="$emit('click', media)"
  >
    <button
      class="delete-btn"
      type="button"
      :disabled="deleting"
      @click.stop="$emit('delete', media)"
    >
      <el-icon v-if="deleting" class="is-loading"><Loading /></el-icon>
      <el-icon v-else><Delete /></el-icon>
    </button>

    <div class="media-thumb">
      <img
        v-if="media.mediaType === 'IMAGE'"
        :src="media.url"
        :alt="media.name"
        loading="lazy"
      />
      <video
        v-else-if="media.mediaType === 'VIDEO'"
        :src="media.url"
        muted
        preload="metadata"
      />
      <div v-else class="audio-placeholder">
        <el-icon size="40"><Microphone /></el-icon>
      </div>
    </div>

    <div class="media-info">
      <p class="media-name" :title="media.name">{{ media.name }}</p>
      <div v-if="variantBadges.length" class="variant-badges">
        <span v-for="badge in variantBadges" :key="badge" class="variant-badge">{{ badge }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Delete, Loading, Microphone } from '@element-plus/icons-vue'
import type { MediaApi } from '@/api/media/types'

const props = defineProps<{
  media: MediaApi.UserMediaVo
  deleting?: boolean
}>()

defineEmits<{
  click: [media: MediaApi.UserMediaVo]
  delete: [media: MediaApi.UserMediaVo]
}>()

const variantBadges = computed(() => {
  const variants = props.media.variants || []
  return variants
    .filter(v => v.status === 'SUCCEEDED')
    .map(v => {
      if (v.variantType === 'HEADSHOT_BUILTIN') return '大头照'
      if (v.variantType === 'HEADSHOT_COMFYUI') return '精修'
      return v.variantType
    })
})
</script>

<style scoped>
.media-card {
  position: relative;
  background: var(--el-bg-color);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
}

.media-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.delete-btn {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease, background 0.2s ease;
  backdrop-filter: blur(4px);
}

.media-card:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover:not(:disabled) {
  background: var(--el-color-danger);
}

.media-thumb {
  aspect-ratio: 4 / 3;
  background: var(--el-fill-color-light);
  overflow: hidden;
}

.media-thumb img,
.media-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.audio-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}

.media-info {
  padding: 12px 14px;
}

.media-name {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.variant-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.variant-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
</style>
