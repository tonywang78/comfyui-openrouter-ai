import { computed } from 'vue'
import { getClientKind, isMobileClient, isDesktopClient } from '@/config/runtime'

export function useClientLayout() {
  const clientKind = computed(() => getClientKind())
  const isMobile = computed(() => isMobileClient())
  const isDesktop = computed(() => isDesktopClient())

  return {
    clientKind,
    isMobile,
    isDesktop
  }
}
