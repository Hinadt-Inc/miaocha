package com.hinadt.miaocha.application.service.sql.processor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 时间颗粒度计算器
 *
 * <p>基于 Kibana 的动态时间颗粒度算法实现，提供更智能的时间分组策略： 1. 根据时间范围动态计算目标桶数量（默认约50个） 2. 按公式计算原始间隔：时间范围/目标桶数 3.
 * 四舍五入到人性化的标准时间单位 4. 支持性能保护和用户自定义颗粒度
 */
@Component
public class TimeGranularityCalculator {
    private static final Logger logger = LoggerFactory.getLogger(TimeGranularityCalculator.class);

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 默认目标桶数量，平衡展示效果和性能 */
    private static final int DEFAULT_TARGET_BUCKETS = 50;

    /** 最大允许桶数量，防止性能问题 */
    private static final int MAX_BUCKETS = 10000;

    /** 最小桶数量，确保基本的数据展示 */
    private static final int MIN_BUCKETS = 5;

    /** 标准时间间隔列表，按从小到大排序 */
    private static final List<TimeInterval> STANDARD_INTERVALS =
            Arrays.asList(
                    new TimeInterval("second", 1, Duration.ofSeconds(1)),
                    new TimeInterval("second", 5, Duration.ofSeconds(5)),
                    new TimeInterval("second", 10, Duration.ofSeconds(10)),
                    new TimeInterval("second", 15, Duration.ofSeconds(15)),
                    new TimeInterval("second", 30, Duration.ofSeconds(30)),
                    new TimeInterval("minute", 1, Duration.ofMinutes(1)),
                    new TimeInterval("minute", 2, Duration.ofMinutes(2)),
                    new TimeInterval("minute", 5, Duration.ofMinutes(5)),
                    new TimeInterval("minute", 10, Duration.ofMinutes(10)),
                    new TimeInterval("minute", 15, Duration.ofMinutes(15)),
                    new TimeInterval("minute", 30, Duration.ofMinutes(30)),
                    new TimeInterval("hour", 1, Duration.ofHours(1)),
                    new TimeInterval("hour", 2, Duration.ofHours(2)),
                    new TimeInterval("hour", 3, Duration.ofHours(3)),
                    new TimeInterval("hour", 6, Duration.ofHours(6)),
                    new TimeInterval("hour", 12, Duration.ofHours(12)),
                    new TimeInterval("day", 1, Duration.ofDays(1)),
                    new TimeInterval("day", 2, Duration.ofDays(2)),
                    new TimeInterval("day", 3, Duration.ofDays(3)),
                    new TimeInterval("day", 7, Duration.ofDays(7)),
                    new TimeInterval("day", 14, Duration.ofDays(14)),
                    new TimeInterval("day", 30, Duration.ofDays(30)));

    /**
     * 计算最优时间颗粒度
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param userSpecifiedUnit 用户指定的时间单位（如果为 "auto" 则自动计算）
     * @param targetBuckets 目标桶数量（可选，默认50）
     * @return 时间颗粒度计算结果
     */
    public TimeGranularityResult calculateOptimalGranularity(
            String startTime, String endTime, String userSpecifiedUnit, Integer targetBuckets) {

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DATETIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATETIME_FORMATTER);

            // 如果用户指定了非auto的时间单位，直接使用
            if (!"auto".equals(userSpecifiedUnit)) {
                return createResultFromUserSpecified(start, end, userSpecifiedUnit);
            }

            // 使用默认目标桶数量
            int actualTargetBuckets =
                    targetBuckets != null ? targetBuckets : DEFAULT_TARGET_BUCKETS;

            return calculateAutomaticGranularity(start, end, actualTargetBuckets);

        } catch (Exception e) {
            logger.error("计算时间颗粒度失败，使用降级策略", e);
            return createFallbackResult();
        }
    }

    /** 基于用户指定的时间单位创建结果 */
    private TimeGranularityResult createResultFromUserSpecified(
            LocalDateTime start, LocalDateTime end, String userUnit) {

        TimeInterval matchedInterval = findStandardInterval(userUnit);
        if (matchedInterval == null) {
            logger.warn("用户指定的时间单位 {} 不在标准列表中，使用降级策略", userUnit);
            return createFallbackResult();
        }

        Duration timeRange = Duration.between(start, end);
        long actualBuckets = timeRange.toSeconds() / matchedInterval.duration.toSeconds();

        return TimeGranularityResult.builder()
                .timeUnit(matchedInterval.unit)
                .interval(matchedInterval.value)
                .estimatedBuckets((int) Math.min(actualBuckets, MAX_BUCKETS))
                .calculationMethod("USER_SPECIFIED")
                .timeRangeDuration(timeRange)
                .build();
    }

    /** 自动计算最优时间颗粒度 */
    private TimeGranularityResult calculateAutomaticGranularity(
            LocalDateTime start, LocalDateTime end, int targetBuckets) {

        Duration timeRange = Duration.between(start, end);

        // 特殊处理：0秒时间范围
        if (timeRange.isZero()) {
            return TimeGranularityResult.builder()
                    .timeUnit("second")
                    .interval(1)
                    .estimatedBuckets(MIN_BUCKETS)
                    .calculationMethod("AUTO_CALCULATED")
                    .timeRangeDuration(timeRange)
                    .targetBuckets(targetBuckets)
                    .rawIntervalSeconds(0L)
                    .build();
        }

        // 计算原始间隔：时间范围 / 目标桶数量
        Duration rawInterval = timeRange.dividedBy(targetBuckets);

        // 找到最接近的标准时间间隔
        TimeInterval optimalInterval = findClosestStandardInterval(rawInterval);

        // 计算实际桶数量
        long actualBuckets = timeRange.toSeconds() / optimalInterval.duration.toSeconds();

        // 性能保护：如果桶数过多，选择更大的间隔
        if (actualBuckets > MAX_BUCKETS) {
            optimalInterval = findIntervalForMaxBuckets(timeRange, MAX_BUCKETS);
            actualBuckets = timeRange.toSeconds() / optimalInterval.duration.toSeconds();
            logger.warn("桶数量超过最大限制，调整为更大的时间间隔: {}", optimalInterval);
        }

        // 确保最小桶数量
        if (actualBuckets < MIN_BUCKETS) {
            optimalInterval = findIntervalForMinBuckets(timeRange, MIN_BUCKETS);
            actualBuckets = timeRange.toSeconds() / optimalInterval.duration.toSeconds();
        }

        logger.debug(
                "时间颗粒度计算: 时间范围={}分钟, 目标桶数={}, 选择间隔={}_{}, 实际桶数={}",
                timeRange.toMinutes(),
                targetBuckets,
                optimalInterval.unit,
                optimalInterval.value,
                actualBuckets);

        return TimeGranularityResult.builder()
                .timeUnit(optimalInterval.unit)
                .interval(optimalInterval.value)
                .estimatedBuckets((int) actualBuckets)
                .calculationMethod("AUTO_CALCULATED")
                .timeRangeDuration(timeRange)
                .targetBuckets(targetBuckets)
                .rawIntervalSeconds(rawInterval.toSeconds())
                .build();
    }

    /** 查找标准时间间隔 */
    private TimeInterval findStandardInterval(String unit) {
        return STANDARD_INTERVALS.stream()
                .filter(interval -> interval.unit.equals(unit) && interval.value == 1)
                .findFirst()
                .orElse(null);
    }

    /** 查找最接近原始间隔的标准时间间隔 */
    private TimeInterval findClosestStandardInterval(Duration rawInterval) {
        long rawSeconds = rawInterval.toSeconds();

        TimeInterval closest = STANDARD_INTERVALS.get(0);
        long minDifference = Math.abs(rawSeconds - closest.duration.toSeconds());

        for (TimeInterval interval : STANDARD_INTERVALS) {
            long difference = Math.abs(rawSeconds - interval.duration.toSeconds());
            if (difference < minDifference) {
                minDifference = difference;
                closest = interval;
            }
        }

        return closest;
    }

    /** 查找能满足最大桶数限制的时间间隔 */
    private TimeInterval findIntervalForMaxBuckets(Duration timeRange, int maxBuckets) {
        long minIntervalSeconds = timeRange.toSeconds() / maxBuckets;

        for (TimeInterval interval : STANDARD_INTERVALS) {
            if (interval.duration.toSeconds() >= minIntervalSeconds) {
                return interval;
            }
        }

        // 如果都不满足，返回最大的间隔
        return STANDARD_INTERVALS.get(STANDARD_INTERVALS.size() - 1);
    }

    /** 查找能满足最小桶数要求的时间间隔 */
    private TimeInterval findIntervalForMinBuckets(Duration timeRange, int minBuckets) {
        long maxIntervalSeconds = timeRange.toSeconds() / minBuckets;

        TimeInterval result = STANDARD_INTERVALS.get(0);
        for (TimeInterval interval : STANDARD_INTERVALS) {
            if (interval.duration.toSeconds() <= maxIntervalSeconds) {
                result = interval;
            } else {
                break;
            }
        }

        return result;
    }

    /** 创建降级结果 */
    private TimeGranularityResult createFallbackResult() {
        return TimeGranularityResult.builder()
                .timeUnit("minute")
                .interval(1)
                .estimatedBuckets(60)
                .calculationMethod("FALLBACK")
                .build();
    }

    /** 时间间隔定义 */
    private static class TimeInterval {
        final String unit; // 时间单位：second, minute, hour, day
        final int value; // 数值：如 5 (5分钟)
        final Duration duration; // 实际时长

        TimeInterval(String unit, int value, Duration duration) {
            this.unit = unit;
            this.value = value;
            this.duration = duration;
        }

        @Override
        public String toString() {
            return String.format("%s_%d", unit, value);
        }
    }

    /** 时间颗粒度计算结果 */
    public static class TimeGranularityResult {
        private String timeUnit; // 时间单位
        private int interval; // 间隔数值
        private int estimatedBuckets; // 预估桶数量
        private String calculationMethod; // 计算方法
        private Duration timeRangeDuration; // 时间范围时长
        private Integer targetBuckets; // 目标桶数量
        private Long rawIntervalSeconds; // 原始间隔秒数

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private TimeGranularityResult result = new TimeGranularityResult();

            public Builder timeUnit(String timeUnit) {
                result.timeUnit = timeUnit;
                return this;
            }

            public Builder interval(int interval) {
                result.interval = interval;
                return this;
            }

            public Builder estimatedBuckets(int estimatedBuckets) {
                result.estimatedBuckets = estimatedBuckets;
                return this;
            }

            public Builder calculationMethod(String calculationMethod) {
                result.calculationMethod = calculationMethod;
                return this;
            }

            public Builder timeRangeDuration(Duration timeRangeDuration) {
                result.timeRangeDuration = timeRangeDuration;
                return this;
            }

            public Builder targetBuckets(Integer targetBuckets) {
                result.targetBuckets = targetBuckets;
                return this;
            }

            public Builder rawIntervalSeconds(Long rawIntervalSeconds) {
                result.rawIntervalSeconds = rawIntervalSeconds;
                return this;
            }

            public TimeGranularityResult build() {
                return result;
            }
        }

        // Getters
        public String getTimeUnit() {
            return timeUnit;
        }

        public int getInterval() {
            return interval;
        }

        public int getEstimatedBuckets() {
            return estimatedBuckets;
        }

        public String getCalculationMethod() {
            return calculationMethod;
        }

        public Duration getTimeRangeDuration() {
            return timeRangeDuration;
        }

        public Integer getTargetBuckets() {
            return targetBuckets;
        }

        public Long getRawIntervalSeconds() {
            return rawIntervalSeconds;
        }

        /** 获取用于SQL查询的时间单位字符串 如果interval > 1，需要特殊处理 */
        public String getSqlTimeUnit() {
            if (interval == 1) {
                return timeUnit;
            } else {
                // 对于非1的间隔，可能需要在SQL层面特殊处理
                // 这里先返回基础单位，具体的间隔处理在SQL构建器中实现
                return timeUnit;
            }
        }

        /** 获取详细描述信息，用于调试和日志 */
        public String getDetailedDescription() {
            return String.format(
                    "TimeGranularity[unit=%s, interval=%d, buckets=%d, method=%s]",
                    timeUnit, interval, estimatedBuckets, calculationMethod);
        }
    }
}
