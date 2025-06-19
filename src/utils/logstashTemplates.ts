export const LOGSTASH_CONFIG_TEMPLATE = `
input {
  kafka {
    bootstrap_servers => "\${kafka_servers}"
    topics => ["\${topic}"]
    group_id => "\${group_id}"
    codec => "json"
  }
}

filter {
  json {
    source => "message"
  }
}

output {
  doris {
    http_hosts => ["\${doris_host}"]
    user => "\${user}"
    password => "\${password}"
    db => "\${db}"
    table => "\${table}"
    headers => { "format" => "json" }
    mapping => {
      "log_time" => "%{@timestamp}"
      "host" => "%{[host][name]}"
      "path" => "%{source}"
    }
  }
}
`;

export const JVM_CONFIG_TEMPLATE = `
## JVM配置

# 基础配置
-Xms1g
-Xmx1g
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/logstash_heapdump.hprof

# 高级配置（可选）
#-XX:+UseG1GC
#-XX:MaxGCPauseMillis=200
#-XX:InitiatingHeapOccupancyPercent=35
#-XX:+ParallelRefProcEnabled
#-XX:+DisableExplicitGC
#-XX:+AlwaysPreTouch

# 日志配置
#-Xlog:gc*,gc+age=trace:file=/var/log/logstash/gc.log:utctime,pid,tags:filecount=32,filesize=64m
`;

export const LOGSTASH_BASE_CONFIG_TEMPLATE = `
# 基础配置
node.name: logstash
path.data: /var/lib/logstash
path.logs: /var/log/logstash

# 管道配置
pipeline.workers: 2
pipeline.batch.size: 125
pipeline.batch.delay: 50

# 自动重载配置
config.reload.automatic: false

# HTTP API配置
http.host: 0.0.0.0
http.port: 9600
`;

export const DORIS_TEMPLATE = `CREATE TABLE \`log_table\` (
  \`log_time\` datetime(3) NOT NULL COMMENT "日志时间",
  \`host\` text NULL COMMENT "主机名或IP",
  \`path\` text NULL COMMENT "日志文件路径",
  -- 添加其他字段
  INDEX idx_host (\`host\`) USING INVERTED, -- 主机名索引
  INDEX idx_path (\`path\`) USING INVERTED -- 路径索引
) ENGINE=OLAP
DUPLICATE KEY(\`log_time\`) -- 去重键
COMMENT '日志存储表'
AUTO PARTITION BY RANGE (date_trunc(\`log_time\`, 'day')) -- 按天分区
()
DISTRIBUTED BY RANDOM BUCKETS 30 -- 随机分桶
PROPERTIES (
"dynamic_partition.enable" = "true", -- 启用动态分区
"dynamic_partition.time_unit" = "day", -- 分区单位：天
"dynamic_partition.time_zone" = "Asia/Shanghai" -- 时区
);`;
