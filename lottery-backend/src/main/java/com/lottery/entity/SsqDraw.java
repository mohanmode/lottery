package com.lottery.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 双色球开奖记录实体
 */
@Data
public class SsqDraw {
    private Long id;
    private String issue;       // 期号
    private String red1;
    private String red2;
    private String red3;
    private String red4;
    private String red5;
    private String red6;
    private String blue;
    private LocalDate drawDate; // 开奖日期
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public List<String> getRedList() {
        return Arrays.asList(red1, red2, red3, red4, red5, red6);
    }

    /** 返回排序后的 int 数组 (用于统计计算) */
    public int[] getRedIntList() {
        int[] arr = {
            Integer.parseInt(red1), Integer.parseInt(red2), Integer.parseInt(red3),
            Integer.parseInt(red4), Integer.parseInt(red5), Integer.parseInt(red6)
        };
        java.util.Arrays.sort(arr);
        return arr;
    }

    public String getRedsComma() {
        return String.join(",", red1, red2, red3, red4, red5, red6);
    }
}
