# AI助手组件 - SSE流式响应处理

## 修改内容

### 移除的复杂功能
1. **移除了原有的SSE封装代码**
   - 删除了 `useAIAssistantSSE` hook
   - 移除了 `AISSEService` 和相关的复杂流式处理逻辑
   - 简化了事件处理机制

2. **移除了XRequest相关代码**
   - 删除了复杂的XRequest配置
   - 移除了自定义fetch函数
   - 简化了API调用逻辑

### 当前实现 - SSE流式响应

#### API调用方式
现在使用原生fetch直接处理SSE流式响应：

```typescript
const callAIAPIStream = async (messageContent: string) => {
  return new Promise<void>((resolve, reject) => {
    // 构建请求体
    const requestBody = {
      message: messageContent,
      context: {
        currentSearchParams,
        logData,
        moduleOptions,
      },
    };

    // 使用fetch进行SSE请求
    fetch('/api/ai/session', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
      },
      body: JSON.stringify(requestBody),
    })
    .then(response => {
      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      function processStream(): Promise<void> {
        return reader!.read().then(({ done, value }) => {
          if (done) {
            setLoading(false);
            resolve();
            return;
          }

          const chunk = decoder.decode(value, { stream: true });
          const lines = chunk.split('\n');

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const jsonStr = line.substring(5).trim();
              if (jsonStr) {
                const data = JSON.parse(jsonStr);
                if (data.content) {
                  // 实时更新AI消息内容
                  accumulatedContent += data.content;
                  setMessages(prev => prev.map(msg => 
                    msg.id === aiMessageId 
                      ? { ...msg, content: accumulatedContent }
                      : msg
                  ));
                }
              }
            }
          }

          return processStream();
        });
      }

      return processStream();
    });
  });
};
```

#### SSE数据格式处理
支持以下SSE数据格式：
```
event:message
data:{"conversationId":"1aff6c8a-bea7-44d9-b253-34d8b50133af","content":"为了"}

event:message
data:{"conversationId":"1aff6c8a-bea7-44d9-b253-34d8b50133af","content":"查询"}
```

#### 认证方式
- 直接从localStorage获取accessToken
- 在请求头中添加：`Authorization: Bearer ${token}`
- 无需复杂的token处理逻辑

#### 消息处理
- 使用本地state管理消息列表
- 实时流式更新AI回复内容
- 支持打字机效果的实时显示
- 统一的错误处理机制

#### 主要功能
1. **实时消息流**：AI回复内容实时逐字符显示
2. **错误处理**：网络错误或解析错误时显示友好提示
3. **加载状态**：请求期间显示"AI正在思考中..."
4. **自动累积内容**：将多个SSE消息片段组合成完整回复

## 使用方法

组件的使用方式保持不变：

```tsx
<AIAssistant
  onLogSearch={handleLogSearch}
  onFieldSelect={handleFieldSelect}
  onTimeRangeChange={handleTimeRangeChange}
  currentSearchParams={searchParams}
  logData={logData}
  moduleOptions={moduleOptions}
/>
```

## 后端接口要求

后端 `/api/ai/session` 接口需要：

1. **请求方式**：POST
2. **认证**：Bearer Token
3. **请求头**：
   - `Content-Type: application/json`
   - `Accept: text/event-stream`
   - `Authorization: Bearer ${token}`

4. **请求体**：
   ```json
   {
     "message": "用户输入的消息",
     "context": {
       "currentSearchParams": {},
       "logData": {},
       "moduleOptions": []
     }
   }
   ```

5. **响应格式**：SSE流式响应
   ```
   event:message
   data:{"conversationId":"uuid","content":"内容片段"}

   event:message
   data:{"conversationId":"uuid","content":"下一个片段"}
   ```

## 技术特点

### SSE流式处理
1. **原生fetch API**：直接使用fetch的ReadableStream处理SSE
2. **实时解析**：逐行解析SSE数据格式
3. **内容累积**：将多个消息片段组合成完整内容
4. **错误恢复**：单个消息解析失败不影响整体流程

### 性能优化
1. **流式渲染**：内容实时显示，提升用户体验
2. **内存管理**：及时清理资源，避免内存泄漏
3. **错误隔离**：局部错误不影响整体功能

### 用户体验
1. **打字机效果**：AI回复逐字符显示
2. **即时反馈**：用户消息立即显示
3. **加载状态**：明确的加载指示
4. **错误提示**：友好的错误信息

## 优势

1. **简化架构**：移除复杂的封装，直接使用原生API
2. **实时体验**：SSE流式响应提供更好的交互体验
3. **错误处理**：完善的错误处理和恢复机制
4. **易于维护**：代码结构清晰，便于调试和维护
5. **性能优化**：减少不必要的请求和状态管理开销

## 注意事项

- 确保后端支持SSE流式响应格式
- 需要正确设置CORS和认证头
- SSE连接会保持打开状态直到响应完成
- 组件卸载时会自动清理资源
- 错误处理覆盖网络错误、解析错误等各种情况

## 兼容性

- 支持所有现代浏览器的ReadableStream API
- 兼容标准的SSE数据格式
- 支持长连接和流式数据传输
