# Conni-X-Pro 移动端（Capacitor）

Capacitor 6 移动壳，加载共享 Vue 前端并连接云端 Spring Boot API。

## 前置条件

- Node.js 18+
- Android: Android Studio + SDK
- iOS: Xcode（仅 macOS）

## 首次初始化

```bash
# 1. 配置 API 域名
# 编辑 vue/.env.mobile，替换 yourdomain.com

# 2. 安装依赖
cd mobile
npm install
cd ../vue && npm install

# 3. 添加原生工程（仅需一次）
cd ../mobile
npm run add:android
# macOS 上可选：npm run add:ios
```

## 构建与同步

```bash
cd mobile
npm run sync          # 构建 vue（mobile 模式）并同步到原生工程
npm run sync:android  # 同步后打开 Android Studio
npm run sync:ios      # 同步后打开 Xcode（macOS）
```

## 移动端特性

- 底部 Tab 导航（`MobileTabBar.vue`）
- 微信登录：在系统浏览器中完成 OAuth（`WeChatLoginPanel.vue`）
- API 地址由 `vue/.env.mobile` + `runtime.ts` 解析

## 配置

| 文件 | 作用 |
|------|------|
| `capacitor.config.json` | App ID、webDir、Android scheme |
| `vue/.env.mobile` | `VITE_CLIENT_KIND=mobile` 与 API 地址 |

## 发布

在 Android Studio / Xcode 中签名并生成 `.apk` / `.aab` / `.ipa`，提交应用商店。
