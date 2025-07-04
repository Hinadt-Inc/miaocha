import sql from 'k6/x/sql';
import driver from 'k6/x/sql/driver/mysql';
import { check, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// 自定义指标
const errorRate = new Rate('errors');
const sqlQueryCount = new Counter('sql_queries_total');
const sqlQueryDuration = new Trend('sql_query_duration');

// 数据库连接 - 按照官方文档格式，整个密码URL编码
const db = sql.open(driver, __ENV.DB_DSN || 'root:root%2540root123@tcp(10.0.20.5:9030)/log_db');

const TEST_MODE = __ENV.TEST_MODE || 'default';
const TABLE_NAME = __ENV.TABLE_NAME || 'log_table_test_env_v3';
const TIME_FIELD = __ENV.TIME_FIELD || 'log_time';

// 模拟helper.js的配置
const CONFIG = {
  TIME_RANGE_MINUTES: parseInt(__ENV.TIME_RANGE_MINUTES) || 15,
  TIME_FLUCTUATION_MINUTES: parseInt(__ENV.TIME_FLUCTUATION_MINUTES) || 1,
};

// 测试模式配置
const TEST_PRESETS = {
  explore: {
    scenarios: {
      sql_exploration: {
        executor: 'ramping-arrival-rate',
        startRate: 10,
        timeUnit: '2s',
        preAllocatedVUs: 200,
        stages: [
          { duration: '5m', target: 10 },
          { duration: '5m', target: 20 },
          { duration: '5m', target: 30 },
          { duration: '5m', target: 40 },
        ],
        gracefulStop: '30s',
      },
    },
    testType: 'sql_exploration',
  },
  benchmark: {
    scenarios: {
      steady_sql_qps: {
        executor: 'constant-arrival-rate',
        rate: parseInt(__ENV.QPS) || 10,
        timeUnit: '1s',
        duration: __ENV.DURATION || '5m',
        preAllocatedVUs: 500,
        gracefulStop: '30s'
      },
    },
    testType: 'sql_benchmark',
  },
  default: {
    scenarios: {
      average_sql_load: {
        executor: 'ramping-vus',
        stages: [
          { duration: '5m', target: 50 },
          { duration: '30m', target: 50 },
          { duration: '5m', target: 0 },
        ],
        gracefulStop: '30s',
        gracefulRampDown: '15s',
      },
    },
    testType: 'average_sql_load_test',
  },
};

const currentPreset = TEST_PRESETS[TEST_MODE] || TEST_PRESETS.default;

export const options = {
  scenarios: currentPreset.scenarios,
  userAgent: 'k6-sql-load-test/1.0',
  tags: {
    test_type: currentPreset.testType,
    module: 'doris_sql_direct',
    test_mode: TEST_MODE,
    table: TABLE_NAME,
  },
};

export function setup() {
  try {
    const testQuery = `SELECT COUNT(*) as total_records
                       FROM ${TABLE_NAME}`;
    const result = db.query(testQuery);
    return { totalRecords: result[0].total_records };
  } catch (error) {
    console.error(`[ERROR] 数据库连接失败:`, {
      error: error.message,
      stack: error.stack,
      table: TABLE_NAME,
      timestamp: new Date().toISOString()
    });
    throw new Error(`数据库连接失败: ${error.message}`);
  }
}

// 主测试函数
export default function (data) {
  const timeParams = generateTimeRange();

  group(`doris_sql_${currentPreset.testType}`, function () {
    const startTime = Date.now();

    try {
      // 执行详情查询和计数查询 (这两个查询是一组并行的)
      const detailResult = executeDetailQuery(timeParams);
      const countResult = executeCountQuery(timeParams);

      // // 执行直方图查询 (包含distributionSql)
      // const histogramResult = executeHistogramQuery(timeParams);

      const duration = Date.now() - startTime;
      sqlQueryDuration.add(duration);
      sqlQueryCount.add(2); // detailSql + countSql

      const detailSuccess = check(detailResult, {
        '详情查询成功': (r) => r !== null && Array.isArray(r),
      });

      const countSuccess = check(countResult, {
        '计数查询成功': (r) => r !== null && Array.isArray(r) && r.length > 0,
      });

      // const histogramSuccess = check(histogramResult, {
      //     '直方图查询成功': (r) => r !== null && Array.isArray(r),
      // });

      errorRate.add(detailSuccess && countSuccess ? 0 : 1);

    } catch (error) {
      errorRate.add(1);
      sqlQueryCount.add(1);

      // 打印详细错误信息
      console.error(`[ERROR] 数据库查询失败:`, {
        error: error.message,
        stack: error.stack,
        timeParams: timeParams,
        table: TABLE_NAME,
        timeField: TIME_FIELD,
        timestamp: new Date().toISOString()
      });
    }
  });
}

// 按照helper.js的generateTimeRange逻辑生成时间范围
function generateTimeRange() {
  const now = new Date();

  // 生成随机波动（±1分钟）
  const fluctuationMs = (Math.random() - 0.5) * 2 * CONFIG.TIME_FLUCTUATION_MINUTES * 60 * 1000;

  // 计算结束时间（当前时间 + 随机波动）
  const endTime = new Date(now.getTime() + fluctuationMs);

  // 计算开始时间（结束时间 - 15分钟）
  const startTime = new Date(endTime.getTime() - CONFIG.TIME_RANGE_MINUTES * 60 * 1000);

  return {
    startTime: formatDateTime(startTime),
    endTime: formatDateTime(endTime),
  };
}

// 按照helper.js的formatDateTime格式化时间
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

// 执行详情查询 - fields空数组对应SELECT *
function executeDetailQuery(timeParams) {
  const detailSql = `SELECT *
                     FROM ${TABLE_NAME}
                     WHERE ${TIME_FIELD} >= '${timeParams.startTime}'
                       AND ${TIME_FIELD} < '${timeParams.endTime}'
                     ORDER BY ${TIME_FIELD} DESC LIMIT 1000
                     OFFSET 0`;

  try {
    const result = db.query(detailSql);
    return result;
  } catch (error) {
    console.error(`[ERROR] 详情查询失败:`, {
      sql: detailSql,
      error: error.message,
      timeParams: timeParams
    });
    throw error;
  }
}

// 执行计数查询 - 对应DetailSearchExecutor的countSql
function executeCountQuery(timeParams) {
  const countSql = `SELECT COUNT(*) AS total
                    FROM ${TABLE_NAME}
                    WHERE ${TIME_FIELD} >= '${timeParams.startTime}'
                      AND ${TIME_FIELD} < '${timeParams.endTime}'`;

  try {
    const result = db.query(countSql);
    return result;
  } catch (error) {
    console.error(`[ERROR] 计数查询失败:`, {
      sql: countSql,
      error: error.message,
      timeParams: timeParams
    });
    throw error;
  }
}

// 执行直方图查询 - 对应HistogramSearchExecutor的distributionSql
function executeHistogramQuery(timeParams) {
  const histogramSql = `SELECT FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(${TIME_FIELD}) / 3600) * 3600) AS log_time_,
                               COUNT(*) AS count
                        FROM ${TABLE_NAME}
                        WHERE ${TIME_FIELD} >= '${timeParams.startTime}'
                          AND ${TIME_FIELD}
                            < '${timeParams.endTime}'
                        GROUP BY FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(${TIME_FIELD}) / 3600) * 3600)
                        ORDER BY log_time_ ASC`;
  return db.query(histogramSql);
}


export function teardown() {
  db.close();
}
