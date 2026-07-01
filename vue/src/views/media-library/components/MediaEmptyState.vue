<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon size="64"><FolderOpened /></el-icon>
    </div>
    <h3>{{ t('mediaLibrary.empty.title') }}</h3>
    <p>{{ t('mediaLibrary.empty.description') }}</p>
  </div>
</template>

<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--el-text-color-secondary);
}

.empty-icon {
  margin-bottom: 24px;
  color: var(--el-color-primary);
}

.empty-state h3 {
  font-size: 18px;
  margin: 0 0 8px;
  color: var(--el-text-color-primary);
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}
</style>
