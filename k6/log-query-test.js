import http from 'k6/http';
import {check, group, sleep} from 'k6';
import {Rate} from 'k6/metrics';
import {
  CHECKS,
  CONFIG,
  createAuthHeaders,
  createBaseHeaders,
  createLoginPayload,
  createLogQueryPayload,
  ENDPOINTS,
  generateTestData,
  logError,
  logInfo,
  logSuccess
} from './helper.js';

// 自定义指标
const errorRate = new Rate('errors');

// 测试模式预设配置
// 使用方式：
// k6 run -e TEST_MODE=explore log-query-test.js     # 探索模式
// k6 run -e TEST_MODE=light log-query-test.js       # 轻量模式
// k6 run -e TEST_MODE=benchmark -e QPS=50 log-query-test.js  # 基准模式
// k6 run log-query-test.js                          # 默认模式

const TEST_MODE = __ENV.TEST_MODE || 'default';

const TEST_PRESETS = {
  // 探索模式：逐步提升QPS找到系统性能边界
  explore: {
    scenarios: {
      qps_exploration: {
        executor: 'ramping-arrival-rate',
        startRate: 10,
        timeUnit: '2s',
        preAllocatedVUs: 200,
        stages: [
          {duration: '5m', target: 10},
          {duration: '5m', target: 20},
          {duration: '5m', target: 30},
          {duration: '5m', target: 40},
        ],
        gracefulStop: '30s',
      },
    },
    testType: 'qps_exploration',
    description: 'Progressive QPS exploration to find system performance limit',
  },

  // 基准模式：固定QPS深度测试
  benchmark: {
    scenarios: {
      steady_qps: {
        executor: 'constant-arrival-rate',
        rate: parseInt(__ENV.QPS) || 10,
        timeUnit: '1s',
        duration: __ENV.DURATION || '5m',
        preAllocatedVUs: 200,
        gracefulStop: '30s'
      },
    },
    testType: 'qps_benchmark',
    description: `Fixed ${__ENV.QPS || 10} QPS stability test`,
  },

  benchmark_constant: {
    scenarios: {
      steady_qps: {
        executor: 'constant-vus',
        vus: 20,
        duration: '5m',
        gracefulStop: '30s',
      }
    },
    testType: 'qps_benchmark_constant',
  },


  // 默认模式：原来的平均负载测试
  default: {
    scenarios: {
      average_load: {
        executor: 'ramping-vus',
        stages: [
          {duration: '5m', target: 50},
          {duration: '30m', target: 50},
          {duration: '5m', target: 0},
        ],
        gracefulStop: '30s',
        gracefulRampDown: '15s',
      },
    },
    testType: 'average_load_test',
    description: '40min progressive user load test',
  },
};

// 获取当前测试配置
const currentPreset = TEST_PRESETS[TEST_MODE] || TEST_PRESETS.default;

// 测试配置
export const options = {
  scenarios: currentPreset.scenarios,


  // 其他配置
  userAgent: 'k6-load-test/1.0',
  noConnectionReuse: false,
  insecureSkipTLSVerify: true,

  // 请求标签
  tags: {
    test_type: currentPreset.testType,
    module: 'log_query',
    test_mode: TEST_MODE,
    description: currentPreset.description,
  },
};

// 设置函数 - 获取认证token
export function setup() {
  const loginPayload = createLoginPayload();

  const loginResponse = http.post(
    `${CONFIG.BASE_URL}${ENDPOINTS.LOGIN}`,
    JSON.stringify(loginPayload),
    {
      headers: createBaseHeaders(),
      tags: {api_name: 'login_init'},
    }
  );

  const loginSuccess = check(loginResponse, CHECKS.LOGIN);

  if (!loginSuccess) {
    // 登录失败时只输出关键错误信息
    throw new Error(`登录失败: HTTP ${loginResponse.status}`);
  }

  const loginData = JSON.parse(loginResponse.body).data;

  return {
    token: loginData.token,
    userId: loginData.userId,
    userRole: loginData.role,
  };
}

// 主测试函数
export default function (data) {
  // 静默运行，专注于性能指标收集
  // 从setup返回的data中获取token
  const headers = createAuthHeaders(data.token);

  // 构造测试数据
  const testData = generateTestData();

  group(`log_query_${currentPreset.testType}`, function () {
    const payload = createLogQueryPayload(testData);

    // 使用 http.batch 模拟前端并行请求
    const responses = http.batch([
      // 请求1: 日志明细查询
      {
        method: 'POST',
        url: `${CONFIG.BASE_URL}${ENDPOINTS.LOG_DETAILS}`,
        body: JSON.stringify(payload),
        params: {
          headers: headers,
          tags: {api_name: 'log_details_query', time_range: testData.description},
        }
      },
      // // 请求2: 日志直方图查询
      // {
      //   method: 'POST',
      //   url: `${CONFIG.BASE_URL}${ENDPOINTS.LOG_HISTOGRAM}`,
      //   body: JSON.stringify(payload),
      //   params: {
      //     headers: headers,
      //     tags: {api_name: 'log_histogram_query', time_range: testData.description},
      //   }
      // }
    ]);

    // 验证明细查询响应
    const detailsSuccess = check(responses[0], CHECKS.DETAILS_QUERY);
    if (!detailsSuccess) {
      errorRate.add(1);
      logError('Details query', testData, responses[0].status, responses[0].body);
    } else {
      errorRate.add(0);
    }

    // // 验证直方图查询响应
    // const histogramSuccess = check(responses[1], CHECKS.HISTOGRAM_QUERY);
    // if (!histogramSuccess) {
    //   errorRate.add(1);
    //   logError('Histogram query', testData, responses[1].status, responses[1].body);
    // } else {
    //   errorRate.add(0);
    // }
  });
}

// 清理函数
export function teardown(data) {
  // 静默清理，只在需要时输出关键信息
  if (__ENV.VERBOSE === 'true') {
    console.log(`📊 测试用户: ${data.userId}, 角色: ${data.userRole}`);
  }
}
