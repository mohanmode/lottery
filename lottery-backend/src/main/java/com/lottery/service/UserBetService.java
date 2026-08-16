package com.lottery.service;

import com.lottery.dto.PageResult;
import com.lottery.entity.UserBet;
import java.math.BigDecimal;

/**
 * 选号投注记录服务
 */
public interface UserBetService {
    Long save(UserBet bet);
    boolean update(UserBet bet);
    boolean delete(Long id);
    UserBet getById(Long id);
    PageResult<UserBet> list(int pageNum, int pageSize, String lotteryType, String betType, Boolean isMatched);

    /** 对某条投注记录执行开奖匹配（双色球） */
    UserBet matchSsqDraw(Long betId, String issue);

    /** 批量计算所有投注记录中的所有双色球记录 */
    int matchAllUnmatchedSsq();
}
