package com.lottery.service;

import com.lottery.dto.PageResult;
import com.lottery.entity.Kl8Draw;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface Kl8Service {
    Kl8Draw getLatestDraw();
    Kl8Draw getByIssue(String issue);
    PageResult<Kl8Draw> listDraws(int pageNum, int pageSize, String keyword, LocalDate startDate, LocalDate endDate);
    int saveDraw(Kl8Draw draw);
    int batchInsertDraws(List<Kl8Draw> draws);
    long countDraws();
    int[] analyzeNumberFrequency(int recentDrawCount);
    Map<String, Object> getStatistics(int recentDrawCount);
}
