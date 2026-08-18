package com.lottery.mapper;

import com.lottery.entity.SsqDraw;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SsqDrawMapper {

    int insert(SsqDraw draw);

    int insertBatch(@Param("list") List<SsqDraw> list);

    int updateById(SsqDraw draw);

    int deleteById(@Param("id") Long id);

    SsqDraw selectById(@Param("id") Long id);

    SsqDraw selectByIssue(@Param("issue") String issue);

    List<SsqDraw> selectList(@Param("issue") String issue,
                             @Param("redList") List<String> redList,
                             @Param("blue") String blue,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);

    long selectCount(@Param("issue") String issue,
                     @Param("redList") List<String> redList,
                     @Param("blue") String blue,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

    SsqDraw selectLatest();

    /**
     * 查询指定日期之后的最近一期开奖（用于匹配开奖）
     */
    SsqDraw selectFirstAfterDate(@Param("date") LocalDate date);

    int countByIssue(@Param("issue") String issue);
}
