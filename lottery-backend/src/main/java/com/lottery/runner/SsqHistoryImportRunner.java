package com.lottery.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.entity.SsqDraw;
import com.lottery.service.SsqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 启动时自动导入双色球历史开奖数据 (classpath:data/ssq_history.json)
 * - 若表里已经有数据，跳过（避免重复导入）
 * - DDL中的奖规则需要另外执行 schema.sql
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SsqHistoryImportRunner implements ApplicationRunner {

    private final SsqService ssqService;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${lottery.import.history-json-path:classpath:data/ssq_history.json}")
    private String historyJsonPath;

    @Value("${lottery.import.auto-import:true}")
    private boolean autoImport;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoImport) {
            log.info("[SSQ导入] auto-import=false，跳过自动导入");
            return;
        }
        long existing = ssqService.countDraws();
        if (existing > 500) {
            log.info("[SSQ导入] 表中已有 {} 条开奖记录，认为已初始化，跳过导入", existing);
            return;
        }
        log.info("[SSQ导入] 开始从 {} 读取历史开奖数据...", historyJsonPath);
        Resource resource = resourceLoader.getResource(historyJsonPath);
        if (!resource.exists()) {
            log.warn("[SSQ导入] 文件不存在: {}，跳过", historyJsonPath);
            return;
        }
        List<SsqDraw> draws = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            JsonNode arr = root.path("draws");
            if (arr.isMissingNode() || !arr.isArray()) {
                log.error("[SSQ导入] JSON中找不到draws数组，终止");
                return;
            }
            Iterator<JsonNode> it = arr.elements();
            while (it.hasNext()) {
                JsonNode n = it.next();
                SsqDraw d = new SsqDraw();
                d.setIssue(n.path("issue").asText());
                JsonNode reds = n.path("reds");
                String r1 = pad(reds.get(0).asText());
                String r2 = pad(reds.get(1).asText());
                String r3 = pad(reds.get(2).asText());
                String r4 = pad(reds.get(3).asText());
                String r5 = pad(reds.get(4).asText());
                String r6 = pad(reds.get(5).asText());
                d.setRed1(r1); d.setRed2(r2); d.setRed3(r3);
                d.setRed4(r4); d.setRed5(r5); d.setRed6(r6);
                d.setBlue(pad(n.path("blue").asText()));
                String dt = n.path("date").asText();
                if (dt != null && !dt.isEmpty()) {
                    d.setDrawDate(LocalDate.parse(dt, df));
                }
                draws.add(d);
            }
        }
        log.info("[SSQ导入] 解析到 {} 条记录，开始写入数据库...", draws.size());
        int inserted = ssqService.batchInsertDraws(draws);
        long total = ssqService.countDraws();
        log.info("[SSQ导入] 完成！本次新增 {} 条，数据库总记录 {} 条", inserted, total);
    }

    private String pad(String s) {
        if (s == null) return "00";
        s = s.trim();
        if (s.length() == 1) return "0" + s;
        return s;
    }
}
