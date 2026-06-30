<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import CreativeIcon from '@/assets/svg/creative.svg'
import AssetsIcon from '@/assets/svg/assets.svg'
import ProfileIcon from '@/assets/svg/profile.svg'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const tabs = computed(() => [
  { id: 'comfyui', title: t('layouts.sidebar.menu.onlineGeneration'), route: '/comfyui', icon: CreativeIcon },
  { id: 'works', title: t('layouts.sidebar.menu.myWorks'), route: '/works', icon: AssetsIcon },
  { id: 'profile', title: t('layouts.sidebar.menu.profile'), route: '/profile', icon: ProfileIcon }
])

const activeRoute = computed(() => route.path)

function isActive(tabRoute: string) {
  return activeRoute.value === tabRoute || activeRoute.value.startsWith(`${tabRoute}/`)
}

function navigate(routePath: string) {
  if (route.path !== routePath) {
    router.push(routePath)
  }
}
</script>

<template>
  <nav class="mobile-tab-bar" aria-label="mobile navigation">
    <button
      v-for="tab in tabs"
      :key="tab.id"
      type="button"
      class="tab-item"
      :class="{ active: isActive(tab.route) }"
      @click="navigate(tab.route)"
    >
      <img :src="tab.icon" :alt="tab.title" class="tab-icon" />
      <span class="tab-label">{{ tab.title }}</span>
    </button>
  </nav>
</template>

<style scoped>
.mobile-tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: stretch;
  height: 60px;
  padding-bottom: env(safe-area-inset-bottom, 0);
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  padding: 6px 4px;
}

.tab-item.active {
  color: var(--el-color-primary);
}

.tab-icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.tab-label {
  font-size: 11px;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
