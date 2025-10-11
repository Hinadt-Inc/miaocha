# 测试指南

## 测试策略
- 单元测试：测试独立的函数和组件
- 集成测试：测试组件间的交互
- E2E 测试：测试完整的用户流程

## 测试工具
- **测试框架**: Vitest
- **测试库**: @testing-library/react
- **断言库**: Vitest 内置
- **Mock 工具**: Vitest Mock

## 组件测试模式
```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Component from './Component';

describe('Component', () => {
  it('should render correctly', () => {
    render(<Component />);
    expect(screen.getByText('Expected Text')).toBeInTheDocument();
  });

  it('should handle user interaction', () => {
    const mockHandler = vi.fn();
    render(<Component onAction={mockHandler} />);
    
    fireEvent.click(screen.getByRole('button'));
    expect(mockHandler).toHaveBeenCalled();
  });
});
```

## Hooks 测试模式
```typescript
import { renderHook, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { useCustomHook } from './useCustomHook';

describe('useCustomHook', () => {
  it('should return initial state', () => {
    const { result } = renderHook(() => useCustomHook());
    expect(result.current.state).toBe(initialState);
  });

  it('should update state on action', () => {
    const { result } = renderHook(() => useCustomHook());
    
    act(() => {
      result.current.actions.updateState(newValue);
    });
    
    expect(result.current.state).toBe(newValue);
  });
});
```

## API 测试模式
```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { getApiData } from './api';

vi.mock('axios');
const mockedAxios = vi.mocked(axios);

describe('API Functions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should fetch data successfully', async () => {
    const mockData = { id: 1, name: 'Test' };
    mockedAxios.get.mockResolvedValue({ data: mockData });

    const result = await getApiData(1);
    
    expect(mockedAxios.get).toHaveBeenCalledWith('/api/data/1');
    expect(result).toEqual(mockData);
  });
});
```

## 测试覆盖率要求
- 组件测试覆盖率 > 80%
- 工具函数测试覆盖率 > 90%
- API 函数测试覆盖率 > 85%

## 测试最佳实践
1. **测试命名**: 使用描述性的测试名称
2. **测试隔离**: 每个测试独立运行
3. **Mock 策略**: 合理使用 Mock 避免外部依赖
4. **断言清晰**: 使用明确的断言语句
5. **测试数据**: 使用有意义的测试数据

## 特定场景测试
1. **异步操作测试**
2. **错误处理测试**
3. **用户交互测试**
4. **路由跳转测试**
5. **表单验证测试**