<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AuthPanel from '@/components/auth/AuthPanel.vue'

const route = useRoute()

const redirect = computed(() => {
  const value = route.query.redirect
  if (typeof value === 'string' && value.startsWith('/')) {
    return value
  }
  return '/comfyui'
})
</script>

<template>
  <div class="login-page">
    <AuthPanel :redirect="redirect" />
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: var(--el-bg-color-page);
  background-image: radial-gradient(circle, var(--el-border-color-lighter) 1px, transparent 1px);
  background-size: 30px 30px;
}
</style>
