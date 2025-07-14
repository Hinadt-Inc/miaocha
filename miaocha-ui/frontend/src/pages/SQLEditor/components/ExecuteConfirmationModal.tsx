import React, { useRef, useEffect, useState } from 'react';
import { Modal, Button, Space, Alert } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import * as monaco from 'monaco-editor';

import { initMonacoEditorLocally } from '../utils/monacoLocalInit';
import styles from './ExecuteConfirmationModal.module.less';

interface ExecuteConfirmationModalProps {
  visible: boolean;
  sql: string;
  onConfirm: () => void;
  onCancel: () => void;
  onSqlChange?: (value: string) => void; // 新增SQL变化回调
  loading?: boolean;
  title?: React.ReactNode;
  readonly?: boolean; // 新增只读模式属性
}

const ExecuteConfirmationModal: React.FC<ExecuteConfirmationModalProps> = ({
  visible,
  sql,
  onConfirm,
  onCancel,
  onSqlChange,
  loading = false,
  title,
  readonly = false,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const onSqlChangeRef = useRef(onSqlChange);
  const [editorLoading, setEditorLoading] = useState(true);
  const [monacoInitialized, setMonacoInitialized] = useState(false);

  // 保持onSqlChange引用最新
  useEffect(() => {
    onSqlChangeRef.current = onSqlChange;
  }, [onSqlChange]);

  // 一次性初始化Monaco Editor
  useEffect(() => {
    if (!monacoInitialized) {
      // 🎯 使用完全本地化的Monaco初始化 - 只初始化一次
      initMonacoEditorLocally();
      setMonacoInitialized(true);

      // 额外的Promise rejection处理器，专门处理ExecuteConfirmationModal的Monaco错误
      const handleRejection = (event: PromiseRejectionEvent) => {
        const reason = event.reason;
        const reasonString = reason?.toString() || '';
        const reasonStack = reason?.stack || '';

        // 检查是否是Monaco相关的取消错误
        if (
          reasonString.includes('Canceled') ||
          reasonStack.includes('Delayer.cancel') ||
          reasonStack.includes('monaco') ||
          reasonStack.includes('chunk-RWT5L')
        ) {
          event.preventDefault();
          // 完全静默，不输出任何信息
        }
      };

      window.addEventListener('unhandledrejection', handleRejection);

      // 组件卸载时清理
      return () => {
        window.removeEventListener('unhandledrejection', handleRejection);
      };
    }
  }, []); // 空依赖数组，只在组件挂载时执行一次

  // 初始化编辑器 - 完全本地化
  useEffect(() => {
    if (!visible || !containerRef.current || !monacoInitialized) return;

    let isMounted = true;
    let disposables: monaco.IDisposable[] = [];

    const initEditor = () => {
      try {
        setEditorLoading(true);

        // 如果编辑器已经存在，检查是否需要重新创建（例如readonly状态变化）
        if (editorRef.current) {
          const currentReadonly = editorRef.current.getOption(monaco.editor.EditorOption.readOnly);
          if (currentReadonly === readonly) {
            // 配置没有变化，不需要重新创建
            setEditorLoading(false);
            return;
          } else {
            // 配置变化了，清理旧编辑器
            editorRef.current.dispose();
            editorRef.current = null;
          }
        }

        // 🎯 直接使用已初始化的Monaco实例
        if (!window.monaco) {
          console.error('Monaco Editor 未初始化');
          setEditorLoading(false);
          return;
        }

        // 确保容器存在且组件未卸载
        if (!containerRef.current || !isMounted) {
          console.log('容器不存在或组件已卸载，跳过编辑器创建');
          setEditorLoading(false);
          return;
        }

        // 使用全局Monaco实例，包装在try-catch中以捕获内部错误
        const editor = window.monaco.editor.create(containerRef.current, {
          value: '', // 使用空字符串作为初始值，通过单独的useEffect来设置内容
          language: 'sql',
          theme: 'vs-dark',
          automaticLayout: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          readOnly: readonly,
          contextmenu: true,
          selectOnLineNumbers: true,
          lineNumbers: 'on',
          fontSize: 14,
          fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
        });

        editorRef.current = editor;

        // 监听内容变化
        if (!readonly && onSqlChangeRef.current) {
          const changeDisposable = editor.onDidChangeModelContent(() => {
            if (isMounted) {
              try {
                const value = editor.getValue();
                onSqlChangeRef.current?.(value);
              } catch (e) {
                // 静默处理获取值时的错误
                if (!e?.toString().includes('Canceled')) {
                  console.warn('获取编辑器值时出错:', e);
                }
              }
            }
          });
          disposables.push(changeDisposable);
        }

        // 设置初始内容
        if (sql) {
          editor.setValue(sql);
        }

        setEditorLoading(false);
        // 移除日志输出，避免每次都打印成功信息
      } catch (error) {
        // 静默处理Monaco内部的取消错误
        const errorString = error?.toString() || '';
        if (errorString.includes('Canceled') || errorString.includes('monaco')) {
          // 静默处理Monaco内部错误
          setEditorLoading(false);
        } else {
          console.error('❌ ExecuteConfirmationModal 编辑器初始化失败:', error);
          setEditorLoading(false);
        }
      }
    };

    initEditor();

    // 清理函数
    return () => {
      isMounted = false;

      // 清理所有disposables - 静默处理错误
      disposables.forEach((disposable) => {
        try {
          disposable.dispose();
        } catch (e) {
          // 静默处理disposable清理错误，特别是Canceled错误
          if (!e?.toString().includes('Canceled')) {
            console.warn('清理disposable时出错:', e);
          }
        }
      });

      // 清理编辑器 - 静默处理错误
      if (editorRef.current) {
        try {
          editorRef.current.dispose();
          editorRef.current = null;
        } catch (e) {
          // 静默处理编辑器清理错误，特别是Canceled错误
          if (!e?.toString().includes('Canceled')) {
            console.warn('清理编辑器时出错:', e);
          }
        }
      }
    };
  }, [visible, monacoInitialized, readonly]); // 移除 onSqlChange 依赖

  // 更新编辑器内容
  useEffect(() => {
    if (editorRef.current && editorRef.current.getValue() !== sql) {
      editorRef.current.setValue(sql);
    }
  }, [sql]);

  return (
    <Modal
      title={title || '确认执行'}
      open={visible}
      onCancel={onCancel}
      width={800}
      maskClosable={false}
      footer={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" icon={<PlayCircleOutlined />} loading={loading} onClick={onConfirm}>
            执行
          </Button>
        </Space>
      }
    >
      {readonly && (
        <Alert message="此模块已有SQL语句，无法编辑" type="info" className={styles.readonlyAlert} showIcon />
      )}
      <div className={styles.editorWrapper}>
        <div ref={containerRef} className={styles.executeModalEditorContainer} />
        {editorLoading && (
          <div className={styles.executeModalLoading}>
            <div>正在加载编辑器...</div>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default ExecuteConfirmationModal;
