// k6 测试辅助工具模块

// 配置常量
export const CONFIG = {
  BASE_URL: __ENV.BASE_URL || 'http://localhost:8080',
  TEST_EMAIL: __ENV.TEST_EMAIL || 'admin@hinadt.com',
  TEST_PASSWORD: __ENV.TEST_PASSWORD || 'admin123',
  TEST_MODULE: __ENV.TEST_MODULE || 'k8s-hina-cloud',
  // 时间范围配置（分钟）
  TIME_RANGE_MINUTES: parseInt(__ENV.TIME_RANGE_MINUTES) || 15,
  // 时间波动范围（分钟）
  TIME_FLUCTUATION_MINUTES: parseInt(__ENV.TIME_FLUCTUATION_MINUTES) || 1,
};


// 生成测试数据
export function generateTestData() {
  return generateTimeRange();
}

// 生成时间范围（绝对时间，带随机波动）
function generateTimeRange() {
  const now = new Date();

  // 生成随机波动（±1分钟）
  const fluctuationMs = (Math.random() - 0.5) * 2 * CONFIG.TIME_FLUCTUATION_MINUTES * 60 * 1000;

  // 计算结束时间（当前时间 + 随机波动）
  const endTime = new Date(now.getTime() + fluctuationMs);

  // 计算开始时间（结束时间 - 配置的时间范围）
  const startTime = new Date(endTime.getTime() - CONFIG.TIME_RANGE_MINUTES * 60 * 1000);

  return {
    startTime: formatDateTime(startTime),
    endTime: formatDateTime(endTime),
    description: `${CONFIG.TIME_RANGE_MINUTES}min_range_pm${CONFIG.TIME_FLUCTUATION_MINUTES}min_fluctuation`,
  };
}

// 格式化时间为项目要求的格式
function formatDateTime(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  const milliseconds = String(date.getMilliseconds()).padStart(3, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${milliseconds}`;
}

// 创建登录请求数据
export function createLoginPayload() {
  return {
    email: CONFIG.TEST_EMAIL,
    password: CONFIG.TEST_PASSWORD,
  };
}

// 创建日志查询请求数据
export function createLogQueryPayload(timeData) {
  return {
    offset: 0,
    pageSize: 1000,
    module: CONFIG.TEST_MODULE,
    startTime: timeData.startTime,
    endTime: timeData.endTime,
    timeGrouping: 'auto',
    fields: [],
    signal: {},
    keywordConditions: []
  };
}

// 创建HTTP请求头
export function createAuthHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

// 创建基础HTTP请求头
export function createBaseHeaders() {
  return {
    'Content-Type': 'application/json',
  };
}

// 验证登录响应
export function validateLoginResponse(response) {
  try {
    const body = JSON.parse(response.body).data;
    return body.token && body.token.length > 0;
  } catch (e) {
    return false;
  }
}

// 验证查询响应数据
export function validateQueryResponseData(response) {
  try {
    const body = JSON.parse(response.body);
    return body.data !== undefined;
  } catch (e) {
    return false;
  }
}

// 验证直方图响应数据
export function validateHistogramResponseData(response) {
  try {
    const body = JSON.parse(response.body).data;
    return body.distributionData && Array.isArray(body.distributionData);
  } catch (e) {
    return false;
  }
}

// API端点
export const ENDPOINTS = {
  LOGIN: '/api/auth/login',
  LOG_DETAILS: '/api/logs/search/details',
  LOG_HISTOGRAM: '/api/logs/search/histogram',
};

// 测试检查项
export const CHECKS = {
  LOGIN: {
    'login status is 200': (r) => r.status === 200,
    'response has token': validateLoginResponse,
  },
  DETAILS_QUERY: {
    'details query status is 200': (r) => r.status === 200,
    // 'details response time < 10s': (r) => r.timings.duration < 10000,
    'details response has data': validateQueryResponseData,
  },
  HISTOGRAM_QUERY: {
    'histogram query status is 200': (r) => r.status === 200,
    // 'histogram response time < 10s': (r) => r.timings.duration < 10000,
    'histogram response has buckets': validateHistogramResponseData,
  },
};

// 日志输出函数 - 极简模式，只记录错误不输出详情
export function logError(type, timeData, status, body) {
  // 静默记录错误，不输出日志，让k6的内置指标来显示性能数据
  // 如果需要详细错误信息，可以设置环境变量 VERBOSE_ERRORS=true
  if (__ENV.VERBOSE_ERRORS === 'true') {
    console.error(`❌ ${type} 失败: HTTP ${status}`);
  }
  // 默认情况下不输出任何错误日志，让用户专注于性能指标
}

export function logSuccess(message) {
  // 只在详细模式下输出成功日志
  if (__ENV.VERBOSE === 'true') {
    console.log(`✅ ${message}`);
  }
}

export function logInfo(message) {
  // 只在详细模式下输出信息日志
  if (__ENV.VERBOSE === 'true') {
    console.log(`🚀 ${message}`);
  }
}
