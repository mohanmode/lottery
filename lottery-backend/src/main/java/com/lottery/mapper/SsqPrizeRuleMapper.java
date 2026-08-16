package com.lottery.mapper;

import com.lottery.entity.SsqPrizeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SsqPrizeRuleMapper {

    List<SsqPrizeRule> selectAllActive();

    /**
     * 根据命中红球数和蓝球命中情况匹配最高奖项 (按sort_no升序)
     */
    List<SsqPrizeRule> matchByHit(@Param("redHit") int redHit,
                                  @Param("blueHit") boolean blueHit);
}
