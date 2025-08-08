package com.hinadt.miaocha.ai.tool;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;

public class DateTimeTools {

    @Tool(description = "获取当前时间")
    public String getCurrentTime() {
        // 获取中国上海时间，支持毫秒精度
        ZonedDateTime shanghaiTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return shanghaiTime.format(formatter);
    }
}
