package com.lottery.crawl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时同步最新开奖结果
 * 策略：
 *   - 启动后延迟 40 秒 执行一次 (给数据导入初始化留出时间)
 *   - 每 30 分钟执行一次 (cron)
 *   - 开奖日 (周二/周四/周日) 21:30~23:30 额外每 5 分钟执行一次
 */
@Component
@Slf4j
public class SsqSyncScheduler {

    private final SsqCrawlService crawlService;

    public SsqSyncScheduler(SsqCrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @PostConstruct
    public void onStart() {
        Thread thread = new Thread(() -> {
            try {
                // 等待 JSON 历史数据导入 Runner 完成
                Thread.sleep(40_000L);
                log.info("[SSQ Sync] 启动后首次同步开始...");
                SsqCrawlService.SyncResult r = crawlService.syncLatest();
                log.info("[SSQ Sync] 首次同步完成: {} {}", r.isSuccess() ? "OK" : "FAIL", r.getMessage());
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("[SSQ Sync] 首次同步异常: {}", e.getMessage());
            }
        }, "ssq-sync-on-start");
        thread.setDaemon(true);
        thread.start();
    }

    /** 全局每 30 分钟同步一次 */
    @Scheduled(cron = "0 10,40 * * * *")
    public void syncEvery30min() {
        log.info("[SSQ Sync] 定时30分钟同步开始");
        try {
            SsqCrawlService.SyncResult r = crawlService.syncLatest();
            log.info("[SSQ Sync] 定时同步完成: {} {}", r.isSuccess() ? "OK" : "FAIL", r.getMessage());
        } catch (Exception e) {
            log.warn("[SSQ Sync] 定时同步异常: {}", e.getMessage());
        }
    }

    /** 开奖日 (周二/周四/周日) 21:30~23:30 每 5 分钟加速同步一次 */
    @Scheduled(cron = "0 */5 21-23 * * TUE,THU,SUN")
    public void syncOnDrawDayEvening() {
        log.info("[SSQ Sync] 开奖日夜间加速同步开始");
        try {
            SsqCrawlService.SyncResult r = crawlService.syncLatest();
            log.info("[SSQ Sync] 加速同步完成: {} {}", r.isSuccess() ? "OK" : "FAIL", r.getMessage());
        } catch (Exception e) {
            log.warn("[SSQ Sync] 加速同步异常: {}", e.getMessage());
        }
    }
}
