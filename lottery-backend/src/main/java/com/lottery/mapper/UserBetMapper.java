package com.lottery.mapper;

import com.lottery.entity.UserBet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserBetMapper {

    int insert(UserBet bet);

    int updateById(UserBet bet);

    int deleteById(@Param("id") Long id);

    UserBet selectById(@Param("id") Long id);

    List<UserBet> selectList(@Param("lotteryType") String lotteryType,
                             @Param("betType") String betType,
                             @Param("isMatched") Boolean isMatched,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);

    long selectCount(@Param("lotteryType") String lotteryType,
                     @Param("betType") String betType,
                     @Param("isMatched") Boolean isMatched);
}
