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
    headers => {
      "format" => "json"
      "read_json_by_line" => "true"
      "load_to_single_tablet" => "true"
      "group_commit" => "async_mode"
    }
    mapping => {
      "log_time" => "%{@timestamp}"
      "host" => "%{[host][name]}"
      "path" => "%{source}"
    }
    log_request => true
    log_progress_interval => 10
  }
}`;

export const JVM_CONFIG_TEMPLATE = `
-Xms8g
-Xmx16g

################################################################
## Expert settings
################################################################
##
## All settings below this section are considered
## expert settings. Don't tamper with them unless
## you understand what you are doing
##
################################################################


## Locale
# Set the locale language
#-Duser.language=en

# Set the locale country
#-Duser.country=US

# Set the locale variant, if any
#-Duser.variant=

## basic

# set the I/O temp directory
#-Djava.io.tmpdir=\${HOME}

# set to headless, just in case
-Djava.awt.headless=true

# ensure UTF-8 encoding by default (e.g. filenames)
-Dfile.encoding=UTF-8

# use our provided JNA always versus the system one
#-Djna.nosys=true

# Turn on JRuby invokedynamic
-Djruby.compile.invokedynamic=true

## heap dumps

# generate a heap dump when an allocation from the Java heap fails
# heap dumps are created in the working directory of the JVM
-XX:+HeapDumpOnOutOfMemoryError

# specify an alternative path for heap dumps
# ensure the directory exists and has sufficient space
#-XX:HeapDumpPath=\${LOGSTASH_HOME}/heapdump.hprof

## GC logging
#-Xlog:gc*,gc+age=trace,safepoint:file=\${LS_GC_LOG_FILE}:utctime,pid,tags:filecount=32,filesize=64m

# Entropy source for randomness
-Djava.security.egd=file:/dev/urandom

# Copy the logging context from parent threads to children
-Dlog4j2.isThreadContextMapInheritable=true

# FasterXML/jackson defaults
#
# Sets the maximum string length (in chars or bytes, depending on input context).
# This limit is not exact and an exception will happen at sizes greater than this limit.
# Some text values that are a little bigger than the limit may be treated as valid but no
# text values with sizes less than or equal to this limit will be treated as invalid.
# This value should be higher than \`logstash.jackson.stream-read-constraints.max-number-length\`.
# The jackson library defaults to 20000000 or 20MB, whereas Logstash defaults to 200MB or 200000000 characters.
#-Dlogstash.jackson.stream-read-constraints.max-string-length=200000000
#
# Sets the maximum number length (in chars or bytes, depending on input context).
# The jackson library defaults to 1000, whereas Logstash defaults to 10000.
#-Dlogstash.jackson.stream-read-constraints.max-number-length=10000
#
# Sets the maximum nesting depth. The depth is a count of objects and arrays that have not
# been closed, \`{\` and \`[\` respectively.
#-Dlogstash.jackson.stream-read-constraints.max-nesting-depth=1000
`;

export const LOGSTASH_BASE_CONFIG_TEMPLATE = `
# logstash 批处理配置
pipeline.batch.size: 2000
pipeline.batch.delay: 500
#
# ------------  Node identity ------------
#
# Use a descriptive name for the node:
#
# node.name: test
#
# If omitted the node name will default to the machine's host name
#
# ------------ Data path ------------------
#
# Which directory should be used by logstash and its plugins
# for any persistent needs. Defaults to LOGSTASH_HOME/data
#
# path.data:
#
# ------------ Pipeline Settings --------------
#
# The ID of the pipeline.
#
# pipeline.id: main
#
# Set the number of workers that will, in parallel, execute the filters+outputs
# stage of the pipeline.
#
# This defaults to the number of the host's CPU cores.
#
# pipeline.workers: 2
#
# How many events to retrieve from inputs before sending to filters+workers
#
# pipeline.batch.size: 125
#
# How long to wait in milliseconds while polling for the next event
# before dispatching an undersized batch to filters+outputs
#
# pipeline.batch.delay: 50
#
# Force Logstash to exit during shutdown even if there are still inflight
# events in memory. By default, logstash will refuse to quit until all
# received events have been pushed to the outputs.
#
# WARNING: Enabling this can lead to data loss during shutdown
#
# pipeline.unsafe_shutdown: false
#
# Set the pipeline event ordering. Options are "auto" (the default), "true" or "false".
# "auto" automatically enables ordering if the 'pipeline.workers' setting
# is also set to '1', and disables otherwise.
# "true" enforces ordering on the pipeline and prevent logstash from starting
# if there are multiple workers.
# "false" disables any extra processing necessary for preserving ordering.
#
# pipeline.ordered: auto
#
# Sets the pipeline's default value for \`ecs_compatibility\`, a setting that is
# available to plugins that implement an ECS Compatibility mode for use with
# the Elastic Common Schema.
# Possible values are:
# - disabled
# - v1
# - v8 (default)
# Pipelines defined before Logstash 8 operated without ECS in mind. To ensure a
# migrated pipeline continues to operate as it did before your upgrade, opt-OUT
# of ECS for the individual pipeline in its \`pipelines.yml\` definition. Setting
# it here will set the default for _all_ pipelines, including new ones.
#
# pipeline.ecs_compatibility: v8
#
# ------------ Pipeline Configuration Settings --------------
#
# Where to fetch the pipeline configuration for the main pipeline
#
# path.config:
#
# Pipeline configuration string for the main pipeline
#
# config.string:
#
# At startup, test if the configuration is valid and exit (dry run)
#
# config.test_and_exit: false
#
# Periodically check if the configuration has changed and reload the pipeline
# This can also be triggered manually through the SIGHUP signal
#
# config.reload.automatic: false
#
# How often to check if the pipeline configuration has changed (in seconds)
# Note that the unit value (s) is required. Values without a qualifier (e.g. 60)
# are treated as nanoseconds.
# Setting the interval this way is not recommended and might change in later versions.
#
# config.reload.interval: 3s
#
# Show fully compiled configuration as debug log message
# NOTE: --log.level must be 'debug'
#
# config.debug: false
#
# When enabled, process escaped characters such as \n and \" in strings in the
# pipeline configuration files.
#
# config.support_escapes: false
#
# ------------ API Settings -------------
# Define settings related to the HTTP API here.
#
# The HTTP API is enabled by default. It can be disabled, but features that rely
# on it will not work as intended.
#
# api.enabled: true
#
# By default, the HTTP API is not secured and is therefore bound to only the
# host's loopback interface, ensuring that it is not accessible to the rest of
# the network.
# When secured with SSL and Basic Auth, the API is bound to _all_ interfaces
# unless configured otherwise.
#
# api.http.host: 127.0.0.1
#
# The HTTP API web server will listen on an available port from the given range.
# Values can be specified as a single port (e.g., \`9600\`), or an inclusive range
# of ports (e.g., \`9600-9700\`).
#
# api.http.port: 9600-9700
#
# The HTTP API includes a customizable "environment" value in its response,
# which can be configured here.
#
# api.environment: "production"
#
# The HTTP API can be secured with SSL (TLS). To do so, you will need to provide
# the path to a password-protected keystore in p12 or jks format, along with credentials.
#
# api.ssl.enabled: false
# api.ssl.keystore.path: /path/to/keystore.jks
# api.ssl.keystore.password: "y0uRp4$$w0rD"
#
# The availability of SSL/TLS protocols depends on the JVM version. Certain protocols are
# disabled by default and need to be enabled manually by changing \`jdk.tls.disabledAlgorithms\`
# in the $JDK_HOME/conf/security/java.security configuration file.
#
# api.ssl.supported_protocols: [TLSv1.2,TLSv1.3]
#
# The HTTP API can be configured to require authentication. Acceptable values are
#  - \`none\`:  no auth is required (default)
#  - \`basic\`: clients must authenticate with HTTP Basic auth, as configured
#             with \`api.auth.basic.*\` options below
# api.auth.type: none
#
# When configured with \`api.auth.type\` \`basic\`, you must provide the credentials
# that requests will be validated against. Usage of Environment or Keystore
# variable replacements is encouraged (such as the value \`"\${HTTP_PASS}"\`, which
# resolves to the value stored in the keystore's \`HTTP_PASS\` variable if present
# or the same variable from the environment)
#
# api.auth.basic.username: "logstash-user"
# api.auth.basic.password: "s3cUreP4$$w0rD"
#
# When setting \`api.auth.basic.password\`, the password should meet
# the default password policy requirements.
# The default password policy requires non-empty minimum 8 char string that
# includes a digit, upper case letter and lower case letter.
# Policy mode sets Logstash to WARN or ERROR when HTTP authentication password doesn't
# meet the password policy requirements.
# The default is WARN. Setting to ERROR enforces stronger passwords (recommended).
#
# api.auth.basic.password_policy.mode: WARN
#
# ------------ Queuing Settings --------------
#
# Internal queuing model, "memory" for legacy in-memory based queuing and
# "persisted" for disk-based acked queueing. Defaults is memory
#
queue.type: persisted
#
# If \`queue.type: persisted\`, the directory path where the pipeline data files will be stored.
# Each pipeline will group its PQ files in a subdirectory matching its \`pipeline.id\`.
# Default is path.data/queue.
#
# path.queue:
#
# If using queue.type: persisted, the page data files size. The queue data consists of
# append-only data files separated into pages. Default is 64mb
#
# queue.page_capacity: 64mb
#
# If using queue.type: persisted, the maximum number of unread events in the queue.
# Default is 0 (unlimited)
#
# queue.max_events: 0
#
# If using queue.type: persisted, the total capacity of the queue in number of bytes.
# If you would like more unacked events to be buffered in Logstash, you can increase the
# capacity using this setting. Please make sure your disk drive has capacity greater than
# the size specified here. If both max_bytes and max_events are specified, Logstash will pick
# whichever criteria is reached first
# Default is 1024mb or 1gb
#
# queue.max_bytes: 1024mb
#
# If using queue.type: persisted, the maximum number of acked events before forcing a checkpoint
# Default is 1024, 0 for unlimited
#
# queue.checkpoint.acks: 1024
#
# If using queue.type: persisted, the maximum number of written events before forcing a checkpoint
# Default is 1024, 0 for unlimited
#
# queue.checkpoint.writes: 1024
#
# If using queue.type: persisted, the interval in milliseconds when a checkpoint is forced on the head page
# Default is 1000, 0 for no periodic checkpoint.
#
# queue.checkpoint.interval: 1000
#
# ------------ Dead-Letter Queue Settings --------------
# Flag to turn on dead-letter queue.
#
# dead_letter_queue.enable: false

# If using dead_letter_queue.enable: true, the maximum size of each dead letter queue. Entries
# will be dropped if they would increase the size of the dead letter queue beyond this setting.
# Default is 1024mb
# dead_letter_queue.max_bytes: 1024mb

# If using dead_letter_queue.enable: true, the interval in milliseconds where if no further events eligible for the DLQ
# have been created, a dead letter queue file will be written. A low value here will mean that more, smaller, queue files
# may be written, while a larger value will introduce more latency between items being "written" to the dead letter queue, and
# being available to be read by the dead_letter_queue input when items are written infrequently.
# Default is 5000.
#
# dead_letter_queue.flush_interval: 5000

# If using dead_letter_queue.enable: true, controls which entries should be dropped to avoid exceeding the size limit.
# Set the value to \`drop_newer\` (default) to stop accepting new events that would push the DLQ size over the limit.
# Set the value to \`drop_older\` to remove queue pages containing the oldest events to make space for new ones.
#
# dead_letter_queue.storage_policy: drop_newer

# If using dead_letter_queue.enable: true, the interval that events have to be considered valid. After the interval has
# expired the events could be automatically deleted from the DLQ.
# The interval could be expressed in days, hours, minutes or seconds, using as postfix notation like 5d,
# to represent a five days interval.
# The available units are respectively d, h, m, s for day, hours, minutes and seconds.
# If not specified then the DLQ doesn't use any age policy for cleaning events.
#
# dead_letter_queue.retain.age: 1d

# If using dead_letter_queue.enable: true, the directory path where the data files will be stored.
# Default is path.data/dead_letter_queue
#
# path.dead_letter_queue:
#
# ------------ Debugging Settings --------------
#
# Options for log.level:
#   * fatal
#   * error
#   * warn
#   * info (default)
#   * debug
#   * trace
# log.level: info
#
# Options for log.format:
#   * plain (default)
#   * json
#
# log.format: plain
# log.format.json.fix_duplicate_message_fields: true
#
# path.logs:
#
# ------------ Other Settings --------------
#
# Allow or block running Logstash as superuser (default: true). Windows are excluded from the checking
# allow_superuser: false
#
# Where to find custom plugins
# path.plugins: []
#
# Flag to output log lines of each pipeline in its separate log file. Each log filename contains the pipeline.name
# Default is false
# pipeline.separate_logs: false
#
# Determine where to allocate memory buffers, for plugins that leverage them.
# Defaults to heap,but can be switched to direct if you prefer using direct memory space instead.
# pipeline.buffer.type: heap
#

allow_superuser: true`;

export const DORIS_TEMPLATE = `CREATE TABLE \`create_table_template\` (
  \`log_time\` datetime(3) NOT NULL,
  \`host\` varchar(255) NULL COMMENT "hostname or ip",
  \`source\` varchar(255) NULL COMMENT "log path",
  \`log_offset\` bigint NULL COMMENT "日志所在kafka主题偏移量",
  \`message\` variant NULL,  -- 消息内容
  \`message_text\` string NULL, -- 消息内容文本,用于倒排索引,关键字检索加速
   
  -- 添加或者删除字段 ....

  INDEX idx_message (\`message_text\`) USING INVERTED PROPERTIES("support_phrase" = "true", "parser" = "unicode", "lower_case" = "true")
) ENGINE=OLAP
DUPLICATE KEY(\`log_time\`)
AUTO PARTITION BY RANGE (date_trunc(\`log_time\`, 'hour'))
()
DISTRIBUTED BY RANDOM BUCKETS 6
PROPERTIES (
-- replication_allocation: 指定副本分配策略，定义数据副本的存储位置和数量
"replication_allocation" = "tag.location.default: 3",
-- min_load_replica_num: 最小加载副本数，-1表示无限制，优化查询性能
"min_load_replica_num" = "-1",
-- is_being_synced: 是否处于同步状态，false表示不进行数据同步
"is_being_synced" = "false",
-- dynamic_partition.enable: 启用动态分区，自动根据时间创建分区
"dynamic_partition.enable" = "true",
-- dynamic_partition.time_unit: 动态分区的粒度，hour表示按小时分区
"dynamic_partition.time_unit" = "hour",
-- dynamic_partition.time_zone: 分区时间使用的时区，设置为Asia/Shanghai
"dynamic_partition.time_zone" = "Asia/Shanghai",
-- dynamic_partition.start: 动态分区的历史范围，-1440表示保留过去1440小时的分区
-- "dynamic_partition.start" = "-1440",
-- dynamic_partition.end: 动态分区的未来范围，0表示不预创建未来分区
"dynamic_partition.end" = "0",
-- dynamic_partition.prefix: 动态分区名称前缀，p为分区名前缀
"dynamic_partition.prefix" = "p",
-- dynamic_partition.replication_allocation: 动态分区副本分配策略，与主表一致
"dynamic_partition.replication_allocation" = "tag.location.default: 3",
-- dynamic_partition.buckets: 动态分区的桶数，6表示数据分布到6个桶
-- 动态分区的桶数，建议每个数据桶最大存储2G， 如果该分区数据最大量10G，则桶数填充 5
-- "dynamic_partition.buckets" = "6",
-- dynamic_partition.create_history_partition: 是否创建历史分区，false表示不创建
"dynamic_partition.create_history_partition" = "false",
-- dynamic_partition.history_partition_num: 历史分区数量，-1表示无限制
"dynamic_partition.history_partition_num" = "-1",
-- dynamic_partition.hot_partition_num: 热分区数量，0表示无热分区
"dynamic_partition.hot_partition_num" = "0",
-- dynamic_partition.reserved_history_periods: 保留的历史分区周期，NULL表示无限制
"dynamic_partition.reserved_history_periods" = "NULL",
-- dynamic_partition.storage_policy: 存储策略，空表示使用默认存储
"dynamic_partition.storage_policy" = "",
-- inverted_index_storage_format: 倒排索引存储格式，V2为当前版本
"inverted_index_storage_format" = "V2",
-- compression: 数据压缩算法，ZSTD提供高压缩比和快速解压
"compression" = "ZSTD",
-- disable_auto_compaction: 是否禁用自动压缩，false表示启用自动压缩
"disable_auto_compaction" = "false",
-- group_commit_interval_ms: 组提交间隔，10000ms表示每10秒提交一批数据
"group_commit_interval_ms" = "10000",
-- group_commit_data_bytes: 组提交数据量阈值，134217728字节（128MB）触发提交
"group_commit_data_bytes" = "134217728",

"compaction_policy" = "time_series",
"time_series_compaction_goal_size_mbytes" = "1024",
"time_series_compaction_file_count_threshold" = "2000",
"time_series_compaction_time_threshold_seconds" = "3600",
"time_series_compaction_empty_rowsets_threshold" = "5",
"time_series_compaction_level_threshold" = "1"
);`;
