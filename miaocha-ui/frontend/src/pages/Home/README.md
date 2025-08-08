# Home页面模块化重构说明

## 📁 文件结构

```
src/pages/Home/
├── hooks/                          # 自定义hooks模块
│   ├── index.ts                    # hooks导出索引
│   ├── useHomePageState.ts         # 状态管理hook
│   ├── useDataRequests.ts          # 数据请求相关hooks
│   ├── useUrlParams.ts             # URL参数处理hook
│   ├── useOAuthCallback.ts         # OAuth回调处理hook
│   └── useBusinessLogic.ts         # 业务逻辑处理hook
├── types.ts                        # 类型定义
├── constants.ts                    # 常量定义
├── utils.ts                        # 工具函数（原有）
├── index.tsx                       # 重构后的主入口组件
└── README.md                       # 模块化说明文档
```

## 🔧 模块化功能说明

### 1. 类型定义 (`types.ts`)
- 统一管理所有接口类型定义
- 包含搜索参数、响应数据、组件Ref等类型
- 便于类型复用和维护

### 2. 常量定义 (`constants.ts`)
- 提取所有魔法数字和字符串常量
- 包含默认参数、存储键名、URL参数等
- 增强代码可读性和维护性

### 3. 状态管理 (`hooks/useHomePageState.ts`)
- 集中管理所有组件状态
- 包含数据状态、配置状态、初始化状态等
- 统一管理所有ref引用

### 4. 数据请求 (`hooks/useDataRequests.ts`)
- 封装所有API请求逻辑
- 包含模块列表、日志详情、时间分布、模块配置等请求
- 统一错误处理和数据格式化

### 5. URL参数处理 (`hooks/useUrlParams.ts`)
- 处理分享链接参数解析
- 管理SessionStorage参数持久化
- 支持相对时间范围重新计算

### 6. OAuth回调 (`hooks/useOAuthCallback.ts`)
- 独立处理CAS登录回调逻辑
- 管理登录状态更新
- 清理登录相关参数

### 7. 业务逻辑 (`hooks/useBusinessLogic.ts`)
- 封装复杂的业务逻辑处理
- 包含分享参数应用、数据请求防抖、模块切换等
- 提供可复用的事件处理函数

## 🚀 使用方式

### 主组件
```tsx
// 使用模块化的Home组件
import HomePage from './index';
```

### 单独使用hooks
```tsx
import { useHomePageState, useLogDetails } from './hooks';

const MyComponent = () => {
  const state = useHomePageState();
  const logDetails = useLogDetails(state.moduleQueryConfig);
  
  // 使用模块化的状态和功能
  return <div>...</div>;
};
```

## ✨ 重构优势

### 1. **代码组织清晰**
- 按功能模块分离代码
- 单一职责原则
- 易于理解和维护

### 2. **可复用性强**
- 独立的hooks可在其他组件中复用
- 类型定义和常量可跨组件共享
- 业务逻辑解耦合

### 3. **测试友好**
- 每个hook可以独立测试
- 业务逻辑与UI分离
- 易于编写单元测试

### 4. **维护性好**
- 修改某个功能只需关注对应模块
- 减少代码重复
- 统一的错误处理和状态管理

### 5. **TypeScript支持**
- 完整的类型定义
- 编译时错误检查
- 更好的IDE支持

## 🔄 迁移完成

### ✅ 已完成的迁移
- ✅ 创建了完整的模块化文件结构
- ✅ 重构了主组件使用新的hooks架构
- ✅ 删除了旧的单体组件文件
- ✅ 更新了文档说明

### 🎯 当前状态
- **主组件**: `index.tsx` 使用模块化架构
- **Hooks**: 完整的hooks模块用于状态管理和业务逻辑
- **类型安全**: 完整的TypeScript类型定义
- **代码组织**: 按功能模块清晰分离

## 📋 最佳实践

1. **导入顺序**：
   ```tsx
   // 1. React相关
   import { useState, useEffect } from 'react';
   
   // 2. 第三方库
   import { Splitter } from 'antd';
   
   // 3. 自定义hooks
   import { useHomePageState, useDataRequests } from './hooks';
   
   // 4. 类型和常量
   import type { ILogSearchParams } from './types';
   import { createDefaultSearchParams } from './constants';
   
   // 使用时调用工厂函数
   const defaultParams = createDefaultSearchParams();
   
   // 5. 组件
   import SearchBar from './SearchBar';
   ```

2. **Hook使用顺序**：
   ```tsx
   const MyComponent = () => {
     // 1. 状态管理
     const state = useHomePageState();
     
     // 2. 副作用处理
     useOAuthCallback();
     const { cleanupUrlParams } = useUrlParams(/*...*/);
     
     // 3. 数据请求
     const modulesList = useModulesList();
     const getDetailData = useLogDetails(state.moduleQueryConfig);
     
     // 4. 业务逻辑
     const { handleSelectedModuleChange } = useBusinessLogic(/*...*/);
     
     // 5. 组件逻辑
     // ...
   };
   ```

3. **错误处理**：
   - 每个hook内部处理自己的错误
   - 统一的错误日志格式
   - 优雅的降级处理

4. **性能优化**：
   - 使用useMemo优化props计算
   - 使用useCallback稳定函数引用
   - 避免不必要的重新渲染

## 🔍 注意事项

1. **保持兼容性**：原有的`index.tsx`文件完全保持不变，确保现有功能正常运行
2. **类型安全**：所有新代码都有完整的TypeScript类型定义
3. **性能考虑**：重构不会影响现有性能，部分逻辑还进行了优化
4. **测试覆盖**：建议为新的hooks编写单元测试
5. **文档更新**：团队成员需要了解新的代码组织方式
