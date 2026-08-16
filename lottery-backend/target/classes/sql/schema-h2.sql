-- ============================================
-- H2 兼容版 DDL (MySQL 兼容模式)
-- ============================================

-- 双色球历史开奖表
DROP TABLE IF EXISTS ssq_draw;
CREATE TABLE ssq_draw (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  issue       VARCHAR(10)  NOT NULL,
  red_1       CHAR(2)      NOT NULL,
  red_2       CHAR(2)      NOT NULL,
  red_3       CHAR(2)      NOT NULL,
  red_4       CHAR(2)      NOT NULL,
  red_5       CHAR(2)      NOT NULL,
  red_6       CHAR(2)      NOT NULL,
  blue        CHAR(2)      NOT NULL,
  draw_date   DATE         NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (issue)
);

-- 用户选号记录表
DROP TABLE IF EXISTS user_bet;
CREATE TABLE user_bet (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  lottery_type    VARCHAR(20)  NOT NULL,
  bet_type        VARCHAR(20)  NOT NULL,
  dan_numbers     VARCHAR(200),
  tuo_numbers     VARCHAR(500),
  main_numbers    VARCHAR(500) NOT NULL,
  extra_numbers   VARCHAR(200),
  combination_cnt INT          NOT NULL DEFAULT 1,
  is_matched      TINYINT      NOT NULL DEFAULT 0,
  match_issue     VARCHAR(10),
  match_level     VARCHAR(20),
  match_prize     DECIMAL(18,2),
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

-- 双色球中奖规则表
DROP TABLE IF EXISTS ssq_prize_rule;
CREATE TABLE ssq_prize_rule (
  id              INT          NOT NULL AUTO_INCREMENT,
  level_code      VARCHAR(10)  NOT NULL,
  level_name      VARCHAR(20)  NOT NULL,
  red_hit_min     INT          NOT NULL DEFAULT 0,
  red_hit_max     INT          NOT NULL DEFAULT 6,
  blue_hit        TINYINT      NOT NULL DEFAULT 0,
  blue_match_any  TINYINT      NOT NULL DEFAULT 0,
  prize_amount    DECIMAL(18,2),
  prize_desc      VARCHAR(200),
  sort_no         INT          NOT NULL DEFAULT 0,
  is_active       TINYINT      NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE (level_code)
);

INSERT INTO ssq_prize_rule
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

-- 快乐8开奖表
DROP TABLE IF EXISTS kl8_draw;
CREATE TABLE kl8_draw (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  issue       VARCHAR(10)  NOT NULL,
  n_1         CHAR(2)      NOT NULL,
  n_2         CHAR(2)      NOT NULL,
  n_3         CHAR(2)      NOT NULL,
  n_4         CHAR(2)      NOT NULL,
  n_5         CHAR(2)      NOT NULL,
  n_6         CHAR(2)      NOT NULL,
  n_7         CHAR(2)      NOT NULL,
  n_8         CHAR(2)      NOT NULL,
  n_9         CHAR(2)      NOT NULL,
  n_10        CHAR(2)      NOT NULL,
  n_11        CHAR(2)      NOT NULL,
  n_12        CHAR(2)      NOT NULL,
  n_13        CHAR(2)      NOT NULL,
  n_14        CHAR(2)      NOT NULL,
  n_15        CHAR(2)      NOT NULL,
  n_16        CHAR(2)      NOT NULL,
  n_17        CHAR(2)      NOT NULL,
  n_18        CHAR(2)      NOT NULL,
  n_19        CHAR(2)      NOT NULL,
  n_20        CHAR(2)      NOT NULL,
  draw_date   DATE         NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (issue)
);
