<template>
  <el-dialog
    v-model="visibleModel"
    :title="t('mediaLibrary.pickerTitle')"
    width="720px"
    destroy-on-close
    @open="onOpen"
  >
    <div v-loading="loading" class="picker-grid">
      <button
        v-for="item in items"
        :key="`${item.mediaId}-${item.variantId || 'orig'}`"
        type="button"
        class="picker-item"
        :class="{ selected: isSelected(item) }"
        @click="select(item)"
      >
        <img :src="item.url" :alt="item.name" loading="lazy" />
        <span class="picker-label">{{ item.variantLabel || item.name }}</span>
      </button>
    </div>
    <el-empty v-if="!loading && !items.length" :description="t('mediaLibrary.empty.description')" />
    <template #footer>
      <el-button @click="visibleModel = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :disabled="!selected" @click="confirm">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { mediaApi } from '@/api/media/media'
import type { MediaApi } from '@/api/media/types'

const props = defineProps<{
  visible: boolean
  mediaType?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  select: [item: MediaApi.MediaPickerItemVo]
}>()

const { t } = useI18n()
const loading = ref(false)
const items = ref<MediaApi.MediaPickerItemVo[]>([])
const selected = ref<MediaApi.MediaPickerItemVo | null>(null)

const visibleModel = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v)
})

function isSelected(item: MediaApi.MediaPickerItemVo) {
  if (!selected.value) return false
  return selected.value.mediaId === item.mediaId && selected.value.variantId === item.variantId
}

function select(item: MediaApi.MediaPickerItemVo) {
  selected.value = item
}

function confirm() {
  if (selected.value) {
    emit('select', selected.value)
    visibleModel.value = false
  }
}

async function onOpen() {
  selected.value = null
  loading.value = true
  try {
    const res = await mediaApi.getPicker({
      page: 1,
      mediaType: props.mediaType
    })
    items.value = res.items || []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
}

.picker-item {
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: var(--el-fill-color-light);
  text-align: left;
}

.picker-item.selected {
  border-color: var(--el-color-primary);
}

.picker-item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
}

.picker-label {
  display: block;
  font-size: 11px;
  padding: 4px 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
