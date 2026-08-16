package com.lottery.controller;

import com.lottery.crawl.Kl8CrawlService;
import com.lottery.crawl.SsqCrawlService;
import com.lottery.dto.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统级接口：健康检查、版本信息、开奖同步
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    private final SsqCrawlService ssqCrawlService;
    private final Kl8CrawlService kl8CrawlService;

    public SystemController(SsqCrawlService ssqCrawlService, Kl8CrawlService kl8CrawlService) {
        this.ssqCrawlService = ssqCrawlService;
        this.kl8CrawlService = kl8CrawlService;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        m.put("service", "lottery-backend");
        m.put("version", "1.1.0");
        m.put("features", new String[]{"双色球SSQ", "快乐8KL8统计", "开奖自动同步"});
        return ApiResult.ok(m);
    }

    /** 手动触发双色球开奖同步（立即从多数据源抓取最新开奖结果） */
    @PostMapping("/sync-ssq")
    public ApiResult<SsqCrawlService.SyncResult> syncSsqManually() {
        SsqCrawlService.SyncResult result = ssqCrawlService.syncLatest();
        return result.isSuccess() ? ApiResult.ok(result) : ApiResult.fail(result.getMessage());
    }

    /** 手动触发快乐8开奖同步（立即从多数据源抓取最新开奖结果） */
    @PostMapping("/sync-kl8")
    public ApiResult<Kl8CrawlService.SyncResult> syncKl8Manually() {
        Kl8CrawlService.SyncResult result = kl8CrawlService.syncLatest();
        return result.isSuccess() ? ApiResult.ok(result) : ApiResult.fail(result.getMessage());
    }

    /** 手动触发快乐8历史数据批量同步（抓取过去10年开奖结果） */
    @PostMapping("/sync-kl8-history")
    public ApiResult<Kl8CrawlService.SyncResult> syncKl8History() {
        Kl8CrawlService.SyncResult result = kl8CrawlService.syncHistory();
        return result.isSuccess() ? ApiResult.ok(result) : ApiResult.fail(result.getMessage());
    }

    /** 查询最近一次开奖同步状态 (双色球 + 快乐8) */
    @GetMapping("/sync-status")
    public ApiResult<Map<String, Object>> syncStatus() {
        Map<String, Object> m = new LinkedHashMap<>();

        // 双色球同步状态
        SsqCrawlService.SyncStatus s = ssqCrawlService.getLastSync();
        Map<String, Object> ssqStatus = new LinkedHashMap<>();
        ssqStatus.put("running", s.isRunning());
        ssqStatus.put("lastAttemptTime", format(s.getLastAttemptTime()));
        ssqStatus.put("lastSuccessTime", format(s.getLastSuccessTime()));
        if (s.getLastResult() != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", s.getLastResult().isSuccess());
            r.put("message", s.getLastResult().getMessage());
            r.put("latestIssue", s.getLastResult().getLatestIssue());
            r.put("latestDrawDate", s.getLastResult().getLatestDrawDate());
            r.put("latestRed", s.getLastResult().getLatestRed());
            r.put("latestBlue", s.getLastResult().getLatestBlue());
            ssqStatus.put("lastResult", r);
        }
        ssqStatus.put("scheduleDesc", "启动40s后首次同步；每日 每小时10分和40分同步；开奖日(二/四/日)21~23点每5分钟加速同步");
        m.put("ssq", ssqStatus);

        // 快乐8同步状态
        Kl8CrawlService.SyncStatus k = kl8CrawlService.getLastSync();
        Map<String, Object> kl8Status = new LinkedHashMap<>();
        kl8Status.put("running", k.isRunning());
        kl8Status.put("lastAttemptTime", format(k.getLastAttemptTime()));
        kl8Status.put("lastSuccessTime", format(k.getLastSuccessTime()));
        if (k.getLastResult() != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", k.getLastResult().isSuccess());
            r.put("message", k.getLastResult().getMessage());
            r.put("latestIssue", k.getLastResult().getLatestIssue());
            r.put("latestDrawDate", k.getLastResult().getLatestDrawDate());
            r.put("latestNumbers", k.getLastResult().getLatestNumbers());
            kl8Status.put("lastResult", r);
        }
        kl8Status.put("scheduleDesc", "启动40s后首次同步；每日 每小时15分和45分同步；每天21~23点每5分钟加速同步");
        m.put("kl8", kl8Status);

        return ApiResult.ok(m);
    }

    private static String format(LocalDateTime dt) {
        return dt == null ? null : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
