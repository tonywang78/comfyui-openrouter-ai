<script setup lang="ts">
import { ref, onMounted } from 'vue'
import lottie from 'lottie-web'
import logoAnimation from '@/assets/lottie/logo.json'

defineProps<{
  collapsed?: boolean
}>()

const logoContainer = ref<HTMLElement | null>(null)

onMounted(() => {
  if (logoContainer.value) {
    lottie.loadAnimation({
      container: logoContainer.value,
      renderer: 'svg',
      loop: true,
      autoplay: true,
      animationData: logoAnimation
    })
  }
})
</script>

<template>
  <div class="logo-container" :class="{ collapsed }">
    <div class="logo-icon">
      <div ref="logoContainer" class="lottie-container"></div>
    </div>
    <span v-show="!collapsed" class="logo-text">慧心云创</span>
  </div>
</template>

<style scoped>
.logo-container {
  display: flex;
  align-items: center;
  padding: 10px 8px;
  margin-bottom: 25px;
  transition: padding 0.25s ease, margin-bottom 0.25s ease, justify-content 0.25s ease;
}

.logo-container.collapsed {
  justify-content: center;
  padding: 6px 0;
  margin-bottom: 16px;
}

.logo-icon {
  margin-right: 12px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.logo-container.collapsed .logo-icon {
  margin-right: 0;
}

.lottie-container {
  width: 42px;
  height: 42px;
}

.logo-container.collapsed .lottie-container {
  width: 36px;
  height: 36px;
}

.logo-text {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
}
</style>
