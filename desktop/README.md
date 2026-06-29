# Conni-X-Pro 桌面客户端（Tauri）

Tauri 2 桌面壳，加载共享 Vue 前端并连接云端 Spring Boot API。

## 前置条件

- [Rust](https://www.rust-lang.org/tools/install)
- Node.js 18+
- Windows: Visual Studio Build Tools（MSVC）

## 开发

```bash
# 终端 1：启动后端（仓库根目录）
.\dev.ps1 backend

# 终端 2：启动桌面客户端（会自动拉起 vue dev server）
cd desktop
npm install
npm run dev
```

开发时 Vue 运行在 `http://localhost:8080`，API 地址由 `vue/.env.development` 配置。

## 生产构建

1. 编辑 [`vue/.env.desktop`](../vue/.env.desktop)，将 `yourdomain.com` 替换为你的 API 域名。
2. 构建安装包：

```bash
cd desktop
npm install
npm run build
```

产物位于 `desktop/src-tauri/target/release/bundle/`（`.msi` / `.dmg` / `.AppImage`）。

## 配置说明

| 文件 | 作用 |
|------|------|
| `src-tauri/tauri.conf.json` | 窗口、构建命令、bundle 配置 |
| `vue/.env.desktop` | 桌面版 API / WebSocket 地址 |
| `vue/src/config/runtime.ts` | 运行时 API 解析（含 localStorage 覆盖） |

## 远程 API

桌面客户端不打包 Java 后端；所有业务请求发往你的云端 `https://api.yourdomain.com/api`。
