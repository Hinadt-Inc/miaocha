import { EditorView } from '@codemirror/view';

/**
 * 检查编辑器中是否有选中的文本
 * @param editor CodeMirror编辑器实例
 * @returns 是否有选中的文本
 */
export const hasSelection = (editor: EditorView | null): boolean => {
  if (!editor || !editor.state) return false;

  try {
    // 获取当前选区
    const selection = editor.state.selection.main;

    // 严格检查选区范围，保证能检测到单行选择
    const hasSelectedContent = selection.from < selection.to;

    if (hasSelectedContent) {
      // 双重检查：获取实际选择的文本内容
      const selectedText = editor.state.sliceDoc(selection.from, selection.to).trim();

      // 如果有选中的文本内容，则确认有选中状态
      return selectedText.length > 0;
    }

    return false;
  } catch (error) {
    console.error('检查选中状态时发生错误:', error);
    return false;
  }
};

/**
 * 获取编辑器中选中的文本
 * @param editor CodeMirror编辑器实例
 * @returns 选中的文本，如果没有选中则返回空字符串
 */
export const getSelectedText = (editor: EditorView | null): string => {
  if (!editor || !editor.state) return '';

  try {
    // 获取主选区
    const selection = editor.state.selection.main;

    // 1. 首先尝试通过常规方法获取选择
    if (selection.from < selection.to) {
      const selectedText = editor.state.sliceDoc(selection.from, selection.to);

      // 如果选中内容为空白，可能需要特别处理
      if (selectedText.trim().length > 0) {
        console.log('选中文本调试:', {
          from: selection.from,
          to: selection.to,
          text: selectedText,
          fromLine: editor.state.doc.lineAt(selection.from).number,
          toLine: editor.state.doc.lineAt(selection.to).number,
        });
        return selectedText;
      }
    } // 2. 如果常规方法失败，尝试检查DOM选择（特别针对单行选择）
    if (window.getSelection) {
      const domSelection = window.getSelection();
      if (domSelection && domSelection.toString().trim()) {
        // 有DOM选择且不为空
        const domSelectedText = domSelection.toString();
        console.log('使用DOM选择获取文本:', domSelectedText);

        // 确认DOM选择文本在编辑器中 (简化检查，实际应用中可能需要更复杂的验证)
        if (editor.state.doc.toString().includes(domSelectedText)) {
          return domSelectedText;
        }
      }
    }

    // 3. 最后尝试获取光标所在行内容 (适用于单行选中的特例)
    const cursorPos = editor.state.selection.main.head;
    const line = editor.state.doc.lineAt(cursorPos);

    // 检查编辑器DOM元素是否包含活跃选择
    const activeElement = document.activeElement;
    if (activeElement && editor.dom.contains(activeElement)) {
      // 如果浏览器认为有选中(但我们前面没有捕获到)，返回当前行
      if (window.getSelection()?.type === 'Range') {
        console.log('检测到单行选择，返回当前行:', line.text);
        return line.text;
      }
    }
  } catch (error) {
    console.error('获取选中文本出错:', error);
  }

  return '';
};

/**
 * 获取活动行内容（光标所在行文本）
 * @param editor CodeMirror编辑器实例
 * @returns 光标所在行的全部文本
 */
export const getCurrentLineText = (editor: EditorView | null): string => {
  if (!editor || !editor.state) return '';

  // 获取光标位置
  const pos = editor.state.selection.main.head;

  // 获取当前行
  const line = editor.state.doc.lineAt(pos);

  return line.text;
};

/**
 * 获取编辑器中全部文本
 * @param editor CodeMirror编辑器实例
 * @returns 编辑器中的全部文本
 */
export const getAllText = (editor: EditorView | null): string => {
  if (!editor || !editor.state) return '';
  return editor.state.doc.toString();
};

/**
 * 清理SQL文本，去除首尾空白和换行符
 * @param text SQL文本
 * @returns 清理后的SQL文本
 */
export const cleanSQLText = (text: string): string => {
  // 移除首尾的空格和换行符
  return text.trim().replace(/^\s*[\r\n]+|\s*[\r\n]+$/g, '');
};

/**
 * 获取编辑器文本（优先获取选中文本，没有选中则获取全部文本）
 * 自动清理SQL文本，去除首尾空白和换行符
 * @param editor CodeMirror编辑器实例
 * @returns 清理后的选中文本或全部文本
 */
export const getEditorText = (editor: EditorView | null): string => {
  if (hasSelection(editor)) {
    const rawText = getSelectedText(editor);
    const cleanedText = cleanSQLText(rawText);
    console.log('使用选中文本执行:', {
      raw: rawText,
      cleaned: cleanedText,
    });
    return cleanedText;
  } else {
    const rawText = getAllText(editor);
    const cleanedText = cleanSQLText(rawText);
    console.log('没有选中文本，使用全部文本执行:', {
      raw: rawText,
      cleaned: cleanedText,
    });
    return cleanedText;
  }
};
