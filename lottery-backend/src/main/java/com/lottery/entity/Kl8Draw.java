package com.lottery.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 快乐8开奖记录实体
 * 规则：从1-80中开出20个号码，每天开奖
 */
@Data
public class Kl8Draw {
    private Long id;
    private String issue;       // 期号
    private String n1;
    private String n2;
    private String n3;
    private String n4;
    private String n5;
    private String n6;
    private String n7;
    private String n8;
    private String n9;
    private String n10;
    private String n11;
    private String n12;
    private String n13;
    private String n14;
    private String n15;
    private String n16;
    private String n17;
    private String n18;
    private String n19;
    private String n20;
    private LocalDate drawDate; // 开奖日期
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public List<String> getNumberList() {
        return Arrays.asList(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10,
            n11, n12, n13, n14, n15, n16, n17, n18, n19, n20);
    }

    /** 返回排序后的 int 数组 (用于统计计算) */
    public int[] getNumberIntList() {
        int[] arr = {
            Integer.parseInt(n1), Integer.parseInt(n2), Integer.parseInt(n3), Integer.parseInt(n4),
            Integer.parseInt(n5), Integer.parseInt(n6), Integer.parseInt(n7), Integer.parseInt(n8),
            Integer.parseInt(n9), Integer.parseInt(n10), Integer.parseInt(n11), Integer.parseInt(n12),
            Integer.parseInt(n13), Integer.parseInt(n14), Integer.parseInt(n15), Integer.parseInt(n16),
            Integer.parseInt(n17), Integer.parseInt(n18), Integer.parseInt(n19), Integer.parseInt(n20)
        };
        Arrays.sort(arr);
        return arr;
    }

    public String getNumbersComma() {
        return String.join(",", n1, n2, n3, n4, n5, n6, n7, n8, n9, n10,
            n11, n12, n13, n14, n15, n16, n17, n18, n19, n20);
    }
}
