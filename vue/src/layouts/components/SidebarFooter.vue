<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Bell } from '@element-plus/icons-vue'
import emitter, { OPEN_NOTICE_ANNOUNCER } from '@/utils/eventBusUtil'

defineProps<{
  collapsed?: boolean
}>()

const { t } = useI18n()

const handleAnnouncementClick = () => {
  emitter.emit(OPEN_NOTICE_ANNOUNCER)
}
</script>

<template>
  <div class="sidebar-footer" :class="{ collapsed }">
    <div class="footer-links">
      <el-tooltip
        v-if="collapsed"
        :content="t('layouts.sidebar.footer.announcement')"
        placement="right"
        :show-after="300"
      >
        <button type="button" class="footer-icon-btn" @click="handleAnnouncementClick">
          <el-icon :size="18"><Bell /></el-icon>
        </button>
      </el-tooltip>
      <span v-else @click="handleAnnouncementClick">{{ t('layouts.sidebar.footer.announcement') }}</span>
    </div>
  </div>
</template>

<style scoped>
.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-dark);
}

.sidebar-footer.collapsed {
  padding-top: 12px;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 0 8px;
  gap: 8px;
}

.sidebar-footer.collapsed .footer-links {
  justify-content: center;
  padding: 0;
}

.footer-links span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  flex-basis: auto;
  text-align: left;
}

.footer-links span:hover {
  color: var(--el-text-color-regular);
}

.footer-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.footer-icon-btn:hover {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}
</style>
