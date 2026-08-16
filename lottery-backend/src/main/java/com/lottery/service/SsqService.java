package com.lottery.service;

import com.lottery.dto.PageResult;
import com.lottery.dto.PrizeMatchResult;
import com.lottery.entity.SsqDraw;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 双色球核心服务接口
 */
public interface SsqService {

    // ========== 开奖历史 ==========
    SsqDraw getLatestDraw();
    SsqDraw getByIssue(String issue);
    PageResult<SsqDraw> listDraws(int pageNum, int pageSize, String keyword, LocalDate startDate, LocalDate endDate);
    int saveDraw(SsqDraw draw);
    int batchInsertDraws(List<SsqDraw> draws);
    long countDraws();

    // ========== 选号生成 ==========
    /** 生成随机一注 (6红+1蓝) */
    String[] generateRandomSingle();
    /** 生成随机多注 */
    List<String[]> generateRandomMulti(int count);

    /**
     * 胆拖选号：生成所有合法组合
     * @param danCodes 胆码 (0-5个红球)
     * @param tuoCodes 拖码 (6-danCodes.size ~ 20+ 个红球)
     * @param blueSet  蓝球集合 (1~16个)
     * @return list of [r1,r2,r3,r4,r5,r6,blue]
     */
    List<String[]> generateDantuo(Set<String> danCodes, Set<String> tuoCodes, Set<String> blueSet);

    /**
     * 复式选号：红球复式 / 蓝球复式 / 全复式
     * @param redSet  红球集合 (>=6 个)
     * @param blueSet 蓝球集合 (>=1 个)
     */
    List<String[]> generateCompound(Set<String> redSet, Set<String> blueSet);

    /** 计算组合数 (用于投注金额：2元/注) */
    long combinationCount(int n, int k);
    long countCompound(int redCount, int blueCount);
    long countDantuo(int danCount, int tuoCount, int blueCount);

    // ========== 中奖判定 ==========
    /**
     * 判奖 (单式): 给定一注号码和开奖号码，返回奖项
     */
    PrizeMatchResult matchPrize(String[] betReds, String betBlue, SsqDraw draw);
    PrizeMatchResult matchPrize(String[] betReds, String betBlue, String drawIssue);

    /**
     * 批量判奖 (复式/胆拖 展开的所有组合)
     */
    PrizeMatchResult matchPrizeBatch(List<String[]> allBets, SsqDraw draw);

    /** 冷/热号分析：返回最近 N 期号码出现频率 */
    int[] analyzeNumberFrequency(int recentDrawCount, boolean isRed);

    // ========== 综合统计 ==========
    /**
     * 综合统计分析：遗漏值、奇偶比、大小比、三区比、和值、跨度、连号、重号、AC值
     * @param recentDrawCount 统计最近 N 期
     * @return 统计结果 Map
     */
    java.util.Map<String, Object> getStatistics(int recentDrawCount);
}
