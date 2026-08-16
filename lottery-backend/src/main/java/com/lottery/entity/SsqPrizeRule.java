package com.lottery.entity;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 双色球中奖规则
 */
@Data
public class SsqPrizeRule {
    private Integer id;
    private String levelCode;   // 1, 2, 3, 4, 4B, 5, 5B, 6, 6B, 6C
    private String levelName;   // 一等奖 ... 六等奖
    private Integer redHitMin;
    private Integer redHitMax;
    private Boolean blueHit;
    private Boolean blueMatchAny;
    private BigDecimal prizeAmount;
    private String prizeDesc;
    private Integer sortNo;
    private Boolean isActive;
}
