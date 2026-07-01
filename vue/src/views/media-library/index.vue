<template>
  <div class="media-library-page page-container">
    <div class="banner-container">
      <MediaLibraryBanner class="header-enter" />
    </div>

    <div class="upload-toolbar">
      <MediaUploadZone @uploaded="refreshList" />
    </div>

    <div class="filter-toolbar">
      <el-radio-group v-model="filterType" size="small" @change="refreshList">
        <el-radio-button label="">{{ t('mediaLibrary.filterAll') }}</el-radio-button>
        <el-radio-button label="IMAGE">{{ t('mediaLibrary.filterImage') }}</el-radio-button>
        <el-radio-button label="VIDEO">{{ t('mediaLibrary.filterVideo') }}</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        size="small"
        clearable
        :placeholder="t('mediaLibrary.searchPlaceholder')"
        class="search-input"
        @clear="refreshList"
        @keyup.enter="refreshList"
      />
    </div>

    <div class="media-container">
      <div class="media-content content-enter">
        <div v-if="items.length" class="media-grid">
          <MediaCard
            v-for="item in items"
            :key="item.mediaId"
            class="grid-item-enter"
            :media="item"
            :deleting="deletingId === item.mediaId"
            @click="openDetail"
            @delete="confirmDelete"
          />
        </div>

        <MediaEmptyState
          v-else-if="!loading"
          class="empty-state-enter"
        />

        <div ref="sentinel" class="scroll-sentinel" />

        <LoadingState v-if="loading" />
      </div>

      <div v-if="!hasMore && items.length > 0" class="no-more">
        <el-divider>
          <span class="no-more-text">{{ t('mediaLibrary.noMore') }}</span>
        </el-divider>
      </div>
    </div>

    <MediaDetailDrawer
      v-model:visible="detailVisible"
      :media="selectedMedia"
      @refresh="refreshDetail"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessageBox, ElNotification } from 'element-plus'
import { mediaApi } from '@/api/media/media'
import type { MediaApi } from '@/api/media/types'
import MediaLibraryBanner from './components/MediaLibraryBanner.vue'
import MediaUploadZone from './components/MediaUploadZone.vue'
import MediaCard from './components/MediaCard.vue'
import MediaDetailDrawer from './components/MediaDetailDrawer.vue'
import MediaEmptyState from './components/MediaEmptyState.vue'
import LoadingState from '@/views/works/components/LoadingState.vue'

const { t } = useI18n()

const items = ref<MediaApi.UserMediaVo[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const filterType = ref('')
const keyword = ref('')
const deletingId = ref<number | null>(null)
const detailVisible = ref(false)
const selectedMedia = ref<MediaApi.UserMediaVo | null>(null)
const sentinel = ref<HTMLElement>()
let observer: IntersectionObserver | null = null

const hasMore = computed(() => items.value.length < total.value)

async function fetchPage(reset = false) {
  if (loading.value) return
  if (!reset && !hasMore.value && total.value > 0) return

  loading.value = true
  try {
    const currentPage = reset ? 1 : page.value
    const res = await mediaApi.getPage({
      page: currentPage,
      mediaType: filterType.value || undefined,
      keyword: keyword.value || undefined
    })
    if (reset) {
      items.value = res.items || []
      page.value = 2
    } else {
      items.value.push(...(res.items || []))
      page.value += 1
    }
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function refreshList() {
  page.value = 1
  total.value = 0
  fetchPage(true)
}

async function openDetail(media: MediaApi.UserMediaVo) {
  try {
    selectedMedia.value = await mediaApi.getDetail(media.mediaId)
    detailVisible.value = true
  } catch {
    ElNotification.error({ message: t('common.error') })
  }
}

async function refreshDetail() {
  if (!selectedMedia.value) return
  selectedMedia.value = await mediaApi.getDetail(selectedMedia.value.mediaId)
  refreshList()
}

async function confirmDelete(media: MediaApi.UserMediaVo) {
  try {
    await ElMessageBox.confirm(
      t('mediaLibrary.deleteConfirm', { name: media.name }),
      t('common.warning'),
      { type: 'warning' }
    )
    deletingId.value = media.mediaId
    await mediaApi.delete(media.mediaId)
    ElNotification.success({ message: t('mediaLibrary.deleteSuccess') })
    if (selectedMedia.value?.mediaId === media.mediaId) {
      detailVisible.value = false
      selectedMedia.value = null
    }
    refreshList()
  } catch {
    // cancelled
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  refreshList()
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && hasMore.value && !loading.value) {
        fetchPage()
      }
    },
    { threshold: 0.1 }
  )
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<style scoped>
.media-library-page {
  min-height: 100vh;
  background: var(--el-bg-color-page);
  width: 100%;
}

.banner-container {
  padding: 10px 10px 0;
  width: 100%;
}

.upload-toolbar {
  padding: 10px 10px 0;
}

.filter-toolbar {
  padding: 10px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 200px;
}

.media-container {
  padding: 10px;
  width: 100%;
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.media-content {
  flex: 1;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 10px;
  width: 100%;
}

.scroll-sentinel {
  height: 20px;
}

.no-more {
  padding: 40px 0;
  text-align: center;
  margin-top: auto;
}

.no-more-text {
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}

@media (max-width: 768px) {
  .banner-container,
  .upload-toolbar {
    padding: 10px 10px 0;
  }

  .media-container {
    padding: 10px;
  }

  .media-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 8px;
  }

  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .media-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 8px;
  }
}
</style>
