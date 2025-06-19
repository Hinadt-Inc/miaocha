import { sql } from '@codemirror/lang-sql';
import { EditorView, drawSelection, highlightActiveLine, keymap } from '@codemirror/view';
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language';
import { tags } from '@lezer/highlight';
import { Extension, SelectionRange } from '@codemirror/state';
import { defaultKeymap, historyKeymap } from '@codemirror/commands';

// 定义语法高亮样式
export const sqlHighlightStyle = HighlightStyle.define([
  { tag: tags.keyword, color: '#0000ff', fontWeight: 'bold' },
  { tag: tags.string, color: '#a31515' },
  { tag: tags.variableName, color: '#001080' },
  { tag: tags.comment, color: '#008000' },
  { tag: tags.number, color: '#098658' },
  { tag: tags.operator, color: '#000000' },
  { tag: tags.punctuation, color: '#000000' },
]);

// 定义主题
export const sqlTheme = EditorView.theme({
  '&': {
    backgroundColor: '#F5F5F5',
    color: '#000000',
    height: '100%',
  },
  '.cm-content': {
    caretColor: '#8B0000',
  },
  '.cm-cursor': {
    borderLeftColor: '#8B0000',
  },
  '.cm-activeLine': {
    backgroundColor: '#F8F8F8',
  },
  '.cm-gutters': {
    backgroundColor: '#F5F5F5',
    color: '#2B91AF',
    border: 'none',
  },
  '.cm-activeLineGutter': {
    backgroundColor: '#F8F8F8',
  },
  '.cm-selectionBackground': {
    backgroundColor: '#ADD6FF',
  },
  '.cm-focused .cm-selectionBackground': {
    backgroundColor: '#ADD6FF',
  },
  '.cm-selectionMatch': {
    backgroundColor: '#E5EBF1',
  },
});

// 将所有扩展组合成一个
export const sqlExtensions = [
  sql(), // SQL 语言支持
  syntaxHighlighting(sqlHighlightStyle), // 语法高亮样式
  sqlTheme, // 编辑器主题
  EditorView.lineWrapping, // 自动换行
  drawSelection({
    // 增强选择显示，提供更好的单行选择支持
    cursorBlinkRate: 1000, // 光标闪烁频率（毫秒）
    drawRangeCursor: true, // 绘制范围光标
  }),
  highlightActiveLine(), // 高亮当前行
  keymap.of([...defaultKeymap, ...historyKeymap]), // 标准键盘映射
  // 增强选择交互
  EditorView.domEventHandlers({
    mousedown: (_event, _view) => {
      // 记录鼠标按下事件，用于改进选择检测
      console.log('编辑器鼠标按下');
      return false; // 不阻止默认行为
    },
  }),
];

/**
 * 初始化 CodeMirror 编辑器配置
 * @returns CodeMirror SQL 配置扩展
 */
const initCodeMirrorEditor = (): Extension => {
  // 返回编辑器配置扩展
  return sqlExtensions;
};

export default initCodeMirrorEditor;
