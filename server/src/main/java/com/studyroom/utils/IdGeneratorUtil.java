package com.studyroom.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * ID生成器工具类
 */
@Component
public class IdGeneratorUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    /**
     * 生成预约编号
     * 格式: RSV + 年月日 + 6位随机数
     * @return 预约编号
     */
    public String generateReservationNo() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String randomStr = String.format("%06d", RANDOM.nextInt(1000000));
        return "RSV" + dateStr + randomStr;
    }
}
