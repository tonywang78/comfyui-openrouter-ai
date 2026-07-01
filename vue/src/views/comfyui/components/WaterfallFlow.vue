<template>
  <div class="waterfall-container" ref="containerRef">
    <div v-if="items.length > 0" class="workflow-grid">
      <div
        v-for="item in items"
        :key="item.id"
        class="workflow-card"
        @click="handleItemClick(item)"
      >
        <div v-if="!item.imageUrl" class="card-placeholder">
          <el-icon size="40" color="#ccc"><Picture /></el-icon>
          <p>{{ item.title }}</p>
        </div>
        <template v-else-if="!imageErrors[item.id]">
          <div v-if="!imageLoaded[item.id]" class="loading-spinner">
            <el-icon class="is-loading" size="24" color="#888"><Loading /></el-icon>
          </div>
          <CoverMedia
            :src="item.imageUrl"
            fit="cover"
            media-class="card-media"
            img-loading="lazy"
            @load="() => handleImageLoad(item)"
            @error="() => handleImageError(item)"
          />
        </template>
        <div v-else class="card-placeholder">
          <el-icon size="40" color="#ccc"><Picture /></el-icon>
          <p>加载失败</p>
        </div>

        <div class="tag" :style="{ opacity: imageLoaded[item.id] || imageErrors[item.id] ? 1 : 0 }">
          {{ item.categoryName }}
        </div>
        <div class="title-overlay" :style="{ opacity: imageLoaded[item.id] || imageErrors[item.id] ? 1 : 0 }">
          <h3>{{ item.title }}</h3>
        </div>
      </div>
    </div>
    <div v-else class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Picture, Loading } from '@element-plus/icons-vue'
import CoverMedia from '@/components/common/CoverMedia.vue'

interface WaterfallItem {
  id: number
  imageUrl: string
  title: string
  categoryName: string
}

const { items } = defineProps<{
  items: WaterfallItem[]
}>()

const containerRef = ref<HTMLElement | null>(null)
const imageErrors = ref<Record<number, boolean>>({})
const imageLoaded = ref<Record<number, boolean>>({})

const onScroll = () => {
  if (!containerRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = containerRef.value
  if (scrollTop + clientHeight >= scrollHeight - 100) {
    emit('reach-bottom')
  }
}

onMounted(() => {
  containerRef.value?.addEventListener('scroll', onScroll)
})

onUnmounted(() => {
  containerRef.value?.removeEventListener('scroll', onScroll)
})

const emit = defineEmits(['item-click', 'reach-bottom'])

const handleItemClick = (item: WaterfallItem) => {
  emit('item-click', item)
}

const handleImageError = (item: WaterfallItem) => {
  imageErrors.value[item.id] = true
}

const handleImageLoad = (item: WaterfallItem) => {
  imageLoaded.value = { ...imageLoaded.value, [item.id]: true }
  imageErrors.value[item.id] = false
}

watch(
  () => items,
  (newItems) => {
    const currentIds = new Set(newItems.map(item => item.id))
    Object.keys(imageErrors.value).forEach(key => {
      const id = Number(key)
      if (!currentIds.has(id)) {
        delete imageErrors.value[id]
        delete imageLoaded.value[id]
      }
    })
  },
  { deep: true }
)
</script>

<style scoped>
.waterfall-container {
  width: 100%;
  height: 100%;
  overflow: auto;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 12px;
  padding: 2px;
}

@media (min-width: 640px) {
  .workflow-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

@media (min-width: 1200px) {
  .workflow-grid {
    grid-template-columns: repeat(auto-fill, minmax(196px, 1fr));
    gap: 14px;
  }
}

@media (min-width: 1920px) {
  .workflow-grid {
    grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  }
}

.workflow-card {
  position: relative;
  aspect-ratio: 4 / 5;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: var(--el-fill-color-light);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.workflow-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.workflow-card :deep(.cover-media-root) {
  width: 100%;
  height: 100%;
}

.workflow-card :deep(.card-media) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 12px;
  text-align: center;
}

.card-placeholder p {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.loading-spinner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;
}

.tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.62);
  color: #fff;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  z-index: 3;
  max-width: calc(100% - 16px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: opacity 0.25s ease;
}

.title-overlay {
  position: absolute;
  inset: auto 0 0 0;
  padding: 28px 12px 10px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.82) 0%, rgba(0, 0, 0, 0) 100%);
  color: #fff;
  z-index: 3;
  transition: opacity 0.25s ease;
}

.title-overlay h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.loading-container {
  padding: 20px;
  width: 100%;
}
</style>
