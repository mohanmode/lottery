package com.lottery.service.impl;

import com.lottery.dto.PageResult;
import com.lottery.entity.Kl8Draw;
import com.lottery.mapper.Kl8DrawMapper;
import com.lottery.service.Kl8Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Kl8ServiceImpl implements Kl8Service {

    private final Kl8DrawMapper drawMapper;

    // ===================== 开奖历史 CRUD =====================
    @Override
    public Kl8Draw getLatestDraw() { return drawMapper.selectLatest(); }

    @Override
    public Kl8Draw getByIssue(String issue) { return drawMapper.selectByIssue(issue); }

    @Override
    public PageResult<Kl8Draw> listDraws(int pageNum, int pageSize, String keyword, LocalDate startDate, LocalDate endDate) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        long total = drawMapper.selectCount(keyword, startDate, endDate);
        List<Kl8Draw> list = drawMapper.selectList(keyword, startDate, endDate, offset, pageSize);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    @Override
    public int saveDraw(Kl8Draw draw) {
        if (drawMapper.countByIssue(draw.getIssue()) > 0) return 0;
        return drawMapper.insert(draw);
    }

    @Override
    public int batchInsertDraws(List<Kl8Draw> draws) {
        if (draws == null || draws.isEmpty()) return 0;
        // 过滤已存在的期号，避免唯一索引冲突
        List<Kl8Draw> toInsert = draws.stream()
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

    @Override
    public int[] analyzeNumberFrequency(int recentDrawCount) {
        int[] freq = new int[81]; // 1-80
        PageResult<Kl8Draw> page = listDraws(1, Math.max(1, Math.min(5000, recentDrawCount)), null, null, null);
        for (Kl8Draw d : page.getList()) {
            for (int n : d.getNumberIntList()) freq[n]++;
        }
        return freq;
    }

    // ===================== 综合统计 =====================
    @Override
    public Map<String, Object> getStatistics(int recentDrawCount) {
        int limit = Math.max(1, Math.min(5000, recentDrawCount));
        PageResult<Kl8Draw> page = listDraws(1, limit, null, null, null);
        List<Kl8Draw> draws = page.getList();
        // 倒序 → 正序 (老→新)，方便遗漏值计算
        Collections.reverse(draws);

        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 1. 遗漏值 (每个号码距离上次出现多少期) ----
        int[] omit = new int[81];
        Arrays.fill(omit, -1);
        for (int i = draws.size() - 1; i >= 0; i--) {
            Kl8Draw d = draws.get(i);
            for (int n : d.getNumberIntList()) {
                if (omit[n] == -1) omit[n] = draws.size() - 1 - i;
            }
        }
        for (int i = 1; i <= 80; i++) if (omit[i] == -1) omit[i] = draws.size();
        Map<String, Integer> omitMap = new LinkedHashMap<>();
        for (int i = 1; i <= 80; i++) omitMap.put(String.format("%02d", i), omit[i]);
        result.put("numberOmit", omitMap);

        // ---- 2. 频次 ----
        int[] freq = new int[81];
        for (Kl8Draw d : draws) {
            for (int n : d.getNumberIntList()) freq[n]++;
        }
        Map<String, Integer> freqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 80; i++) freqMap.put(String.format("%02d", i), freq[i]);
        result.put("numberFreq", freqMap);

        // ---- 3. 冷热号 Top10 ----
        List<Map<String, Object>> hot = new ArrayList<>();
        List<Map<String, Object>> cold = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", String.format("%02d", i));
            m.put("freq", freq[i]);
            m.put("omit", omit[i]);
            hot.add(m);
            cold.add(new LinkedHashMap<>(m));
        }
        hot.sort((a, b) -> ((int) b.get("freq")) - ((int) a.get("freq")));
        cold.sort((a, b) -> ((int) a.get("freq")) - ((int) b.get("freq")));
        result.put("hotTop10", hot.subList(0, Math.min(10, hot.size())));
        result.put("coldTop10", cold.subList(0, Math.min(10, cold.size())));

        // ---- 4. 奇偶比分布 (20个号码中奇数个数) ----
        Map<String, Integer> oddEvenDist = new LinkedHashMap<>();
        for (Kl8Draw d : draws) {
            int odd = 0;
            for (int n : d.getNumberIntList()) if (n % 2 != 0) odd++;
            String key = odd + ":" + (20 - odd);
            oddEvenDist.put(key, oddEvenDist.getOrDefault(key, 0) + 1);
        }
        result.put("oddEvenDist", oddEvenDist);

        // ---- 5. 大小比分布 (1-40=小, 41-80=大) ----
        Map<String, Integer> bigSmallDist = new LinkedHashMap<>();
        for (Kl8Draw d : draws) {
            int big = 0;
            for (int n : d.getNumberIntList()) if (n >= 41) big++;
            String key = big + ":" + (20 - big);
            bigSmallDist.put(key, bigSmallDist.getOrDefault(key, 0) + 1);
        }
        result.put("bigSmallDist", bigSmallDist);

        // ---- 6. 四区比分布 (1-20 / 21-40 / 41-60 / 61-80) ----
        Map<String, Integer> zoneDist = new LinkedHashMap<>();
        for (Kl8Draw d : draws) {
            int z1 = 0, z2 = 0, z3 = 0, z4 = 0;
            for (int n : d.getNumberIntList()) {
                if (n <= 20) z1++;
                else if (n <= 40) z2++;
                else if (n <= 60) z3++;
                else z4++;
            }
            String key = z1 + ":" + z2 + ":" + z3 + ":" + z4;
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

        // ---- 7. 和值分布 (20个号码之和) ----
        // 分桶: 200-399,400-599,600-799,800-999,1000-1199,1200+
        int[] sumBuckets = new int[6];
        int minSum = Integer.MAX_VALUE, maxSum = 0;
        double avgSum = 0;
        for (Kl8Draw d : draws) {
            int sum = 0;
            for (int n : d.getNumberIntList()) sum += n;
            avgSum += sum;
            minSum = Math.min(minSum, sum);
            maxSum = Math.max(maxSum, sum);
            int idx;
            if (sum < 400) idx = 0;
            else if (sum < 600) idx = 1;
            else if (sum < 800) idx = 2;
            else if (sum < 1000) idx = 3;
            else if (sum < 1200) idx = 4;
            else idx = 5;
            sumBuckets[idx]++;
        }
        avgSum = draws.isEmpty() ? 0 : avgSum / draws.size();
        Map<String, Object> sumInfo = new LinkedHashMap<>();
        sumInfo.put("min", draws.isEmpty() ? 0 : minSum);
        sumInfo.put("max", maxSum);
        sumInfo.put("avg", Math.round(avgSum * 10) / 10.0);
        List<Map<String, Object>> sumHist = new ArrayList<>();
        String[] sumLabels = {"200-399", "400-599", "600-799", "800-999", "1000-1199", "1200+"};
        for (int i = 0; i < sumBuckets.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("range", sumLabels[i]);
            m.put("count", sumBuckets[i]);
            sumHist.add(m);
        }
        sumInfo.put("histogram", sumHist);
        result.put("sumStats", sumInfo);

        // ---- 8. 跨度分布 (max-min) ----
        // 分桶: 19-29,30-39,40-49,50-59,60-69,70-79
        int[] spanBuckets = new int[6];
        int minSpan = Integer.MAX_VALUE, maxSpan = 0;
        double avgSpan = 0;
        for (Kl8Draw d : draws) {
            int[] nums = d.getNumberIntList();
            int span = nums[19] - nums[0];
            avgSpan += span;
            minSpan = Math.min(minSpan, span);
            maxSpan = Math.max(maxSpan, span);
            int idx;
            if (span < 30) idx = 0;
            else if (span < 40) idx = 1;
            else if (span < 50) idx = 2;
            else if (span < 60) idx = 3;
            else if (span < 70) idx = 4;
            else idx = 5;
            spanBuckets[idx]++;
        }
        avgSpan = draws.isEmpty() ? 0 : avgSpan / draws.size();
        Map<String, Object> spanInfo = new LinkedHashMap<>();
        spanInfo.put("min", draws.isEmpty() ? 0 : minSpan);
        spanInfo.put("max", maxSpan);
        spanInfo.put("avg", Math.round(avgSpan * 10) / 10.0);
        List<Map<String, Object>> spanHist = new ArrayList<>();
        String[] spanLabels = {"19-29", "30-39", "40-49", "50-59", "60-69", "70-79"};
        for (int i = 0; i < spanBuckets.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("range", spanLabels[i]);
            m.put("count", spanBuckets[i]);
            spanHist.add(m);
        }
        spanInfo.put("histogram", spanHist);
        result.put("spanStats", spanInfo);

        // ---- 9. 连号统计 ----
        int hasConsecutive = 0;
        int totalConsecutiveGroups = 0;
        for (Kl8Draw d : draws) {
            int[] nums = d.getNumberIntList();
            int groups = 0;
            boolean inGroup = false;
            for (int i = 1; i < nums.length; i++) {
                if (nums[i] - nums[i - 1] == 1) {
                    if (!inGroup) {
                        groups++;
                        inGroup = true;
                    }
                } else {
                    inGroup = false;
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

        // ---- 10. 重号统计 (与上期重复) ----
        int hasRepeat = 0;
        int totalRepeatNums = 0;
        for (int i = 1; i < draws.size(); i++) {
            Set<Integer> prevNums = new HashSet<>();
            for (int n : draws.get(i - 1).getNumberIntList()) prevNums.add(n);
            int repeat = 0;
            for (int n : draws.get(i).getNumberIntList()) if (prevNums.contains(n)) repeat++;
            if (repeat > 0) hasRepeat++;
            totalRepeatNums += repeat;
        }
        Map<String, Object> repeatInfo = new LinkedHashMap<>();
        repeatInfo.put("hasRepeatCount", hasRepeat);
        repeatInfo.put("totalRepeatNums", totalRepeatNums);
        repeatInfo.put("avgRepeat", draws.size() <= 1 ? 0 : Math.round(totalRepeatNums * 100.0 / (draws.size() - 1)) / 100.0);
        result.put("repeatStats", repeatInfo);

        // ---- 11. AC 值统计 ----
        // AC 值 = 20个号码两两差值的不同值个数 - 19
        int[] acBuckets = new int[8]; // 0-5, 6, 7, 8, 9, 10, 11, 12+
        double avgAc = 0;
        for (Kl8Draw d : draws) {
            int[] nums = d.getNumberIntList();
            Set<Integer> diffs = new HashSet<>();
            for (int i = 0; i < nums.length; i++) {
                for (int j = i + 1; j < nums.length; j++) {
                    diffs.add(Math.abs(nums[j] - nums[i]));
                }
            }
            int ac = diffs.size() - 19;
            avgAc += ac;
            int idx;
            if (ac <= 5) idx = 0;
            else if (ac == 6) idx = 1;
            else if (ac == 7) idx = 2;
            else if (ac == 8) idx = 3;
            else if (ac == 9) idx = 4;
            else if (ac == 10) idx = 5;
            else if (ac == 11) idx = 6;
            else idx = 7;
            acBuckets[idx]++;
        }
        avgAc = draws.isEmpty() ? 0 : avgAc / draws.size();
        Map<String, Object> acInfo = new LinkedHashMap<>();
        acInfo.put("avg", Math.round(avgAc * 10) / 10.0);
        List<Map<String, Object>> acHist = new ArrayList<>();
        String[] acLabels = {"0-5", "6", "7", "8", "9", "10", "11", "12+"};
        for (int i = 0; i < acBuckets.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ac", acLabels[i]);
            m.put("count", acBuckets[i]);
            acHist.add(m);
        }
        acInfo.put("histogram", acHist);
        result.put("acStats", acInfo);

        // ---- 12. 质合比分布 ----
        // 质数集合={2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79}
        Set<Integer> primes = new HashSet<>(Arrays.asList(
            2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79));
        Map<String, Integer> primeCompDist = new LinkedHashMap<>();
        for (Kl8Draw d : draws) {
            int prime = 0;
            for (int n : d.getNumberIntList()) if (primes.contains(n)) prime++;
            String key = prime + ":" + (20 - prime);
            primeCompDist.put(key, primeCompDist.getOrDefault(key, 0) + 1);
        }
        result.put("primeCompDist", primeCompDist);

        result.put("totalDraws", draws.size());
        result.put("sampleSize", limit);
        return result;
    }
}
