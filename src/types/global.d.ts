export {};
declare global {
  interface IStatus {
    value?: string;
    label?: string;
    [key: string]: any;
  }

  // 相对时间
  interface IRelativeTime {
    label: string; // 显示的标签
    value: string; // 后端的值
    unitCN: string; // 单位中文
    unitEN: string; // 单位英文
    format: string; // 时间格式
  }

  // 时间组件的提交参数
  interface ISubmitTime {
    label?: string; // 显示的标签
    value?: string; // 值
    range: string[]; // 时间范围
    format?: string[]; // 时间格式
    [key: string]: any;
  }

  // 字段值分布列表，按数量降序排序
  interface IValueDistributions {
    count: number; // 出现次数
    percentage: number; // 占比百分比
    value: string; // 字段值
  }

  interface IFieldDistributions {
    fieldName: string; // 字段名称
    nonNullCount: number; // 非空记录数
    nullCount: number; // 空记录数
    totalCount: number; // 总记录数
    uniqueValueCount: number; // 唯一值数量
    valueDistributions: IValueDistributions[];
  }
}
