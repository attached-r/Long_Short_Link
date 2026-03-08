
# Long_Short_Link

一个简单、高性能、**前后端分离**的短链接（URL Shortener）服务，支持生成短链接、302/301跳转、访问日志异步记录、管理后台查看数据。

项目目标：提供轻量、可自托管的短链接解决方案，适合个人、小团队、营销活动等场景。

当前版本：**开发中**（核心功能已可用，后续持续迭代）

[![GitHub stars](https://img.shields.io/github/stars/attached-r/Long_Short_Link?style=social)](https://github.com/attached-r/Long_Short_Link)
[![GitHub forks](https://img.shields.io/github/forks/attached-r/Long_Short_Link?style=social)](https://github.com/attached-r/Long_Short_Link)
[![License](https://img.shields.io/github/license/attached-r/Long_Short_Link)](https://github.com/attached-r/Long_Short_Link/blob/main/LICENSE)

## 功能特性

- 生成短链接（支持自定义短码，后续计划）
- 短链接跳转（302 重定向，支持统计点击）
- 访问日志异步记录（不阻塞主流程）
- 管理后台（Vue 3）：
  - 短链接列表
  - 创建/删除短链接
  - 基础访问记录查看
- 高性能：后端异步处理 + 数据库优化（后续可加 Redis 缓存）

## 技术栈

| 部分     | 技术                          | 说明                              |
|----------|-------------------------------|-----------------------------------|
| 后端     | Java 17+ · Spring Boot        | 核心服务、API、跳转逻辑           |
| 前端     | Vue 3 · Vite                  | 管理后台（Composition API）       |
| 数据库   | MySQL 8+ / PostgreSQL / H2    | 存储链接和访问记录                |
| 构建工具 | Maven（后端） · npm / pnpm    | 前端包管理                        |
| 其他     | CompletableFuture / @Async    | 异步日志记录                      |
| 部署     | JAR 包 / Docker（计划中）     | 支持一键部署                      |

## 项目结构

```
Long_Short_Link/
├── Backend/
│   └── highlink/                  # Spring Boot 后端主项目
│       ├── src/
│       ├── pom.xml
│       └── ...
├── front/
│   └── my-shortlink-admin/        # Vue 3 管理后台
│       ├── src/
│       ├── public/
│       ├── vite.config.js
│       └── package.json
├── .gitignore
└── README.md
```

## 快速开始

### 环境要求

- JDK 17 或更高
- Node.js 18+ / pnpm 8+（推荐）或 npm
- MySQL 8+（或使用 H2 内存数据库开发测试）
- Git

### 步骤

1. 克隆仓库

   ```bash
   git clone https://github.com/attached-r/Long_Short_Link.git
   cd Long_Short_Link
   ```

2. 启动后端

   ```bash
   cd Backend/highlink

   # 安装依赖 & 构建
   mvn clean install

   # 开发模式启动
   mvn spring-boot:run

   # 或者打包后运行
   mvn package
   java -jar target/highlink-*.jar
   ```

   默认端口：**8080**  
   配置文件：`src/main/resources/application.yml`（请修改数据库连接、端口等）

3. 启动前端管理后台

   ```bash
   cd ../../front/my-shortlink-admin

   # 安装依赖（推荐 pnpm，更快）
   pnpm install
   # 或 npm install

   # 启动开发服务器
   pnpm dev
   # 或 npm run dev
   ```

   默认访问地址：**http://localhost:5173**（Vite 默认端口）

4. 使用流程

   1. 打开管理后台 → 登录（当前可能无认证，后续添加）
   2. 输入长链接 → 创建短链接
   3. 复制生成的短链接（如 `http://your-domain/abc123`）
   4. 在浏览器访问短链接 → 自动跳转 + 记录访问
   5. 返回管理后台查看访问记录

## 配置说明

重要配置文件位置：

- 后端：`Backend/highlink/src/main/resources/application.yml`
  - spring.datasource.url / username / password
  - server.port
- 前端：`front/my-shortlink-admin/.env.development` 或 `vite.config.js`
  - VITE_API_BASE_URL=http://localhost:8080

## 常见问题

- 后端启动报数据库连接失败？  
  → 检查 application.yml 中的数据库配置，或先用 H2 内存模式测试

- 前端跨域问题？  
  → 确保后端 CORS 已配置，或临时在 vite.config.js 中设置 proxy

- 短链接跳转不记录？  
  → 检查日志服务是否正常启动，查看控制台是否有异步异常

## 未来规划（Roadmap）

- [ ] 用户认证 & 权限管理
- [ ] 自定义短码规则
- [ ] 短链接过期 / 密码保护
- [ ] 访问统计图表（PV/UV、地域、浏览器、设备等）
- [ ] Redis 缓存短链接映射
- [ ] Docker Compose 一键部署
- [ ] API 文档（Springdoc OpenAPI / Swagger）
- [ ] 批量导入 / 导出功能

## 贡献指南

欢迎 Issue、PR！  
提交代码前请：

1. fork 本仓库
2. 创建 feature/xxx 分支
3. 提交清晰的 commit message
4. 提交 Pull Request

## 许可证

[MIT License](LICENSE)

---

最后更新：2026年3月  

作者：attached

项目地址：https://github.com/attached-r/Long_Short_Link
