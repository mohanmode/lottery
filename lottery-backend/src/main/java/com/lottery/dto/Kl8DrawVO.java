package com.lottery.dto;

import lombok.Data;

/**
 * 快乐8开奖记录 VO (用于前端展示)
 */
@Data
public class Kl8DrawVO {
    private Long id;
    private String issue;
    private String[] numbers; // 20个号码
    private String drawDate;  // yyyy-MM-dd
}
