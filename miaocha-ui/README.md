# 日志管理系统前端

本项目是日志管理系统的前端部分，基于React开发。

## 开发注意事项

### 项目结构

```
log-manage-ui/
├── src/main/frontend/      # 前端源码目录
│   ├── public/             # 静态资源目录
│   │   ├── favicon.ico     # 网站图标
│   │   └── ...             # 其他静态资源
│   ├── src/                # React源码
│   ├── index.html          # HTML模板
│   ├── package.json        # 依赖配置
│   └── vite.config.js      # Vite配置文件
└── pom.xml                 # Maven配置
```

### 静态资源配置

1. **重要**：所有静态资源（如favicon.ico、logo图片、全局样式表等）应放在 `public/` 目录下
2. 该目录下的文件在构建时会被直接复制到输出目录的根目录
3. 在HTML或组件中引用时，使用根路径：`/favicon.ico`（不需要加`public/`前缀）

### 编译输出要求

为确保前端资源能被正确打包成jar并集成到后端项目中，编译输出必须满足以下要求：

1. **输出目录结构**：

   ```
   dist/
   ├── index.html           # 应用入口HTML
   ├── favicon.ico          # 网站图标（来自public目录）
   ├── assets/              # 编译后的JS/CSS文件
   │   ├── index-[hash].js
   │   ├── index-[hash].css
   │   └── ...
   └── ...                  # 其他从public目录复制的静态资源
   ```
2. **Vite配置要求**：
   - 确保`vite.config.js`中设置了正确的`build.outDir`路径
   - 配置`build.assetsDir`为"assets"
   - 设置`build.emptyOutDir`为true，确保每次构建前清空输出目录

   示例配置：

   ```js
   export default {
     build: {
       outDir: 'dist',
       assetsDir: 'assets',
       emptyOutDir: true
     }
   }
   ```
3. **Maven集成注意事项**：
   - 编译输出的所有文件将被打包到jar的`META-INF/resources/`目录下
   - 确保`pom.xml`中正确配置了前端构建插件和资源复制配置
   - 默认情况下，`dist/`目录中的所有内容将作为静态资源供Spring Boot访问

### 特别注意事项

1. **favicon.ico**：
   - 必须放在`public/`目录下，确保被复制到dist根目录
   - 确保index.html中包含引用：`<link rel="icon" href="/favicon.ico" />`
2. **路径配置**：
   - 所有API请求应使用相对路径，如`/api/users`而非绝对URL
   - 前端路由应使用`BrowserRouter`而非`HashRouter`，以支持后端SPA路由配置
3. **构建命令**：
   - 开发模式：`npm run dev`
   - 生产构建：`npm run build`
   - Maven集成构建：自动触发`npm run build`

## 常见问题

1. **静态资源404**：
   - 检查资源是否正确放在`public/`目录
   - 检查构建后的`dist/`目录是否包含该资源
   - 确认WebMvcConfig中的资源处理配置正确
2. **前端路由刷新404**：
   - 这是由于后端需要配置SPA路由转发
   - 确认WebMvcConfig中包含了将非API、非静态资源请求转发到index.html的配置
3. **API请求问题**：
   - 检查后端API路径是否正确
   - 验证跨域(CORS)配置是否正确
   - 确认请求中包含必要的认证信息

