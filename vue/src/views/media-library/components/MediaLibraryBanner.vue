<template>
  <div class="media-library-banner" @mousemove="handleMouseMove" ref="bannerRef">
    <div class="banner-bg">
      <div class="gradient-overlay"></div>
      <div class="decoration-circles">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
      </div>
      <div class="floating-particles">
        <div v-for="i in 8" :key="i" class="particle" :style="getParticleStyle(i)"></div>
      </div>
      <div class="mouse-glow" :style="mouseGlowStyle"></div>
    </div>

    <div class="banner-content">
      <div class="text-section">
        <h1 class="title">{{ displayTitle }}<span class="cursor" v-show="showCursor">|</span></h1>
        <p class="description">{{ displayDesc }}</p>
        <div class="stats">
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor"/>
                <path d="M21 15l-5-5L5 21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>{{ t('mediaLibrary.banner.stats.personalAssets') }}</span>
          </div>
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="2"/>
                <path d="M6 20c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <span>{{ t('mediaLibrary.banner.stats.headshot') }}</span>
          </div>
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" stroke-width="2"/>
                <path d="M8 12L11 15L16 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>{{ t('mediaLibrary.banner.stats.reuse') }}</span>
          </div>
        </div>
      </div>

      <div class="visual-section">
        <div class="artwork-showcase">
          <svg viewBox="0 0 120 120" class="showcase-svg">
            <defs>
              <linearGradient id="mediaGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="var(--el-color-primary-light-5)" stop-opacity="0.9"/>
                <stop offset="50%" stop-color="var(--el-color-primary-light-7)" stop-opacity="0.7"/>
                <stop offset="100%" stop-color="var(--el-color-primary-light-9)" stop-opacity="0.5"/>
              </linearGradient>
              <filter id="mediaGlow">
                <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
                <feMerge>
                  <feMergeNode in="coloredBlur"/>
                  <feMergeNode in="SourceGraphic"/>
                </feMerge>
              </filter>
            </defs>
            <g class="artwork-main">
              <rect x="28" y="32" width="52" height="52" rx="8" fill="none" stroke="url(#mediaGradient)" stroke-width="2" opacity="0.5">
                <animate attributeName="y" values="32;28;32" dur="3s" repeatCount="indefinite"/>
              </rect>
              <rect x="40" y="24" width="52" height="52" rx="8" fill="url(#mediaGradient)" filter="url(#mediaGlow)" opacity="0.85">
                <animate attributeName="y" values="24;20;24" dur="3.5s" repeatCount="indefinite"/>
              </rect>
              <circle cx="54" cy="42" r="6" fill="rgba(255,255,255,0.9)"/>
              <path d="M46 58 L54 50 L62 54 L70 46" stroke="rgba(255,255,255,0.9)" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </g>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const bannerRef = ref(null)
const displayTitle = ref('')
const displayDesc = ref('')
const showCursor = ref(true)
const mouseGlowStyle = ref({ left: '50%', top: '50%', opacity: 0 })

const fullTitle = t('mediaLibrary.banner.title')
const fullDesc = t('mediaLibrary.banner.description')

let titleIndex = 0
let descIndex = 0
let titleTimer = null
let descTimer = null
let cursorTimer = null

const typeTitle = () => {
  if (titleIndex < fullTitle.length) {
    displayTitle.value += fullTitle[titleIndex]
    titleIndex++
    titleTimer = setTimeout(typeTitle, 120)
  } else {
    setTimeout(typeDesc, 500)
  }
}

const typeDesc = () => {
  if (descIndex < fullDesc.length) {
    displayDesc.value += fullDesc[descIndex]
    descIndex++
    descTimer = setTimeout(typeDesc, 60)
  } else {
    showCursor.value = false
  }
}

const blinkCursor = () => {
  showCursor.value = !showCursor.value
  cursorTimer = setTimeout(blinkCursor, 500)
}

const getParticleStyle = (index) => {
  const size = 2 + Math.random() * 3
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: size + 'px',
    height: size + 'px',
    animationDelay: Math.random() * 4 + 's',
    animationDuration: 6 + Math.random() * 4 + 's'
  }
}

const handleMouseMove = (event) => {
  if (!bannerRef.value) return
  const rect = bannerRef.value.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * 100
  const y = ((event.clientY - rect.top) / rect.height) * 100
  mouseGlowStyle.value = { left: x + '%', top: y + '%', opacity: 0.6 }
}

onMounted(() => {
  blinkCursor()
  setTimeout(typeTitle, 500)
})

onUnmounted(() => {
  if (titleTimer) clearTimeout(titleTimer)
  if (descTimer) clearTimeout(descTimer)
  if (cursorTimer) clearTimeout(cursorTimer)
})
</script>

<style scoped>
.media-library-banner {
  position: relative;
  min-height: 200px;
  background: linear-gradient(135deg, var(--custom-gradient-start) 0%, var(--custom-gradient-end) 100%);
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 0;
  cursor: pointer;
}

.banner-bg {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.gradient-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
    rgba(var(--el-color-primary-rgb), 0.9) 0%,
    rgba(var(--el-color-primary-rgb), 0.7) 50%,
    rgba(var(--el-color-primary-rgb), 0.5) 100%);
}

.decoration-circles {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.circle-1 { width: 120px; height: 120px; top: -60px; right: -60px; }
.circle-2 { width: 80px; height: 80px; bottom: -40px; left: 20%; animation-delay: 2s; }
.circle-3 { width: 60px; height: 60px; top: 30%; left: -30px; animation-delay: 4s; }

.floating-particles { position: absolute; inset: 0; }

.particle {
  position: absolute;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  animation: particleFloat 6s linear infinite;
}

.mouse-glow {
  position: absolute;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  transition: all 0.3s ease;
  z-index: 2;
}

.banner-content {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 30px;
  color: white;
}

.text-section { flex: 1; max-width: 60%; }

.title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 12px;
  line-height: 1.2;
}

.cursor {
  color: rgba(255, 255, 255, 0.8);
  animation: blink 1s infinite;
}

.description {
  font-size: 15px;
  line-height: 1.5;
  margin: 0 0 20px;
  opacity: 0.9;
}

.stats {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  opacity: 0.9;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
}

.visual-section { flex-shrink: 0; margin-left: 30px; }
.artwork-showcase { width: 100px; height: 100px; }
.showcase-svg { width: 100%; height: 100%; filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3)); }

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(180deg); }
}

@keyframes particleFloat {
  0% { transform: translateY(100vh) rotate(0deg); opacity: 0; }
  10% { opacity: 0.8; }
  90% { opacity: 0.8; }
  100% { transform: translateY(-100vh) rotate(360deg); opacity: 0; }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

@media (max-width: 768px) {
  .media-library-banner { min-height: 160px; }
  .banner-content { flex-direction: column; text-align: center; padding: 20px 15px; }
  .text-section { max-width: 100%; margin-bottom: 15px; }
  .title { font-size: 22px; }
  .description { font-size: 14px; }
  .stats { justify-content: center; gap: 16px; }
  .visual-section { margin-left: 0; }
  .artwork-showcase { width: 70px; height: 70px; }
}

@media (max-width: 480px) {
  .media-library-banner { min-height: 140px; }
  .stats { flex-direction: column; gap: 10px; }
  .title { font-size: 18px; }
}
</style>
