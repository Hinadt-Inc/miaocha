import { vi } from 'vitest';

// CodeMirror mocks
vi.mock('@codemirror/state', () => ({
  EditorState: {
    create: vi.fn(() => ({
      doc: {
        toString: vi.fn(),
        lineAt: vi.fn(() => ({ text: 'mock line text' })),
        line: vi.fn(),
        length: 0,
      },
      selection: {
        main: { head: 0, anchor: 0 },
        ranges: [],
      },
    })),
  },
  Extension: {},
}));

vi.mock('@codemirror/view', () => ({
  EditorView: vi.fn(() => ({
    state: {
      doc: {
        toString: vi.fn(),
        lineAt: vi.fn(() => ({ text: 'mock line text' })),
        line: vi.fn(),
        length: 0,
      },
      selection: {
        main: { head: 0, anchor: 0 },
        ranges: [],
      },
    },
    dispatch: vi.fn(),
    focus: vi.fn(),
  })),
}));

vi.mock('@codemirror/lang-sql', () => ({
  sql: vi.fn(() => ({})),
}));

// Mock @uiw/react-codemirror
vi.mock('@uiw/react-codemirror', () => ({
  default: vi.fn((props) => {
    return {
      $$typeof: Symbol.for('react.element'),
      type: 'div',
      props: {
        'data-testid': 'codemirror-mock',
        children: props.value,
        onChange: props.onChange,
      },
    };
  }),
}));
