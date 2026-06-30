import { ref, computed, onUnmounted } from 'vue'

/** 与后端 SmsUtil 发送锁时长一致 */
export const VERIFICATION_CODE_COOLDOWN_SECONDS = 60

export function useVerificationCodeCountdown(cooldownSeconds = VERIFICATION_CODE_COOLDOWN_SECONDS) {
  const countdown = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  const isCounting = computed(() => countdown.value > 0)

  const clearTimer = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  const startCountdown = (from = cooldownSeconds) => {
    clearTimer()
    countdown.value = from
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearTimer()
        countdown.value = 0
      }
    }, 1000)
  }

  onUnmounted(clearTimer)

  return { countdown, isCounting, startCountdown }
}
