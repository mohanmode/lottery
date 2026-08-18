package com.lottery.mapper;

import com.lottery.entity.Kl8Draw;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface Kl8DrawMapper {

    int insert(Kl8Draw draw);

    int insertBatch(@Param("list") List<Kl8Draw> list);

    int updateById(Kl8Draw draw);

    int deleteById(@Param("id") Long id);

    Kl8Draw selectById(@Param("id") Long id);

    Kl8Draw selectByIssue(@Param("issue") String issue);

    List<Kl8Draw> selectList(@Param("issue") String issue,
                             @Param("numberList") List<String> numberList,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);

    long selectCount(@Param("issue") String issue,
                     @Param("numberList") List<String> numberList,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

    Kl8Draw selectLatest();

    int countByIssue(@Param("issue") String issue);
}
