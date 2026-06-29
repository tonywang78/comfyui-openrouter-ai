[English](./README_en-US.md) | [简体中文](./README.md) | [日本語](./README_ja-JP.md)

# 慧心云创 - 快速部署指南

## 📖 项目简介

慧心云创是一个基于 Vue 3 的现代化 AI 应用平台，提供多种 AI 功能服务。

### 技术栈

- **前端框架**：Vue 3.5.13 + TypeScript
- **构建工具**：Vite 6.2.0
- **UI 组件库**：Element Plus 2.9.11
- **状态管理**：Pinia 3.0.3
- **路由管理**：Vue Router 4.5.0
- **国际化**：Vue I18n 9.14.5
- **HTTP 客户端**：Axios 1.9.0
- **其他特性**：Markdown 渲染、代码高亮、3D 模型预览、瀑布流布局

### 主要功能模块

- 🤖 **AI 聊天**：智能对话交互
- 🎨 **ComfyUI 工作流**：可视化 AI 工作流管理
- 📁 **作品管理**：用户作品展示和管理
- 👤 **个人中心**：用户信息、积分管理
- 🔐 **用户认证**：登录、注册、权限控制
- 🛠️ **系统管理**：后台管理功能（需管理员权限）

---

## ⚙️ 环境要求

### 开发环境

- **Node.js**：>= 18.x（推荐使用 18.x 或 20.x LTS 版本）
- **npm**：>= 9.x 或 **pnpm**：>= 8.x
- **操作系统**：Windows / macOS / Linux

### 生产环境

- **Nginx**：>= 1.18.x
- **操作系统**：建议使用 Linux（Ubuntu 20.04+、CentOS 8+ 等）

### 更新日志

- **2025-10-28**： 
  - 新增 ComfyUI 涂抹组件
  - 新增作品批量删除功能
    - 支持多选作品进行批量删除
    - 优化暗色主题下的复选框样式
    - 优化暗色主题下的按钮和对话框样式
    - 完善删除确认交互体验

---

## 🚀 本地开发

### 1. 克隆项目

```bash
git clone <your-repository-url>
cd vue
```

### 2. 安装依赖

使用 npm：
```bash
npm install
```

或使用 pnpm：
```bash
pnpm install
```

### 3. 配置环境变量

在项目根目录创建 `.env.development` 文件：

```env
# 开发环境的环境变量

# 开发环境API地址
VITE_API_BASE_URL=http://localhost:9898/api 

# WS环境   
VITE_API_WS_URL=ws://127.0.0.1:9898/api
```

**说明**：
- `VITE_API_BASE_URL`：API 接口的完整地址（包括协议、域名、端口）
- `VITE_API_WS_URL`：WebSocket 连接地址

### 4. 修改开发代理配置

编辑 `vite.config.ts`，修改代理目标地址：

```typescript
server: {
  proxy: {
    [env.VITE_API_BASE_URL]: {
      target: 'http://your-backend-api-url', // 修改为实际后端 API 地址
      changeOrigin: true,
      rewrite: (path) =>
        path.replace(new RegExp('^' + env.VITE_API_BASE_URL), '')
    }
  }
}
```

### 5. 启动开发服务器

```bash
npm run dev
```

访问 `http://localhost:5173` 即可查看应用。

---

## 📦 生产构建

### 1. 配置生产环境变量

在项目根目录创建 `.env.production` 文件：

```env
# 生产环境的环境变量

# 生产环境API地址
VITE_API_BASE_URL=http://localhost:9898/api 

# WS环境    
VITE_API_WS_URL=ws://127.0.0.1:9898/api
```

**说明**：
- `VITE_API_BASE_URL`：生产环境的完整 API 地址（请替换为实际的后端地址，如 `https://api.yourdomain.com`）
- `VITE_API_WS_URL`：生产环境的 WebSocket 地址（请替换为实际的 WebSocket 地址，如 `wss://api.yourdomain.com`）
- 如果前端部署在子路径（非根路径），需要配置 `BASE_URL`（如：`BASE_URL=/app/`）

### 2. 执行构建命令

```bash
npm run build
```

构建完成后，会在项目根目录生成 `dist` 目录，包含所有静态资源文件。

**构建产物说明**：
- 📦 **代码压缩和混淆**：使用 Terser 进行深度压缩和变量名混淆
- 🗜️ **Gzip 压缩**：自动生成 `.gz` 文件，减少传输体积
- 🎯 **代码分割**：智能分包，提升首屏加载速度
- 🧹 **去除调试代码**：自动移除 `console.log`、`debugger` 等调试代码
- 📊 **构建分析报告**：生成 `dist/stats.html` 查看打包体积分析

### 3. 预览构建结果（可选）

```bash
npm run preview
```

### 4. 查看构建分析报告（可选）

构建完成后，可以打开 `dist/stats.html` 查看详细的打包分析：
- 各个模块的体积占比
- Gzip 压缩后的大小
- 依赖关系可视化

---

## 🌐 Nginx 部署配置

### 方案一：域名根路径部署

适用于将应用部署在域名根路径，如 `https://www.yourdomain.com`

#### 1. 上传构建文件

将 `dist` 目录中的所有文件上传到服务器，建议路径：

```bash
/var/www/cogni-x/
```

#### 2. Nginx 配置文件

创建或编辑 Nginx 配置文件 `/etc/nginx/sites-available/cogni-x`：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name yourdomain.com www.yourdomain.com;

    # 强制跳转到 HTTPS（建议配置 SSL 证书）
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    # SSL 证书配置
    ssl_certificate /path/to/your/certificate.crt;
    ssl_certificate_key /path/to/your/private.key;
    
    # SSL 安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 网站根目录
    root /var/www/cogni-x;
    index index.html;

    # Gzip 压缩配置
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript 
               application/json application/javascript application/xml+rss 
               application/rss+xml font/truetype font/opentype 
               application/vnd.ms-fontobject image/svg+xml;

    # 静态资源缓存配置
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    # API 代理转发（如果需要通过 Nginx 代理后端 API）
    location /api/ {
        proxy_pass http://your-backend-server:port/;
        proxy_http_version 1.1;
        
        # 代理头设置
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持（如果 API 使用 WebSocket）
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Vue Router History 模式支持
    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # 安全头配置
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 日志配置
    access_log /var/log/nginx/cogni-x-access.log;
    error_log /var/log/nginx/cogni-x-error.log;
}
```

#### 3. 启用配置并重启 Nginx

```bash
# 创建软链接启用配置
sudo ln -s /etc/nginx/sites-available/cogni-x /etc/nginx/sites-enabled/

# 测试配置是否正确
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

---

### 方案二：子路径部署

适用于将应用部署在子路径，如 `https://www.yourdomain.com/app/`

#### 1. 修改环境变量

编辑 `.env.production`：

```env
# 生产环境的环境变量

# 生产环境API地址
VITE_API_BASE_URL=https://api.yourdomain.com

# WS环境    
VITE_API_WS_URL=wss://api.yourdomain.com

# 子路径部署配置
BASE_URL=/app/
```

#### 2. 重新构建

```bash
npm run build
```

#### 3. 上传构建文件

将 `dist` 目录中的所有文件上传到：

```bash
/var/www/html/app/
```

#### 4. Nginx 配置文件

```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /path/to/your/certificate.crt;
    ssl_certificate_key /path/to/your/private.key;

    # 根目录（如有其他应用）
    root /var/www/html;

    # 子路径应用配置
    location /app {
        alias /var/www/html/app;
        index index.html;
        
        # Vue Router History 模式支持
        try_files $uri $uri/ /app/index.html;
        
        # 禁用缓存 HTML
        location ~* /app/index\.html$ {
            add_header Cache-Control "no-cache, no-store, must-revalidate";
        }
        
        # 静态资源缓存
        location ~* /app/.*\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # API 代理
    location /api/ {
        proxy_pass http://your-backend-server:port/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

### 方案三：无 SSL（仅 HTTP，不推荐生产使用）

如果暂时没有 SSL 证书，仅用于测试：

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    root /var/www/cogni-x;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public";
    }

    # API 代理
    location /api/ {
        proxy_pass http://your-backend-server:port/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Vue Router 支持
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 🔧 高级配置

### 1. 配置免费 SSL 证书（Let's Encrypt）

```bash
# 安装 Certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# 自动配置 SSL 证书
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# 测试自动续期
sudo certbot renew --dry-run
```

### 2. 性能优化配置

在 Nginx 主配置文件 `/etc/nginx/nginx.conf` 中添加：

```nginx
http {
    # 开启 Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript 
               application/json application/javascript application/xml+rss 
               application/rss+xml font/truetype font/opentype 
               application/vnd.ms-fontobject image/svg+xml;

    # 开启 Brotli 压缩（需要安装模块）
    # brotli on;
    # brotli_comp_level 6;
    # brotli_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # 客户端缓冲区大小
    client_body_buffer_size 128k;
    client_max_body_size 20m;
    client_header_buffer_size 1k;
    large_client_header_buffers 4 4k;

    # FastCGI 缓存（如果使用）
    fastcgi_buffer_size 128k;
    fastcgi_buffers 4 256k;
    fastcgi_busy_buffers_size 256k;

    # 连接超时
    keepalive_timeout 65;
    send_timeout 60;
}
```

### 3. 安全加固

```nginx
server {
    # ... 其他配置 ...

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }

    # 限制请求方法
    if ($request_method !~ ^(GET|HEAD|POST|PUT|DELETE|OPTIONS)$ ) {
        return 405;
    }

    # 防止点击劫持
    add_header X-Frame-Options "SAMEORIGIN" always;
    
    # 启用 XSS 过滤
    add_header X-XSS-Protection "1; mode=block" always;
    
    # 禁止内容类型嗅探
    add_header X-Content-Type-Options "nosniff" always;
    
    # 内容安全策略（根据实际需求调整）
    add_header Content-Security-Policy "default-src 'self' https:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https:; style-src 'self' 'unsafe-inline' https:; img-src 'self' data: https:; font-src 'self' data: https:;" always;
    
    # HSTS（强制 HTTPS）
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}
```

### 4. 日志轮转配置

创建 `/etc/logrotate.d/cogni-x`：

```
/var/log/nginx/cogni-x-*.log {
    daily
    rotate 14
    missingok
    notifempty
    compress
    delaycompress
    sharedscripts
    postrotate
        [ -f /var/run/nginx.pid ] && kill -USR1 `cat /var/run/nginx.pid`
    endscript
}
```

---

## ⚡ 构建优化配置

项目已配置完整的代码压缩、混淆和性能优化，确保生产环境的代码安全性和加载速度。

### 优化特性

#### 1. 代码压缩和混淆
- ✅ **Terser 压缩**：深度压缩 JavaScript 代码
- ✅ **变量名混淆**：混淆变量名、函数名，提升代码安全性
- ✅ **删除注释**：移除所有代码注释
- ✅ **移除调试代码**：自动删除 `console.log`、`debugger` 等调试语句

#### 2. Gzip 压缩
- ✅ **自动生成 .gz 文件**：大于 10KB 的文件自动生成 Gzip 压缩版本
- ✅ **减少传输体积**：通常可减少 60-80% 的文件大小
- ✅ **保留原文件**：同时保留原文件和压缩文件

#### 3. 代码分割（Code Splitting）
- ✅ **Vue 框架独立打包**：`vue`、`vue-router`、`pinia` 单独打包
- ✅ **UI 框架独立打包**：`element-plus` 单独打包
- ✅ **第三方库分类**：`axios`、`markdown-it`、`highlight.js` 等分别打包
- ✅ **提升缓存命中率**：框架代码更新频率低，用户缓存利用率高

#### 4. 构建分析
- ✅ **可视化报告**：构建后生成 `dist/stats.html` 分析报告
- ✅ **体积分析**：查看每个模块的体积占比
- ✅ **依赖关系**：可视化查看模块依赖关系

### 配置说明

所有优化配置位于 `vite.config.ts`：

```typescript
build: {
  // Terser 压缩配置
  minify: 'terser',
  terserOptions: {
    compress: {
      drop_console: true,      // 移除 console
      drop_debugger: true,     // 移除 debugger
      pure_funcs: ['console.log', 'console.info'],
      passes: 2                // 压缩两遍
    },
    mangle: {
      toplevel: true,          // 混淆顶级作用域变量名
    }
  }
}
```

### 构建优化效果

典型的优化效果（实际效果取决于项目大小）：

| 优化项 | 优化前 | 优化后 | 提升 |
|--------|--------|--------|------|
| **JS 体积** | ~800KB | ~350KB | 56% ↓ |
| **Gzip 后** | ~280KB | ~120KB | 57% ↓ |
| **首屏加载** | 2.5s | 1.2s | 52% ↑ |
| **代码可读性** | 完全可读 | 混淆难读 | 🔒 安全 |

### 禁用优化（开发调试）

如果需要在生产构建时保留 console 或禁用混淆（用于调试），可以修改 `vite.config.ts`：

```typescript
terserOptions: {
  compress: {
    drop_console: false,  // 保留 console
    drop_debugger: false, // 保留 debugger
  },
  mangle: false           // 禁用混淆
}
```

---

## 🔍 环境变量说明

### 开发环境 `.env.development`

| 变量名 | 说明 | 示例值 | 必需 |
|--------|------|--------|------|
| `VITE_API_BASE_URL` | API 基础路径 | `http://localhost:9898/api` | ✅ |
| `VITE_API_WS_URL` | WebSocket 连接地址 | `ws://127.0.0.1:9898/api` | ✅ |

### 生产环境 `.env.production`

| 变量名 | 说明 | 示例值 | 必需 |
|--------|------|--------|------|
| `VITE_API_BASE_URL` | API 完整地址 | `https://api.yourdomain.com` | ✅ |
| `VITE_API_WS_URL` | WebSocket 连接地址 | `wss://api.yourdomain.com` | ✅ |
| `BASE_URL` | 前端部署基础路径 | `/` 或 `/app/` | ❌ |

**注意**：
- 所有以 `VITE_` 开头的变量都会被 Vite 处理并在前端代码中可用
- `BASE_URL` 影响路由和静态资源路径，部署在子路径时必须配置
- 生产环境变量在构建时会被编译到代码中，修改后需要重新构建

---

## 🐛 常见问题与解决方案

### 1. 刷新页面出现 404

**原因**：Vue Router 使用 History 模式，需要服务器支持。

**解决方案**：确保 Nginx 配置了 `try_files $uri $uri/ /index.html;`

### 2. API 请求跨域问题

**原因**：前端域名与后端 API 域名不一致。

**解决方案**：
- 方案 A：通过 Nginx 代理 API（推荐）
- 方案 B：后端配置 CORS 允许跨域请求

### 3. 静态资源 404

**原因**：`BASE_URL` 配置不正确或部署路径不匹配。

**解决方案**：
- 检查 `.env.production` 中的 `BASE_URL` 配置
- 确保 Nginx 配置的路径与实际部署路径一致

### 4. CSS/JS 文件加载失败

**原因**：资源路径错误或缓存问题。

**解决方案**：
```bash
# 清除浏览器缓存或强制刷新（Ctrl + F5）
# 检查 Network 面板查看实际请求路径
# 确认 dist 目录已完整上传
```

### 5. 构建后文件过大

**解决方案**：
- 启用 Nginx Gzip 压缩
- 检查是否引入了不必要的大型依赖
- 考虑配置代码分割和懒加载

### 6. WebSocket 连接失败

**原因**：Nginx 代理未正确配置 WebSocket 支持。

**解决方案**：在 Nginx API 代理配置中添加：
```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

### 7. 权限问题导致 Nginx 无法访问文件

**解决方案**：
```bash
# 设置正确的文件权限
sudo chown -R www-data:www-data /var/www/cogni-x
sudo chmod -R 755 /var/www/cogni-x
```

---

## 📋 部署检查清单

部署前请确认以下事项：

- [ ] 已正确配置生产环境变量 `.env.production`
- [ ] 已执行构建命令 `npm run build`
- [ ] `dist` 目录文件已完整上传到服务器
- [ ] Nginx 配置文件已正确编写并测试通过
- [ ] 已配置 SSL 证书（生产环境必需）
- [ ] 已配置 API 代理或 CORS
- [ ] 已启用 Gzip 压缩
- [ ] 已配置静态资源缓存
- [ ] 已测试 Vue Router 路由刷新是否正常
- [ ] 已测试 API 请求是否正常
- [ ] 已配置日志记录和监控

---

## 📞 技术支持

如遇到部署问题，请检查：
1. Nginx 错误日志：`/var/log/nginx/cogni-x-error.log`
2. 浏览器控制台错误信息
3. 网络请求详情（Network 面板）

---

## 📄 许可证

[根据您的项目许可证填写]

---

**最后更新时间**：2025-10-13

