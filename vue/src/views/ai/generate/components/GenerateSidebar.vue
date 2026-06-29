<template>
  <aside class="generate-sidebar" :class="{ collapsed }">
    <div class="sidebar-header">
      <button v-if="!collapsed" class="new-chat" type="button" @click="$emit('new-session')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        {{ t('generate.sidebar.newSession') }}
      </button>
      <button class="toggle" type="button" :title="collapsed ? t('generate.sidebar.expand') : t('generate.sidebar.collapse')" @click="collapsed = !collapsed">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 12h18M3 6h18M3 18h18"/>
        </svg>
      </button>
    </div>

    <div v-if="!collapsed" class="session-list">
      <p v-if="!sessions.length" class="empty-hint">{{ t('generate.sidebar.empty') }}</p>
      <button
        v-for="s in sessions"
        :key="s.id"
        type="button"
        class="session-item"
        :class="{ active: s.id === activeSessionId }"
        @click="$emit('select-session', s.id)"
      >
        <span class="session-icon">💬</span>
        <span class="title">{{ s.title }}</span>
        <button type="button" class="delete" @click.stop="$emit('delete-session', s.id)">×</button>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { GenerationSession } from '@/stores/modules/generation'

defineProps<{
  sessions: GenerationSession[]
  activeSessionId: string | null
}>()

defineEmits<{
  'new-session': []
  'select-session': [id: string]
  'delete-session': [id: string]
}>()

const { t } = useI18n()
const collapsed = ref(false)
</script>

<style scoped>
.generate-sidebar {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;
  background: var(--el-fill-color-blank);
  transition: width 0.2s ease;
}

.generate-sidebar.collapsed { width: 52px; }

.sidebar-header {
  display: flex;
  gap: 8px;
  padding: 14px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.new-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 10px;
  border: none;
  background: var(--el-color-primary);
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: opacity 0.15s ease;
}

.new-chat:hover { opacity: 0.9; }

.toggle {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-regular);
  flex-shrink: 0;
}

.toggle:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 8px;
}

.empty-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: center;
  padding: 24px 12px;
  margin: 0;
}

.session-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  margin-bottom: 4px;
  transition: background 0.15s ease;
}

.session-item:hover { background: var(--el-fill-color); }
.session-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.session-icon { font-size: 14px; flex-shrink: 0; }

.title {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete {
  border: none;
  background: none;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  opacity: 0;
  font-size: 16px;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  flex-shrink: 0;
}

.session-item:hover .delete { opacity: 1; }
.delete:hover { background: var(--el-color-danger-light-9); color: var(--el-color-danger); }
</style>
