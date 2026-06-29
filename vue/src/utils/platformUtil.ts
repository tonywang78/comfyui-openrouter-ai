import { Capacitor } from '@capacitor/core'
import { isMobileClient } from '@/config/runtime'

/**
 * 在移动端用系统浏览器 / Capacitor Browser 打开外链（如微信 OAuth）。
 */
export async function openExternalUrl(url: string): Promise<void> {
  if (isMobileClient() && Capacitor.isNativePlatform()) {
    const { Browser } = await import('@capacitor/browser')
    await Browser.open({ url })
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

export function buildWechatOAuthUrl(appId: string, redirectUri: string, state: string): string {
  const params = new URLSearchParams({
    appid: appId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'snsapi_login',
    state
  })
  return `https://open.weixin.qq.com/connect/qrconnect?${params.toString()}#wechat_redirect`
}
