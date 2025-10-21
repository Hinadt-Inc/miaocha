/**
 * 直方图组件的类型定义
 */

export interface IHistogramChartProps {
  data: ILogHistogramData; // 直方图数据
  searchParams: ILogSearchParams; // 搜索参数
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
}

export interface IAggregatedData {
  values: number[];
  labels: string[];
  originalData: ILogHistogramData;
}

export interface IChartEvents {
  click: (params: any) => void;
  brushEnd: (params: { areas: { coordRange: [number, number] }[] }) => void;
}
