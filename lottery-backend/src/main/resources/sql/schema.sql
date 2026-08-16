-- ============================================
-- 彩票选号系统 数据库 DDL
-- 数据库: lottery_db
-- ============================================

CREATE DATABASE IF NOT EXISTS lottery_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lottery_db;

-- --------------------------------------------
-- 双色球历史开奖表
-- --------------------------------------------
DROP TABLE IF EXISTS `ssq_draw`;
CREATE TABLE `ssq_draw` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `issue`       VARCHAR(10)  NOT NULL COMMENT '期号，如 26093',
  `red_1`       CHAR(2)      NOT NULL COMMENT '红球1',
  `red_2`       CHAR(2)      NOT NULL COMMENT '红球2',
  `red_3`       CHAR(2)      NOT NULL COMMENT '红球3',
  `red_4`       CHAR(2)      NOT NULL COMMENT '红球4',
  `red_5`       CHAR(2)      NOT NULL COMMENT '红球5',
  `red_6`       CHAR(2)      NOT NULL COMMENT '红球6',
  `blue`        CHAR(2)      NOT NULL COMMENT '蓝球',
  `draw_date`   DATE         NOT NULL COMMENT '开奖日期',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_issue` (`issue`),
  KEY `idx_draw_date` (`draw_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='双色球历史开奖记录表';

-- --------------------------------------------
-- 用户选号记录表
-- --------------------------------------------
DROP TABLE IF EXISTS `user_bet`;
CREATE TABLE `user_bet` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `lottery_type`    VARCHAR(20)  NOT NULL COMMENT '彩种: SSQ(双色球) / KL8(快乐8)',
  `bet_type`        VARCHAR(20)  NOT NULL COMMENT '投注类型: SINGLE(单式) / DANTUO(胆拖) / COMPOUND(复式)',
  `dan_numbers`     VARCHAR(200) NULL     COMMENT '胆码(逗号分隔)',
  `tuo_numbers`     VARCHAR(500) NULL     COMMENT '拖码(逗号分隔)',
  `main_numbers`    VARCHAR(500) NOT NULL COMMENT '主号码(逗号分隔，双色球6红+蓝 / 快乐8 1-10个)',
  `extra_numbers`   VARCHAR(200) NULL     COMMENT '附加号码(逗号分隔，如蓝球区多个)',
  `combination_cnt` INT          NOT NULL DEFAULT 1 COMMENT '组合注数',
  `is_matched`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已匹配开奖: 0-否 1-是',
  `match_issue`     VARCHAR(10)  NULL     COMMENT '匹配的开奖期号',
  `match_level`     VARCHAR(20)  NULL     COMMENT '中奖等级: 一等奖/二等奖/.../未中奖',
  `match_prize`     DECIMAL(18,2) NULL    COMMENT '中奖金额(元)',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lottery_type` (`lottery_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户选号投注记录表';

-- --------------------------------------------
-- 双色球中奖规则表
-- --------------------------------------------
DROP TABLE IF EXISTS `ssq_prize_rule`;
CREATE TABLE `ssq_prize_rule` (
  `id`              INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `level_code`      VARCHAR(10)  NOT NULL COMMENT '等级编码 1-6',
  `level_name`      VARCHAR(20)  NOT NULL COMMENT '等级名称 一等奖...六等奖',
  `red_hit_min`     INT          NOT NULL DEFAULT 0 COMMENT '红球命中下限',
  `red_hit_max`     INT          NOT NULL DEFAULT 6 COMMENT '红球命中上限',
  `blue_hit`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否需蓝球命中: 0-否 1-是',
  `blue_match_any`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '蓝球任意匹配: 1=中或不中都可 (仅用于不含蓝球的组合, 如6+0二等奖)',
  `prize_amount`    DECIMAL(18,2) NULL    COMMENT '固定奖金额 (元), NULL表示浮动奖金',
  `prize_desc`      VARCHAR(200) NULL     COMMENT '奖金描述',
  `sort_no`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `is_active`       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_code` (`level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='双色球中奖规则表';

-- 初始化双色球中奖规则（官方规则）
INSERT INTO `ssq_prize_rule`
(level_code, level_name, red_hit_min, red_hit_max, blue_hit, blue_match_any, prize_amount, prize_desc, sort_no) VALUES
('1', '一等奖', 6, 6, 1, 0, NULL, '当奖池资金低于1亿元时，奖金总额为当期高奖级奖金的75%与奖池中累积的资金之和，单注奖金按注均分，单注最高限额封顶500万元', 1),
('2', '二等奖', 6, 6, 0, 1, NULL, '奖金总额为当期高奖级奖金的25%，单注奖金按注均分，单注最高限额封顶500万元', 2),
('3', '三等奖', 5, 5, 1, 0, 3000.00, '单注奖金额固定为3000元', 3),
('4', '四等奖', 5, 5, 0, 1, 200.00, '单注奖金额固定为200元 (5+0 或 4+1)', 4),
('4B','四等奖', 4, 4, 1, 0, 200.00, '单注奖金额固定为200元 (4+1)', 5),
('5', '五等奖', 4, 4, 0, 1, 10.00, '单注奖金额固定为10元 (4+0 或 3+1)', 6),
('5B','五等奖', 3, 3, 1, 0, 10.00, '单注奖金额固定为10元 (3+1)', 7),
('6', '六等奖', 2, 2, 1, 0, 5.00, '单注奖金额固定为5元 (2+1 / 1+1 / 0+1)', 8),
('6B','六等奖', 1, 1, 1, 0, 5.00, '单注奖金额固定为5元', 9),
('6C','六等奖', 0, 0, 1, 0, 5.00, '单注奖金额固定为5元', 10);

-- --------------------------------------------
-- 快乐8中奖规则表 (选10玩法 - 最主流)
-- --------------------------------------------
DROP TABLE IF EXISTS `kl8_prize_rule`;
CREATE TABLE `kl8_prize_rule` (
  `id`              INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pick_count`      INT          NOT NULL COMMENT '选号个数 (1-10)',
  `hit_count_min`   INT          NOT NULL COMMENT '命中下限',
  `hit_count_max`   INT          NOT NULL COMMENT '命中上限',
  `level_name`      VARCHAR(20)  NOT NULL COMMENT '奖项名称',
  `prize_amount`    DECIMAL(18,2) NOT NULL COMMENT '单注奖金(元)',
  `sort_no`         INT          NOT NULL DEFAULT 0,
  `is_active`       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_pick_count` (`pick_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快乐8中奖规则表';

-- 初始化快乐8 选10玩法奖金
INSERT INTO `kl8_prize_rule` (pick_count, hit_count_min, hit_count_max, level_name, prize_amount, sort_no) VALUES
(10, 10, 10, '选10中10', 5000000.00, 1),
(10,  9,  9, '选10中9',   80000.00, 2),
(10,  8,  8, '选10中8',    1500.00, 3),
(10,  7,  7, '选10中7',     200.00, 4),
(10,  6,  6, '选10中6',      50.00, 5),
(10,  5,  5, '选10中5',      10.00, 6),
(10,  0,  0, '选10中0',       5.00, 7),
(9,   9,  9, '选9中9',   300000.00, 11),
(9,   8,  8, '选9中8',    20000.00, 12),
(9,   7,  7, '选9中7',     2000.00, 13),
(9,   6,  6, '选9中6',      100.00, 14),
(9,   5,  5, '选9中5',       10.00, 15),
(9,   4,  4, '选9中4',        3.00, 16),
(8,   8,  8, '选8中8',    50000.00, 21),
(8,   7,  7, '选8中7',     2500.00, 22),
(8,   6,  6, '选8中6',      200.00, 23),
(8,   5,  5, '选8中5',       20.00, 24),
(8,   4,  4, '选8中4',        5.00, 25),
(8,   0,  0, '选8中0',        2.00, 26);
