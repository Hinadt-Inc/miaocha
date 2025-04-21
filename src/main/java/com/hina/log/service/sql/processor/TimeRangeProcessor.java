package com.hina.log.service.sql.processor;

import com.hina.log.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 时间范围处理器
 */
@Component
public class TimeRangeProcessor {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理时间范围
     */
    public void processTimeRange(LogSearchDTO dto) {
        if (StringUtils.isNotBlank(dto.getStartTime()) && StringUtils.isNotBlank(dto.getEndTime())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = null;
        LocalDateTime endTime = now;

        if (StringUtils.isNotBlank(dto.getTimeRange())) {
            TimeRange timeRange = TimeRange.fromString(dto.getTimeRange());
            if (timeRange != null) {
                startTime = timeRange.calculateStartTime(now);
                if (timeRange == TimeRange.YESTERDAY) {
                    endTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                }
            }
        }

        if (startTime == null) {
            startTime = now.minus(24, ChronoUnit.HOURS);
        }

        dto.setStartTime(startTime.format(DATETIME_FORMATTER));
        dto.setEndTime(endTime.format(DATETIME_FORMATTER));
    }

    /**
     * 确定时间分组单位
     */
    public String determineTimeUnit(LogSearchDTO dto) {
        if (!"auto".equals(dto.getTimeGrouping())) {
            return dto.getTimeGrouping();
        }

        try {
            LocalDateTime startTime = LocalDateTime.parse(dto.getStartTime(), DATETIME_FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(dto.getEndTime(), DATETIME_FORMATTER);

            long seconds = ChronoUnit.SECONDS.between(startTime, endTime);

            if (seconds <= 300) { // 5分钟内
                return "second";
            } else if (seconds <= 3600) { // 1小时内
                return "minute";
            } else if (seconds <= 86400 * 2 ) { // 2天内
                return "hour";
            } else {
                return "day";
            }
        } catch (Exception e) {
            return "minute";
        }
    }

    /**
     * 预定义时间范围枚举
     */
    private enum TimeRange {
        LAST_5M("last_5m", (now) -> now.minus(5, ChronoUnit.MINUTES)),
        LAST_15M("last_15m", (now) -> now.minus(15, ChronoUnit.MINUTES)),
        LAST_30M("last_30m", (now) -> now.minus(30, ChronoUnit.MINUTES)),
        LAST_1H("last_1h", (now) -> now.minus(1, ChronoUnit.HOURS)),
        LAST_8H("last_8h", (now) -> now.minus(8, ChronoUnit.HOURS)),
        LAST_24H("last_24h", (now) -> now.minus(24, ChronoUnit.HOURS)),
        TODAY("today", (now) -> LocalDateTime.of(LocalDate.now(), LocalTime.MIN)),
        YESTERDAY("yesterday", (now) -> LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIN)),
        LAST_WEEK("last_week", (now) -> now.minus(7, ChronoUnit.DAYS));

        private final String value;
        private final TimeRangeCalculator calculator;

        TimeRange(String value, TimeRangeCalculator calculator) {
            this.value = value;
            this.calculator = calculator;
        }

        public LocalDateTime calculateStartTime(LocalDateTime now) {
            return calculator.calculate(now);
        }

        public static TimeRange fromString(String value) {
            for (TimeRange range : values()) {
                if (range.value.equals(value)) {
                    return range;
                }
            }
            return null;
        }

        @FunctionalInterface
        private interface TimeRangeCalculator {
            LocalDateTime calculate(LocalDateTime now);
        }
    }
}