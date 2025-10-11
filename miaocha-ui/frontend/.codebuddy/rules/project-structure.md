# 项目结构规范

## 目录结构
```
src/
├── api/                    # API 接口定义
│   ├── auth.ts            # 认证相关 API
│   ├── datasource.ts      # 数据源 API
│   └── ...
├── assets/                # 静态资源
│   └── login/            # 登录页面资源
├── components/            # 通用组件
│   ├── AIAssistant/      # AI 助手组件
│   ├── common/           # 基础通用组件
│   └── ...
├── config/               # 配置文件
├── hooks/                # 全局自定义 Hooks
├── layouts/              # 布局组件
│   └── MainLayout/       # 主布局
├── pages/                # 页面组件
│   ├── Home/            # 首页（日志查询）
│   ├── SQLEditor/       # SQL 编辑器
│   ├── system/          # 系统管理页面
│   └── ...
├── providers/            # Context Providers
├── routes/               # 路由配置
├── store/                # Redux 状态管理
├── styles/               # 全局样式
├── types/                # 全局类型定义
└── utils/                # 工具函数
```

## 组件目录结构
```
ComponentName/
├── index.tsx             # 主组件文件
├── components/           # 子组件
│   ├── SubComponent.tsx
│   └── index.ts         # 导出文件
├── hooks/               # 组件专用 Hooks
│   ├── useComponentHook.ts
│   └── index.ts
├── types.ts             # 类型定义
├── constants.ts         # 常量定义
├── utils.ts             # 工具函数
└── styles.ts            # 样式定义
```

## 页面目录结构
```
PageName/
├── index.tsx            # 页面主文件
├── components/          # 页面专用组件
├── hooks/              # 页面专用 Hooks
├── utils/              # 页面专用工具
├── types.ts            # 页面类型定义
└── constants.ts        # 页面常量
```

## 文件命名规范
- **组件文件**: PascalCase.tsx (如 UserProfile.tsx)
- **Hook 文件**: camelCase.ts (如 useUserData.ts)
- **工具文件**: camelCase.ts (如 formatUtils.ts)
- **类型文件**: types.ts 或 index.ts
- **常量文件**: constants.ts
- **样式文件**: styles.ts 或 index.ts

## 导入导出规范
```typescript
// 统一使用 index.ts 作为模块入口
// components/index.ts
export { default as Button } from './Button';
export { default as Modal } from './Modal';

// 页面中的导入
import { Button, Modal } from '@/components';
```

## 路径别名配置
- `@/` 指向 `src/` 目录
- 使用绝对路径导入，避免相对路径

## 代码组织原则
1. **单一职责**: 每个文件只负责一个功能
2. **模块化**: 相关功能组织在同一目录
3. **可复用**: 通用组件和工具函数抽离
4. **可维护**: 清晰的目录结构和命名
5. **可扩展**: 预留扩展空间