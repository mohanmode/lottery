package com.lottery.controller;

import com.lottery.dto.*;
import com.lottery.entity.SsqDraw;
import com.lottery.service.SsqService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 双色球开奖 / 选号 / 中奖判定 API
 */
@RestController
@RequestMapping("/ssq")
@RequiredArgsConstructor
public class SsqController {

    private final SsqService ssqService;

    // ===================== 开奖历史 =====================
    @GetMapping("/draws/latest")
    public ApiResult<SsqDrawVO> latest() {
        SsqDraw d = ssqService.getLatestDraw();
        return ApiResult.ok(toVO(d));
    }

    @GetMapping("/draws/issue/{issue}")
    public ApiResult<SsqDrawVO> byIssue(@PathVariable String issue) {
        return ApiResult.ok(toVO(ssqService.getByIssue(issue)));
    }

    @GetMapping("/draws")
    public ApiResult<PageResult<SsqDrawVO>> listDraws(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String issue,
            @RequestParam(required = false) String red,
            @RequestParam(required = false) String blue,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        PageResult<SsqDraw> page = ssqService.listDraws(pageNum, pageSize, issue, red, blue, startDate, endDate);
        List<SsqDrawVO> voList = page.getList().stream().map(this::toVO).collect(Collectors.toList());
        return ApiResult.ok(PageResult.of(page.getTotal(), pageNum, pageSize, voList));
    }

    @GetMapping("/draws/count")
    public ApiResult<Long> countDraws() { return ApiResult.ok(ssqService.countDraws()); }

    // ===================== 选号生成 =====================
    @GetMapping("/pick/random")
    public ApiResult<Map<String, Object>> randomSingle() {
        String[] s = ssqService.generateRandomSingle();
        return ApiResult.ok(buildPickResult(s));
    }

    @GetMapping("/pick/random/{count}")
    public ApiResult<List<Map<String, Object>>> randomMulti(@PathVariable int count) {
        List<String[]> list = ssqService.generateRandomMulti(count);
        return ApiResult.ok(list.stream().map(this::buildPickResult).collect(Collectors.toList()));
    }

    /** 复式选号：参数 reds=逗号分隔 (>=6), blues=逗号分隔 */
    @PostMapping("/pick/compound")
    public ApiResult<Map<String, Object>> compoundPick(@RequestBody Map<String, Object> body) {
        Set<String> reds = toStrSet(body.get("reds"));
        Set<String> blues = toStrSet(body.get("blues"));
        List<String[]> bets = ssqService.generateCompound(reds, blues);
        long cnt = ssqService.countCompound(reds.size(), blues.size());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("combinationCount", cnt);
        res.put("cost", cnt * 2);
        res.put("samples", bets.stream().limit(500).map(this::buildPickResult).collect(Collectors.toList()));
        res.put("previewOnly", bets.size() > 500);
        res.put("reds", reds);
        res.put("blues", blues);
        return ApiResult.ok(res);
    }

    /** 胆拖选号：dans=胆码(0-5), tuos=拖码, blues=蓝球 */
    @PostMapping("/pick/dantuo")
    public ApiResult<Map<String, Object>> dantuoPick(@RequestBody Map<String, Object> body) {
        Set<String> dans = toStrSet(body.get("dans"));
        Set<String> tuos = toStrSet(body.get("tuos"));
        Set<String> blues = toStrSet(body.get("blues"));
        List<String[]> bets = ssqService.generateDantuo(dans, tuos, blues);
        long cnt = ssqService.countDantuo(dans.size(), tuos.size(), blues.size());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("combinationCount", cnt);
        res.put("cost", cnt * 2);
        res.put("samples", bets.stream().limit(500).map(this::buildPickResult).collect(Collectors.toList()));
        res.put("previewOnly", bets.size() > 500);
        res.put("dans", dans);
        res.put("tuos", tuos);
        res.put("blues", blues);
        return ApiResult.ok(res);
    }

    // ===================== 中奖判定 =====================
    /** 单式判奖：保持接口一致性，返回带 details 数组的统一结构 */
    @PostMapping("/match/single")
    public ApiResult<PrizeMatchResult> matchSingle(@RequestBody Map<String, Object> body,
                                                    @RequestParam(required = false) String issue) {
        Set<String> reds = toStrSet(body.get("reds"));
        String blue = firstOf(body.get("blue"));
        String[] redArr = reds.stream().sorted().toArray(String[]::new);
        SsqDraw draw = issue != null ? ssqService.getByIssue(issue) : ssqService.getLatestDraw();
        PrizeMatchResult r = ssqService.matchPrize(redArr, blue, draw);
        // 为单式也补充 details 字段，前端统一用 details[0] 渲染
        PrizeMatchResult.BetResult br = new PrizeMatchResult.BetResult();
        br.setReds(Arrays.asList(redArr));
        br.setBlue(blue);
        br.setRedHit(r.getRedHit());
        br.setBlueHit(r.isBlueHit());
        br.setLevelName(r.getLevelName());
        br.setPrizeAmount(r.getPrizeAmount() == null ? BigDecimal.ZERO : r.getPrizeAmount());
        r.setDetails(Collections.singletonList(br));
        r.setTotalBets(1);
        r.setWonBets(r.isWon() ? 1 : 0);
        r.setTotalPrize(r.getPrizeAmount() == null ? BigDecimal.ZERO : r.getPrizeAmount());
        return ApiResult.ok(r);
    }

    /** 复式判奖（先展开，再批量判） */
    @PostMapping("/match/compound")
    public ApiResult<PrizeMatchResult> matchCompound(@RequestBody Map<String, Object> body,
                                                     @RequestParam(required = false) String issue) {
        Set<String> reds = toStrSet(body.get("reds"));
        Set<String> blues = toStrSet(body.get("blues"));
        List<String[]> bets = ssqService.generateCompound(reds, blues);
        SsqDraw draw = issue != null ? ssqService.getByIssue(issue) : ssqService.getLatestDraw();
        return ApiResult.ok(ssqService.matchPrizeBatch(bets, draw));
    }

    /** 胆拖判奖 */
    @PostMapping("/match/dantuo")
    public ApiResult<PrizeMatchResult> matchDantuo(@RequestBody Map<String, Object> body,
                                                   @RequestParam(required = false) String issue) {
        Set<String> dans = toStrSet(body.get("dans"));
        Set<String> tuos = toStrSet(body.get("tuos"));
        Set<String> blues = toStrSet(body.get("blues"));
        List<String[]> bets = ssqService.generateDantuo(dans, tuos, blues);
        SsqDraw draw = issue != null ? ssqService.getByIssue(issue) : ssqService.getLatestDraw();
        return ApiResult.ok(ssqService.matchPrizeBatch(bets, draw));
    }

    /** 冷热号分析 */
    @GetMapping("/analysis/frequency")
    public ApiResult<Map<String, Object>> frequency(@RequestParam(defaultValue = "100") int recent) {
        int[] redFreq = ssqService.analyzeNumberFrequency(recent, true);
        int[] blueFreq = ssqService.analyzeNumberFrequency(recent, false);
        Map<String, Object> res = new LinkedHashMap<>();
        Map<String, Integer> redMap = new LinkedHashMap<>();
        for (int i = 1; i <= 33; i++) redMap.put(String.format("%02d", i), redFreq[i]);
        Map<String, Integer> blueMap = new LinkedHashMap<>();
        for (int i = 1; i <= 16; i++) blueMap.put(String.format("%02d", i), blueFreq[i]);
        res.put("red", redMap);
        res.put("blue", blueMap);
        res.put("sampleDraws", recent);
        return ApiResult.ok(res);
    }

    /** 综合统计分析：遗漏/奇偶/大小/三区/和值/跨度/连号/重号/AC/质合/蓝球 */
    @GetMapping("/statistics")
    public ApiResult<Map<String, Object>> statistics(@RequestParam(defaultValue = "100") int recent) {
        return ApiResult.ok(ssqService.getStatistics(recent));
    }

    // ===================== 工具 =====================
    private SsqDrawVO toVO(SsqDraw d) {
        if (d == null) return null;
        SsqDrawVO vo = new SsqDrawVO();
        vo.setId(d.getId());
        vo.setIssue(d.getIssue());
        vo.setReds(new String[]{d.getRed1(), d.getRed2(), d.getRed3(), d.getRed4(), d.getRed5(), d.getRed6()});
        vo.setBlue(d.getBlue());
        vo.setDrawDate(d.getDrawDate() == null ? null : d.getDrawDate().toString());
        return vo;
    }

    private Map<String, Object> buildPickResult(String[] s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reds", Arrays.copyOfRange(s, 0, 6));
        m.put("blue", s[6]);
        return m;
    }

    @SuppressWarnings("unchecked")
    private Set<String> toStrSet(Object o) {
        Set<String> set = new LinkedHashSet<>();
        if (o == null) return set;
        if (o instanceof Collection) {
            for (Object e : (Collection<Object>) o) {
                String s = normalize(e);
                if (s != null) set.add(s);
            }
        } else if (o instanceof String) {
            for (String p : ((String) o).split("[,，;；\\s]+")) {
                String s = normalize(p);
                if (s != null) set.add(s);
            }
        }
        return set;
    }

    private String firstOf(Object o) {
        if (o == null) return null;
        if (o instanceof Collection<?> c && !c.isEmpty()) return normalize(c.iterator().next());
        String s = o.toString().split("[,，;；\\s]+")[0];
        return normalize(s);
    }

    private String normalize(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        if (s.length() == 1 && Character.isDigit(s.charAt(0))) s = "0" + s;
        return s;
    }
}
