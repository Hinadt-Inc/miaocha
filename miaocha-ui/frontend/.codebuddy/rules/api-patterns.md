# API 调用模式

## API 函数定义
```typescript
// src/api/feature.ts
export interface FeatureRequest {
  // 请求参数类型
}

export interface FeatureResponse {
  // 响应数据类型
}

export const getFeatureData = async (params: FeatureRequest): Promise<FeatureResponse> => {
  const response = await axios.get('/api/feature', { params });
  return response.data;
};
```

## React Query 使用模式
```typescript
// 查询数据
const useFeatureData = (params: FeatureRequest) => {
  return useQuery({
    queryKey: ['feature', params],
    queryFn: () => getFeatureData(params),
    enabled: !!params.id, // 条件查询
  });
};

// 变更数据
const useUpdateFeature = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: updateFeature,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature'] });
    },
  });
};
```

## 错误处理模式
- 统一的错误拦截器
- 用户友好的错误提示
- 网络错误重试机制
- 权限错误处理

## 加载状态管理
- 全局加载状态
- 组件级加载状态
- 防抖和节流处理

## 数据缓存策略
- 查询结果缓存
- 乐观更新
- 后台数据同步

## 分页数据处理
```typescript
const usePaginatedData = (params: PaginationParams) => {
  return useInfiniteQuery({
    queryKey: ['data', params],
    queryFn: ({ pageParam = 1 }) => fetchData({ ...params, page: pageParam }),
    getNextPageParam: (lastPage) => lastPage.hasMore ? lastPage.page + 1 : undefined,
  });
};
```

## 实时数据处理
- WebSocket 连接管理
- SSE (Server-Sent Events) 处理
- 数据流订阅和取消订阅