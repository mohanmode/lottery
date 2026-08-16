package com.lottery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 选号系统 启动类
 * 提供双色球与快乐8 选号、开奖历史查询、中奖判定等能力
 */
@SpringBootApplication
@MapperScan("com.lottery.mapper")
@EnableScheduling
public class LotteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LotteryApplication.class, args);
        System.out.println("\n================================================");
        System.out.println("  选号系统启动成功! ");
        System.out.println("  后端地址: http://localhost:8080/api");
        System.out.println("  前端(Vue3): http://localhost:5173");
        System.out.println("================================================");
    }
}
