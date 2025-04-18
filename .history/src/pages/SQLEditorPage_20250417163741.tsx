import { PageContainer } from '@ant-design/pro-components';
import React, { useState, useEffect, useRef } from 'react';
import { 
  Input, Select, Button, Space, Table, Tabs, 
  Card, message, Tooltip, Modal, Form, 
  Spin, Drawer, Radio, Divider 
} from 'antd';
import { 
  PlayCircleOutlined, SaveOutlined, HistoryOutlined, 
  DeleteOutlined, DownloadOutlined, LineChartOutlined, 
  DatabaseOutlined, CopyOutlined, FileTextOutlined,
  ReloadOutlined, SettingOutlined
} from '@ant-design/icons';
import axios from 'axios';
// 注意: 需要安装 @monaco-editor/react
import Editor from '@monaco-editor/react';
import ReactECharts from 'echarts-for-react';
import './SQLEditorPage.less';

const { Option } = Select;
const { TabPane } = Tabs;
const { TextArea } = Input;

interface DataSource {
  id: string;
  name: string;
  type: string;
  host: string;
}

interface QueryResult {
  columns: string[];
  rows: any[];
  total: number;
  executionTime: number;
  status: 'success' | 'error';
  message?: string;
}

interface SavedQuery {
  id: string;
  name: string;
  sql: string;
  dataSourceId: string;
  createdAt: string;
  updatedAt: string;
}

interface QueryHistory {
  id: string;
  sql: string;
  dataSourceId: string;
  executionTime: number;
  status: 'success' | 'error';
  timestamp: string;
}

const SQLEditorPage: React.FC = () => {
  // 状态管理
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [selectedDataSource, setSelectedDataSource] = useState<string>('');
  const [sqlQuery, setSqlQuery] = useState<string>('SELECT * FROM users LIMIT 10;');
  const [queryResults, setQueryResults] = useState<QueryResult | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<string>('results');
  const [queryHistory, setQueryHistory] = useState<QueryHistory[]>([]);
  const [savedQueries, setSavedQueries] = useState<SavedQuery[]>([]);
  const [saveModalVisible, setSaveModalVisible] = useState<boolean>(false);
  const [historyDrawerVisible, setHistoryDrawerVisible] = useState<boolean>(false);
  const [queryName, setQueryName] = useState<string>('');
  const [visualization, setVisualization] = useState<'table' | 'chart'>('table');
  const [chartType, setChartType] = useState<'bar' | 'line' | 'pie'>('bar');
  const [xAxisField, setXAxisField] = useState<string>('');
  const [yAxisField, setYAxisField] = useState<string>('');
  
  const editorRef = useRef<any>(null);
  const [form] = Form.useForm();

  // 模拟加载数据源
  useEffect(() => {
    // 实际应用中，这里应该调用API获取数据源列表
    const mockDataSources: DataSource[] = [
      { id: '1', name: 'MySQL生产库', type: 'mysql', host: 'mysql-prod.example.com' },
      { id: '2', name: 'PostgreSQL分析库', type: 'postgresql', host: 'pg-analytics.example.com' },
      { id: '3', name: 'Clickhouse数仓', type: 'clickhouse', host: 'clickhouse.example.com' },
    ];
    setDataSources(mockDataSources);
    if (mockDataSources.length > 0) {
      setSelectedDataSource(mockDataSources[0].id);
    }

    // 加载最近的查询历史
    loadQueryHistory();
    
    // 加载保存的查询
    loadSavedQueries();
  }, []);

  // 加载查询历史
  const loadQueryHistory = () => {
    // 模拟加载查询历史
    const mockHistory: QueryHistory[] = [
      { 
        id: '1', 
        sql: 'SELECT * FROM users ORDER BY created_at DESC LIMIT 100;', 
        dataSourceId: '1', 
        executionTime: 350, 
        status: 'success', 
        timestamp: '2025-04-17 14:30:22' 
      },
      { 
        id: '2', 
        sql: 'SELECT count(*) as total FROM orders WHERE created_at > now() - INTERVAL 7 DAY;', 
        dataSourceId: '2', 
        executionTime: 780, 
        status: 'success', 
        timestamp: '2025-04-16 11:25:10' 
      },
      { 
        id: '3', 
        sql: 'SELECT * FROM invalid_table;', 
        dataSourceId: '3', 
        executionTime: 120, 
        status: 'error', 
        timestamp: '2025-04-15 16:42:35' 
      }
    ];
    setQueryHistory(mockHistory);
  };

  // 加载已保存的查询
  const loadSavedQueries = () => {
    // 模拟加载已保存的查询
    const mockSavedQueries: SavedQuery[] = [
      {
        id: '101',
        name: '用户注册趋势查询',
        sql: 'SELECT DATE(created_at) as date, COUNT(*) as count FROM users GROUP BY DATE(created_at) ORDER BY date DESC LIMIT 30;',
        dataSourceId: '1',
        createdAt: '2025-03-20 10:15:00',
        updatedAt: '2025-04-05 14:30:22'
      },
      {
        id: '102',
        name: '订单金额统计',
        sql: 'SELECT sum(amount) as total_amount, avg(amount) as avg_amount FROM orders WHERE status = "completed";',
        dataSourceId: '2',
        createdAt: '2025-04-01 09:20:00',
        updatedAt: '2025-04-10 11:45:30'
      }
    ];
    setSavedQueries(mockSavedQueries);
  };

  // 执行查询
  const executeQuery = () => {
    if (!selectedDataSource) {
      message.error('请选择数据源');
      return;
    }

    if (!sqlQuery.trim()) {
      message.error('SQL查询不能为空');
      return;
    }

    setLoading(true);
    setActiveTab('results');

    // 在实际应用中，这里应该调用后端API执行查询
    // 这里使用模拟数据
    setTimeout(() => {
      const isSuccess = Math.random() > 0.2; // 80% 概率成功
      
      if (isSuccess) {
        // 模拟成功的查询结果
        const mockResult: QueryResult = {
          columns: ['id', 'name', 'email', 'created_at', 'status', 'order_count', 'total_amount'],
          rows: Array(20).fill(0).map((_, index) => ({
            id: index + 1,
            name: `用户${index + 1}`,
            email: `user${index + 1}@example.com`,
            created_at: `2025-${(index % 12) + 1}-${(index % 28) + 1}`,
            status: index % 3 === 0 ? '活跃' : (index % 3 === 1 ? '非活跃' : '新用户'),
            order_count: Math.floor(Math.random() * 50),
            total_amount: Math.floor(Math.random() * 10000)
          })),
          total: 20,
          executionTime: Math.floor(Math.random() * 1000) + 100,
          status: 'success'
        };
        setQueryResults(mockResult);
        
        // 添加到查询历史
        const newHistory: QueryHistory = {
          id: Date.now().toString(),
          sql: sqlQuery,
          dataSourceId: selectedDataSource,
          executionTime: mockResult.executionTime,
          status: 'success',
          timestamp: new Date().toLocaleString()
        };
        setQueryHistory(prev => [newHistory, ...prev]);
        message.success(`查询成功，耗时 ${mockResult.executionTime} ms`);
      } else {
        // 模拟失败的查询结果
        const errorMessages = [
          '语法错误：在第1行附近发现未预期的关键字',
          '表 "unknown_table" 不存在',
          '访问权限不足',
          '查询超时，已中断',
          '连接数据库失败'
        ];
        const randomError = errorMessages[Math.floor(Math.random() * errorMessages.length)];
        
        setQueryResults({
          columns: [],
          rows: [],
          total: 0,
          executionTime: Math.floor(Math.random() * 200) + 50,
          status: 'error',
          message: randomError
        });
        
        // 添加到查询历史
        const newHistory: QueryHistory = {
          id: Date.now().toString(),
          sql: sqlQuery,
          dataSourceId: selectedDataSource,
          executionTime: 0,
          status: 'error',
          timestamp: new Date().toLocaleString()
        };
        setQueryHistory(prev => [newHistory, ...prev]);
        message.error(`查询失败: ${randomError}`);
      }
      setLoading(false);
    }, 1500);
  };

  // 保存查询
  const saveQuery = () => {
    if (!queryName.trim()) {
      message.error('请输入查询名称');
      return;
    }

    // 在实际应用中，这里应该调用后端API保存查询
    const newSavedQuery: SavedQuery = {
      id: Date.now().toString(),
      name: queryName,
      sql: sqlQuery,
      dataSourceId: selectedDataSource,
      createdAt: new Date().toLocaleString(),
      updatedAt: new Date().toLocaleString()
    };

    setSavedQueries(prev => [newSavedQuery, ...prev]);
    setSaveModalVisible(false);
    setQueryName('');
    message.success('查询已保存');
  };

  // 加载已保存的查询
  const loadSavedQuery = (query: SavedQuery) => {
    setSqlQuery(query.sql);
    setSelectedDataSource(query.dataSourceId);
    setHistoryDrawerVisible(false);
    message.success(`已加载查询: ${query.name}`);
  };

  // 加载历史查询
  const loadHistoryQuery = (query: QueryHistory) => {
    setSqlQuery(query.sql);
    setSelectedDataSource(query.dataSourceId);
    setHistoryDrawerVisible(false);
    message.success('已从历史记录加载查询');
  };

  // 生成图表配置
  const getChartOption = () => {
    if (!queryResults || queryResults.rows.length === 0 || queryResults.status === 'error') {
      return {
        title: { text: '无可用数据' },
        tooltip: {},
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: [] }]
      };
    }

    const xData = queryResults.rows.map(row => row[xAxisField]);
    const yData = queryResults.rows.map(row => row[yAxisField]);

    if (chartType === 'bar' || chartType === 'line') {
      return {
        title: { text: '查询结果可视化' },
        tooltip: {},
        xAxis: { 
          type: 'category', 
          data: xData,
          axisLabel: {
            interval: 0,
            rotate: xData.length > 10 ? 45 : 0,
            fontSize: 10
          }
        },
        yAxis: { type: 'value' },
        series: [{
          name: yAxisField,
          type: chartType,
          data: yData,
          itemStyle: {
            color: chartType === 'bar' ? '#1890ff' : '#52c41a'
          }
        }]
      };
    } else if (chartType === 'pie') {
      const pieData = xData.map((x, index) => ({
        name: x,
        value: yData[index]
      }));
      
      return {
        title: { text: '查询结果可视化' },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          data: xData
        },
        series: [
          {
            name: yAxisField,
            type: 'pie',
            radius: '60%',
            center: ['50%', '50%'],
            data: pieData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };
    }
  };

  // 处理编辑器加载完成事件
  function handleEditorDidMount(editor: any) {
    editorRef.current = editor;
  }

  // 下载查询结果为CSV
  const downloadResults = () => {
    if (!queryResults || queryResults.rows.length === 0 || queryResults.status === 'error') {
      message.warn('没有可下载的数据');
      return;
    }

    const header = queryResults.columns.join(',');
    const csvContent = queryResults.rows.map(row => 
      queryResults.columns.map(col => row[col] !== undefined ? `"${row[col]}"` : '""').join(',')
    );

    const csv = [header, ...csvContent].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    
    const link = document.createElement('a');
    link.href = url;
    setAttribute('download', `query_results_${new Date().getTime()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // 复制SQL到剪贴板
  const copySqlToClipboard = () => {
    navigator.clipboard.writeText(sqlQuery).then(() => {
      message.success('SQL已复制到剪贴板');
    }, () => {
      message.error('复制失败');
    });
  };

  // 格式化SQL
  const formatSql = () => {
    // 这里应该使用更专业的SQL格式化库
    // 这里只是一个简单的示例
    try {
      let formattedSql = sqlQuery.replace(/\s+/g, ' ')
        .replace(/\s*,\s*/g, ', ')
        .replace(/\s*;\s*/g, ';')
        .replace(/\s*=\s*/g, ' = ')
        .replace(/\s*>\s*/g, ' > ')
        .replace(/\s*<\s*/g, ' < ')
        .replace(/\s*AND\s*/gi, '\nAND ')
        .replace(/\s*OR\s*/gi, '\nOR ')
        .replace(/\s*SELECT\s*/gi, 'SELECT\n  ')
        .replace(/\s*FROM\s*/gi, '\nFROM\n  ')
        .replace(/\s*WHERE\s*/gi, '\nWHERE\n  ')
        .replace(/\s*GROUP BY\s*/gi, '\nGROUP BY\n  ')
        .replace(/\s*ORDER BY\s*/gi, '\nORDER BY\n  ')
        .replace(/\s*HAVING\s*/gi, '\nHAVING\n  ')
        .replace(/\s*LIMIT\s*/gi, '\nLIMIT ');
      
      setSqlQuery(formattedSql);
      message.success('SQL已格式化');
    } catch (error) {
      message.error('SQL格式化失败');
    }
  };

  return (
    <PageContainer>
      <div className="sql-editor-page">
        <Card className="sql-editor-card">
          <div className="toolbar">
            <Space style={{ marginBottom: '16px' }}>
              <Select 
                value={selectedDataSource} 
                onChange={setSelectedDataSource}
                style={{ width: 200 }}
                placeholder="选择数据源"
              >
                {dataSources.map(ds => (
                  <Option key={ds.id} value={ds.id}>
                    <DatabaseOutlined /> {ds.name} ({ds.type})
                  </Option>
                ))}
              </Select>
              <Tooltip title="执行查询 (Ctrl+Enter)">
                <Button 
                  type="primary" 
                  icon={<PlayCircleOutlined />} 
                  onClick={executeQuery}
                  loading={loading}
                >
                  执行
                </Button>
              </Tooltip>
              <Tooltip title="保存查询">
                <Button 
                  icon={<SaveOutlined />} 
                  onClick={() => setSaveModalVisible(true)}
                >
                  保存
                </Button>
              </Tooltip>
              <Tooltip title="查询历史">
                <Button 
                  icon={<HistoryOutlined />} 
                  onClick={() => setHistoryDrawerVisible(true)}
                >
                  历史
                </Button>
              </Tooltip>
              <Tooltip title="格式化SQL">
                <Button 
                  icon={<FileTextOutlined />} 
                  onClick={formatSql}
                >
                  格式化
                </Button>
              </Tooltip>
              <Tooltip title="复制SQL">
                <Button 
                  icon={<CopyOutlined />} 
                  onClick={copySqlToClipboard}
                >
                  复制
                </Button>
              </Tooltip>
            </Space>
          </div>
          
          <div className="editor-container" style={{ height: '200px', border: '1px solid #d9d9d9', borderRadius: '2px' }}>
            <Editor
              height="100%"
              defaultLanguage="sql"
              defaultValue={sqlQuery}
              onChange={(value) => setSqlQuery(value || '')}
              onMount={handleEditorDidMount}
              options={{
                minimap: { enabled: false },
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
                tabSize: 2,
              }}
            />
          </div>

          <Divider style={{ margin: '16px 0' }} />

          <Spin spinning={loading} tip="执行查询中...">
            <Tabs activeKey={activeTab} onChange={setActiveTab}>
              <TabPane tab="查询结果" key="results">
                {queryResults && (
                  <div className="results-container">
                    <div className="results-header">
                      <Space>
                        {queryResults.status === 'success' ? (
                          <span className="success-text">
                            查询成功，返回 {queryResults.total} 条记录，耗时 {queryResults.executionTime} ms
                          </span>
                        ) : (
                          <span className="error-text">
                            查询失败: {queryResults.message}
                          </span>
                        )}
                        {queryResults.status === 'success' && queryResults.rows.length > 0 && (
                          <>
                            <Tooltip title="下载为CSV">
                              <Button 
                                type="text" 
                                icon={<DownloadOutlined />} 
                                onClick={downloadResults}
                              />
                            </Tooltip>
                            <Radio.Group 
                              value={visualization} 
                              onChange={(e) => setVisualization(e.target.value)}
                              buttonStyle="solid"
                              size="small"
                            >
                              <Radio.Button value="table">表格</Radio.Button>
                              <Radio.Button value="chart">图表</Radio.Button>
                            </Radio.Group>
                          </>
                        )}
                      </Space>
                    </div>

                    {queryResults.status === 'success' && queryResults.rows.length > 0 && visualization === 'table' && (
                      <Table 
                        dataSource={queryResults.rows} 
                        columns={queryResults.columns.map(col => ({
                          title: col,
                          dataIndex: col,
                          key: col,
                          sorter: (a, b) => {
                            if (typeof a[col] === 'number') return a[col] - b[col];
                            if (typeof a[col] === 'string') return a[col].localeCompare(b[col]);
                            return 0;
                          }
                        }))}
                        rowKey="id"
                        scroll={{ x: 'max-content' }}
                        pagination={{ pageSize: 10 }}
                        size="small"
                      />
                    )}

                    {queryResults.status === 'success' && queryResults.rows.length > 0 && visualization === 'chart' && (
                      <div>
                        <div className="chart-controls">
                          <Space>
                            <Select 
                              value={chartType}
                              onChange={setChartType}
                              style={{ width: 120 }}
                            >
                              <Option value="bar">柱状图</Option>
                              <Option value="line">折线图</Option>
                              <Option value="pie">饼图</Option>
                            </Select>
                            <Select
                              placeholder="X轴字段"
                              style={{ width: 120 }}
                              value={xAxisField}
                              onChange={setXAxisField}
                            >
                              {queryResults.columns.map(col => (
                                <Option key={col} value={col}>{col}</Option>
                              ))}
                            </Select>
                            <Select
                              placeholder="Y轴字段"
                              style={{ width: 120 }}
                              value={yAxisField}
                              onChange={setYAxisField}
                            >
                              {queryResults.columns.map(col => (
                                <Option key={col} value={col}>{col}</Option>
                              ))}
                            </Select>
                          </Space>
                        </div>
                        {xAxisField && yAxisField ? (
                          <div style={{ height: '400px', marginTop: '16px' }}>
                            <ReactECharts 
                              option={getChartOption()}
                              style={{ height: '100%' }}
                              notMerge={true}
                            />
                          </div>
                        ) : (
                          <div style={{ textAlign: 'center', marginTop: '50px', color: '#999' }}>
                            请选择要显示的数据字段
                          </div>
                        )}
                      </div>
                    )}

                    {(!queryResults.rows.length || queryResults.status === 'error') && (
                      <div className="no-results">
                        {queryResults.status === 'error' ? (
                          <div className="error-message">{queryResults.message}</div>
                        ) : (
                          <div>没有返回数据</div>
                        )}
                      </div>
                    )}
                  </div>
                )}
                
                {!queryResults && (
                  <div className="no-results">
                    <div>请执行查询以查看结果</div>
                  </div>
                )}
              </TabPane>
            </Tabs>
          </Spin>
        </Card>

        {/* 保存查询的模态框 */}
        <Modal
          title="保存SQL查询"
          open={saveModalVisible}
          onCancel={() => setSaveModalVisible(false)}
          onOk={saveQuery}
          okText="保存"
          cancelText="取消"
        >
          <Form form={form} layout="vertical">
            <Form.Item
              label="查询名称"
              rules={[{ required: true, message: '请输入查询名称' }]}
            >
              <Input
                placeholder="输入便于识别的查询名称"
                value={queryName}
                onChange={e => setQueryName(e.target.value)}
                maxLength={50}
              />
            </Form.Item>
            <Form.Item label="SQL查询">
              <TextArea
                value={sqlQuery}
                autoSize={{ minRows: 3, maxRows: 8 }}
                disabled
                style={{ backgroundColor: '#f5f5f5' }}
              />
            </Form.Item>
          </Form>
        </Modal>

        {/* 历史记录抽屉 */}
        <Drawer
          title="查询历史"
          placement="right"
          width={600}
          onClose={() => setHistoryDrawerVisible(false)}
          open={historyDrawerVisible}
        >
          <Tabs defaultActiveKey="history">
            <TabPane tab="历史记录" key="history">
              <div className="history-list">
                {queryHistory.map(query => (
                  <div 
                    key={query.id} 
                    className={`history-item ${query.status === 'error' ? 'error-history' : ''}`}
                    onClick={() => loadHistoryQuery(query)}
                  >
                    <div className="history-item-header">
                      <span className="history-time">{query.timestamp}</span>
                      {query.status === 'success' ? (
                        <span className="history-success">成功 ({query.executionTime} ms)</span>
                      ) : (
                        <span className="history-error">失败</span>
                      )}
                    </div>
                    <div className="history-sql">{query.sql}</div>
                  </div>
                ))}
              </div>
            </TabPane>
            <TabPane tab="已保存查询" key="saved">
              <div className="saved-queries-list">
                {savedQueries.map(query => (
                  <div 
                    key={query.id} 
                    className="saved-query-item"
                    onClick={() => loadSavedQuery(query)}
                  >
                    <div className="saved-query-name">{query.name}</div>
                    <div className="saved-query-time">
                      创建于: {query.createdAt}
                      <br />
                      更新于: {query.updatedAt}
                    </div>
                    <div className="saved-query-sql">{query.sql}</div>
                  </div>
                ))}
              </div>
            </TabPane>
          </Tabs>
        </Drawer>
      </div>
    </PageContainer>
  );
};

export default SQLEditorPage;
