import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';

import { RobotOutlined, UserOutlined, CloseOutlined, ClearOutlined } from '@ant-design/icons';
import { Sender, Bubble } from '@ant-design/x';
import { Modal, Button, message, Avatar, Tooltip } from 'antd';
import Draggable from 'react-draggable';
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';

import { fetchLogDetails, fetchLogHistogram, fetchDistributions } from '@/api/logs';

import { ThinkingIndicator } from './components/ThinkingIndicator';
import { AIAssistantProvider } from './context/AIAssistantContext';
import styles from './index.module.less';
import markdownStyles from './markdown.module.less';
import './highlight.css';

interface IAIAssistantProps {
  searchParams: ILogSearchParams;
  onLogSearch?: (data: any) => void; // 支持传递搜索参数和结果
  onFieldSelect?: (fields: string[]) => void;
  onTimeRangeChange?: (data: any) => void; // 支持传递时间范围和直方图数据
}

interface IMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  type?: 'text' | 'action';
  actionData?: {
    toolName: string;
    payload: any;
    result?: any;
    loading?: boolean;
  };
}

const AIAssistantComponent: React.FC<IAIAssistantProps> = ({
  onLogSearch,
  onFieldSelect,
  onTimeRangeChange,
  searchParams,
}) => {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<IMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [disabled, setDisabled] = useState(true);
  const [bounds, setBounds] = useState({ left: 0, top: 0, bottom: 0, right: 0 });
  const [executingActions] = useState(new Set<string>()); // 防止重复执行action
  const [conversationId, setConversationId] = useState<string | null>(null); // 会话ID，用于上下文对话
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const draggleRef = useRef<HTMLDivElement>(null);
  const aiDraggleRef = useRef<HTMLDivElement>(null);
  const [aiDragging, setAiDragging] = useState(false); // AI按钮拖拽状态
  const aiTrueDragging = useRef(false); // AI按钮真实拖拽状态
  const [modalRect, setModalRect] = useState({
    width: 700,
    height: window.innerHeight * 0.6,
  });
  const modalContentRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const downPosition = useRef({ x: 0, y: 0 });
  const modalContentElementRect = useRef({
    width: 0,
    height: 0,
    direction: 'l',
    minWidth: 700,
    minHeight: window.innerHeight * 0.6,
    maxWidth: window.innerWidth * 1,
    maxHeight: window.innerHeight - 0,
  });
  const animationFrameRef = useRef<number | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // 自动滚动到底部
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 计算拖拽边界 - 使用useCallback优化性能
  const onStart = useCallback((_event: any, uiData: any) => {
    const { innerWidth, innerHeight } = window;
    const targetRect = draggleRef.current?.getBoundingClientRect();
    if (!targetRect) {
      return;
    }
    setBounds({
      left: -targetRect.left + uiData.x,
      right: innerWidth - (targetRect.right - uiData.x),
      top: -targetRect.top + uiData.y,
      bottom: innerHeight - (targetRect.bottom - uiData.y),
    });
  }, []);

  // 拖拽开始时的处理
  const handleDragStart = useCallback(() => {
    // 拖拽时可以添加一些视觉反馈
    if (draggleRef.current) {
      draggleRef.current.style.cursor = 'grabbing';
    }
  }, []);

  // 拖拽停止时的处理
  const handleDragStop = useCallback(() => {
    // 恢复鼠标样式
    if (draggleRef.current) {
      draggleRef.current.style.cursor = 'grab';
    }
  }, []);

  // 处理action执行
  const handleActionExecution = useCallback(async (messageId: string, toolName: string, payload: any) => {
    // 生成唯一的action标识符，防止重复执行
    const actionKey = `${toolName}_${JSON.stringify(payload)}_${messageId}`;

    // 如果这个action正在执行，直接返回
    if (executingActions.has(actionKey)) {
      return;
    }

    // 标记action开始执行
    executingActions.add(actionKey);

    try {
      switch (toolName) {
        case 'sendSearchLogDetailsAction':
          await executeLogSearchAction(payload);
          break;
        case 'sendSearchLogHistogramAction':
          await executeLogHistogramAction(payload);
          break;
        case 'sendSearchFieldDistributionAction':
          await executeFieldDistributionAction(payload);
          break;
        default:
          break;
      }
    } catch (error) {
      console.error('执行action失败:', error);
    } finally {
      // 执行完成后移除标记
      executingActions.delete(actionKey);
    }
  }, []);

  // 执行日志搜索action
  const executeLogSearchAction = async (payload: ILogSearchParams) => {
    try {
      const result = await fetchLogDetails(payload);

      // 如果有回调函数，调用它来更新外部状态，只传递结果，不重复触发请求
      if (onLogSearch) {
        onLogSearch({
          searchParams: payload,
          searchResult: result,
          skipRequest: true, // 标记跳过重复请求
        });
      }

      return {
        type: 'logSearch',
        params: payload,
        data: result,
        success: result.success,
        totalCount: result.totalCount,
        executionTimeMs: result.executionTimeMs,
        message: `日志搜索完成，找到 ${result.totalCount} 条记录，耗时 ${result.executionTimeMs}ms`,
      };
    } catch (error) {
      console.error('❌ 日志搜索失败:', error);
      throw new Error(error instanceof Error ? error.message : '日志搜索失败');
    }
  };

  // 执行日志直方图action
  const executeLogHistogramAction = async (payload: ILogSearchParams) => {
    try {
      const result = await fetchLogHistogram(payload);
      // 如果有时间范围变更回调，触发它，但不重复请求
      if (onTimeRangeChange && (payload.timeRange || (payload.startTime && payload.endTime))) {
        onTimeRangeChange({
          timeRange: payload.timeRange,
          startTime: payload.startTime,
          endTime: payload.endTime,
          histogramData: result,
          skipRequest: true, // 标记跳过重复请求
        });
      }

      return {
        type: 'logHistogram',
        params: payload,
        data: result,
        distributionCount: result.distributionData?.length || 0,
        message: `日志直方图生成完成，包含 ${result.distributionData?.length || 0} 个时间分布点`,
      };
    } catch (error) {
      console.error('❌ 日志直方图生成失败:', error);
      throw new Error(error instanceof Error ? error.message : '日志直方图生成失败');
    }
  };

  // 执行字段分布action
  const executeFieldDistributionAction = async (payload: ILogSearchParams) => {
    try {
      const result = await fetchDistributions(payload);

      // 如果有回调函数，调用它来更新外部状态
      if (onFieldSelect && payload.fields) {
        onFieldSelect(payload.fields);
      }

      return {
        type: 'fieldDistribution',
        params: payload,
        data: result,
        success: result.success,
        fieldCount: result.fieldDistributions?.length || 0,
        sampleSize: result.sampleSize,
        executionTimeMs: result.executionTimeMs,
        message: `字段分布分析完成，分析了 ${result.fieldDistributions?.length || 0} 个字段，样本大小 ${result.sampleSize}，耗时 ${result.executionTimeMs}ms`,
      };
    } catch (error) {
      console.error('字段分布分析失败:', error);
      throw new Error(error instanceof Error ? error.message : '字段分布分析失败');
    }
  };

  // 渲染单个消息 - 使用 Ant Design X Bubble 组件
  const renderMessage = (message: IMessage) => {
    const isUser = message.role === 'user';
    const isAssistantThinking = !isUser && !message.content && loading;

    // 准备渲染内容
    const getContent = () => {
      if (isUser) {
        return message.content;
      } else if (isAssistantThinking) {
        return <ThinkingIndicator message="正在思考中..." size="medium" theme="default" />;
      } else {
        return (
          <div className={markdownStyles.markdownContent}>
            <ReactMarkdown rehypePlugins={[rehypeHighlight, rehypeRaw]} remarkPlugins={[remarkGfm]}>
              {message.content || ''}
            </ReactMarkdown>
          </div>
        );
      }
    };

    // 准备时间信息
    const getFooter = () => {
      if (isAssistantThinking || !message.content) return undefined;

      return (
        <div className={styles.messageTime}>
          {new Date(message.timestamp).toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
          })}
        </div>
      );
    };

    return (
      <div key={message.id} className={styles.messageWrapper}>
        <Bubble
          avatar={
            isUser ? (
              <Avatar className={styles.avatarUser} icon={<UserOutlined />} size={36} />
            ) : (
              <Avatar className={styles.avatarBot} icon={<RobotOutlined />} size={36} />
            )
          }
          className={isUser ? styles.userMessage : styles.assistantMessage}
          content={getContent()}
          footer={getFooter()}
          placement={isUser ? 'end' : 'start'}
          shape="round"
          variant="filled"
        />
      </div>
    );
  };

  // 提示集配置
  const promptItems = [
    {
      label: '智能建议',
      key: 'intelligent-suggestions',
      children: [
        {
          key: 'error-logs',
          label: '错误日志查询',
          description: '查找系统中的错误和异常日志',
          children: [
            {
              key: 'recent-errors',
              label: '最近错误',
              description: '查找最近1小时内的错误日志，按时间倒序排列',
            },
            {
              key: 'critical-errors',
              label: '严重错误',
              description: '查找所有FATAL级别的错误日志，包含堆栈信息',
            },
            {
              key: 'module-errors',
              label: '模块错误',
              description: '查找指定模块的错误日志，帮我分析错误原因',
            },
          ],
        },
        {
          key: 'performance',
          label: '性能分析',
          description: '分析系统性能相关的日志',
          children: [
            {
              key: 'slow-queries',
              label: '慢查询',
              description: '查找执行时间超过1秒的数据库查询，分析性能瓶颈',
            },
            {
              key: 'high-memory',
              label: '高内存使用',
              description: '查找内存使用率超过80%的日志记录',
            },
            {
              key: 'response-time',
              label: 'API响应时间',
              description: '分析API接口的响应时间，找出响应时间较长的接口',
            },
          ],
        },
        {
          key: 'business',
          label: '业务日志',
          description: '查询业务相关的日志信息',
          children: [
            {
              key: 'user-actions',
              label: '用户操作',
              description: '查找特定用户的操作日志，按时间顺序展示',
            },
            {
              key: 'transaction',
              label: '事务日志',
              description: '查找事务处理失败的日志，分析失败原因',
            },
            {
              key: 'audit',
              label: '审计日志',
              description: '查看系统的审计日志，包含权限变更和敏感操作',
            },
          ],
        },
      ],
    },
    {
      label: '快速开始',
      key: 'quick-start',
      children: [
        {
          label: '',
          key: 'quick',
          children: [
            {
              label: '查找错误日志',
              key: 'find-error-logs',
              description: '查找最近1小时内的错误日志',
            },
            {
              label: '分析异常情况',
              key: 'analyze-exceptions',
              description: '分析最近的异常情况',
            },
            {
              label: '查看日志模块',
              key: 'view-log-modules',
              description: '显示所有可用的日志模块',
            },
          ],
        },
      ],
    },
  ];

  // SSE流式API调用函数
  const callAIAPIStream = useCallback(
    async (messageContent: string) => {
      return new Promise<void>((resolve, reject) => {
        const userMessage: IMessage = {
          id: Date.now().toString(),
          role: 'user',
          content: messageContent,
          timestamp: Date.now(),
        };
        setMessages((prev) => [...prev, userMessage]);
        setLoading(true);

        const aiMessageId = (Date.now() + 1).toString();
        let accumulatedContent = '';

        // 立即创建一个带有思考状态的AI消息
        setMessages((prev) => [
          ...prev,
          {
            id: aiMessageId,
            role: 'assistant',
            content: '', // 空内容，但会显示思考状态
            timestamp: Date.now(),
          },
        ]);

        try {
          let prefix = `用户目前在${searchParams.module}模块下，目前时间范围是${searchParams.startTime} ~ ${searchParams.endTime}`;
          if (searchParams.keywords && searchParams.keywords.length > 0) {
            prefix += `，查询关键字是：${searchParams.keywords.join(',')}`;
          }
          if (searchParams.whereSqls && searchParams.whereSqls.length > 0) {
            prefix += `，查询 where子句为：${searchParams.whereSqls.join(' AND ')}`;
          }
          const requestBody = {
            message: prefix + '\n' + messageContent,
            ...(conversationId && { conversationId }), // 如果有conversationId则传递，没有则不传
          };
          // 取消上一次的请求
          if (abortControllerRef.current) {
            abortControllerRef.current.abort();
          }
          abortControllerRef.current = new AbortController();
          fetch('/api/ai/session', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Accept: 'text/event-stream',
              Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
            },
            body: JSON.stringify(requestBody),
            signal: abortControllerRef.current.signal,
          })
            .then((response) => {
              if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
              }

              const reader = response.body?.getReader();
              if (!reader) {
                throw new Error('无法获取响应流');
              }

              const decoder = new TextDecoder();
              let messageBuffer = '';

              const processStream = (): Promise<void> => {
                return reader.read().then(({ done, value }) => {
                  if (done) {
                    setLoading(false);
                    resolve();
                    return;
                  }

                  const chunk = decoder.decode(value, { stream: true });
                  messageBuffer += chunk;

                  const lines = messageBuffer.split('\n');
                  messageBuffer = lines.pop() || '';

                  for (const line of lines) {
                    if (line.trim() === '') continue;

                    if (line.startsWith('data:')) {
                      try {
                        const jsonStr = line.substring(5).trim();
                        if (jsonStr && jsonStr !== '[DONE]') {
                          const data = JSON.parse(jsonStr);

                          if (data.conversationId) {
                            // 保存会话ID以用于后续对话
                            if (!conversationId) {
                              setConversationId(data.conversationId);
                            }

                            if (data.content !== undefined) {
                              accumulatedContent += data.content;
                              setMessages((prev) =>
                                prev.map((msg) =>
                                  msg.id === aiMessageId ? { ...msg, content: accumulatedContent } : msg,
                                ),
                              );
                            } else if (data.toolName && data.payload) {
                              const actionMessageId = (Date.now() + Math.random()).toString();
                              // 直接执行action，不显示"正在执行"消息
                              handleActionExecution(actionMessageId, data.toolName, data.payload);
                            }
                          }
                        } else if (jsonStr === '[DONE]') {
                          setLoading(false);
                          resolve();
                          return;
                        }
                      } catch (parseError) {
                        console.warn('解析SSE数据失败:', parseError);
                      }
                    }
                  }

                  return processStream();
                });
              };

              return processStream();
            })
            .catch((error) => {
              console.error('SSE请求错误:', error);
              const isAbort = error.message.includes('aborted');
              const msgContent = isAbort ? '已暂停生成' : '抱歉，AI服务暂时不可用，请稍后重试。';
              setLoading(false);
              setMessages((prev) =>
                prev.map((msg) => (msg.id === aiMessageId ? { ...msg, content: msgContent } : msg)),
              );
              reject(error instanceof Error ? error : new Error(String(error)));
            })
            .finally(() => {
              abortControllerRef.current = null;
            });
        } catch (error) {
          console.error('创建SSE请求失败:', error);
          setLoading(false);
          reject(error instanceof Error ? error : new Error(String(error)));
        }
      });
    },
    [conversationId, handleActionExecution, searchParams],
  );

  // 清空对话，开始新会话
  const handleClearChat = () => {
    setMessages([]);
    setConversationId(null);
    setLoading(false);
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
  };

  // 发送消息处理函数
  const handleSendMessage = useCallback(
    async (messageContent: string) => {
      const value = messageContent.trim();
      if (!value) return;
      setInputValue('');
      try {
        await callAIAPIStream(value);
      } catch (error) {
        console.error('发送消息失败:', error);
        message.error('AI服务暂时不可用，请稍后重试');
      }
    },
    [callAIAPIStream],
  );

  // 稳定的事件处理函数 - 移到组件外部避免重新创建
  const handleInputChange = useCallback((value: string) => {
    setInputValue(value);
  }, []);

  const handleInputCancel = () => {
    if (loading) {
      if (abortControllerRef.current) {
        setLoading(false);
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    }
  };

  const handleInputSubmit = useCallback(
    (value: string) => {
      if (value.trim() && !loading) {
        handleSendMessage(value);
      }
    },
    [loading, handleSendMessage],
  );

  // 缓存 placeholder 文本
  const placeholderText = useMemo(() => {
    if (loading) {
      return 'AI 正在思考中，请稍候...';
    }
    return conversationId ? '继续询问日志分析相关问题...' : '询问任何关于日志分析的问题...';
  }, [loading, conversationId]);

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (!modalContentRef.current) return;
    const { clientX, clientY } = e;
    const rect = modalContentRef.current.getBoundingClientRect();
    const directionMap = {
      l: [rect.left, rect.left + 10],
      r: [rect.right - 10, rect.right],
      d: [rect.bottom - 10, rect.bottom],
    };
    let direction = '';
    Object.entries(directionMap).forEach(([key, value]) => {
      if (clientX >= value[0] && clientX <= value[1]) {
        direction += key;
      }
      if (clientY >= value[0] && clientY <= value[1]) {
        direction += key;
      }
    });
    if (!direction) return;
    setIsDragging(true);
    downPosition.current = {
      x: clientX,
      y: clientY,
    };
    modalContentElementRect.current = {
      ...modalContentElementRect.current,
      width: rect.width,
      height: rect.height,
      direction,
    };
  }, []);
  const handleMouseMove = useCallback((e: MouseEvent) => {
    let deltaX = e.clientX - downPosition.current.x;
    let deltaY = e.clientY - downPosition.current.y;
    if (modalContentElementRect.current.direction === 'd') {
      deltaX = 0;
    } else if (modalContentElementRect.current.direction === 'l' || modalContentElementRect.current.direction === 'r') {
      deltaY = 0;
    }
    // x轴方向向量
    const xVector = modalContentElementRect.current.direction.includes('l') ? -1 : 1;
    const x = Math.min(
      modalContentElementRect.current.maxWidth,
      Math.max(modalContentElementRect.current.width + deltaX * xVector * 2, modalContentElementRect.current.minWidth),
    );
    const y = Math.min(
      modalContentElementRect.current.maxHeight,
      Math.max(modalContentElementRect.current.height + deltaY, modalContentElementRect.current.minHeight),
    );
    cancelAnimationFrame(animationFrameRef.current!);
    animationFrameRef.current = requestAnimationFrame(() => {
      setModalRect({ width: x, height: y });
    });
  }, []);
  const handleMouseUp = useCallback(() => {
    cancelAnimationFrame(animationFrameRef.current!);
    setIsDragging(false);
  }, []);
  useEffect(() => {
    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    } else {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    }

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging, handleMouseMove, handleMouseUp]);
  // 拖拽标题栏组件
  const DraggableTitle = () => {
    return (
      <div
        className={styles.draggableTitle}
        onMouseEnter={() => setDisabled(false)}
        onMouseLeave={() => setDisabled(true)}
      >
        <div className={styles.draggableTitleLeft}>
          <span className={styles.draggableTitleText}>AI 智能助手</span>
        </div>
        <div className={styles.draggableTitleRight}>
          {/* 清空对话按钮，只在有对话记录时显示 */}
          {messages.length > 0 && (
            <Button
              className={styles.clearButton}
              icon={<ClearOutlined />}
              size="small"
              title="清空对话，开始新会话"
              type="text"
              onClick={handleClearChat}
            />
          )}
          <Button
            className={styles.closeButton}
            icon={<CloseOutlined />}
            size="small"
            title="关闭AI助手"
            type="text"
            onClick={() => setOpen(false)}
          />
        </div>
      </div>
    );
  };
  return (
    <>
      {/*AI 智能助手*/}
      <div
        style={{
          position: 'fixed',
          zIndex: 999,
          left: 70,
          bottom: 80,
          pointerEvents: 'none',
        }}
      >
        <Draggable
          bounds={{
            left: -70,
            bottom: 80,
            top: -window.innerHeight + 80 + 60,
            right: window.innerWidth - 60 - 70,
          }}
          nodeRef={aiDraggleRef}
          onDrag={() => {
            aiTrueDragging.current = true;
          }}
          onStart={() => setAiDragging(true)}
          onStop={() => {
            setTimeout(() => {
              aiTrueDragging.current = false;
              setAiDragging(false);
            }, 10);
          }}
        >
          <div ref={aiDraggleRef}>
            <Tooltip placement="top" title={aiDragging ? '' : 'AI 智能助手'}>
              <Button
                className={styles.floatButton}
                icon={<RobotOutlined />}
                onClick={() => {
                  if (aiTrueDragging.current) return;
                  setOpen(true);
                }}
              />
            </Tooltip>
          </div>
        </Draggable>
      </div>
      {/* 可拖拽的弹框 */}
      <Modal
        className={styles.draggableModal}
        destroyOnHidden={false}
        footer={null}
        mask={false}
        maskClosable={false}
        modalRender={(modal) => (
          <Draggable
            bounds={bounds}
            cancel=".ant-btn, .ant-input, button"
            defaultPosition={{ x: 0, y: 0 }}
            disabled={disabled}
            enableUserSelectHack={false}
            handle=".ant-modal-header"
            nodeRef={draggleRef}
            scale={1}
            onDrag={handleDragStart}
            onStart={(event, uiData) => onStart(event, uiData)}
            onStop={handleDragStop}
          >
            <div ref={draggleRef}>{modal}</div>
          </Draggable>
        )}
        open={open}
        title={<DraggableTitle />}
        width={modalRect.width}
        onCancel={() => setOpen(false)}
      >
        <div
          ref={modalContentRef}
          className={styles.modalContent}
          style={{ height: `${modalRect.height}px` }}
          onMouseDown={handleMouseDown}
        >
          {/* 鼠标放缩类型 */}
          <div>
            <span className={styles.directionL}></span>
            <span className={styles.directionLD}></span>
            <span className={styles.directionD}></span>
            <span className={styles.directionRD}></span>
            <span className={styles.directionR}></span>
          </div>
          <div className={styles.drawerContent}>
            <div className={styles.chatInterface}>
              {messages.length === 0 ? (
                <div className={styles.welcomeContainer}>
                  <div className={styles.welcomeHeader}>
                    <div className={styles.welcomeContent}>
                      <h2 className={styles.welcomeTitle}>AI 智能助手</h2>
                      <p className={styles.welcomeDescription}>我是您的日志分析专家，可以帮您快速查找和分析日志数据</p>
                      {conversationId && (
                        <div className={styles.sessionStatus}>
                          <span className={styles.sessionIndicator}>💬</span>
                          <span className={styles.sessionText}>当前会话ID: {conversationId.slice(-8)}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className={styles.promptsSection}>
                    {promptItems.map((item) => (
                      <div key={item.key}>
                        <h3 className={styles.sectionTitle}>{item.label}</h3>
                        <div className={styles.promptCards}>
                          {item.children.map((prompt) => (
                            <div key={prompt.key}>
                              {prompt.label && (
                                <div className={styles.promptCardHeader}>
                                  <span className={styles.promptCardTitle}>{prompt.label}</span>
                                </div>
                              )}
                              <div className={styles.promptCardContent}>
                                {prompt.children?.map((child) => (
                                  <div
                                    key={child.key}
                                    className={styles.promptItem}
                                    onClick={() => handleSendMessage(child.description)}
                                  >
                                    {child.label}
                                  </div>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className={styles.messagesContainer}>
                  {messages.map((message) => renderMessage(message))}
                  <div ref={messagesEndRef} />
                </div>
              )}
            </div>
          </div>
          {/* 输入框 */}
          <div className={styles.inputContainer}>
            <div className={`${styles.customInputContainer} ${loading ? styles.inputDisabled : ''}`}>
              <Sender
                key="ai-assistant-sender" // 添加固定key防止重新挂载
                loading={loading}
                placeholder={placeholderText}
                style={{
                  background: '#f7f9fa',
                  borderRadius: '24px',
                  border: '1px solid #e1e8ed',
                }}
                value={inputValue}
                onCancel={handleInputCancel}
                onChange={handleInputChange}
                onSubmit={handleInputSubmit}
              />
              <div className={styles.inputHint}>
                {loading ? (
                  <span className={styles.thinkingHint}>AI 正在思考中...</span>
                ) : (
                  <>
                    <span>按 Enter 发送，Shift + Enter 换行</span>
                    {conversationId && <span className={styles.sessionHint}>· 上下文对话中</span>}
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      </Modal>
    </>
  );
};

// 包装组件，提供Context
export const AIAssistant: React.FC<IAIAssistantProps> = (props) => {
  return (
    <AIAssistantProvider>
      <AIAssistantComponent {...props} />
    </AIAssistantProvider>
  );
};

export default AIAssistant;
