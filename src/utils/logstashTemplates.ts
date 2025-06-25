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

export const DORIS_TEMPLATE = `CREATE TABLE \`create_table_template\` (
  \`log_time\` datetime(3) NOT NULL,
  \`host\` text NULL COMMENT "hostname or ip",
  \`source\` text NULL COMMENT "log path",
  \`log_offset\` text NULL COMMENT "日志所在kafka主题偏移量",
  \`message\` variant NULL,
  \`message_text\` text NULL,
   
  # 添加或者删除字段 ....

  INDEX idx_message (\`message_text\`) USING INVERTED PROPERTIES("support_phrase" = "true", "parser" = "unicode", "lower_case" = "true")
) ENGINE=OLAP
DUPLICATE KEY(\`log_time\`)
AUTO PARTITION BY RANGE (date_trunc(\`log_time\`, 'hour'))
()
DISTRIBUTED BY RANDOM BUCKETS 6
PROPERTIES (
# replication_allocation: 指定副本分配策略，定义数据副本的存储位置和数量
"replication_allocation" = "tag.location.default: 3",
# min_load_replica_num: 最小加载副本数，-1表示无限制，优化查询性能
"min_load_replica_num" = "-1",
# is_being_synced: 是否处于同步状态，false表示不进行数据同步
"is_being_synced" = "false",
# dynamic_partition.enable: 启用动态分区，自动根据时间创建分区
"dynamic_partition.enable" = "true",
# dynamic_partition.time_unit: 动态分区的粒度，hour表示按小时分区
"dynamic_partition.time_unit" = "hour",
# dynamic_partition.time_zone: 分区时间使用的时区，设置为Asia/Shanghai
"dynamic_partition.time_zone" = "Asia/Shanghai",
# dynamic_partition.start: 动态分区的历史范围，-1440表示保留过去1440小时的分区
"dynamic_partition.start" = "-1440",
# dynamic_partition.end: 动态分区的未来范围，0表示不预创建未来分区
"dynamic_partition.end" = "0",
# dynamic_partition.prefix: 动态分区名称前缀，p为分区名前缀
"dynamic_partition.prefix" = "p",
# dynamic_partition.replication_allocation: 动态分区副本分配策略，与主表一致
"dynamic_partition.replication_allocation" = "tag.location.default: 3",
# dynamic_partition.buckets: 动态分区的桶数，6表示数据分布到6个桶
"dynamic_partition.buckets" = "6",
# dynamic_partition.create_history_partition: 是否创建历史分区，false表示不创建
"dynamic_partition.create_history_partition" = "false",
# dynamic_partition.history_partition_num: 历史分区数量，-1表示无限制
"dynamic_partition.history_partition_num" = "-1",
# dynamic_partition.hot_partition_num: 热分区数量，0表示无热分区
"dynamic_partition.hot_partition_num" = "0",
# dynamic_partition.reserved_history_periods: 保留的历史分区周期，NULL表示无限制
"dynamic_partition.reserved_history_periods" = "NULL",
# dynamic_partition.storage_policy: 存储策略，空表示使用默认存储
"dynamic_partition.storage_policy" = "",
# inverted_index_storage_format: 倒排索引存储格式，V2为当前版本
"inverted_index_storage_format" = "V2",
# compression: 数据压缩算法，ZSTD提供高压缩比和快速解压
"compression" = "ZSTD",
# disable_auto_compaction: 是否禁用自动压缩，false表示启用自动压缩
"disable_auto_compaction" = "false",
# group_commit_interval_ms: 组提交间隔，10000ms表示每10秒提交一批数据
"group_commit_interval_ms" = "10000",
# group_commit_data_bytes: 组提交数据量阈值，134217728字节（128MB）触发提交
"group_commit_data_bytes" = "134217728"
);`;
