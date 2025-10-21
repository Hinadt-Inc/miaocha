import { useRef, useMemo } from 'react';
import { Empty, Form, Select } from 'antd';
import ReactECharts from 'echarts-for-react';
import { QueryResult } from '../types';
import { colorPrimary } from '../../../utils/utils';
import styles from '../SQLEditorPage.module.less';

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
}) => {
  const chartRef = useRef<any>(null);

  // 使用 useMemo 创建图表选项
  const chartOption = useMemo(() => {
    if (!queryResults?.rows?.length || !xField || !yField) {
      return {
        title: {
          text: '暂无数据',
          textStyle: {
            color: '#666',
            fontSize: 14,
          },
        },
        tooltip: {},
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: [] }],
      };
    }

    const xData = queryResults.rows.map((row) => String(row[xField] || ''));
    const yData = queryResults.rows.map((row) => {
      const value = row[yField];
      return typeof value === 'number' ? value : parseFloat(String(value)) || 0;
    });

    const baseOption = {
      backgroundColor: '#ffffff',
      title: {
        text: '查询结果可视化',
        textStyle: {
          color: '#333',
          fontSize: 16,
          fontWeight: 'normal',
        },
        left: 'center',
      },
      grid: {
        top: '15%',
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      toolbox: {
        feature: {
          saveAsImage: {
            title: '保存为图片',
            icon: 'path://M819.2 768a51.2 51.2 0 0 1-51.2 51.2H256a51.2 51.2 0 0 1-51.2-51.2V256a51.2 51.2 0 0 1 51.2-51.2h512a51.2 51.2 0 0 1 51.2 51.2v512z M710.4 409.6a51.2 51.2 0 0 0-51.2-51.2H364.8a51.2 51.2 0 0 0-51.2 51.2v204.8a51.2 51.2 0 0 0 51.2 51.2h294.4a51.2 51.2 0 0 0 51.2-51.2V409.6z',
          },
          dataZoom: {
            yAxisIndex: 'none',
            title: {
              zoom: '区域缩放',
              back: '还原',
            },
          },
          restore: { title: '还原' },
        },
        right: 20,
      },
    };

    if (chartType === 'pie') {
      return {
        ...baseOption,
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#eee',
          borderWidth: 1,
          padding: [10, 15],
          textStyle: {
            color: '#666',
          },
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'middle',
          type: 'scroll',
          textStyle: {
            color: '#666',
          },
        },
        series: [
          {
            name: yField,
            type: 'pie',
            radius: ['45%', '65%'],
            center: ['50%', '50%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderColor: '#fff',
              borderWidth: 2,
            },
            label: {
              show: false,
              position: 'outside',
              formatter: '{b}: {d}%',
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 14,
                fontWeight: 'bold',
              },
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.2)',
              },
            },
            labelLine: {
              show: true,
              length: 15,
              length2: 10,
            },
            data: xData.map((name, index) => ({
              value: yData[index],
              name,
              itemStyle: {
                borderRadius: 4,
              },
            })),
          },
        ],
      };
    }

    return {
      ...baseOption,
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#eee',
        borderWidth: 1,
        padding: [10, 15],
        textStyle: {
          color: '#666',
        },
        axisPointer: {
          type: chartType === 'line' ? 'cross' : 'shadow',
        },
      },
      xAxis: {
        type: 'category',
        data: xData,
        axisTick: {
          alignWithLabel: true,
        },
        axisLine: {
          lineStyle: {
            color: '#ddd',
          },
        },
        axisLabel: {
          color: '#666',
          rotate: xData.length > 12 ? 45 : 0,
        },
      },
      yAxis: {
        type: 'value',
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: '#eee',
          },
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#ddd',
          },
        },
        axisLabel: {
          color: '#666',
        },
      },
      series: [
        {
          name: yField,
          type: chartType,
          data: yData,
          itemStyle: {
            color: colorPrimary,
            borderRadius: chartType === 'bar' ? 4 : 0,
          },
          lineStyle:
            chartType === 'line'
              ? {
                  width: 3,
                  shadowColor: 'rgba(0,0,0,0.1)',
                  shadowBlur: 10,
                }
              : undefined,
          smooth: chartType === 'line',
          label: {
            show: false,
            position: 'top',
            distance: 10,
            color: '#666',
            formatter: '{c}',
          },
          emphasis: {
            focus: 'series',
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.1)',
            },
          },
          animationDuration: 1000,
          animationEasing: 'cubicOut',
        },
      ],
    };
  }, [queryResults, xField, yField, chartType]);

  if (!queryResults?.rows?.length) {
    return <Empty description="需要有查询结果才能创建可视化" />;
  }

  return (
    <div className={styles.visualizationPanel}>
      <div className={styles.chartControls}>
        <Form layout="inline">
          <Form.Item label="图表类型">
            <Select style={{ width: 120 }} value={chartType} onChange={(value) => setChartType(value)}>
              <Option value="bar">柱状图</Option>
              <Option value="line">折线图</Option>
              <Option value="pie">饼图</Option>
            </Select>
          </Form.Item>
          <Form.Item label="X轴/名称">
            <Select placeholder="选择字段" style={{ width: 120 }} value={xField} onChange={setXField}>
              {queryResults.columns?.map((col) => (
                <Option key={col} value={col}>
                  {col}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="Y轴/值">
            <Select placeholder="选择字段" style={{ width: 120 }} value={yField} onChange={setYField}>
              {queryResults.columns?.map((col) => (
                <Option key={col} value={col}>
                  {col}
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </div>
      <div className={styles.chartContainer}>
        <ReactECharts
          ref={chartRef}
          option={chartOption}
          opts={{ renderer: 'canvas' }}
          style={{ height: 400, width: '100%' }}
        />
      </div>
    </div>
  );
};

export default VisualizationPanel;
