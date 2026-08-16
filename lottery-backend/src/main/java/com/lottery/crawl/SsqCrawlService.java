package com.lottery.crawl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.entity.SsqDraw;
import com.lottery.mapper.SsqDrawMapper;
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
 * 双色球开奖结果抓取服务
 * 数据源：按顺序尝试多个公开可用接口，任一成功即返回
 */
@Service
@Slf4j
public class SsqCrawlService {

    private final SsqDrawMapper ssqDrawMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 最近一次同步结果：用于 /system/sync-status 接口展示 */
    private final SyncStatus lastSync = new SyncStatus();

    public SsqCrawlService(SsqDrawMapper ssqDrawMapper) {
        this.ssqDrawMapper = ssqDrawMapper;
    }

    public SyncStatus getLastSync() {
        return lastSync;
    }

    // ============== 公开接口 ==============

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

        // 数据源1: 开彩网 opencai.net (免费JSON接口, 最新50期)
        try {
            String text = httpGet("https://www.opencai.net/api/history/data?code=ssq&rows=5", 10);
            items = parseOpenCai(text);
            if (!items.isEmpty()) {
                log.info("[SSQ Crawler] 开彩网解析成功: {} 条", items.size());
            }
        } catch (Exception e) {
            log.warn("[SSQ Crawler] 开彩网抓取失败: {}", e.getMessage());
        }

        // 数据源2: 500.com (最新10期 HTML)
        if (items.isEmpty()) {
            try {
                String text = httpGet("https://datachart.500.com/ssq/history/newinc/history.php?start=0&end=99999", 12);
                items = parse500Html(text, 5);
                if (!items.isEmpty()) {
                    log.info("[SSQ Crawler] 500.com 解析成功: {} 条", items.size());
                }
            } catch (Exception e) {
                log.warn("[SSQ Crawler] 500.com 抓取失败: {}", e.getMessage());
            }
        }

        // 数据源3: kaijiang API JSON
        if (items.isEmpty()) {
            try {
                String text = httpGet("https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd=&pageNo=1&pageSize=10&week=&systemType=PC", 10);
                items = parseCwl(text, 5);
                if (!items.isEmpty()) {
                    log.info("[SSQ Crawler] 中彩网解析成功: {} 条", items.size());
                }
            } catch (Exception e) {
                log.warn("[SSQ Crawler] 中彩网抓取失败: {}", e.getMessage());
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
                // 归一化：原始数据里可能 2026093(7位) 或 26093(5位) 或 2026093(7位)，统一存后5位
                it.issue = normIssue;
                if (ssqDrawMapper.countByIssue(normIssue) > 0) {
                    skipped++;
                    continue;
                }
                SsqDraw d = new SsqDraw();
                d.setIssue(normIssue);
                String[] balls = padRed(it.reds);
                d.setRed1(balls[0]); d.setRed2(balls[1]); d.setRed3(balls[2]);
                d.setRed4(balls[3]); d.setRed5(balls[4]); d.setRed6(balls[5]);
                d.setBlue(pad(it.blue));
                LocalDate drawDate = parseDate(it.drawDate);
                d.setDrawDate(drawDate != null ? drawDate : LocalDate.now());
                d.setCreatedAt(LocalDateTime.now());
                d.setUpdatedAt(LocalDateTime.now());
                ssqDrawMapper.insert(d);
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
                    result.setLatestRed(String.join(",", padRed(latest.reds)));
                    result.setLatestBlue(pad(latest.blue));
                }
            }
        } catch (Exception e) {
            log.error("[SSQ Crawler] 入库失败", e);
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
                String[] parts = open.split("\\+");
                if (parts.length != 2) parts = open.split("\\|");
                if (parts.length != 2) {
                    String[] all = open.split("[,\\s]+");
                    if (all.length >= 7) {
                        List<String> reds = new ArrayList<>();
                        for (int i = 0; i < 6; i++) reds.add(all[i]);
                        it.reds = reds;
                        it.blue = all[6];
                    } else continue;
                } else {
                    it.reds = splitByComma(parts[0]);
                    it.blue = parts[1].trim();
                }
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
            // 找到 <tr class="t_tr1"> 或 <tr> 带 issue（格式：26093 等5位数字）
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
                if (cells.size() < 9) continue;
                String issue = cells.get(0);
                if (!issue.matches("\\d{5,7}")) continue;
                CandidateItem it = new CandidateItem();
                it.issue = issue;
                // 格式: cells(1..6)=红球, cell(7)=蓝球, cell(最后1~3个)=日期
                List<String> reds = new ArrayList<>();
                for (int i = 1; i <= 6; i++) if (cells.get(i).matches("\\d{1,2}")) reds.add(cells.get(i));
                if (reds.size() != 6) continue;
                it.reds = reds;
                String blue = cells.get(7);
                if (!blue.matches("\\d{1,2}")) blue = cells.get(8);
                if (!blue.matches("\\d{1,2}")) continue;
                it.blue = blue;
                // 找日期
                for (int i = cells.size() - 1; i >= 8; i--) {
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
                it.reds = splitByComma(textOr(n.get("red"), n.get("red")));
                it.blue = textOr(n.get("blue"), n.get("blue"));
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

    private static String[] padRed(List<String> reds) {
        String[] arr = new String[]{"00", "00", "00", "00", "00", "00"};
        if (reds == null) return arr;
        for (int i = 0; i < Math.min(6, reds.size()); i++) arr[i] = pad(reds.get(i));
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

    /** 期号归一化：统一存 5 位年份+期号（截掉"20"等世纪前缀，如 2026093 → 26093） */
    private static String normalizeIssue(String issue) {
        if (issue == null) return "00000";
        issue = issue.trim();
        if (issue.length() > 5) issue = issue.substring(issue.length() - 5);
        while (issue.length() < 5) issue = "0" + issue;
        return issue;
    }

    // ============== 内部类 ==============

    private static class CandidateItem {
        String issue;
        List<String> reds;
        String blue;
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
        private String latestRed;
        private String latestBlue;
    }
}
