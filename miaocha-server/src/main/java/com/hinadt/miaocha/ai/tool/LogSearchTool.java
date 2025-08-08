package com.hinadt.miaocha.ai.tool;

import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.cache.SystemCacheDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchCacheDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 日志搜索工具类
 *
 * <p>提供AI助手调用的日志搜索相关功能，包括日志明细查询、时间分布统计、字段分布分析、 表结构查询和搜索条件管理等功能。
 */
@Component
@Slf4j
public class LogSearchTool {

    private final LogSearchService logSearchService;

    public LogSearchTool(LogSearchService logSearchService) {
        this.logSearchService = logSearchService;
    }

    /**
     * 执行日志明细查询
     *
     * <p>根据指定的搜索条件查询日志明细数据，支持关键字搜索、时间范围过滤、 自定义SQL条件、分页和排序等功能。
     */
    @Tool(description = "通知前端做日志明细查询，根据搜索条件查询具体的日志记录列表。支持关键字搜索、时间范围过滤、SQL条件、分页排序等功能。")
    public String sendSearchLogDetailsAction(
            @ToolParam(
                            description =
                                    "查询日志的模块名称, 如 k8s-hina-cloud ... ,"
                                            + " 等，不同业务线的应用通过模块名称来进行区分，一个模块对应实际Doris存储的一张日志表",
                            required = true)
                    String module,
            @ToolParam(
                            description =
                                    "搜索关键字列表，支持逻辑操作符：&& (AND)、|| (OR)、- (NOT)，支持括号和嵌套（最多两层）。示例：-"
                                        + " 'debug'（排除debug）、- 'error' && -"
                                        + " 'engine'（同时排除error和engine）、error && (- timeout) && (-"
                                        + " retry)（查询error但排除timeout和retry）")
                    List<String> keywords,
            @ToolParam(
                            description =
                                    "WHERE条件SQL列表，每个条件直接拼接到SQL中，多个条件用AND连接。注意：必须使用模块对应的Doris表中实际存在的字段名")
                    List<String> whereSqls,
            @ToolParam(description = "开始时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String startTime,
            @ToolParam(description = "结束时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String endTime,
            @ToolParam(
                            description =
                                    "预定义时间范围：last_5m, last_15m, last_30m, last_1h, last_8h,"
                                            + " last_24h, today, yesterday,"
                                            + " last_week。注意：建议优先使用startTime和endTime手动指定时间范围")
                    String timeRange,
            @ToolParam(description = "分页大小，默认50，最大5000, 建议调用设为 20") Integer pageSize,
            @ToolParam(description = "分页偏移量，默认0") Integer offset,
            @ToolParam(
                            description =
                                    "查询字段列表，必须指定模块对应的Doris表中实际存在的字段名，不能为空。常用字段如：log_time,"
                                            + " message_text 等",
                            required = true)
                    List<String> fields) {

        LogSearchDTO dto = new LogSearchDTO();
        dto.setModule(module);
        dto.setKeywords(keywords);
        dto.setWhereSqls(whereSqls);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setTimeRange(timeRange);
        dto.setPageSize(pageSize != null ? pageSize : 50);
        dto.setOffset(offset != null ? offset : 0);
        dto.setFields(fields);

        log.info("日志明细前端工具触发");
        return "日志明细已经查询，结果已经呈现在前端日志查询主界面上";
    }

    /**
     * 执行日志时间分布查询
     *
     * <p>查询日志在时间维度上的分布情况，用于生成时间分布柱状图。 系统会自动计算最优的时间颗粒度以获得合适的桶数量。
     */
    @Tool(description = "通知前端做日志时间分布直方图查询，查询日志在时间维度上的分布数据，用于生成时间分布柱状图。")
    public String sendSearchLogHistogramAction(
            @ToolParam(
                            description =
                                    "查询日志的模块名称, 如 k8s-hina-cloud ... ,"
                                            + " 等，不同业务线的应用通过模块名称来进行区分，一个模块对应实际Doris存储的一张日志表",
                            required = true)
                    String module,
            @ToolParam(
                            description =
                                    "搜索关键字列表，支持逻辑操作符：&& (AND)、|| (OR)、- (NOT)，支持括号和嵌套（最多两层）。示例：-"
                                        + " 'debug'（排除debug）、- 'error' && -"
                                        + " 'engine'（同时排除error和engine）、error && (- timeout) && (-"
                                        + " retry)（查询error但排除timeout和retry）")
                    List<String> keywords,
            @ToolParam(
                            description =
                                    "WHERE条件SQL列表，每个条件直接拼接到SQL中，多个条件用AND连接。注意：必须使用模块对应的Doris表中实际存在的字段名")
                    List<String> whereSqls,
            @ToolParam(description = "开始时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String startTime,
            @ToolParam(description = "结束时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String endTime,
            @ToolParam(
                            description =
                                    "预定义时间范围：last_5m, last_15m, last_30m, last_1h, last_8h,"
                                            + " last_24h, today, yesterday,"
                                            + " last_week。注意：建议优先使用startTime和endTime手动指定时间范围")
                    String timeRange,
            @ToolParam(
                            description =
                                    "时间分组单位：millisecond, second, minute, hour, day,"
                                            + " auto（默认auto，建议使用默认值）")
                    String timeGrouping,
            @ToolParam(description = "目标桶数量，用于智能时间颗粒度计算，默认50，建议使用默认值") Integer targetBuckets) {

        LogSearchDTO dto = new LogSearchDTO();
        dto.setModule(module);
        dto.setKeywords(keywords);
        dto.setWhereSqls(whereSqls);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setTimeRange(timeRange);
        dto.setTimeGrouping(timeGrouping != null ? timeGrouping : "auto");
        dto.setTargetBuckets(targetBuckets != null ? targetBuckets : 50);

        log.info("日志时间分布直方图前端工具触发");
        return "日志时间分布直方图已经查询，结果已经呈现在前端日志查询主界面上";
    }

    /**
     * 执行字段分布查询
     *
     * <p>查询指定字段的TOP5值分布情况，用于分析字段数据的分布特征。 基于采样数据进行统计以提升查询性能。
     */
    @Tool(description = "通知前端执行字段TOP5分布查询，查询指定字段的数据分布情况。基于采样数据统计，返回各字段的Top5值及占比信息。")
    public String sendSearchFieldDistributionsAction(
            @ToolParam(
                            description =
                                    "查询日志的模块名称, 如 k8s-hina-cloud ... ,"
                                            + " 等，不同业务线的应用通过模块名称来进行区分，一个模块对应实际Doris存储的一张日志表",
                            required = true)
                    String module,
            @ToolParam(description = "需要分析分布的字段列表", required = true) List<String> fields,
            @ToolParam(
                            description =
                                    "搜索关键字列表，支持逻辑操作符：&& (AND)、|| (OR)、- (NOT)，支持括号和嵌套（最多两层）。示例：-"
                                        + " 'debug'（排除debug）、- 'error' && -"
                                        + " 'engine'（同时排除error和engine）、error && (- timeout) && (-"
                                        + " retry)（查询error但排除timeout和retry）")
                    List<String> keywords,
            @ToolParam(
                            description =
                                    "WHERE条件SQL列表，每个条件直接拼接到SQL中，多个条件用AND连接。注意：必须使用模块对应的Doris表中实际存在的字段名")
                    List<String> whereSqls,
            @ToolParam(description = "开始时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String startTime,
            @ToolParam(description = "结束时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String endTime,
            @ToolParam(
                            description =
                                    "预定义时间范围：last_5m, last_15m, last_30m, last_1h, last_8h,"
                                            + " last_24h, today, yesterday,"
                                            + " last_week。注意：建议优先使用startTime和endTime手动指定时间范围")
                    String timeRange) {

        LogSearchDTO dto = new LogSearchDTO();
        dto.setModule(module);
        dto.setFields(fields);
        dto.setKeywords(keywords);
        dto.setWhereSqls(whereSqls);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setTimeRange(timeRange);

        log.info("字段分布查询前端工具触发");
        return "字段分布查询结果已经生成，结果已经呈现在前端日志查询主界面上";
    }

    /**
     * 获取日志表结构信息
     *
     * <p>查询指定模块对应日志表的字段结构信息，包括字段名、数据类型、 是否主键、是否可为空等详细信息。
     */
    @Tool(description = "获取指定模块的日志表结构信息，包括字段名、数据类型、是否主键、是否可为空等详细信息。")
    public List<SchemaInfoDTO.ColumnInfoDTO> getModuleLogTableColumns(
            @ToolParam(
                            description =
                                    "查询日志的模块名称, 如 k8s-hina-cloud ... ,"
                                            + " 等，不同业务线的应用通过模块名称来进行区分，一个模块对应实际Doris存储的一张日志表",
                            required = true)
                    String module) {

        return logSearchService.getTableColumns(module);
    }

    /**
     * 保存日志搜索条件
     *
     * <p>将用户的个性化日志搜索条件保存为缓存，方便后续快速调用。 包括搜索参数、条件名称和描述信息。
     */
    @Tool(description = "保存用户的个性化日志搜索条件，包括搜索参数、名称和描述，返回生成的缓存键。")
    public String saveLogSearchCondition(
            @ToolParam(description = "搜索条件名称", required = true) String name,
            @ToolParam(description = "搜索条件描述") String description,
            @ToolParam(
                            description =
                                    "查询日志的模块名称, 如 k8s-hina-cloud ... ,"
                                            + " 等，不同业务线的应用通过模块名称来进行区分，一个模块对应实际Doris存储的一张日志表",
                            required = true)
                    String module,
            @ToolParam(description = "搜索关键字列表，支持逻辑操作符：&& (AND)、|| (OR)、- (NOT)，支持括号和嵌套（最多两层）")
                    List<String> keywords,
            @ToolParam(
                            description =
                                    "WHERE条件SQL列表，每个条件直接拼接到SQL中，多个条件用AND连接。注意：必须使用模块对应的Doris表中实际存在的字段名")
                    List<String> whereSqls,
            @ToolParam(description = "开始时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String startTime,
            @ToolParam(description = "结束时间，格式：yyyy-MM-dd HH:mm:ss.SSS，建议优先使用此参数而非timeRange")
                    String endTime,
            @ToolParam(
                            description =
                                    "预定义时间范围：last_5m, last_15m, last_30m, last_1h, last_8h,"
                                            + " last_24h, today, yesterday,"
                                            + " last_week。注意：建议优先使用startTime和endTime手动指定时间范围")
                    String timeRange,
            @ToolParam(
                            description =
                                    "时间分组单位：millisecond, second, minute, hour, day,"
                                            + " auto（默认auto，建议使用默认值）")
                    String timeGrouping,
            @ToolParam(description = "目标桶数量，用于智能时间颗粒度计算，默认50，建议使用默认值") Integer targetBuckets,
            @ToolParam(description = "分页大小") Integer pageSize,
            @ToolParam(description = "分页偏移量") Integer offset,
            @ToolParam(
                            description =
                                    "查询字段列表，必须指定模块对应的Doris表中实际存在的字段名，不能为空。常用字段如：timestamp, level,"
                                            + " message, module等",
                            required = true)
                    List<String> fields) {

        LogSearchCacheDTO cacheDTO = new LogSearchCacheDTO();
        cacheDTO.setName(name);
        cacheDTO.setDescription(description);
        cacheDTO.setModule(module);
        cacheDTO.setKeywords(keywords);
        cacheDTO.setWhereSqls(whereSqls);
        cacheDTO.setStartTime(startTime);
        cacheDTO.setEndTime(endTime);
        cacheDTO.setTimeRange(timeRange);
        cacheDTO.setTimeGrouping(timeGrouping);
        cacheDTO.setTargetBuckets(targetBuckets);
        cacheDTO.setPageSize(pageSize);
        cacheDTO.setOffset(offset);
        cacheDTO.setFields(fields);

        return logSearchService.saveSearchCondition(cacheDTO);
    }

    /**
     * 获取用户保存的搜索条件列表
     *
     * <p>获取当前用户保存的所有个性化日志搜索条件，包括条件详情、 创建时间、缓存键等信息。
     */
    @Tool(description = "获取当前用户保存的所有个性化日志搜索条件列表，包括条件详情、创建时间、缓存键等信息。")
    public List<SystemCacheDTO<LogSearchCacheDTO>> getUserLogSearchConditions() {
        return logSearchService.getUserSearchConditions();
    }
}
