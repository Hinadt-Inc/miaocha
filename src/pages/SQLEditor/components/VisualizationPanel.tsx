import { useRef, useMemo } from 'react';
import { Empty, Form, Select } from 'antd';
import ReactECharts from 'echarts-for-react';
import { QueryResult } from '../types';
import './VisualizationPanel.less';

const { Option } = Select;

interface VisualizationPanelProps {
  queryResults: QueryResult | null;
  chartType: 'bar' | 'line' | 'pie';
  setChartType: (type: 'bar' | 'line' | 'pie') => void;
  xField: string;
  setXField: (field: string) => void;
  yField: string;
  setYField: (field: string) => void;
  fullscreen: boolean;
}

const VisualizationPanel: React.FC<VisualizationPanelProps> = ({
  queryResults,
  chartType,
  setChartType,
  xField,
  setXField,
  yField,
  setYField,
  fullscreen
}) => {
  const chartRef = useRef<any>(null);
  
  // 使用 useMemo 创建图表选项
  const chartOption = useMemo(() => {
    if (!queryResults?.rows?.length || !xField || !yField) {
      return {
        title: { text: '暂无数据' },
        tooltip: {},
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: [] }]
      };
    }

    const xData = queryResults.rows.map(row => String(row[xField] || ''));
    const yData = queryResults.rows.map(row => {
      const value = row[yField];
      return typeof value === 'number' ? value : parseFloat(String(value)) || 0;
    });

    if (chartType === 'pie') {
      return {
        title: { text: '查询结果可视化' },
        tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', right: 10, top: 'center' },
        series: [
          {
            name: '数据',
            type: 'pie',
            radius: ['50%', '70%'],
            avoidLabelOverlap: false,
            label: { show: false },
            emphasis: {
              label: { show: true, fontSize: '16', fontWeight: 'bold' }
            },
            labelLine: { show: false },
            data: xData.map((name, index) => ({ value: yData[index], name }))
          }
        ]
      };
    }

    return {
      title: { text: '查询结果可视化' },
      tooltip: { trigger: 'axis' },
      toolbox: {
        feature: {
          saveAsImage: { title: '保存为图片' }
        }
      },
      xAxis: { type: 'category', data: xData },
      yAxis: { type: 'value' },
      series: [{ 
        name: yField, 
        type: chartType, 
        data: yData,
        label: { show: true, position: 'top' }
      }]
    };
  }, [queryResults, xField, yField, chartType]);

  if (!queryResults || !queryResults.rows || !queryResults.rows.length) {
    return <Empty description="需要有查询结果才能创建可视化" />;
  }

  return (
    <div>
      <div className="chart-controls">
        <Form layout="inline">
          <Form.Item label="图表类型">
            <Select 
              value={chartType}
              onChange={value => setChartType(value)}
              style={{ width: 120 }}
            >
              <Option value="bar">柱状图</Option>
              <Option value="line">折线图</Option>
              <Option value="pie">饼图</Option>
            </Select>
          </Form.Item>
          <Form.Item label="X轴/名称">
            <Select 
              value={xField}
              onChange={setXField}
              style={{ width: 120 }} 
              placeholder="选择字段"
            >
              {queryResults.columns?.map(col => (
                <Option key={col} value={col}>{col}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="Y轴/值">
            <Select 
              value={yField}
              onChange={setYField}
              style={{ width: 120 }} 
              placeholder="选择字段"
            >
              {queryResults.columns?.map(col => (
                <Option key={col} value={col}>{col}</Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </div>
      <div className="chart-container">
        <ReactECharts 
          ref={chartRef}
          option={chartOption}
          style={{ height: fullscreen ? window.innerHeight - 450 : 400 }}
          opts={{ renderer: 'canvas' }}
        />
      </div>
    </div>
  );
};

export default VisualizationPanel;