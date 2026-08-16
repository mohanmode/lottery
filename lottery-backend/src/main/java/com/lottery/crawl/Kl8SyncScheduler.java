package com.lottery.crawl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 快乐8定时同步最新开奖结果
 * 策略：
 *   - 启动后延迟 40 秒 执行一次 (给数据导入初始化留出时间)
 *   - 每 30 分钟执行一次 (cron) - 与SSQ错开5分钟
 *   - 快乐8每天开奖，每天21:00-23:00每5分钟加速同步
 */
@Component
@Slf4j
public class Kl8SyncScheduler {

    private final Kl8CrawlService crawlService;

    public Kl8SyncScheduler(Kl8CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @PostConstruct
    public void onStart() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(40_000L);
                log.info("[KL8 Sync] 启动后首次同步开始...");
                Kl8CrawlService.SyncResult r = crawlService.syncLatest();
                log.info("[KL8 Sync] 首次同步完成: {} {}", r.isSuccess() ? "OK" : "FAIL", r.getMessage());
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("[KL8 Sync] 首次同步异常: {}", e.getMessage());
            }
        }, "kl8-sync-on-start");
        thread.setDaemon(true);
        thread.start();
    }

    /** 全局每 30 分钟同步一次 (与SSQ错开5分钟) */
    @Scheduled(cron = "0 15,45 * * * *")
    public void syncEvery30min() {
        log.info("[KL8 Sync] 定时30分钟同步开始");
        try {
            Kl8CrawlService.SyncResult r = crawlService.syncLatest();
            log.info("[KL8 Sync] 定时同步完成: {} {}", r.isSuccess() ? "OK" : "FAIL", r.getMessage());
        } catch (Exception e) {
            log.warn("[KL8 Sync] 定时同步异常: {}", e.getMessage());
        }
    }

    /** 快乐8每天开奖，每天21:00-23:00每5分钟加速同步一次 */
    @Scheduled(cron = "0 */5 21-22 * * *")
    public void syncOnDrawDayEvening() {
        log.info("[KL8 Sync] 夜间加速同步开始");
        try {
            Kl8CrawlService.SyncResult r = crawlService.syncLatest();
            log.info("[KL8 Sync] 加速同步完成: {} {}", r.isSuccess() ? "OK" : "FAIL", r.getMessage());
        } catch (Exception e) {
            log.warn("[KL8 Sync] 加速同步异常: {}", e.getMessage());
        }
    }
}
