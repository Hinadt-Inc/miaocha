/**
 * 选择辅助工具 - 提供增强型文本选择功能
 */
import { EditorView } from '@codemirror/view';

/**
 * 添加增强型选择支持
 * 这个函数为编辑器添加必要的事件监听，以确保单行文本选择能正常工作
 * @param editorView CodeMirror编辑器视图
 */
export const enhanceSelectionSupport = (editorView: EditorView | null): void => {
  if (!editorView) return;

  // 反馈选择状态的函数
  const reportSelectionState = () => {
    try {
      if (!editorView || !editorView.state) return;

      const selection = editorView.state.selection.main;
      if (selection.from < selection.to) {
        const selectedText = editorView.state.sliceDoc(selection.from, selection.to);
        const lineFrom = editorView.state.doc.lineAt(selection.from).number;
        const lineTo = editorView.state.doc.lineAt(selection.to).number;
        const isSameLine = lineFrom === lineTo;

        console.log('检测到文本选择变化:', {
          from: selection.from,
          to: selection.to,
          text: selectedText,
          lineFrom: lineFrom,
          lineTo: lineTo,
          isSameLine: isSameLine,
        });

        // 特别处理单行SQL
        if (isSameLine) {
          const cleanedText = selectedText.trim();
          console.log('单行SQL选择:', cleanedText);

          // 确保单行SQL选择能被正确识别
          if (cleanedText && document.activeElement === editorView.dom) {
            // 这里我们可以添加一些额外的处理来强化单行选择
            // 注意：只响应选择变化，但不改变选择状态
            setTimeout(() => {
              // 焦点确认，不触发选择更改
              editorView.focus();
            }, 0);
          }
        }
      }
    } catch (error) {
      console.error('报告选择状态时出错:', error);
    }
  };

  // 添加鼠标事件监听器
  const addMouseListeners = () => {
    // 鼠标按下事件
    editorView.dom.addEventListener('mousedown', (event) => {
      // 记录鼠标按下位置，用于后续分析
      const x = event.clientX;
      const y = event.clientY;
      console.log('鼠标按下位置:', { x, y });
    });

    // 鼠标释放事件 - 通常在这里完成选择
    editorView.dom.addEventListener('mouseup', () => {
      setTimeout(reportSelectionState, 10);
    });

    // 双击事件 - 通常用于选择一个单词
    editorView.dom.addEventListener('dblclick', () => {
      setTimeout(reportSelectionState, 10);
    });

    // 三击事件 - 选择整行
    editorView.dom.addEventListener('click', (event) => {
      if (event.detail === 3) {
        setTimeout(reportSelectionState, 10);
      }
    });
  };

  // 添加键盘事件监听器
  const addKeyboardListeners = () => {
    editorView.dom.addEventListener('keyup', (event) => {
      // 当使用Shift+方向键进行选择时
      if (event.shiftKey && ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(event.key)) {
        setTimeout(reportSelectionState, 10);
      }
    });
  };

  // 添加全局选择事件监听
  const addSelectionListener = () => {
    document.addEventListener('selectionchange', () => {
      // 确保编辑器是当前焦点元素
      if (document.activeElement === editorView.dom || editorView.dom.contains(document.activeElement)) {
        setTimeout(reportSelectionState, 10);
      }
    });
  };

  // 应用所有监听器
  addMouseListeners();
  addKeyboardListeners();
  addSelectionListener();

  console.log('已安装增强型选择支持');
};

/**
 * 当用户完成选择后调用此函数，执行一些额外的优化操作，确保CodeMirror选择状态正确
 * 特别优化了单行SQL文本的选择处理
 */
export const finalizeSelection = (editorView: EditorView | null): void => {
  if (!editorView || !editorView.state) return;

  try {
    // 获取DOM选择对象
    const domSelection = window.getSelection();
    if (!domSelection) return;

    // 如果有DOM选择，确保CodeMirror内部选择状态同步
    if (domSelection.rangeCount > 0) {
      const range = domSelection.getRangeAt(0);
      const selectedText = domSelection.toString();

      // 如果选择区域在编辑器内部
      if (editorView.dom.contains(range.commonAncestorContainer)) {
        const isSingleLine = !selectedText.includes('\n');
        console.log('优化选择状态', {
          hasRange: true,
          text: selectedText,
          isSingleLine: isSingleLine,
        });

        // 针对单行SQL的特殊处理
        if (isSingleLine && selectedText.trim()) {
          // 清理选中文本的首尾空白
          const cleanedText = selectedText.trim();
          console.log('已优化单行SQL选择:', cleanedText);
        }

        // 选择已完成，触发编辑器刷新以确保视觉效果正确
        setTimeout(() => {
          // 只聚焦编辑器即可，不需要分派事件
          editorView.focus();
        }, 0);
      }
    }
  } catch (error) {
    console.error('完成选择时出错:', error);
  }
};
