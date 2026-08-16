package com.lottery.service.impl;

import com.lottery.dto.PageResult;
import com.lottery.dto.PrizeMatchResult;
import com.lottery.entity.SsqDraw;
import com.lottery.entity.SsqPrizeRule;
import com.lottery.mapper.SsqDrawMapper;
import com.lottery.mapper.SsqPrizeRuleMapper;
import com.lottery.service.SsqService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SsqServiceImpl implements SsqService {

    private final SsqDrawMapper drawMapper;
    private final SsqPrizeRuleMapper prizeRuleMapper;

    // ===================== 开奖历史 CRUD =====================
    @Override
    public SsqDraw getLatestDraw() { return drawMapper.selectLatest(); }

    @Override
    public SsqDraw getByIssue(String issue) { return drawMapper.selectByIssue(issue); }

    @Override
    public PageResult<SsqDraw> listDraws(int pageNum, int pageSize, String keyword, LocalDate startDate, LocalDate endDate) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        long total = drawMapper.selectCount(keyword, startDate, endDate);
        List<SsqDraw> list = drawMapper.selectList(keyword, startDate, endDate, offset, pageSize);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    @Override
    public int saveDraw(SsqDraw draw) {
        if (drawMapper.countByIssue(draw.getIssue()) > 0) return 0;
        return drawMapper.insert(draw);
    }

    @Override
    public int batchInsertDraws(List<SsqDraw> draws) {
        if (draws == null || draws.isEmpty()) return 0;
        // 过滤已存在的期号，避免唯一索引冲突
        List<SsqDraw> toInsert = draws.stream()
                .filter(d -> drawMapper.countByIssue(d.getIssue()) == 0)
                .collect(Collectors.toList());
        if (toInsert.isEmpty()) return 0;
        // 分批插入 (MyBatis foreach 过大可能报错)
        int total = 0;
        int batchSize = 200;
        for (int i = 0; i < toInsert.size(); i += batchSize) {
            int end = Math.min(i + batchSize, toInsert.size());
            total += drawMapper.insertBatch(toInsert.subList(i, end));
        }
        return total;
    }

    @Override
    public long countDraws() { return drawMapper.selectCount(null, null, null); }

    // ===================== 选号生成 =====================
    private static final String[] RED_POOL;
    private static final String[] BLUE_POOL;
    static {
        RED_POOL = IntStream.rangeClosed(1, 33).mapToObj(i -> String.format("%02d", i)).toArray(String[]::new);
        BLUE_POOL = IntStream.rangeClosed(1, 16).mapToObj(i -> String.format("%02d", i)).toArray(String[]::new);
    }

    @Override
    public String[] generateRandomSingle() {
        List<String> reds = new ArrayList<>(Arrays.asList(RED_POOL));
        Collections.shuffle(reds, new Random());
        String[] pick6 = reds.subList(0, 6).stream().sorted().toArray(String[]::new);
        String blue = BLUE_POOL[new Random().nextInt(16)];
        return new String[]{pick6[0], pick6[1], pick6[2], pick6[3], pick6[4], pick6[5], blue};
    }

    @Override
    public List<String[]> generateRandomMulti(int count) {
        count = Math.min(1000, Math.max(1, count));
        List<String[]> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) result.add(generateRandomSingle());
        return result;
    }

    @Override
    public List<String[]> generateDantuo(Set<String> danCodes, Set<String> tuoCodes, Set<String> blueSet) {
        if (danCodes == null) danCodes = Collections.emptySet();
        if (danCodes.size() > 5) throw new IllegalArgumentException("胆码不能超过5个");
        if (tuoCodes == null || tuoCodes.size() < (6 - danCodes.size()))
            throw new IllegalArgumentException("拖码数量不足 (至少需要 " + (6 - danCodes.size()) + " 个)");
        if (blueSet == null || blueSet.isEmpty()) throw new IllegalArgumentException("蓝球不能为空");

        List<String> danList = new ArrayList<>(danCodes);
        List<String> tuoList = new ArrayList<>(tuoCodes);
        int needFromTuo = 6 - danList.size();

        List<List<String>> tuoCombos = combinations(tuoList, needFromTuo);
        List<String> blues = new ArrayList<>(blueSet);

        List<String[]> result = new ArrayList<>(tuoCombos.size() * blues.size());
        for (List<String> tc : tuoCombos) {
            for (String b : blues) {
                String[] reds = new String[6];
                for (int i = 0; i < danList.size(); i++) reds[i] = danList.get(i);
                for (int i = 0; i < tc.size(); i++) reds[danList.size() + i] = tc.get(i);
                Arrays.sort(reds);
                result.add(new String[]{reds[0], reds[1], reds[2], reds[3], reds[4], reds[5], b});
            }
        }
        return result;
    }

    @Override
    public List<String[]> generateCompound(Set<String> redSet, Set<String> blueSet) {
        if (redSet == null || redSet.size() < 6) throw new IllegalArgumentException("红球复式至少需要6个");
        if (blueSet == null || blueSet.isEmpty()) throw new IllegalArgumentException("蓝球不能为空");
        List<String> redList = new ArrayList<>(redSet);
        List<String> blueList = new ArrayList<>(blueSet);
        List<List<String>> redCombos = combinations(redList, 6);
        List<String[]> result = new ArrayList<>(redCombos.size() * blueList.size());
        for (List<String> rc : redCombos) {
            String[] rs = rc.stream().sorted().toArray(String[]::new);
            for (String b : blueList) {
                result.add(new String[]{rs[0], rs[1], rs[2], rs[3], rs[4], rs[5], b});
            }
        }
        return result;
    }

    @Override
    public long combinationCount(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        k = Math.min(k, n - k);
        long res = 1;
        for (int i = 1; i <= k; i++) {
            res = res * (n - k + i) / i;
        }
        return res;
    }

    @Override
    public long countCompound(int redCount, int blueCount) {
        return combinationCount(redCount, 6) * Math.max(1, blueCount);
    }

    @Override
    public long countDantuo(int danCount, int tuoCount, int blueCount) {
        return combinationCount(tuoCount, 6 - danCount) * Math.max(1, blueCount);
    }

    // ===================== 中奖判定 =====================
    @Override
    public PrizeMatchResult matchPrize(String[] betReds, String betBlue, SsqDraw draw) {
        Set<String> drawRedSet = new HashSet<>(draw.getRedList());
        int redHit = 0;
        for (String r : betReds) if (drawRedSet.contains(r)) redHit++;
        boolean blueHit = draw.getBlue().equals(betBlue);
        PrizeMatchResult r = buildMatchResult(redHit, blueHit);
        r.setRedHit(redHit);
        r.setBlueHit(blueHit);
        return r;
    }

    @Override
    public PrizeMatchResult matchPrize(String[] betReds, String betBlue, String drawIssue) {
        SsqDraw draw = drawMapper.selectByIssue(drawIssue);
        if (draw == null) {
            PrizeMatchResult r = new PrizeMatchResult();
            r.setMsg("未找到期号: " + drawIssue);
            return r;
        }
        return matchPrize(betReds, betBlue, draw);
    }

    @Override
    public PrizeMatchResult matchPrizeBatch(List<String[]> allBets, SsqDraw draw) {
        if (draw == null || allBets == null || allBets.isEmpty()) return new PrizeMatchResult();
        Set<String> drawRedSet = new HashSet<>(draw.getRedList());
        String drawBlue = draw.getBlue();

        List<PrizeMatchResult.BetResult> details = new ArrayList<>(allBets.size());
        BigDecimal totalPrize = BigDecimal.ZERO;
        int wonBets = 0;
        PrizeMatchResult best = null;

        for (String[] bet : allBets) {
            String[] reds = Arrays.copyOfRange(bet, 0, 6);
            String blue = bet[6];
            int rh = 0;
            for (String r : reds) if (drawRedSet.contains(r)) rh++;
            boolean bh = drawBlue.equals(blue);
            PrizeMatchResult m = buildMatchResult(rh, bh);
            PrizeMatchResult.BetResult br = new PrizeMatchResult.BetResult();
            br.setReds(Arrays.asList(reds));
            br.setBlue(blue);
            br.setRedHit(rh);
            br.setBlueHit(bh);
            br.setLevelName(m.getLevelName());
            br.setPrizeAmount(m.getPrizeAmount());
            details.add(br);
            if (m.isWon()) {
                wonBets++;
                if (m.getPrizeAmount() != null) totalPrize = totalPrize.add(m.getPrizeAmount());
                if (best == null || compareLevel(m, best) < 0) best = m;
            }
        }
        PrizeMatchResult r = best != null ? best : buildMatchResult(0, false);
        r.setDetails(details);
        r.setTotalBets(allBets.size());
        r.setWonBets(wonBets);
        r.setTotalPrize(totalPrize);
        r.setRedHit(0);
        r.setBlueHit(false);
        return r;
    }

    @Override
    public int[] analyzeNumberFrequency(int recentDrawCount, boolean isRed) {
        int total = isRed ? 34 : 17;
        int[] freq = new int[total];
        PageResult<SsqDraw> page = listDraws(1, Math.max(1, Math.min(5000, recentDrawCount)), null, null, null);
        for (SsqDraw d : page.getList()) {
            if (isRed) {
                freq[Integer.parseInt(d.getRed1())]++;
                freq[Integer.parseInt(d.getRed2())]++;
                freq[Integer.parseInt(d.getRed3())]++;
                freq[Integer.parseInt(d.getRed4())]++;
                freq[Integer.parseInt(d.getRed5())]++;
                freq[Integer.parseInt(d.getRed6())]++;
            } else {
                freq[Integer.parseInt(d.getBlue())]++;
            }
        }
        return freq;
    }

    // ===================== 综合统计 =====================
    @Override
    public Map<String, Object> getStatistics(int recentDrawCount) {
        int limit = Math.max(1, Math.min(5000, recentDrawCount));
        PageResult<SsqDraw> page = listDraws(1, limit, null, null, null);
        List<SsqDraw> draws = page.getList();
        // 倒序 → 正序 (老→新)，方便遗漏值计算
        Collections.reverse(draws);

        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 1. 遗漏值 (每个号码距离上次出现多少期) ----
        int[] redOmit = new int[34];
        int[] blueOmit = new int[17];
        Arrays.fill(redOmit, -1);
        Arrays.fill(blueOmit, -1);
        for (int i = draws.size() - 1; i >= 0; i--) {
            SsqDraw d = draws.get(i);
            int[] reds = d.getRedIntList();
            for (int r : reds) if (redOmit[r] == -1) redOmit[r] = draws.size() - 1 - i;
            int b = Integer.parseInt(d.getBlue());
            if (blueOmit[b] == -1) blueOmit[b] = draws.size() - 1 - i;
        }
        for (int i = 1; i <= 33; i++) if (redOmit[i] == -1) redOmit[i] = draws.size();
        for (int i = 1; i <= 16; i++) if (blueOmit[i] == -1) blueOmit[i] = draws.size();
        Map<String, Integer> redOmitMap = new LinkedHashMap<>();
        for (int i = 1; i <= 33; i++) redOmitMap.put(String.format("%02d", i), redOmit[i]);
        Map<String, Integer> blueOmitMap = new LinkedHashMap<>();
        for (int i = 1; i <= 16; i++) blueOmitMap.put(String.format("%02d", i), blueOmit[i]);
        result.put("redOmit", redOmitMap);
        result.put("blueOmit", blueOmitMap);

        // ---- 2. 频次 + 冷热号 ----
        int[] redFreq = new int[34];
        int[] blueFreq = new int[17];
        for (SsqDraw d : draws) {
            for (int r : d.getRedIntList()) redFreq[r]++;
            blueFreq[Integer.parseInt(d.getBlue())]++;
        }
        Map<String, Integer> redFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 33; i++) redFreqMap.put(String.format("%02d", i), redFreq[i]);
        Map<String, Integer> blueFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 16; i++) blueFreqMap.put(String.format("%02d", i), blueFreq[i]);
        result.put("redFreq", redFreqMap);
        result.put("blueFreq", blueFreqMap);

        // 冷热号 Top 排名
        List<Map<String, Object>> redHot = new ArrayList<>();
        List<Map<String, Object>> redCold = new ArrayList<>();
        for (int i = 1; i <= 33; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", String.format("%02d", i));
            m.put("freq", redFreq[i]);
            m.put("omit", redOmit[i]);
            redHot.add(m);
            redCold.add(new LinkedHashMap<>(m));
        }
        redHot.sort((a, b) -> ((int) b.get("freq")) - ((int) a.get("freq")));
        redCold.sort((a, b) -> ((int) a.get("freq")) - ((int) b.get("freq")));
        result.put("redHotTop10", redHot.subList(0, Math.min(10, redHot.size())));
        result.put("redColdTop10", redCold.subList(0, Math.min(10, redCold.size())));

        List<Map<String, Object>> blueHot = new ArrayList<>();
        List<Map<String, Object>> blueCold = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", String.format("%02d", i));
            m.put("freq", blueFreq[i]);
            m.put("omit", blueOmit[i]);
            blueHot.add(m);
            blueCold.add(new LinkedHashMap<>(m));
        }
        blueHot.sort((a, b) -> ((int) b.get("freq")) - ((int) a.get("freq")));
        blueCold.sort((a, b) -> ((int) a.get("freq")) - ((int) b.get("freq")));
        result.put("blueHotTop5", blueHot.subList(0, Math.min(5, blueHot.size())));
        result.put("blueColdTop5", blueCold.subList(0, Math.min(5, blueCold.size())));

        // ---- 3. 奇偶比分布 ----
        Map<String, Integer> oddEvenDist = new LinkedHashMap<>();
        oddEvenDist.put("6:0", 0); oddEvenDist.put("5:1", 0); oddEvenDist.put("4:2", 0);
        oddEvenDist.put("3:3", 0); oddEvenDist.put("2:4", 0); oddEvenDist.put("1:5", 0); oddEvenDist.put("0:6", 0);
        for (SsqDraw d : draws) {
            int odd = 0;
            for (int r : d.getRedIntList()) if (r % 2 != 0) odd++;
            oddEvenDist.put(odd + ":" + (6 - odd), oddEvenDist.get(odd + ":" + (6 - odd)) + 1);
        }
        result.put("oddEvenDist", oddEvenDist);

        // ---- 4. 大小比分布 (1-16=小, 17-33=大) ----
        Map<String, Integer> bigSmallDist = new LinkedHashMap<>();
        bigSmallDist.put("6:0", 0); bigSmallDist.put("5:1", 0); bigSmallDist.put("4:2", 0);
        bigSmallDist.put("3:3", 0); bigSmallDist.put("2:4", 0); bigSmallDist.put("1:5", 0); bigSmallDist.put("0:6", 0);
        for (SsqDraw d : draws) {
            int big = 0;
            for (int r : d.getRedIntList()) if (r >= 17) big++;
            bigSmallDist.put(big + ":" + (6 - big), bigSmallDist.get(big + ":" + (6 - big)) + 1);
        }
        result.put("bigSmallDist", bigSmallDist);

        // ---- 5. 三区比分布 (1-11 / 12-22 / 23-33) ----
        Map<String, Integer> zoneDist = new LinkedHashMap<>();
        for (SsqDraw d : draws) {
            int z1 = 0, z2 = 0, z3 = 0;
            for (int r : d.getRedIntList()) {
                if (r <= 11) z1++; else if (r <= 22) z2++; else z3++;
            }
            String key = z1 + ":" + z2 + ":" + z3;
            zoneDist.put(key, zoneDist.getOrDefault(key, 0) + 1);
        }
        // 取出现次数 Top 10
        List<Map<String, Object>> zoneTop = new ArrayList<>();
        zoneDist.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(10)
            .forEach(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ratio", e.getKey());
                m.put("count", e.getValue());
                zoneTop.add(m);
            });
        result.put("zoneDistTop10", zoneTop);

        // ---- 6. 和值分布 ----
        int[] sumBuckets = new int[12]; // 21-30, 31-40, ..., 121-130, 131+
        int minSum = 999, maxSum = 0;
        double avgSum = 0;
        for (SsqDraw d : draws) {
            int sum = 0;
            for (int r : d.getRedIntList()) sum += r;
            avgSum += sum;
            minSum = Math.min(minSum, sum);
            maxSum = Math.max(maxSum, sum);
            int idx = (sum - 21) / 10;
            if (idx < 0) idx = 0;
            if (idx >= sumBuckets.length) idx = sumBuckets.length - 1;
            sumBuckets[idx]++;
        }
        avgSum = draws.isEmpty() ? 0 : avgSum / draws.size();
        Map<String, Object> sumInfo = new LinkedHashMap<>();
        sumInfo.put("min", minSum == 999 ? 0 : minSum);
        sumInfo.put("max", maxSum);
        sumInfo.put("avg", Math.round(avgSum * 10) / 10.0);
        List<Map<String, Object>> sumHist = new ArrayList<>();
        String[] sumLabels = {"21-30","31-40","41-50","51-60","61-70","71-80","81-90","91-100","101-110","111-120","121-130","131+"};
        for (int i = 0; i < sumBuckets.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("range", sumLabels[i]);
            m.put("count", sumBuckets[i]);
            sumHist.add(m);
        }
        sumInfo.put("histogram", sumHist);
        result.put("sumStats", sumInfo);

        // ---- 7. 跨度分布 ----
        int[] spanBuckets = new int[7]; // 5-9, 10-14, ..., 30-32
        int minSpan = 999, maxSpan = 0;
        double avgSpan = 0;
        for (SsqDraw d : draws) {
            int[] reds = d.getRedIntList();
            int span = reds[5] - reds[0];
            avgSpan += span;
            minSpan = Math.min(minSpan, span);
            maxSpan = Math.max(maxSpan, span);
            int idx = (span - 5) / 5;
            if (idx < 0) idx = 0;
            if (idx >= spanBuckets.length) idx = spanBuckets.length - 1;
            spanBuckets[idx]++;
        }
        avgSpan = draws.isEmpty() ? 0 : avgSpan / draws.size();
        Map<String, Object> spanInfo = new LinkedHashMap<>();
        spanInfo.put("min", minSpan == 999 ? 0 : minSpan);
        spanInfo.put("max", maxSpan);
        spanInfo.put("avg", Math.round(avgSpan * 10) / 10.0);
        List<Map<String, Object>> spanHist = new ArrayList<>();
        String[] spanLabels = {"5-9","10-14","15-19","20-24","25-29","30-32","33+"};
        for (int i = 0; i < spanBuckets.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("range", spanLabels[i]);
            m.put("count", spanBuckets[i]);
            spanHist.add(m);
        }
        spanInfo.put("histogram", spanHist);
        result.put("spanStats", spanInfo);

        // ---- 8. 连号统计 ----
        int hasConsecutive = 0;
        int totalConsecutiveGroups = 0;
        for (SsqDraw d : draws) {
            int[] reds = d.getRedIntList();
            int groups = 0;
            for (int i = 1; i < reds.length; i++) {
                if (reds[i] - reds[i - 1] == 1) {
                    groups++;
                    // 连续多个只算一组
                    if (i < 2 || reds[i - 1] - reds[i - 2] != 1) {
                        // 新的连号组开始
                    }
                }
            }
            if (groups > 0) hasConsecutive++;
            totalConsecutiveGroups += groups;
        }
        Map<String, Object> consecInfo = new LinkedHashMap<>();
        consecInfo.put("hasConsecutiveCount", hasConsecutive);
        consecInfo.put("noConsecutiveCount", draws.size() - hasConsecutive);
        consecInfo.put("totalGroups", totalConsecutiveGroups);
        consecInfo.put("consecutiveRate", draws.isEmpty() ? 0 : Math.round(hasConsecutive * 1000.0 / draws.size()) / 10.0);
        result.put("consecutiveStats", consecInfo);

        // ---- 9. 重号统计 (与上期重复) ----
        int hasRepeat = 0;
        int totalRepeatNums = 0;
        for (int i = 1; i < draws.size(); i++) {
            Set<Integer> prevReds = new HashSet<>();
            for (int r : draws.get(i - 1).getRedIntList()) prevReds.add(r);
            int repeat = 0;
            for (int r : draws.get(i).getRedIntList()) if (prevReds.contains(r)) repeat++;
            if (repeat > 0) hasRepeat++;
            totalRepeatNums += repeat;
        }
        Map<String, Object> repeatInfo = new LinkedHashMap<>();
        repeatInfo.put("hasRepeatCount", hasRepeat);
        repeatInfo.put("totalRepeatNums", totalRepeatNums);
        repeatInfo.put("avgRepeat", draws.size() <= 1 ? 0 : Math.round(totalRepeatNums * 100.0 / (draws.size() - 1)) / 100.0);
        result.put("repeatStats", repeatInfo);

        // ---- 10. AC 值统计 ----
        // AC 值 = 号码两两差值的集合中不同值的个数 - 5
        int[] acBuckets = new int[8]; // 0-3, 4, 5, 6, 7, 8, 9, 10+
        double avgAc = 0;
        for (SsqDraw d : draws) {
            int[] reds = d.getRedIntList();
            Set<Integer> diffs = new HashSet<>();
            for (int i = 0; i < reds.length; i++) {
                for (int j = i + 1; j < reds.length; j++) {
                    diffs.add(Math.abs(reds[j] - reds[i]));
                }
            }
            int ac = diffs.size() - 5;
            avgAc += ac;
            int idx = ac;
            if (idx < 0) idx = 0;
            if (idx >= acBuckets.length) idx = acBuckets.length - 1;
            acBuckets[idx]++;
        }
        avgAc = draws.isEmpty() ? 0 : avgAc / draws.size();
        Map<String, Object> acInfo = new LinkedHashMap<>();
        acInfo.put("avg", Math.round(avgAc * 10) / 10.0);
        List<Map<String, Object>> acHist = new ArrayList<>();
        String[] acLabels = {"0-3","4","5","6","7","8","9","10+"};
        for (int i = 0; i < acBuckets.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ac", acLabels[i]);
            m.put("count", acBuckets[i]);
            acHist.add(m);
        }
        acInfo.put("histogram", acHist);
        result.put("acStats", acInfo);

        // ---- 11. 质合比分布 ----
        // 质数: 2,3,5,7,11,13,17,19,23,29,31
        Set<Integer> primes = new HashSet<>(Arrays.asList(2,3,5,7,11,13,17,19,23,29,31));
        Map<String, Integer> primeCompDist = new LinkedHashMap<>();
        for (int i = 0; i <= 6; i++) primeCompDist.put(i + ":" + (6 - i), 0);
        for (SsqDraw d : draws) {
            int prime = 0;
            for (int r : d.getRedIntList()) if (primes.contains(r)) prime++;
            primeCompDist.put(prime + ":" + (6 - prime), primeCompDist.get(prime + ":" + (6 - prime)) + 1);
        }
        result.put("primeCompDist", primeCompDist);

        // ---- 12. 蓝球奇偶/大小分布 ----
        Map<String, Object> blueAnalysis = new LinkedHashMap<>();
        int blueOdd = 0, blueEven = 0, blueBig = 0, blueSmall = 0;
        for (SsqDraw d : draws) {
            int b = Integer.parseInt(d.getBlue());
            if (b % 2 != 0) blueOdd++; else blueEven++;
            if (b <= 8) blueSmall++; else blueBig++;
        }
        blueAnalysis.put("oddCount", blueOdd);
        blueAnalysis.put("evenCount", blueEven);
        blueAnalysis.put("bigCount", blueBig);
        blueAnalysis.put("smallCount", blueSmall);
        result.put("blueAnalysis", blueAnalysis);

        result.put("totalDraws", draws.size());
        result.put("sampleSize", limit);
        return result;
    }

    // ===================== 私有工具 =====================
    private PrizeMatchResult buildMatchResult(int redHit, boolean blueHit) {
        PrizeMatchResult r = new PrizeMatchResult();
        r.setRedHit(redHit);
        r.setBlueHit(blueHit);
        List<SsqPrizeRule> rules = prizeRuleMapper.matchByHit(redHit, blueHit);
        if (rules == null || rules.isEmpty()) {
            r.setWon(false);
            r.setLevelName("未中奖");
            r.setLevelCode("NONE");
            r.setPrizeAmount(BigDecimal.ZERO);
            return r;
        }
        SsqPrizeRule rule = rules.get(0); // sort_no 最小的 (最高奖)
        r.setWon(true);
        r.setLevelCode(rule.getLevelCode());
        r.setLevelName(rule.getLevelName());
        r.setPrizeAmount(rule.getPrizeAmount() != null ? rule.getPrizeAmount() : BigDecimal.ZERO);
        r.setPrizeDesc(rule.getPrizeDesc());
        return r;
    }

    /** -1 = a更高奖 (更小sortNo), 1 = b更高奖 */
    private int compareLevel(PrizeMatchResult a, PrizeMatchResult b) {
        return priorityOf(a.getLevelCode()) - priorityOf(b.getLevelCode());
    }
    private int priorityOf(String code) {
        return switch (code == null ? "NONE" : code) {
            case "1" -> 1; case "2" -> 2; case "3" -> 3;
            case "4", "4B" -> 4; case "5", "5B" -> 5; case "6", "6B", "6C" -> 6;
            default -> 99;
        };
    }

    /** 组合枚举：C(n,k) 的所有组合 */
    private <T> List<List<T>> combinations(List<T> list, int k) {
        List<List<T>> result = new ArrayList<>();
        int n = list.size();
        if (k > n) return result;
        int[] idx = new int[k];
        for (int i = 0; i < k; i++) idx[i] = i;
        while (true) {
            List<T> combo = new ArrayList<>(k);
            for (int i : idx) combo.add(list.get(i));
            result.add(combo);
            int i = k - 1;
            while (i >= 0 && idx[i] == n - k + i) i--;
            if (i < 0) break;
            idx[i]++;
            for (int j = i + 1; j < k; j++) idx[j] = idx[j - 1] + 1;
        }
        return result;
    }
}
