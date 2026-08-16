package com.lottery.service.impl;

import com.lottery.dto.PageResult;
import com.lottery.dto.PrizeMatchResult;
import com.lottery.entity.SsqDraw;
import com.lottery.entity.UserBet;
import com.lottery.mapper.UserBetMapper;
import com.lottery.service.SsqService;
import com.lottery.service.UserBetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserBetServiceImpl implements UserBetService {

    private final UserBetMapper betMapper;
    private final SsqService ssqService;

    @Override
    public Long save(UserBet bet) {
        if (bet.getCombinationCnt() == null) bet.setCombinationCnt(1);
        if (bet.getIsMatched() == null) bet.setIsMatched(false);
        betMapper.insert(bet);
        return bet.getId();
    }

    @Override
    public boolean update(UserBet bet) { return betMapper.updateById(bet) > 0; }

    @Override
    public boolean delete(Long id) { return betMapper.deleteById(id) > 0; }

    @Override
    public UserBet getById(Long id) { return betMapper.selectById(id); }

    @Override
    public PageResult<UserBet> list(int pageNum, int pageSize, String lotteryType, String betType, Boolean isMatched) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        long total = betMapper.selectCount(lotteryType, betType, isMatched);
        List<UserBet> list = betMapper.selectList(lotteryType, betType, isMatched, offset, pageSize);
        return PageResult.of(total, pageNum, pageSize, list);
    }

    @Override
    public UserBet matchSsqDraw(Long betId, String issue) {
        UserBet bet = betMapper.selectById(betId);
        if (bet == null || !"SSQ".equals(bet.getLotteryType())) return bet;
        SsqDraw draw = (issue == null || issue.isBlank())
                ? ssqService.getLatestDraw()
                : ssqService.getByIssue(issue);
        if (draw == null) return bet;
        List<String[]> bets = expandBetsFromBet(bet);
        PrizeMatchResult match = ssqService.matchPrizeBatch(bets, draw);
        bet.setIsMatched(true);
        bet.setMatchIssue(issue);
        bet.setMatchLevel(match.getLevelName());
        bet.setMatchPrize(match.getTotalPrize());
        betMapper.updateById(bet);
        return bet;
    }

    @Override
    public int matchAllUnmatchedSsq() {
        SsqDraw latest = ssqService.getLatestDraw();
        if (latest == null) return 0;
        int totalUpdated = 0;
        int pageNum = 1;
        int pageSize = 100;
        while (true) {
            PageResult<UserBet> page = list(pageNum++, pageSize, "SSQ", null, false);
            if (page.getList().isEmpty()) break;
            for (UserBet bet : page.getList()) {
                matchSsqDraw(bet.getId(), latest.getIssue());
                totalUpdated++;
            }
        }
        return totalUpdated;
    }

    private List<String[]> expandBetsFromBet(UserBet bet) {
        Set<String> mainSet = csvToSet(bet.getMainNumbers());
        Set<String> extraSet = csvToSet(bet.getExtraNumbers());
        if ("SINGLE".equalsIgnoreCase(bet.getBetType())) {
            List<String> reds = new ArrayList<>(mainSet);
            String blue = extraSet.isEmpty() ? "" : extraSet.iterator().next();
            while (reds.size() < 6) reds.add("00");
            reds = reds.subList(0, 6);
            Collections.sort(reds);
            String[] arr = new String[7];
            for (int i = 0; i < 6; i++) arr[i] = reds.get(i);
            arr[6] = blue;
            return Collections.singletonList(arr);
        }
        if ("COMPOUND".equalsIgnoreCase(bet.getBetType())) {
            return ssqService.generateCompound(mainSet, extraSet.isEmpty() ? Collections.singleton("01") : extraSet);
        }
        if ("DANTUO".equalsIgnoreCase(bet.getBetType())) {
            Set<String> danSet = csvToSet(bet.getDanNumbers());
            Set<String> tuoSet = csvToSet(bet.getTuoNumbers());
            return ssqService.generateDantuo(danSet, tuoSet, extraSet.isEmpty() ? Collections.singleton("01") : extraSet);
        }
        return Collections.emptyList();
    }

    private Set<String> csvToSet(String s) {
        Set<String> set = new LinkedHashSet<>();
        if (!StringUtils.hasText(s)) return set;
        for (String p : s.split("[,，;；\\s]+")) {
            String v = p.trim();
            if (!v.isEmpty()) {
                if (v.length() == 1) v = "0" + v;
                set.add(v);
            }
        }
        return set;
    }
}
