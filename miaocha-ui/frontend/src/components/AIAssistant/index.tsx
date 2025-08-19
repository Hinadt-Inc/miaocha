import { useState, useEffect, useRef, useCallback, memo } from 'react';
import { FloatButton, Modal, Button, Space, Typography, Card, message, Input, Avatar } from 'antd';
import {
  SearchOutlined,
  SendOutlined,
  RobotOutlined,
  UserOutlined,
  DragOutlined,
  CloseOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import { Welcome } from '@ant-design/x';
import Draggable from 'react-draggable';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import rehypeRaw from 'rehype-raw';
import { AIAssistantProvider } from './context/AIAssistantContext';
import { ThinkingIndicator } from './components/ThinkingIndicator';
import { fetchLogDetails, fetchLogHistogram, fetchDistributions } from '../../api/logs';
import styles from './index.module.less';
import markdownStyles from './markdown.module.less';
import './highlight.css';

const { Title } = Typography;

interface IAIAssistantProps {
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

const AIAssistantComponent: React.FC<IAIAssistantProps> = ({ onLogSearch, onFieldSelect, onTimeRangeChange }) => {
  const [open, setOpen] = useState(false);
  const [currentTab, setCurrentTab] = useState<'chat' | 'suggestions' | 'history'>('chat');
  const [messages, setMessages] = useState<IMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [suggestions] = useState([]);
  const [searchHistory] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [disabled, setDisabled] = useState(true);
  const [bounds, setBounds] = useState({ left: 0, top: 0, bottom: 0, right: 0 });
  const [executingActions] = useState(new Set<string>()); // 防止重复执行action
  const [conversationId, setConversationId] = useState<string | null>(null); // 会话ID，用于上下文对话
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<any>(null);
  const draggleRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 计算拖拽边界 - 使用useCallback优化性能
  const onStart = useCallback((_event: any, uiData: any) => {
    const { clientWidth, clientHeight } = window.document.documentElement;
    const targetRect = draggleRef.current?.getBoundingClientRect();
    if (!targetRect) {
      return;
    }
    setBounds({
      left: -targetRect.left + uiData.x,
      right: clientWidth - (targetRect.right - uiData.x),
      top: -targetRect.top + uiData.y,
      bottom: clientHeight - (targetRect.bottom - uiData.y),
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
  const handleActionExecution = async (messageId: string, toolName: string, payload: any) => {
    // 生成唯一的action标识符，防止重复执行
    const actionKey = `${toolName}_${JSON.stringify(payload)}_${messageId}`;

    // 如果这个action正在执行，直接返回
    if (executingActions.has(actionKey)) {
      console.log('Action正在执行中，跳过重复调用:', actionKey);
      return;
    }

    // 标记action开始执行
    executingActions.add(actionKey);

    try {
      let result;
      switch (toolName) {
        case 'sendSearchLogDetailsAction':
          result = await executeLogSearchAction(payload);
          break;
        case 'sendSearchLogHistogramAction':
          result = await executeLogHistogramAction(payload);
          break;
        case 'sendSearchFieldDistributionAction':
          result = await executeFieldDistributionAction(payload);
          break;
        default:
          result = { error: `未知的工具类型: ${toolName}` };
      }

      // 更新消息结果
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === messageId
            ? {
                ...msg,
                content: result.message || `${toolName} 执行完成`,
                actionData: {
                  ...msg.actionData!,
                  result,
                  loading: false,
                },
              }
            : msg,
        ),
      );
    } catch (error) {
      console.error('执行action失败:', error);
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === messageId
            ? {
                ...msg,
                content: `${toolName} 执行失败`,
                actionData: {
                  ...msg.actionData!,
                  result: { error: error instanceof Error ? error.message : '执行失败' },
                  loading: false,
                },
              }
            : msg,
        ),
      );
    } finally {
      // 执行完成后移除标记
      executingActions.delete(actionKey);
    }
  };

  // 执行日志搜索action
  const executeLogSearchAction = async (payload: ILogSearchParams) => {
    console.log('🔍 执行日志搜索:', payload);
    try {
      const result = await fetchLogDetails(payload);
      console.log('✅ 日志搜索完成:', result);

      // 如果有回调函数，调用它来更新外部状态，只传递结果，不重复触发请求
      if (onLogSearch) {
        console.log('📤 调用onLogSearch回调');
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
    console.log('📊 执行日志直方图:', payload);
    console.log('📊 详细参数检查:', {
      datasourceId: payload.datasourceId,
      module: payload.module,
      startTime: payload.startTime,
      endTime: payload.endTime,
      timeRange: payload.timeRange,
      timeGrouping: payload.timeGrouping,
      offset: payload.offset,
      keywords: payload.keywords,
    });

    try {
      const result = await fetchLogHistogram(payload);
      console.log('✅ 日志直方图完成:', result);
      console.log('📊 完整的API响应:', JSON.stringify(result, null, 2));
      console.log('📊 直方图数据结构检查:', {
        hasDistributionData: !!result.distributionData,
        distributionDataLength: result.distributionData?.length || 0,
        firstDistribution: result.distributionData?.[0],
      });

      // 检查实际的直方图数据
      if (result.distributionData?.[0]?.distributionData) {
        const histogramPoints = result.distributionData[0].distributionData;
        console.log('📊 直方图数据点总数:', histogramPoints.length);
        console.log('📊 前5个数据点:', histogramPoints.slice(0, 5));

        const nonZeroPoints = histogramPoints.filter((point: any) => point.count > 0);
        console.log('📊 非零count的数据点数量:', nonZeroPoints.length);
        if (nonZeroPoints.length > 0) {
          console.log('📊 前5个非零数据点:', nonZeroPoints.slice(0, 5));
        }

        const totalCount = histogramPoints.reduce((sum: number, point: any) => sum + (point.count || 0), 0);
        console.log('📊 所有数据点的count总和:', totalCount);
      } else {
        console.log('📊 ❌ 没有找到distributionData或distributionData为空');
      }

      // 如果有时间范围变更回调，触发它，但不重复请求
      if (onTimeRangeChange && (payload.timeRange || (payload.startTime && payload.endTime))) {
        console.log('📤 调用onTimeRangeChange回调');
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
    console.log('执行字段分布:', payload);
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

  // 渲染单个消息 - Grok风格
  const renderMessage = (message: IMessage) => {
    const isUser = message.role === 'user';

    return (
      <div
        key={message.id}
        className={`${styles.messageWrapper} ${isUser ? styles.userMessage : styles.assistantMessage}`}
      >
        <div className={styles.messageContent}>
          {!isUser && (
            <div className={styles.messageAvatar}>
              <Avatar size={36} icon={<RobotOutlined />} className={styles.avatarBot} />
            </div>
          )}

          <div className={styles.messageBubble}>
            <div className={styles.messageText}>
              {isUser ? (
                message.content
              ) : (
                <div className={markdownStyles.markdownContent}>
                  <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight, rehypeRaw]}>
                    {message.content}
                  </ReactMarkdown>
                </div>
              )}
            </div>

            <div className={styles.messageTime}>
              {new Date(message.timestamp).toLocaleTimeString('zh-CN', {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </div>
          </div>

          {isUser && (
            <div className={styles.messageAvatar}>
              <Avatar size={36} icon={<UserOutlined />} className={styles.avatarUser} />
            </div>
          )}
        </div>
      </div>
    );
  };

  // 提示集配置
  const promptItems = [
    {
      key: 'error-logs',
      label: '🚨 错误日志查询',
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
      label: '⚡ 性能分析',
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
      label: '📊 业务日志',
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
  ];

  // SSE流式API调用函数
  const callAIAPIStream = async (messageContent: string) => {
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

      setMessages((prev) => [
        ...prev,
        {
          id: aiMessageId,
          role: 'assistant',
          content: '',
          timestamp: Date.now(),
        },
      ]);

      try {
        const requestBody = {
          message: messageContent,
          ...(conversationId && { conversationId }), // 如果有conversationId则传递，没有则不传
        };

        fetch('/api/ai/session', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
            Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
          },
          body: JSON.stringify(requestBody),
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
                            console.log('🎯 接收到action:', data.toolName, data.payload);
                            const actionMessageId = (Date.now() + Math.random()).toString();
                            const actionMessage: IMessage = {
                              id: actionMessageId,
                              role: 'assistant',
                              content: `正在执行 ${data.toolName}...`,
                              timestamp: Date.now(),
                              type: 'action',
                              actionData: {
                                toolName: data.toolName,
                                payload: data.payload,
                                loading: true,
                              },
                            };

                            setMessages((prev) => [...prev, actionMessage]);
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
            setLoading(false);
            setMessages((prev) =>
              prev.map((msg) =>
                msg.id === aiMessageId ? { ...msg, content: '抱歉，AI服务暂时不可用，请稍后重试。' } : msg,
              ),
            );
            reject(error instanceof Error ? error : new Error(String(error)));
          });
      } catch (error) {
        console.error('创建SSE请求失败:', error);
        setLoading(false);
        reject(error instanceof Error ? error : new Error(String(error)));
      }
    });
  };

  // 清空对话，开始新会话
  const handleClearChat = () => {
    setMessages([]);
    setConversationId(null);
    setCurrentTab('chat');
  };

  // 发送消息处理函数
  const handleSendMessage = async (messageContent: string) => {
    if (!messageContent.trim()) return;
    setInputValue('');

    try {
      await callAIAPIStream(messageContent);
    } catch (error) {
      console.error('发送消息失败:', error);
      message.error('AI服务暂时不可用，请稍后重试');
    }
  };

  // 自定义输入框组件
  const CustomInput = () => {
    return (
      <div className={styles.customInputContainer}>
        <div className={styles.inputWrapper}>
          <Input.TextArea
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder={getPlaceholderText()}
            autoSize={{ minRows: 1, maxRows: 4 }}
            className={styles.customTextArea}
            onPressEnter={(e) => {
              if (!e.shiftKey) {
                e.preventDefault();
                handleSendMessage(inputValue);
              }
            }}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={() => handleSendMessage(inputValue)}
            disabled={!inputValue.trim() || loading}
            className={styles.sendButton}
          />
        </div>
        <div className={styles.inputHint}>
          <span>按 Enter 发送，Shift + Enter 换行</span>
          {conversationId && <span className={styles.sessionHint}>· 上下文对话中</span>}
        </div>
      </div>
    );
  };

  const getPlaceholderText = () => {
    const baseText = conversationId ? '继续对话...' : '开始新对话...';
    switch (currentTab) {
      case 'chat':
        return conversationId ? '继续询问日志分析相关问题...' : '询问任何关于日志分析的问题...';
      case 'suggestions':
        return '选择上面的建议或输入自定义查询...';
      case 'history':
        return '重新执行历史查询或输入新的查询...';
      default:
        return baseText;
    }
  };

  const renderChatInterface = () => (
    <div className={styles.chatInterface}>
      <div className={styles.chatContainer}>
        {messages.length === 0 && !loading ? (
          <div className={styles.welcomeContainer}>
            <div className={styles.welcomeHeader}>
              <Avatar size={64} icon={<RobotOutlined />} className={styles.welcomeAvatar} />
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
              <h3 className={styles.sectionTitle}>💡 智能建议</h3>
              <div className={styles.promptCards}>
                {promptItems.map((item) => (
                  <div key={item.key} className={styles.promptCard}>
                    <div className={styles.promptCardHeader}>
                      <span className={styles.promptCardTitle}>{item.label}</span>
                    </div>
                    <div className={styles.promptCardContent}>
                      {item.children?.map((child) => (
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

            <div className={styles.quickActionsSection}>
              <h3 className={styles.sectionTitle}>🚀 快速开始</h3>
              <div className={styles.quickActions}>
                <Button
                  className={styles.quickActionButton}
                  onClick={() => handleSendMessage('查找最近1小时内的错误日志')}
                >
                  查找错误日志
                </Button>
                <Button className={styles.quickActionButton} onClick={() => handleSendMessage('分析最近的异常情况')}>
                  分析异常情况
                </Button>
                <Button
                  className={styles.quickActionButton}
                  onClick={() => handleSendMessage('显示所有可用的日志模块')}
                >
                  查看日志模块
                </Button>
              </div>
            </div>
          </div>
        ) : (
          <div className={styles.messagesContainer}>
            {messages.map((message) => renderMessage(message))}
            {loading && (
              <div className={styles.messageWrapper}>
                <div className={styles.messageContent}>
                  <div className={styles.messageAvatar}>
                    <Avatar size={36} icon={<RobotOutlined />} className={styles.avatarBot} />
                  </div>
                  <div className={styles.messageBubble}>
                    <ThinkingIndicator />
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>
    </div>
  );

  const renderSuggestions = () => (
    <div className={styles.suggestionsContainer}>
      <Title level={4}>智能建议</Title>
      {suggestions.length === 0 ? (
        <Welcome
          variant="filled"
          icon="💡"
          title="智能建议"
          description="基于您的使用习惯，我会为您推荐常用的查询建议"
          extra={
            <Button type="primary" onClick={() => setCurrentTab('chat')}>
              开始对话
            </Button>
          }
        />
      ) : (
        <Space direction="vertical" size="middle" className={styles.suggestionsList}>
          {suggestions.map((item: any) => (
            <Card
              key={item.id}
              size="small"
              hoverable
              className={styles.suggestionCard}
              actions={[
                <Button
                  key="execute"
                  type="primary"
                  size="small"
                  icon={<SearchOutlined />}
                  onClick={() => handleSendMessage(item.query)}
                >
                  执行查询
                </Button>,
              ]}
            >
              <Card.Meta title={item.title} description={item.description} />
            </Card>
          ))}
        </Space>
      )}
    </div>
  );

  const renderHistory = () => (
    <div className={styles.historyContainer}>
      <Title level={4}>搜索历史</Title>
      {searchHistory.length === 0 ? (
        <Welcome
          variant="filled"
          icon="📚"
          title="搜索历史"
          description="您的查询历史将显示在这里，方便您快速重新执行之前的查询"
          extra={
            <Button type="primary" onClick={() => setCurrentTab('chat')}>
              开始查询
            </Button>
          }
        />
      ) : (
        <Space direction="vertical" size="middle" className={styles.historyList}>
          {searchHistory.map((item: any) => (
            <Card
              key={item.id}
              size="small"
              hoverable
              className={styles.historyCard}
              actions={[
                <Button key="rerun" type="default" size="small" onClick={() => handleSendMessage(item.query)}>
                  重新执行
                </Button>,
              ]}
            >
              <Card.Meta title={item.query} description={`${item.timestamp} · ${item.results} 条结果`} />
            </Card>
          ))}
        </Space>
      )}
    </div>
  );

  const tabs = [
    { key: 'chat', label: '💬 对话', content: renderChatInterface() },
    { key: 'suggestions', label: '💡 建议', content: renderSuggestions() },
    { key: 'history', label: '📚 历史', content: renderHistory() },
  ];

  // 拖拽标题栏组件 - 使用React.memo优化性能
  const DraggableTitle = memo(() => (
    <div
      className={styles.draggableTitle}
      onMouseEnter={() => setDisabled(false)}
      onMouseLeave={() => setDisabled(true)}
    >
      <div className={styles.draggableTitleLeft}>
        <DragOutlined style={{ color: '#666' }} />
        <span className={styles.draggableTitleText}>AI 智能助手</span>
      </div>
      <div className={styles.draggableTitleRight}>
        <div className={styles.drawerTabs}>
          {tabs.map((tab) => (
            <Button
              key={tab.key}
              type={currentTab === tab.key ? 'primary' : 'text'}
              onClick={() => setCurrentTab(tab.key as any)}
              size="small"
              className={styles.tabButton}
            >
              {tab.label}
            </Button>
          ))}
        </div>
        {/* 清空对话按钮，只在有对话记录时显示 */}
        {messages.length > 0 && (
          <Button
            type="text"
            icon={<ClearOutlined />}
            onClick={handleClearChat}
            size="small"
            className={styles.clearButton}
            title="清空对话，开始新会话"
          />
        )}
        <Button
          type="text"
          icon={<CloseOutlined />}
          onClick={() => setOpen(false)}
          size="small"
          className={styles.closeButton}
        />
      </div>
    </div>
  ));

  return (
    <>
      {/* 悬浮按钮 */}
      <FloatButton
        icon={<RobotOutlined />}
        tooltip="AI 智能助手"
        onClick={() => setOpen(true)}
        className={styles.floatButton}
      />

      {/* 可拖拽的弹框 */}
      <Modal
        title={<DraggableTitle />}
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={600}
        className={styles.draggableModal}
        mask={false}
        modalRender={(modal) => (
          <Draggable
            disabled={disabled}
            bounds={bounds}
            onStart={(event, uiData) => onStart(event, uiData)}
            onDrag={handleDragStart}
            onStop={handleDragStop}
            handle=".ant-modal-header"
            nodeRef={draggleRef}
            enableUserSelectHack={false}
            scale={1}
            defaultPosition={{ x: 0, y: 0 }}
            cancel=".ant-btn, .ant-input, button"
          >
            <div ref={draggleRef}>{modal}</div>
          </Draggable>
        )}
        destroyOnClose={false}
        maskClosable={false}
      >
        <div className={styles.modalContent}>
          <div className={styles.drawerContent}>{tabs.find((tab) => tab.key === currentTab)?.content}</div>

          {/* 输入框 */}
          <div className={styles.inputContainer}>
            <CustomInput />
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
