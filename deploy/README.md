# Conni-X-Pro 生产部署参考

部署前请修改以下占位符：

- `yourdomain.com` → 你的主域名
- `127.0.0.1:9000` → Spring Boot 实际监听地址（Docker 内网可用服务名）

## 前端环境变量

编辑 [`vue/.env.production`](../vue/.env.production)：

```env
VITE_CLIENT_KIND=web
VITE_API_BASE_URL=https://api.yourdomain.com/api
VITE_API_WS_URL=wss://api.yourdomain.com/api
```

构建：

```bash
cd vue
npm ci
npm run build:web
```

将 `vue/dist` 同步到服务器 `/var/www/conni-x-pro`。

## Nginx

- [`nginx/app.conf`](nginx/app.conf) — 静态前端（`app.yourdomain.com`）
- [`nginx/api.conf`](nginx/api.conf) — API 反代 + WebSocket（`api.yourdomain.com`）

```bash
sudo cp deploy/nginx/*.conf /etc/nginx/sites-available/
sudo ln -sf /etc/nginx/sites-available/conni-x-app.conf /etc/nginx/sites-enabled/
sudo ln -sf /etc/nginx/sites-available/conni-x-api.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

## 后端 CORS

在 [`singleton/application/src/main/resources/application.yml`](../singleton/application/src/main/resources/application.yml) 中将 `https://*.yourdomain.com` 替换为你的真实域名，或追加 App / 桌面客户端 Origin。

## 客户端探测

- 健康检查：`GET https://api.yourdomain.com/api/client/health`
- 版本信息：`GET https://api.yourdomain.com/api/client/version`
