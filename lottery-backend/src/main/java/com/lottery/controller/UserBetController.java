package com.lottery.controller;

import com.lottery.dto.ApiResult;
import com.lottery.dto.PageResult;
import com.lottery.entity.UserBet;
import com.lottery.service.UserBetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/bet")
@RequiredArgsConstructor
public class UserBetController {

    private final UserBetService betService;

    @PostMapping
    public ApiResult<Long> save(@RequestBody UserBet bet) {
        Long id = betService.save(bet);
        return ApiResult.ok("保存成功", id);
    }

    @PutMapping
    public ApiResult<Boolean> update(@RequestBody UserBet bet) {
        return ApiResult.ok(betService.update(bet));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        return ApiResult.ok(betService.delete(id));
    }

    @GetMapping("/{id}")
    public ApiResult<UserBet> getById(@PathVariable Long id) {
        return ApiResult.ok(betService.getById(id));
    }

    @GetMapping
    public ApiResult<PageResult<UserBet>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String lotteryType,
            @RequestParam(required = false) String betType,
            @RequestParam(required = false) Boolean isMatched) {
        return ApiResult.ok(betService.list(pageNum, pageSize, lotteryType, betType, isMatched));
    }

    /** 对某条投注匹配指定期号开奖（issue 为空则用最新开奖） */
    @PostMapping("/{id}/match")
    public ApiResult<Map<String, Object>> matchDraw(@PathVariable Long id,
                                                     @RequestParam(required = false) String issue) {
        UserBet bet = betService.matchSsqDraw(id, issue);
        Map<String, Object> res = new LinkedHashMap<>();
        // 同时返回实体字段 + 前端易读的判奖字段
        if (bet != null) {
            res.put("id", bet.getId());
            res.put("lotteryType", bet.getLotteryType());
            res.put("betType", bet.getBetType());
            res.put("danNumbers", bet.getDanNumbers());
            res.put("tuoNumbers", bet.getTuoNumbers());
            res.put("mainNumbers", bet.getMainNumbers());
            res.put("extraNumbers", bet.getExtraNumbers());
            res.put("combinationCnt", bet.getCombinationCnt());
            res.put("isMatched", bet.getIsMatched());
            res.put("matchIssue", bet.getMatchIssue());
            res.put("createdAt", bet.getCreatedAt());
            res.put("won", bet.getMatchLevel() != null && !"未中奖".equals(bet.getMatchLevel()));
            res.put("levelName", bet.getMatchLevel() == null ? "未判奖" : bet.getMatchLevel());
            res.put("prizeAmount", bet.getMatchPrize() == null ? BigDecimal.ZERO : bet.getMatchPrize());
            res.put("totalPrize", bet.getMatchPrize() == null ? BigDecimal.ZERO : bet.getMatchPrize());
        }
        return ApiResult.ok(res);
    }

    /** 匹配所有未匹配的SSQ投注 */
    @PostMapping("/match-all-ssq")
    public ApiResult<Map<String, Object>> matchAllSsq() {
        int cnt = betService.matchAllUnmatchedSsq();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("matched", cnt);
        m.put("wonCount", 0);
        m.put("message", "已更新 " + cnt + " 条投注记录");
        return ApiResult.ok(m);
    }
}
