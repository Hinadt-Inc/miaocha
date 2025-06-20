import React, { useState, useEffect, memo, useRef, useCallback } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { Extension } from '@codemirror/state';
import { EditorView } from '@codemirror/view';
import { EditorSettings } from '../types';
import './QueryEditor.less';

import initCodeMirrorEditor from '../utils/codeMirrorInit';
import { getSelectedText } from '../utils/selectionUtils';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount?: (editor: any) => void;
  editorSettings: EditorSettings;
  collapsed?: boolean;
  height?: number | string;
}

/**
 * SQL查询编辑器组件
 * 使用CodeMirror编辑器提供语法高亮和自动完成功能
 * 支持收起/展开功能
 */
const QueryEditor: React.FC<QueryEditorProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings,
  collapsed = false,
  height,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [extensions, setExtensions] = useState<Extension[]>([]);
  const editorViewRef = useRef<EditorView | null>(null);

  useEffect(() => {
    setIsCollapsed(collapsed);
    // 初始化 CodeMirror 编辑器扩展
    const sqlExtensions = initCodeMirrorEditor();
    setExtensions([sqlExtensions]);
  }, [collapsed]);

  // 处理编辑器值变更
  const handleChange = (value: string) => {
    if (onChange) {
      onChange(value);
    }
  };

  // 处理编辑器挂载
  const handleEditorMount = useCallback(
    (viewUpdate: EditorView) => {
      editorViewRef.current = viewUpdate;

      // 添加所有可能导致选中文本变化的事件监听
      const eventHandler = () => {
        const selection = viewUpdate.state.selection.main;
        if (selection.from !== selection.to) {
          // 获取选中文本
          const selectedText = viewUpdate.state.sliceDoc(selection.from, selection.to);
          console.log('编辑器组件检测到选中文本', {
            from: selection.from,
            to: selection.to,
            text: selectedText,
            lineStart: viewUpdate.state.doc.lineAt(selection.from).number,
            lineEnd: viewUpdate.state.doc.lineAt(selection.to).number,
          });
        }
      };

      // 监听多种可能导致选择变化的事件
      viewUpdate.dom.addEventListener('mouseup', eventHandler);
      viewUpdate.dom.addEventListener('keyup', eventHandler);
      viewUpdate.dom.addEventListener('selectionchange', eventHandler);

      // 特别处理单击和双击事件（用于单行选择）
      viewUpdate.dom.addEventListener('click', (event) => {
        if (event.detail === 3) {
          // 三击选中整行
          setTimeout(eventHandler, 10); // 延迟执行确保选择已更新
        }
      });

      viewUpdate.dom.addEventListener('dblclick', () => {
        // 双击选中单词后需要延迟检查
        setTimeout(eventHandler, 10);
      });

      if (onEditorMount) {
        onEditorMount(viewUpdate);
      }
    },
    [onEditorMount],
  );

  // 如果折叠，则返回一个简单的视图
  if (isCollapsed) {
    return (
      <div className="editor-container collapsed">
        <div className="editor-wrapper collapsed" />
      </div>
    );
  }

  return (
    <div className="editor-container">
      <div className="editor-wrapper">
        <CodeMirror
          value={sqlQuery}
          height={typeof height === 'number' && height > 0 ? `${height}px` : '100%'}
          extensions={extensions}
          onChange={handleChange}
          onCreateEditor={handleEditorMount}
          theme={editorSettings.theme === 'vs-dark' ? 'dark' : 'light'}
          className="custom-editor"
          basicSetup={{
            lineNumbers: true,
            foldGutter: true,
            autocompletion: editorSettings.autoComplete,
            highlightActiveLine: true,
            tabSize: editorSettings.tabSize,
            drawSelection: true, // 确保选择高亮
            highlightSelectionMatches: true, // 高亮匹配选择
            highlightActiveLineGutter: true, // 高亮活动行槽
            indentOnInput: true, // 在输入时自动缩进
          }}
        />
      </div>
    </div>
  );
};

export default memo(QueryEditor);
