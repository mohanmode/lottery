package com.lottery.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户投注/选号记录实体
 */
@Data
public class UserBet {
    private Long id;
    private String lotteryType;   // SSQ / KL8
    private String betType;       // SINGLE / DANTUO / COMPOUND
    private String danNumbers;    // 胆码 逗号分隔
    private String tuoNumbers;    // 拖码 逗号分隔
    private String mainNumbers;   // 主号码 逗号分隔
    private String extraNumbers;  // 附加号码（如蓝球多个）
    private Integer combinationCnt; // 组合注数
    private Boolean isMatched;    // 是否已匹配开奖
    private String matchIssue;    // 匹配的开奖期号
    private String matchLevel;    // 中奖等级
    private BigDecimal matchPrize;// 中奖金额
    private LocalDateTime createdAt;
}
