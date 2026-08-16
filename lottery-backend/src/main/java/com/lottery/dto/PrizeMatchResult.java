package com.lottery.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 中奖判定结果
 */
@Data
public class PrizeMatchResult {
    private boolean won;              // 是否中奖
    private String levelName;         // 等级名称，如 "一等奖"
    private String levelCode;         // 等级编码
    private BigDecimal prizeAmount;   // 固定奖金或预估奖金
    private String prizeDesc;         // 奖金描述
    private String msg;               // 额外提示 (如开奖期号不存在)
    private int redHit;               // 命中红球数
    private boolean blueHit;          // 是否命中蓝球
    /** 复式/胆拖 时返回每注明细 */
    private List<BetResult> details;
    private int totalBets;            // 总注数
    private int wonBets;              // 中奖注数
    private BigDecimal totalPrize;    // 累计奖金

    @Data
    public static class BetResult {
        private List<String> reds;
        private String blue;
        private int redHit;
        private boolean blueHit;
        private String levelName;
        private BigDecimal prizeAmount;
    }
}
