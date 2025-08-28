package com.jakdang.labs.api.lnuyasha.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 한국 시간대 변환을 위한 유틸리티 클래스
 */
public class TimeZoneUtil {
    
    private static final ZoneId KOREAN_ZONE = ZoneId.of("Asia/Seoul");
    
    /**
     * Instant를 한국 시간 LocalDateTime으로 변환
     * @param instant 변환할 Instant
     * @return 한국 시간 LocalDateTime, null인 경우 null 반환
     */
    public static LocalDateTime toKoreanTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, KOREAN_ZONE);
    }
    
    /**
     * LocalDateTime을 한국 시간으로 변환 (시스템 기본 시간대에서 한국 시간대로 변환)
     * @param localDateTime 변환할 LocalDateTime
     * @return 한국 시간 LocalDateTime, null인 경우 null 반환
     */
    public static LocalDateTime toKoreanTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        // 시스템 기본 시간대에서 한국 시간대로 변환
        return localDateTime.atZone(ZoneId.systemDefault())
                           .withZoneSameInstant(KOREAN_ZONE)
                           .toLocalDateTime();
    }
    
    /**
     * 현재 시간을 한국 시간으로 반환
     * @return 한국 시간 LocalDateTime
     */
    public static LocalDateTime nowKoreanTime() {
        return LocalDateTime.now(KOREAN_ZONE);
    }
    
    /**
     * 한국 시간대 ZoneId 반환
     * @return Asia/Seoul ZoneId
     */
    public static ZoneId getKoreanZone() {
        return KOREAN_ZONE;
    }
} 