package com.lottery.crawl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.entity.Kl8Draw;
import com.lottery.mapper.Kl8DrawMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 快乐8开奖结果抓取服务
 * 数据源：按顺序尝试多个公开可用接口，任一成功即返回
 * 快乐8规则：从1-80中开出20个号码，每天开奖
 */
@Service
@Slf4j
public class Kl8CrawlService {

    private final Kl8DrawMapper kl8DrawMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 最近一次同步结果：用于 /system/sync-status 接口展示 */
    private final SyncStatus lastSync = new SyncStatus();

    public Kl8CrawlService(Kl8DrawMapper kl8DrawMapper) {
        this.kl8DrawMapper = kl8DrawMapper;
    }

    public SyncStatus getLastSync() {
        return lastSync;
    }

    // ============== 公开接口 ==============

    /**
     * 批量抓取历史开奖数据（过去10年）
     * 从多个数据源分批抓取，尽量获取尽可能多的历史数据
     * @return 同步结果信息
     */
    public synchronized SyncResult syncHistory() {
        lastSync.setRunning(true);
        lastSync.setLastAttemptTime(LocalDateTime.now());
        SyncResult result = new SyncResult();
        result.setSuccess(false);
        result.setMessage("未找到可用数据源");

        List<CandidateItem> allItems = new ArrayList<>();

        // 数据源1: 500.com 全量历史数据 (start=0&end=99999 可获取全部历史)
        try {
            String text = httpGet("https://datachart.500.com/kl8/history/newinc/history.php?start=0&end=99999", 20);
            List<CandidateItem> items500 = parse500Html(text, 5000);
            if (!items500.isEmpty()) {
                log.info("[KL8 History] 500.com 解析成功: {} 条", items500.size());
                allItems.addAll(items500);
            }
        } catch (Exception e) {
            log.warn("[KL8 History] 500.com 抓取失败: {}", e.getMessage());
        }

        // 数据源2: 中彩网 cwl.gov.cn 分页抓取历史数据
        if (allItems.size() < 1000) {
            for (int pageNo = 1; pageNo <= 20; pageNo++) {
                try {
                    String text = httpGet(
                        "https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=kl8"
                        + "&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd="
                        + "&pageNo=" + pageNo + "&pageSize=500&week=&systemType=PC", 15);
                    List<CandidateItem> itemsCwl = parseCwl(text, 500);
                    if (itemsCwl.isEmpty()) {
                        log.info("[KL8 History] 中彩网第 {} 页无数据，停止分页", pageNo);
                        break;
                    }
                    allItems.addAll(itemsCwl);
                    log.info("[KL8 History] 中彩网第 {} 页解析 {} 条，累计 {} 条", pageNo, itemsCwl.size(), allItems.size());
                    if (itemsCwl.size() < 500) break; // 不足一页说明已到末尾
                    Thread.sleep(500); // 避免请求过快
                } catch (Exception e) {
                    log.warn("[KL8 History] 中彩网第 {} 页抓取失败: {}", pageNo, e.getMessage());
                    break;
                }
            }
        }

        // 数据源3: opencai.net 大批量历史
        if (allItems.size() < 1000) {
            try {
                String text = httpGet("https://www.opencai.net/api/history/data?code=kl8&rows=5000", 15);
                List<CandidateItem> itemsOpen = parseOpenCai(text);
                if (!itemsOpen.isEmpty()) {
                    log.info("[KL8 History] 开彩网解析成功: {} 条", itemsOpen.size());
                    allItems.addAll(itemsOpen);
                }
            } catch (Exception e) {
                log.warn("[KL8 History] 开彩网抓取失败: {}", e.getMessage());
            }
        }

        // 按期号去重
        java.util.Map<String, CandidateItem> dedupMap = new java.util.LinkedHashMap<>();
        for (CandidateItem it : allItems) {
            String norm = normalizeIssue(it.issue);
            it.issue = norm;
            if (!dedupMap.containsKey(norm)) dedupMap.put(norm, it);
        }
        List<CandidateItem> items = new ArrayList<>(dedupMap.values());
        log.info("[KL8 History] 去重后共 {} 条", items.size());

        // 入库 (分批)
        int inserted = 0;
        int skipped = 0;
        CandidateItem latest = null;
        try {
            items.sort((a, b) -> Long.compare(Long.parseLong(b.issue), Long.parseLong(a.issue)));
            List<Kl8Draw> batch = new ArrayList<>();
            for (CandidateItem it : items) {
                if (kl8DrawMapper.countByIssue(it.issue) > 0) {
                    skipped++;
                    continue;
                }
                Kl8Draw d = new Kl8Draw();
                d.setIssue(it.issue);
                String[] balls = padNumbers(it.numbers);
                d.setN1(balls[0]); d.setN2(balls[1]); d.setN3(balls[2]); d.setN4(balls[3]);
                d.setN5(balls[4]); d.setN6(balls[5]); d.setN7(balls[6]); d.setN8(balls[7]);
                d.setN9(balls[8]); d.setN10(balls[9]); d.setN11(balls[10]); d.setN12(balls[11]);
                d.setN13(balls[12]); d.setN14(balls[13]); d.setN15(balls[14]); d.setN16(balls[15]);
                d.setN17(balls[16]); d.setN18(balls[17]); d.setN19(balls[18]); d.setN20(balls[19]);
                LocalDate drawDate = parseDate(it.drawDate);
                d.setDrawDate(drawDate != null ? drawDate : LocalDate.now());
                d.setCreatedAt(LocalDateTime.now());
                d.setUpdatedAt(LocalDateTime.now());
                batch.add(d);
                if (latest == null) latest = it;
                if (batch.size() >= 200) {
                    kl8DrawMapper.insertBatch(batch);
                    inserted += batch.size();
                    batch.clear();
                    log.info("[KL8 History] 已入库 {} 条...", inserted);
                }
            }
            if (!batch.isEmpty()) {
                kl8DrawMapper.insertBatch(batch);
                inserted += batch.size();
            }

            if (items.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("所有数据源均未返回历史数据");
            } else {
                result.setSuccess(true);
                result.setMessage(
                    String.format("解析 %d 条(去重后)，新入库 %d 期，跳过已存在 %d 期", items.size(), inserted, skipped));
                if (latest != null) {
                    result.setLatestIssue(latest.issue);
                    result.setLatestDrawDate(
                        parseDate(latest.drawDate) != null ? parseDate(latest.drawDate).toString() : null);
                    result.setLatestNumbers(String.join(",", padNumbers(latest.numbers)));
                }
            }
        } catch (Exception e) {
            log.error("[KL8 History] 入库失败", e);
            result.setSuccess(false);
            result.setMessage("入库失败: " + e.getMessage() + " (已入库 " + inserted + " 条)");
        }

        lastSync.setRunning(false);
        lastSync.setLastResult(result);
        lastSync.setLastSuccessTime(result.isSuccess() ? LocalDateTime.now() : lastSync.getLastSuccessTime());
        return result;
    }

    /**
     * 抓取最新开奖数据并写入数据库（如果期号不存在）
     * @return 同步结果信息
     */
    public synchronized SyncResult syncLatest() {
        lastSync.setRunning(true);
        lastSync.setLastAttemptTime(LocalDateTime.now());
        SyncResult result = new SyncResult();
        result.setSuccess(false);
        result.setMessage("未找到可用数据源");

        List<CandidateItem> items = new ArrayList<>();

        // 数据源1: 开彩网 opencai.net (免费JSON接口)
        try {
            String text = httpGet("https://www.opencai.net/api/history/data?code=kl8&rows=5", 10);
            items = parseOpenCai(text);
            if (!items.isEmpty()) {
                log.info("[KL8 Crawler] 开彩网解析成功: {} 条", items.size());
            }
        } catch (Exception e) {
            log.warn("[KL8 Crawler] 开彩网抓取失败: {}", e.getMessage());
        }

        // 数据源2: 500.com (HTML 表格)
        if (items.isEmpty()) {
            try {
                String text = httpGet("https://datachart.500.com/kl8/history/newinc/history.php?start=0&end=99999", 12);
                items = parse500Html(text, 5);
                if (!items.isEmpty()) {
                    log.info("[KL8 Crawler] 500.com 解析成功: {} 条", items.size());
                }
            } catch (Exception e) {
                log.warn("[KL8 Crawler] 500.com 抓取失败: {}", e.getMessage());
            }
        }

        // 数据源3: 中彩网 cwl.gov.cn JSON
        if (items.isEmpty()) {
            try {
                String text = httpGet("https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=kl8&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd=&pageNo=1&pageSize=10&week=&systemType=PC", 10);
                items = parseCwl(text, 5);
                if (!items.isEmpty()) {
                    log.info("[KL8 Crawler] 中彩网解析成功: {} 条", items.size());
                }
            } catch (Exception e) {
                log.warn("[KL8 Crawler] 中彩网抓取失败: {}", e.getMessage());
            }
        }

        // 入库
        int inserted = 0;
        int skipped = 0;
        CandidateItem latest = (items.isEmpty()) ? null : items.get(0); // always record top even if all skipped
        try {
            // 按期号降序 (新到旧)
            items.sort((a, b) -> Long.compare(Long.parseLong(normalizeIssue(b.issue)), Long.parseLong(normalizeIssue(a.issue))));
            for (CandidateItem it : items) {
                String normIssue = normalizeIssue(it.issue);
                it.issue = normIssue;
                if (kl8DrawMapper.countByIssue(normIssue) > 0) {
                    skipped++;
                    continue;
                }
                Kl8Draw d = new Kl8Draw();
                d.setIssue(normIssue);
                String[] balls = padNumbers(it.numbers);
                d.setN1(balls[0]); d.setN2(balls[1]); d.setN3(balls[2]); d.setN4(balls[3]);
                d.setN5(balls[4]); d.setN6(balls[5]); d.setN7(balls[6]); d.setN8(balls[7]);
                d.setN9(balls[8]); d.setN10(balls[9]); d.setN11(balls[10]); d.setN12(balls[11]);
                d.setN13(balls[12]); d.setN14(balls[13]); d.setN15(balls[14]); d.setN16(balls[15]);
                d.setN17(balls[16]); d.setN18(balls[17]); d.setN19(balls[18]); d.setN20(balls[19]);
                LocalDate drawDate = parseDate(it.drawDate);
                d.setDrawDate(drawDate != null ? drawDate : LocalDate.now());
                d.setCreatedAt(LocalDateTime.now());
                d.setUpdatedAt(LocalDateTime.now());
                kl8DrawMapper.insert(d);
                inserted++;
                if (latest == null) latest = it;
            }
            if (items.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("所有数据源均未返回数据，请检查网络或稍后重试");
            } else {
                result.setSuccess(true);
                result.setMessage(
                    String.format("解析 %d 条，新入库 %d 期，跳过已存在 %d 期", items.size(), inserted, skipped));
                if (latest != null) {
                    result.setLatestIssue(latest.issue);
                    result.setLatestDrawDate(
                        parseDate(latest.drawDate) != null ? parseDate(latest.drawDate).toString() : null);
                    result.setLatestNumbers(String.join(",", padNumbers(latest.numbers)));
                }
            }
        } catch (Exception e) {
            log.error("[KL8 Crawler] 入库失败", e);
            result.setSuccess(false);
            result.setMessage("入库失败: " + e.getMessage());
        }

        lastSync.setRunning(false);
        lastSync.setLastResult(result);
        lastSync.setLastSuccessTime(result.isSuccess() ? LocalDateTime.now() : lastSync.getLastSuccessTime());
        return result;
    }

    // ============== 各数据源解析 ==============

    private List<CandidateItem> parseOpenCai(String text) {
        List<CandidateItem> out = new ArrayList<>();
        if (text == null) return out;
        try {
            JsonNode root = objectMapper.readTree(text);
            JsonNode arr = root.isArray() ? root : root.get("data");
            if (arr == null) return out;
            for (JsonNode n : arr) {
                CandidateItem it = new CandidateItem();
                it.issue = textOr(n.get("expect"), n.get("issue"), n.get("code"));
                if (it.issue == null) continue;
                String open = textOr(n.get("opencode"), n.get("result"));
                if (open == null) continue;
                // KL8的opencode是20个逗号分隔的号码
                List<String> numbers = splitByComma(open);
                if (numbers.size() < 20) continue;
                it.numbers = numbers.subList(0, 20);
                it.drawDate = textOr(n.get("opentime"), n.get("opentimestamp"), n.get("time"));
                out.add(it);
            }
        } catch (Exception e) {
            log.warn("opencai parse err: {}", e.getMessage());
        }
        return out;
    }

    private List<CandidateItem> parse500Html(String text, int limit) {
        List<CandidateItem> out = new ArrayList<>();
        if (text == null) return out;
        try {
            // 去除控制字符
            text = text.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", " ");
            // 找到 <tr> 行
            java.util.regex.Pattern trPattern = java.util.regex.Pattern.compile(
                "<tr[^>]*>([\\s\\S]*?)</tr>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher trMatcher = trPattern.matcher(text);
            java.util.regex.Pattern tdPattern = java.util.regex.Pattern.compile(
                "<t[dh][^>]*>([\\s\\S]*?)</t[dh]>", java.util.regex.Pattern.CASE_INSENSITIVE);
            DateTimeFormatter fmtA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter fmtB = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            while (trMatcher.find() && out.size() < limit) {
                String row = trMatcher.group(1);
                List<String> cells = new ArrayList<>();
                java.util.regex.Matcher m = tdPattern.matcher(row);
                while (m.find()) {
                    String cell = m.group(1)
                        .replaceAll("<[^>]+>", "")
                        .replace("&nbsp;", " ").trim();
                    cells.add(cell);
                }
                // 格式: cells(0)=期号, cells(1~20)=20个号码, 后面找日期
                if (cells.size() < 21) continue;
                String issue = cells.get(0);
                if (!issue.matches("\\d{5,9}")) continue;
                List<String> numbers = new ArrayList<>();
                for (int i = 1; i <= 20; i++) {
                    if (cells.get(i).matches("\\d{1,2}")) numbers.add(cells.get(i));
                }
                if (numbers.size() != 20) continue;
                CandidateItem it = new CandidateItem();
                it.issue = issue;
                it.numbers = numbers;
                // 找日期 (从后往前找)
                for (int i = cells.size() - 1; i >= 21; i--) {
                    String c = cells.get(i).replace("年", "-").replace("月", "-").replace("日", "");
                    try {
                        if (c.contains("-")) {
                            LocalDate.parse(c, fmtA);
                            it.drawDate = c;
                            break;
                        } else if (c.contains("/")) {
                            LocalDate d = LocalDate.parse(c, fmtB);
                            it.drawDate = d.toString();
                            break;
                        }
                    } catch (Exception ignore) {}
                }
                out.add(it);
            }
        } catch (Exception e) {
            log.warn("500.com parse err: {}", e.getMessage());
        }
        return out;
    }

    private List<CandidateItem> parseCwl(String text, int limit) {
        List<CandidateItem> out = new ArrayList<>();
        if (text == null) return out;
        try {
            JsonNode root = objectMapper.readTree(text);
            JsonNode arr = root.path("result");
            for (JsonNode n : arr) {
                if (out.size() >= limit) break;
                CandidateItem it = new CandidateItem();
                it.issue = textOr(n.get("code"), n.get("issue"));
                if (it.issue == null) continue;
                // KL8的red字段包含20个逗号分隔号码
                List<String> numbers = splitByComma(textOr(n.get("red"), n.get("red")));
                if (numbers.size() < 20) continue;
                it.numbers = numbers.subList(0, 20);
                it.drawDate = textOr(n.get("date"), n.get("drawDate"), n.get("publishingDate"));
                out.add(it);
            }
        } catch (Exception e) {
            log.warn("cwl parse err: {}", e.getMessage());
        }
        return out;
    }

    // ============== 工具 ==============

    private String httpGet(String url, int timeoutSec) throws Exception {
        URI uri = URI.create(url);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeoutSec * 1000);
        conn.setReadTimeout(timeoutSec * 1000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Safari/537.36");
        conn.setRequestProperty("Accept", "application/json,text/html,*/*;q=0.8");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static String textOr(JsonNode... nodes) {
        for (JsonNode n : nodes) {
            if (n == null || n.isNull()) continue;
            String s = n.asText();
            if (s != null && !s.isEmpty()) return s;
        }
        return null;
    }

    private static List<String> splitByComma(String s) {
        if (s == null) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        for (String p : s.split("[,\\s+|]+")) {
            String t = p.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    private static String pad(String s) {
        if (s == null) return "00";
        s = s.trim();
        if (s.length() == 1) return "0" + s;
        return s;
    }

    /** 将20个号码字符串补零到2位并返回String数组 */
    private static String[] padNumbers(List<String> numbers) {
        String[] arr = new String[20];
        java.util.Arrays.fill(arr, "00");
        if (numbers == null) return arr;
        for (int i = 0; i < Math.min(20, numbers.size()); i++) arr[i] = pad(numbers.get(i));
        return arr;
    }

    private static LocalDate parseDate(String s) {
        if (s == null) return null;
        s = s.trim().replace("年", "-").replace("月", "-").replace("日", "").replace("/", "-");
        if (s.length() >= 10) s = s.substring(0, 10);
        try { return LocalDate.parse(s); } catch (Exception ignore) {}
        // 尝试 2026-8-13 这类 1 位月日
        try {
            String[] parts = s.split("-");
            if (parts.length == 3) {
                return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
        } catch (Exception ignore) {}
        return null;
    }

    /** 期号归一化：KL8期号通常为7位(如2026233)，统一存7位。如果长度>7则截取后7位，如果<7则前面补0 */
    private static String normalizeIssue(String issue) {
        if (issue == null) return "0000000";
        issue = issue.trim();
        if (issue.length() > 7) issue = issue.substring(issue.length() - 7);
        while (issue.length() < 7) issue = "0" + issue;
        return issue;
    }

    // ============== 内部类 ==============

    private static class CandidateItem {
        String issue;
        List<String> numbers; // 20个号码
        String drawDate;
    }

    @Data
    public static class SyncStatus {
        private boolean running;
        private LocalDateTime lastAttemptTime;
        private LocalDateTime lastSuccessTime;
        private SyncResult lastResult;
    }

    @Data
    public static class SyncResult {
        private boolean success;
        private String message;
        private String latestIssue;
        private String latestDrawDate;
        private String latestNumbers;
    }
}
