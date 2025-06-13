package com.hinadt.miaocha.application.service.sql.processor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 时间颗粒度计算器测试类 验证基于 Kibana 算法的时间颗粒度计算逻辑 */
@DisplayName("时间颗粒度计算器测试")
class TimeGranularityCalculatorTest {

    private TimeGranularityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new TimeGranularityCalculator();
    }

    @Nested
    @DisplayName("自动计算时间颗粒度测试")
    class AutoCalculationTests {

        @Test
        @DisplayName("5分钟时间范围 - 应选择秒级颗粒度")
        void testFiveMinuteRange() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:05:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertEquals("second", result.getTimeUnit());
            assertTrue(result.getInterval() >= 1);
            assertTrue(result.getEstimatedBuckets() <= 300); // 5分钟 = 300秒
            assertEquals("AUTO_CALCULATED", result.getCalculationMethod());
        }

        @Test
        @DisplayName("1小时时间范围 - 应选择分钟级颗粒度")
        void testOneHourRange() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertEquals("minute", result.getTimeUnit());
            assertTrue(result.getInterval() >= 1);
            assertTrue(result.getEstimatedBuckets() >= 30 && result.getEstimatedBuckets() <= 60);
            assertEquals("AUTO_CALCULATED", result.getCalculationMethod());
        }

        @Test
        @DisplayName("24小时时间范围 - 应选择30分钟颗粒度")
        void testOneDayRange() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-02 00:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // 86400秒 ÷ 50 = 1728秒 ≈ 29分钟，最接近的是30分钟
            assertEquals("minute", result.getTimeUnit());
            assertEquals(30, result.getInterval());
            assertEquals(48, result.getEstimatedBuckets()); // 86400 ÷ 1800 = 48
            assertEquals("AUTO_CALCULATED", result.getCalculationMethod());
        }

        @Test
        @DisplayName("7天时间范围 - 应选择小时或天级颗粒度")
        void testOneWeekRange() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-08 00:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertTrue("hour".equals(result.getTimeUnit()) || "day".equals(result.getTimeUnit()));
            assertTrue(result.getEstimatedBuckets() >= 30 && result.getEstimatedBuckets() <= 168);
            assertEquals("AUTO_CALCULATED", result.getCalculationMethod());
        }

        @Test
        @DisplayName("30天时间范围 - 应选择12小时颗粒度")
        void testOneMonthRange() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-31 00:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // 2592000秒 ÷ 50 = 51840秒 ≈ 14.4小时，最接近的是12小时
            assertEquals("hour", result.getTimeUnit());
            assertEquals(12, result.getInterval());
            assertEquals(60, result.getEstimatedBuckets()); // 2592000 ÷ 43200 = 60
            assertEquals("AUTO_CALCULATED", result.getCalculationMethod());
        }
    }

    @Nested
    @DisplayName("用户指定时间单位测试")
    class UserSpecifiedTests {

        @Test
        @DisplayName("用户指定分钟级 - 应直接使用用户指定")
        void testUserSpecifiedMinute() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "minute", null);

            assertEquals("minute", result.getTimeUnit());
            assertEquals(1, result.getInterval());
            assertEquals("USER_SPECIFIED", result.getCalculationMethod());
        }

        @Test
        @DisplayName("用户指定小时级 - 应直接使用用户指定")
        void testUserSpecifiedHour() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-02 00:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "hour", null);

            assertEquals("hour", result.getTimeUnit());
            assertEquals(1, result.getInterval());
            assertEquals("USER_SPECIFIED", result.getCalculationMethod());
        }

        @Test
        @DisplayName("用户指定无效单位 - 应降级处理")
        void testUserSpecifiedInvalidUnit() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "invalid", null);

            assertEquals("minute", result.getTimeUnit());
            assertEquals("FALLBACK", result.getCalculationMethod());
        }
    }

    @Nested
    @DisplayName("自定义目标桶数量测试")
    class CustomTargetBucketsTests {

        @Test
        @DisplayName("目标桶数量20 - 应选择更粗的颗粒度")
        void testLowerTargetBuckets() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result20 =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 20);
            TimeGranularityCalculator.TimeGranularityResult result50 =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // 目标桶数量更少时，桶数应该更少或相等（考虑标准间隔限制）
            // 但是由于新的最小桶数量限制为45，这个测试需要调整
            assertTrue(result20.getEstimatedBuckets() >= 45); // 不能少于45
            assertTrue(result50.getEstimatedBuckets() >= 45); // 不能少于45
        }

        @Test
        @DisplayName("目标桶数量100 - 应选择更细的颗粒度")
        void testHigherTargetBuckets() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result50 =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);
            TimeGranularityCalculator.TimeGranularityResult result100 =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 100);

            // 目标桶数量更多时，桶数应该更多
            assertTrue(result100.getEstimatedBuckets() >= result50.getEstimatedBuckets());
            assertTrue(result100.getEstimatedBuckets() >= 60); // 接近目标100
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("极短时间范围 - 应确保最小桶数量")
        void testVeryShortTimeRange() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:00:30"; // 30秒

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertTrue(result.getEstimatedBuckets() >= 45); // 更新为45最小桶数量
            // 30秒范围现在可以使用毫秒级别，所以应该是millisecond或second
            assertTrue(
                    "millisecond".equals(result.getTimeUnit())
                            || "second".equals(result.getTimeUnit()));
        }

        @Test
        @DisplayName("极长时间范围 - 应确保不超过最大桶数量")
        void testVeryLongTimeRange() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2025-01-01 00:00:00"; // 2年

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 10000);

            assertTrue(result.getEstimatedBuckets() <= 10000); // 不超过最大桶数量
            // 63072000秒 ÷ 10000 = 6307秒 ≈ 1.75小时，最接近的是2小时
            assertEquals("hour", result.getTimeUnit());
            assertEquals(2, result.getInterval());
        }

        @Test
        @DisplayName("无效时间格式 - 应降级处理")
        void testInvalidTimeFormat() {
            String startTime = "invalid-time";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertEquals("FALLBACK", result.getCalculationMethod());
            assertEquals("minute", result.getTimeUnit());
        }

        @Test
        @DisplayName("零时间范围 - 特殊处理")
        void testZeroTimeRange() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:00:00"; // 0秒

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertTrue(result.getEstimatedBuckets() >= 45); // 更新为45最小桶数量
            assertEquals("millisecond", result.getTimeUnit()); // 现在使用毫秒级别
        }
    }

    @Nested
    @DisplayName("结果对象测试")
    class ResultObjectTests {

        @Test
        @DisplayName("结果对象完整性 - 所有字段应正确设置")
        void testResultCompleteness() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertNotNull(result.getTimeUnit());
            assertTrue(result.getInterval() > 0);
            assertTrue(result.getEstimatedBuckets() > 0);
            assertNotNull(result.getCalculationMethod());
            assertNotNull(result.getTimeRangeDuration());
            assertNotNull(result.getTargetBuckets());
            assertNotNull(result.getRawIntervalSeconds());
            assertNotNull(result.getSqlTimeUnit());
            assertNotNull(result.getDetailedDescription());
        }

        @Test
        @DisplayName("详细描述信息 - 应包含关键信息")
        void testDetailedDescription() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "minute", null);

            String description = result.getDetailedDescription();
            assertTrue(description.contains("minute"));
            assertTrue(description.contains("interval=1"));
            assertTrue(description.contains("USER_SPECIFIED"));
        }
    }

    @Nested
    @DisplayName("精确间隔分组验证测试")
    class PreciseIntervalTests {

        @Test
        @DisplayName("15分钟范围 - 验证秒级间隔选择")
        void test15MinuteRangeIntervals() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:15:00"; // 15分钟 = 900秒

            // 目标50桶：900÷50=18秒，应选择最接近的15秒间隔
            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertEquals("second", result.getTimeUnit());
            assertTrue(
                    result.getInterval() == 15
                            || result.getInterval() == 10
                            || result.getInterval() == 30);
            assertTrue(result.getEstimatedBuckets() >= 30 && result.getEstimatedBuckets() <= 90);
        }

        @Test
        @DisplayName("2小时范围 - 验证分钟级间隔选择")
        void test2HourRangeIntervals() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 12:00:00"; // 2小时 = 7200秒

            // 目标50桶：7200÷50=144秒≈2.4分钟，应选择2分钟间隔
            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertEquals("minute", result.getTimeUnit());
            assertTrue(result.getInterval() >= 1 && result.getInterval() <= 5);
            assertTrue(result.getEstimatedBuckets() >= 24 && result.getEstimatedBuckets() <= 120);
        }

        @Test
        @DisplayName("12小时范围 - 验证小时级间隔选择")
        void test12HourRangeIntervals() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-01 12:00:00"; // 12小时

            // 目标50桶：12÷50=0.24小时≈15分钟，应选择分钟或小时级
            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertTrue(
                    "minute".equals(result.getTimeUnit()) || "hour".equals(result.getTimeUnit()));
            if ("minute".equals(result.getTimeUnit())) {
                assertTrue(result.getInterval() >= 10 && result.getInterval() <= 30);
            } else {
                assertTrue(result.getInterval() >= 1 && result.getInterval() <= 2);
            }
        }

        @Test
        @DisplayName("3天范围 - 验证小时级间隔选择")
        void test3DayRangeIntervals() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-04 00:00:00"; // 3天 = 72小时

            // 目标50桶：72÷50=1.44小时，应选择1-2小时间隔
            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertTrue("hour".equals(result.getTimeUnit()) || "day".equals(result.getTimeUnit()));
            if ("hour".equals(result.getTimeUnit())) {
                assertTrue(result.getInterval() >= 1 && result.getInterval() <= 3);
            }
            assertTrue(result.getEstimatedBuckets() >= 24 && result.getEstimatedBuckets() <= 72);
        }
    }

    @Nested
    @DisplayName("Kibana算法一致性验证测试")
    class KibanaAlgorithmConsistencyTests {

        @Test
        @DisplayName("标准Kibana场景1 - 最近1小时查询")
        void testKibanaScenario1Hour() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // Kibana通常为1小时选择1分钟桶，得到60桶
            assertEquals("minute", result.getTimeUnit());
            assertEquals(1, result.getInterval());
            assertEquals(60, result.getEstimatedBuckets());
        }

        @Test
        @DisplayName("标准Kibana场景2 - 最近24小时查询")
        void testKibanaScenario24Hours() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-02 00:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // Kibana通常为24小时选择30分钟桶，得到48桶
            assertEquals("minute", result.getTimeUnit());
            assertEquals(30, result.getInterval());
            assertEquals(48, result.getEstimatedBuckets());
        }

        @Test
        @DisplayName("标准Kibana场景3 - 最近7天查询")
        void testKibanaScenario7Days() {
            String startTime = "2023-01-01 00:00:00";
            String endTime = "2023-01-08 00:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // Kibana通常为7天选择3小时桶，得到56桶
            assertTrue("hour".equals(result.getTimeUnit()) || "day".equals(result.getTimeUnit()));
            if ("hour".equals(result.getTimeUnit())) {
                assertEquals(3, result.getInterval());
                assertEquals(56, result.getEstimatedBuckets());
            }
        }

        @Test
        @DisplayName("高精度查询 - 目标桶数100验证")
        void testHighPrecisionQuery() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 100);

            // 1小时目标100桶：3600÷100=36秒，应选择30秒间隔，得到120桶
            assertEquals("second", result.getTimeUnit());
            assertEquals(30, result.getInterval());
            assertEquals(120, result.getEstimatedBuckets());
        }

        @Test
        @DisplayName("粗粒度查询 - 验证小目标桶数")
        void testCoarseGrainQuery() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 11:00:00";

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 15);

            // 即使目标桶数量很小，也不应该少于最小桶数量45
            assertTrue(result.getEstimatedBuckets() >= 45);
        }
    }

    @Nested
    @DisplayName("性能保护机制验证测试")
    class PerformanceProtectionTests {

        @Test
        @DisplayName("超大时间范围 - 验证最大桶数限制")
        void testMaxBucketsLimit() {
            String startTime = "2020-01-01 00:00:00";
            String endTime = "2023-01-01 00:00:00"; // 3年

            // 即使目标桶数很大，也应该被限制在最大桶数内
            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 20000);

            assertTrue(result.getEstimatedBuckets() <= 10000); // 最大桶数限制
            // 94608000秒 ÷ 10000 = 9460秒 ≈ 2.6小时，最接近的是3小时
            assertEquals("hour", result.getTimeUnit());
            assertEquals(3, result.getInterval());
        }

        @Test
        @DisplayName("极短时间范围 - 验证最小桶数保证")
        void testMinBucketsGuarantee() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:00:10"; // 10秒

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertTrue(result.getEstimatedBuckets() >= 45); // 更新为45最小桶数量
            // 10秒范围现在可以使用毫秒级别，所以应该是millisecond或second
            assertTrue(
                    "millisecond".equals(result.getTimeUnit())
                            || "second".equals(result.getTimeUnit()));
            assertTrue(result.getInterval() >= 1 && result.getInterval() <= 500); // 调整间隔范围
        }
    }

    @Nested
    @DisplayName("算法精确性验证测试")
    class AlgorithmPrecisionTests {

        @Test
        @DisplayName("精确匹配标准间隔 - 验证算法准确性")
        void testExactStandardIntervalMatch() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:25:00"; // 25分钟 = 1500秒

            // 目标50桶：1500÷50=30秒，应该精确选择30秒间隔
            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertEquals("second", result.getTimeUnit());
            assertEquals(30, result.getInterval());
            assertEquals(50, result.getEstimatedBuckets());
            assertEquals("AUTO_CALCULATED", result.getCalculationMethod());
        }

        @Test
        @DisplayName("接近匹配验证 - 四舍五入到最近标准间隔")
        void testNearestStandardIntervalMatch() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:25:00"; // 25分钟 = 1500秒

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // 1500秒 ÷ 50 = 30秒，应该选择30秒间隔
            // 1500 ÷ 30 = 50桶，正好在45-60范围内
            assertTrue(result.getEstimatedBuckets() >= 45);
            assertTrue(result.getEstimatedBuckets() <= 60); // 在建议范围内

            // 验证时间单位合理（秒级或分钟级）
            assertTrue(
                    "second".equals(result.getTimeUnit()) || "minute".equals(result.getTimeUnit()));

            // 期望选择30秒间隔，得到50桶
            assertEquals("second", result.getTimeUnit());
            assertEquals(30, result.getInterval());
            assertEquals(50, result.getEstimatedBuckets());
        }

        @Test
        @DisplayName("跨单位边界验证 - 分钟到小时的过渡")
        void testUnitBoundaryTransition() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 12:30:00"; // 2.5小时 = 9000秒

            // 目标50桶：9000÷50=180秒=3分钟
            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            assertEquals("minute", result.getTimeUnit());
            assertTrue(result.getInterval() >= 2 && result.getInterval() <= 5); // 应该在2-5分钟范围内
            assertTrue(result.getEstimatedBuckets() >= 30 && result.getEstimatedBuckets() <= 75);
        }
    }

    @Nested
    @DisplayName("小时间范围精确度测试")
    class SmallTimeRangeTests {

        @Test
        @DisplayName("6秒时间范围 - 验证桶数量应该在45-60之间")
        void test6SecondRange() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:00:06"; // 6秒

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // 用户期望：6秒时间范围应该有45-60个桶，而不是仅仅6个
            assertTrue(
                    result.getEstimatedBuckets() >= 45 && result.getEstimatedBuckets() <= 60,
                    "6秒时间范围应该有45-60个桶，实际：" + result.getEstimatedBuckets());

            // 应该使用毫秒级别的时间单位
            assertEquals("millisecond", result.getTimeUnit(), "应该使用毫秒级别的时间单位");
        }

        @Test
        @DisplayName("30秒时间范围 - 验证桶数量优化")
        void test30SecondRange() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:00:30"; // 30秒

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // 30秒时间范围应该有合理的桶数量
            assertTrue(
                    result.getEstimatedBuckets() >= 45 && result.getEstimatedBuckets() <= 60,
                    "30秒时间范围应该有45-60个桶，实际：" + result.getEstimatedBuckets());
        }

        @Test
        @DisplayName("2分钟时间范围 - 验证毫秒级别支持")
        void test2MinuteRange() {
            String startTime = "2023-01-01 10:00:00";
            String endTime = "2023-01-01 10:02:00"; // 2分钟

            TimeGranularityCalculator.TimeGranularityResult result =
                    calculator.calculateOptimalGranularity(startTime, endTime, "auto", 50);

            // 2分钟时间范围应该有合理的桶数量
            assertTrue(
                    result.getEstimatedBuckets() >= 45 && result.getEstimatedBuckets() <= 60,
                    "2分钟时间范围应该有45-60个桶，实际：" + result.getEstimatedBuckets());
        }
    }
}
