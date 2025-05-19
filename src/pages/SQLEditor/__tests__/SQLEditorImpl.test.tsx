import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import SQLEditorImpl from '../SQLEditorImpl';

// 导入模拟组件
import './__mocks__/components';

// 为所有自定义hooks创建模拟
vi.mock('../hooks/useDataSources', () => ({
  useDataSources: () => ({
    dataSources: [
      { id: 'ds1', name: 'MySQL测试', type: 'mysql', host: 'localhost' },
      { id: 'ds2', name: 'PostgreSQL测试', type: 'postgresql', host: 'localhost' },
    ],
    selectedSource: 'ds1',
    setSelectedSource: vi.fn(),
    loading: false,
  }),
}));

vi.mock('../hooks/useQueryExecution', () => {
  const executeQueryMock = vi.fn().mockResolvedValue({
    status: 'success',
    columns: ['id', 'name', 'age'],
    rows: [
      { id: 1, name: '张三', age: 30 },
      { id: 2, name: '李四', age: 25 },
    ],
    executionTimeMs: 100,
  });

  return {
    useQueryExecution: () => ({
      queryResults: {
        status: 'success',
        columns: ['id', 'name', 'age'],
        rows: [
          { id: 1, name: '张三', age: 30 },
          { id: 2, name: '李四', age: 25 },
        ],
        executionTimeMs: 100,
      },
      loading: false,
      sqlQuery: 'SELECT * FROM users',
      setSqlQuery: vi.fn(),
      executeQuery: executeQueryMock,
    }),
  };
});

vi.mock('../hooks/useDatabaseSchema', () => ({
  useDatabaseSchema: () => ({
    databaseSchema: {
      databaseName: 'test_db',
      tables: [
        {
          tableName: 'users',
          tableComment: '用户表',
          columns: [
            {
              columnName: 'id',
              dataType: 'int',
              columnComment: '用户ID',
              isPrimaryKey: true,
              isNullable: false,
            },
            {
              columnName: 'name',
              dataType: 'varchar',
              columnComment: '用户名',
              isPrimaryKey: false,
              isNullable: false,
            },
            {
              columnName: 'age',
              dataType: 'int',
              columnComment: '年龄',
              isPrimaryKey: false,
              isNullable: true,
            },
          ],
        },
        {
          tableName: 'orders',
          tableComment: '订单表',
          columns: [
            {
              columnName: 'order_id',
              dataType: 'int',
              columnComment: '订单ID',
              isPrimaryKey: true,
              isNullable: false,
            },
            {
              columnName: 'user_id',
              dataType: 'int',
              columnComment: '用户ID',
              isPrimaryKey: false,
              isNullable: false,
            },
            {
              columnName: 'amount',
              dataType: 'decimal',
              columnComment: '金额',
              isPrimaryKey: false,
              isNullable: false,
            },
          ],
        },
      ],
    },
    loadingSchema: false,
    fetchDatabaseSchema: vi.fn(),
  }),
}));

vi.mock('../hooks/useEditorSettings', () => ({
  useEditorSettings: () => ({
    settings: {
      fontSize: 14,
      theme: 'vs',
      wordWrap: 'on',
      minimap: { enabled: false },
      formatOnPaste: true,
      autoIndent: true,
    },
    saveSettings: vi.fn(),
  }),
}));

vi.mock('../hooks/useQueryHistory', () => {
  const queryHistory = [
    {
      id: '1',
      sql: 'SELECT * FROM users WHERE id = 1',
      dataSourceId: 'ds1',
      executionTime: Date.now() - 60000,
      status: 'success',
      timestamp: new Date(Date.now() - 60000).toISOString(),
    },
    {
      id: '2',
      sql: 'SELECT * FROM orders',
      dataSourceId: 'ds1',
      executionTime: Date.now() - 120000,
      status: 'success',
      timestamp: new Date(Date.now() - 120000).toISOString(),
    },
  ];

  return {
    useQueryHistory: () => ({
      history: queryHistory,
      addHistory: vi.fn(),
      clearHistory: vi.fn(),
      clearAllHistory: vi.fn(),
      isHistoryOpen: false,
      toggleHistory: vi.fn(),
    }),
  };
});

// 包装组件以提供路由上下文
const SQLEditorWithRouter = () => (
  <BrowserRouter>
    <SQLEditorImpl />
  </BrowserRouter>
);

describe('SQLEditorImpl组件', () => {
  beforeEach(() => {
    // 清理模拟函数
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('应该正确渲染SQL编辑器组件', async () => {
    render(<SQLEditorWithRouter />);

    // 验证主要组件是否渲染
    expect(screen.getByText('SQL 查询')).toBeInTheDocument();
    expect(screen.getByText('查询结果')).toBeInTheDocument();

    // 验证数据库结构树是否渲染
    expect(screen.getByTestId('schema-tree')).toBeInTheDocument();
    expect(screen.getByTestId('table-users')).toBeInTheDocument();
    expect(screen.getByTestId('table-orders')).toBeInTheDocument();
  });

  it('应该正确显示查询结果', async () => {
    render(<SQLEditorWithRouter />);

    // 验证结果查看器是否渲染并显示查询结果
    expect(screen.getByTestId('results-viewer')).toBeInTheDocument();
    expect(screen.getByTestId('result-row-0')).toBeInTheDocument();
    expect(screen.getByTestId('result-row-1')).toBeInTheDocument();
  });

  it('应该可以执行SQL查询', async () => {
    render(<SQLEditorWithRouter />);

    // 查找执行按钮并点击
    const executeButton = screen.getByTestId('execute-query-btn');
    fireEvent.click(executeButton);

    // 验证查询是否执行
    await waitFor(() => {
      // 检查是否显示了查询结果
      expect(screen.getByTestId('results-viewer')).toBeInTheDocument();
      expect(screen.getByTestId('result-stats')).toHaveTextContent('2行结果');
    });
  });

  it('可以切换历史记录抽屉', async () => {
    render(<SQLEditorWithRouter />);

    // 查找历史按钮并点击
    const historyButton = screen.getByTestId('toggle-history-btn');
    fireEvent.click(historyButton);

    // 打开历史抽屉后，点击关闭按钮
    const closeHistoryButton = screen.getByTestId('close-history-btn');
    fireEvent.click(closeHistoryButton);
  });

  it('可以切换设置抽屉', async () => {
    render(<SQLEditorWithRouter />);

    // 查找设置按钮并点击
    const settingsButton = screen.getByTestId('toggle-settings-btn');
    fireEvent.click(settingsButton);

    // 打开设置抽屉后，点击关闭按钮
    const closeSettingsButton = screen.getByTestId('close-settings-btn');
    fireEvent.click(closeSettingsButton);
  });

  it('可以选择不同的数据源', async () => {
    render(<SQLEditorWithRouter />);

    // 选择第二个数据源
    const select = screen.getByTestId('datasource-select');
    fireEvent.change(select, { target: { value: 'ds2' } });

    // 验证setSelectedSource是否被调用
    const mockModule =
      await vi.importMock<typeof import('../hooks/useDataSources')>('../hooks/useDataSources');
    expect(mockModule.useDataSources().setSelectedSource).toHaveBeenCalledWith('ds2');
  });

  it('可以修改SQL编辑器的高度', async () => {
    render(<SQLEditorWithRouter />);

    // 获取初始高度
    const editor = screen.getByTestId('query-editor');
    const initialHeight = editor.style.height;

    // 点击增加高度按钮
    const increaseButton = screen.getByTestId('increase-height');
    fireEvent.click(increaseButton);

    // 验证高度是否增加
    expect(editor.style.height).not.toBe(initialHeight);
  });

  it('可以插入表到SQL编辑器', async () => {
    render(<SQLEditorWithRouter />);

    // 查找插入表按钮并点击
    const insertTableButton = screen.getByTestId('insert-table-users');
    fireEvent.click(insertTableButton);

    // 验证insertTextToEditor是否被调用
    const { insertTextToEditor } = await import('../utils/editorUtils');
    expect(vi.mocked(insertTextToEditor)).toHaveBeenCalled();
  });

  it('可以双击表名添加到编辑器', async () => {
    render(<SQLEditorWithRouter />);

    // 查找表名并双击
    const tableName = screen.getByTestId('table-name-users');
    fireEvent.doubleClick(tableName);

    // 验证handleTreeNodeDoubleClick是否触发
    const { insertTextToEditor } = await import('../utils/editorUtils');
    expect(vi.mocked(insertTextToEditor)).toHaveBeenCalled();
  });

  it('可以在查询结果和可视化之间切换', async () => {
    render(<SQLEditorWithRouter />);

    // 默认显示查询结果选项卡
    expect(screen.getByTestId('results-viewer')).toBeInTheDocument();

    // 切换到可视化选项卡
    const tabs = screen.getAllByRole('tab');
    const visualizationTab = tabs.find((tab) => tab.textContent === '可视化');

    if (visualizationTab) {
      fireEvent.click(visualizationTab);

      // 验证是否显示了可视化面板
      await waitFor(() => {
        expect(screen.getByTestId('visualization-panel')).toBeInTheDocument();
      });
    }
  });

  it('可以折叠和展开编辑器', async () => {
    render(<SQLEditorWithRouter />);

    // 获取折叠按钮并点击
    const collapseButton = screen.getByTestId('toggle-editor-collapse');
    fireEvent.click(collapseButton);

    // 再次点击展开
    fireEvent.click(collapseButton);
  });

  it('可以折叠和展开侧边栏', async () => {
    render(<SQLEditorWithRouter />);

    // 获取侧边栏折叠按钮并点击
    const toggleSiderButton = screen.getByTestId('toggle-sider-btn');
    fireEvent.click(toggleSiderButton);
  });
});
