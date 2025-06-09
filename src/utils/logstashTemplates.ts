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
