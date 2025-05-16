import { vi } from 'vitest';

// Basic monaco-editor mock
vi.mock('monaco-editor', () => ({
  editor: {
    create: vi.fn(() => ({
      onDidChangeModelContent: vi.fn(),
      dispose: vi.fn(),
      getModel: vi.fn(),
      setValue: vi.fn(),
    })),
    setTheme: vi.fn(),
    defineTheme: vi.fn(),
  },
  languages: {
    register: vi.fn(),
    setLanguageConfiguration: vi.fn(),
    setMonarchTokensProvider: vi.fn(),
    registerCompletionItemProvider: vi.fn(),
    CompletionItemKind: {
      Text: 0,
      Method: 1,
      Function: 2,
      Constructor: 3,
      Field: 4,
      Variable: 5,
      Class: 6,
      Interface: 7,
      Module: 8,
      Property: 9,
      Unit: 10,
      Value: 11,
      Enum: 12,
      Keyword: 13,
      Snippet: 14,
      Color: 15,
      File: 16,
      Reference: 17,
      Folder: 18,
      EnumMember: 19,
      Constant: 20,
      Struct: 21,
      Event: 22,
      Operator: 23,
      TypeParameter: 24,
    },
  },
  KeyMod: {
    CtrlCmd: 2048,
    Shift: 1024,
    Alt: 512,
    WinCtrl: 256,
  },
  KeyCode: {
    Enter: 13,
    Space: 32,
    Tab: 9,
  },
}));

// Mock @monaco-editor/react
vi.mock('@monaco-editor/react', () => ({
  default: vi.fn((props) => {
    return {
      $$typeof: Symbol.for('react.element'),
      type: 'div',
      props: {
        'data-testid': 'monaco-mock',
        children: props.value,
        onChange: props.onChange,
      },
    };
  }),
}));
