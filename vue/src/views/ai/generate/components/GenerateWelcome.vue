<template>
  <div class="generate-welcome">
    <div class="hero-badge">
      <span class="dot" />
      {{ t('generate.welcome.badge') }}
    </div>
    <h1>{{ t('generate.welcome.title') }}</h1>
    <p class="subtitle">{{ t('generate.welcome.subtitle') }}</p>

    <div class="workflow-section">
      <span class="section-label">{{ t('generate.welcome.pickWorkflow') }}</span>
      <p class="workflow-hint">{{ t('generate.welcome.pickWorkflowHint') }}</p>
    </div>

    <div class="feature-grid">
      <div v-for="f in features" :key="f.title" class="feature-card">
        <span class="feature-icon">{{ f.icon }}</span>
        <div class="feature-text">
          <strong>{{ f.title }}</strong>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </div>

    <div class="suggestions">
      <span class="section-label">{{ t('generate.welcome.tryThese') }}</span>
      <div class="suggestion-list">
        <button
          v-for="(s, i) in suggestions"
          :key="i"
          class="suggestion"
          @click="$emit('send-suggestion', s.message)"
        >
          <span class="suggestion-title">{{ s.title }}</span>
          <span class="suggestion-desc">{{ s.desc }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{ webSearchEnabled?: boolean }>()

defineEmits<{
  'send-suggestion': [message: string]
}>()

const { t } = useI18n()

const features = computed(() => [
  { icon: '💬', title: t('generate.welcome.feature1Title'), desc: t('generate.welcome.feature1Desc') },
  { icon: '📎', title: t('generate.welcome.feature2Title'), desc: t('generate.welcome.feature2Desc') },
  { icon: '✅', title: t('generate.welcome.feature3Title'), desc: t('generate.welcome.feature3Desc') }
])

const suggestions = computed(() => [
  { title: t('generate.welcome.suggestion1'), desc: t('generate.welcome.suggestion1Desc'), message: t('generate.welcome.suggestion1Msg') },
  { title: t('generate.welcome.suggestion2'), desc: t('generate.welcome.suggestion2Desc'), message: t('generate.welcome.suggestion2Msg') },
  { title: t('generate.welcome.suggestion3'), desc: t('generate.welcome.suggestion3Desc'), message: t('generate.welcome.suggestion3Msg') }
])

</script>

<style scoped>
.generate-welcome {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  padding: 8px 0 24px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 16px;
}

.hero-badge .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

h1 {
  font-size: clamp(26px, 4vw, 34px);
  font-weight: 700;
  margin: 0 0 10px;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.subtitle {
  color: var(--el-text-color-secondary);
  margin: 0 0 28px;
  font-size: 15px;
  line-height: 1.6;
  max-width: 560px;
}

.section-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 10px;
}

.workflow-section {
  margin-bottom: 28px;
}

.workflow-hint {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 28px;
}

.feature-card {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.feature-icon { font-size: 22px; line-height: 1; flex-shrink: 0; }
.feature-text strong { display: block; font-size: 13px; margin-bottom: 4px; }
.feature-text p { margin: 0; font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.45; }

.suggestions { margin-bottom: 8px; }

.suggestion-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

.suggestion {
  text-align: left;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.suggestion:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1);
}

.suggestion-title { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); }
.suggestion-desc { font-size: 11px; color: var(--el-text-color-secondary); line-height: 1.4; }
</style>
