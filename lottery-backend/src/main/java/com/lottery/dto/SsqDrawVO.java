package com.lottery.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 双色球开奖记录 VO (用于前端展示)
 */
@Data
public class SsqDrawVO {
    private Long id;
    private String issue;
    private String[] reds;   // 6个红球
    private String blue;
    private String drawDate; // yyyy-MM-dd
}
