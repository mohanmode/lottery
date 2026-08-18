package com.lottery.controller;

import com.lottery.dto.*;
import com.lottery.entity.Kl8Draw;
import com.lottery.service.Kl8Service;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 快乐8开奖 / 统计分析 API
 */
@RestController
@RequestMapping("/kl8")
@RequiredArgsConstructor
public class Kl8Controller {

    private final Kl8Service kl8Service;

    // ===================== 开奖历史 =====================
    @GetMapping("/draws/latest")
    public ApiResult<Kl8DrawVO> latest() {
        Kl8Draw d = kl8Service.getLatestDraw();
        return ApiResult.ok(toVO(d));
    }

    @GetMapping("/draws/issue/{issue}")
    public ApiResult<Kl8DrawVO> byIssue(@PathVariable String issue) {
        return ApiResult.ok(toVO(kl8Service.getByIssue(issue)));
    }

    @GetMapping("/draws")
    public ApiResult<PageResult<Kl8DrawVO>> listDraws(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String issue,
            @RequestParam(required = false) String numbers,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        PageResult<Kl8Draw> page = kl8Service.listDraws(pageNum, pageSize, issue, numbers, startDate, endDate);
        List<Kl8DrawVO> voList = page.getList().stream().map(this::toVO).collect(Collectors.toList());
        return ApiResult.ok(PageResult.of(page.getTotal(), pageNum, pageSize, voList));
    }

    @GetMapping("/draws/count")
    public ApiResult<Long> countDraws() { return ApiResult.ok(kl8Service.countDraws()); }

    // ===================== 统计分析 =====================
    /** 综合统计分析：遗漏/频次/冷热/奇偶/大小/四区/和值/跨度/连号/重号/AC/质合 */
    @GetMapping("/statistics")
    public ApiResult<Map<String, Object>> statistics(@RequestParam(defaultValue = "100") int recent) {
        return ApiResult.ok(kl8Service.getStatistics(recent));
    }

    /** 冷热号频次分析 (1-80) */
    @GetMapping("/analysis/frequency")
    public ApiResult<Map<String, Object>> frequency(@RequestParam(defaultValue = "100") int recent) {
        int[] freq = kl8Service.analyzeNumberFrequency(recent);
        Map<String, Object> res = new LinkedHashMap<>();
        Map<String, Integer> freqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 80; i++) freqMap.put(String.format("%02d", i), freq[i]);
        res.put("numbers", freqMap);
        res.put("sampleDraws", recent);
        return ApiResult.ok(res);
    }

    // ===================== 工具 =====================
    private Kl8DrawVO toVO(Kl8Draw d) {
        if (d == null) return null;
        Kl8DrawVO vo = new Kl8DrawVO();
        vo.setId(d.getId());
        vo.setIssue(d.getIssue());
        vo.setNumbers(new String[]{
            d.getN1(), d.getN2(), d.getN3(), d.getN4(), d.getN5(),
            d.getN6(), d.getN7(), d.getN8(), d.getN9(), d.getN10(),
            d.getN11(), d.getN12(), d.getN13(), d.getN14(), d.getN15(),
            d.getN16(), d.getN17(), d.getN18(), d.getN19(), d.getN20()
        });
        vo.setDrawDate(d.getDrawDate() == null ? null : d.getDrawDate().toString());
        return vo;
    }
}
