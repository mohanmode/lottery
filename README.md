# 智能选号与历史数据分析系统

基于 Spring Boot 3 + Vue 3 的双色球 & 快乐8 选号、开奖历史查询、统计分析与自动同步平台。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2.8 + MyBatis 3.0.3 + Lombok 1.18.44 + JDK 17+ |
| 数据库 | MySQL 8（生产）/ H2 内存数据库（开发，MySQL 兼容模式） |
| 前端 | Vue 3.4 + Vite 5.4 + Vue Router 4 + Element Plus 2.8 + ECharts 5 + Sass |
| 数据抓取 | 多数据源爬虫+ 定时任务 |

## 核心功能

### 双色球
- **螺旋选号**：阿基米德螺旋图 SVG 交互式选号（红球 2.8 圈 + 蓝球 2.0 圈），支持单式/胆拖/复式
- **历史查询**：1591 期历史开奖（2016~2026），支持期号/号码/日期搜索，一键跳转螺旋图高亮
- **统计分析**：11 个 ECharts 图表 + 4 个排名表格，涵盖频次/遗漏/奇偶/大小/质合/和值/跨度/AC值/三区比/连号/重号
- **中奖分析**：单式/复式/胆拖 3 Tab 判奖，冷热号频率柱状图 + 折线面积图
- **自动同步**：定时抓取最新开奖（每 30 分钟 + 开奖夜 5 分钟加速）

### 快乐8
- **选号**：80 选 N（1~10）玩法，复式展开预览
- **统计分析**：11 个 ECharts 图表 + 4 个排名表格，80 个号码完整频次/遗漏/冷热/四区比
- **历史同步**：2038 期历史数据（2020~2026，快乐8 发行至今全部）

## 快速启动

### 方式一：H2 内存模式（推荐，无需 MySQL）

```bat
cd lottery-backend
set SPRING_PROFILES_ACTIVE=h2
mvn spring-boot:run
```

或双击 `lottery-backend\start-backend-h2.bat`

```bat
cd lottery-frontend
npm install
npm run dev
```

打开 http://localhost:5173

### 方式二：MySQL 模式

```sql
CREATE DATABASE IF NOT EXISTS lottery_db DEFAULT CHARSET utf8mb4;
```

修改 `application.yml` 中的数据库连接信息，然后：

```bat
cd lottery-backend
mvn spring-boot:run

cd lottery-frontend
npm install
npm run dev
```

## 页面路由

| 页面 | 路由 | 说明 |
|------|------|------|
| 首页 | `/` | 统计概览、最新开奖、同步状态 |
| 双色球螺旋选号 | `/ssq-spiral` | 螺旋图选号 + 历史期号高亮 |
| 双色球开奖历史 | `/ssq-history` | 1591 期分页查询 + 跳转螺旋图 |
| 双色球统计分析 | `/ssq-statistics` | 11 图表 + 4 排行表 |
| 双色球中奖分析 | `/ssq-match` | 频率分析 + 多模式判奖 |
| 快乐8选号 | `/kl8` | 80 选 N 复式选号 |
| 快乐8统计分析 | `/kl8-statistics` | 11 图表 + 4 排行表 |
| 我的投注 | `/my-bets` | 投注 CRUD + 批量判奖 |

## 后端 API

| Method | 路径 | 说明 |
|--------|------|------|
| GET | `/api/system/health` | 健康检查 |
| GET | `/api/system/sync-status` | 同步状态 |
| POST | `/api/system/sync-ssq` | 手动同步双色球 |
| POST | `/api/system/sync-kl8` | 手动同步快乐8 |
| POST | `/api/system/sync-kl8-history` | 快乐8历史数据批量同步 |
| GET | `/api/ssq/draws/latest` | 最新双色球开奖 |
| GET | `/api/ssq/draws/issue/{issue}` | 按期号查询 |
| GET | `/api/ssq/draws?pageNum=1&pageSize=20` | 分页查询 |
| GET | `/api/ssq/statistics?recent=100` | 统计分析 |
| GET | `/api/ssq/pick/random/{count}` | 随机选号 |
| POST | `/api/ssq/pick/compound` | 复式选号 |
| POST | `/api/ssq/pick/dantuo` | 胆拖选号 |
| POST | `/api/ssq/match/single` | 单式判奖 |
| GET | `/api/kl8/draws/latest` | 最新快乐8开奖 |
| GET | `/api/kl8/statistics?recent=100` | 快乐8统计 |
| POST | `/api/bet` | 保存投注 |
| GET | `/api/bet?pageNum=1&pageSize=20` | 投注列表 |

### 统一返回格式

```json
{
  "code": 0,
  "msg": "success",
  "data": { ... }
}
```

## 项目结构

```
├── lottery-backend/
│   ├── src/main/java/com/lottery/
│   │   ├── LotteryApplication.java          # 启动类 (@EnableScheduling)
│   │   ├── config/                           # CORS + 全局异常
│   │   ├── controller/                       # Ssq/Kl8/System/UserBet Controller
│   │   ├── service/                          # 业务逻辑层
│   │   ├── mapper/                            # MyBatis Mapper 接口
│   │   ├── entity/                            # 实体类
│   │   ├── dto/                              # VO/ApiResult/PageResult
│   │   ├── crawl/                            # 多数据源爬虫 + 定时任务
│   │   └── runner/                            # 启动数据导入
│   └── src/main/resources/
│       ├── application.yml                   # MySQL 配置
│       ├── application-h2.yml                # H2 配置
│       ├── sql/schema.sql                    # MySQL DDL
│       ├── sql/schema-h2.sql                 # H2 DDL
│       ├── mapper/*.xml                      # MyBatis XML
│       └── data/ssq_history.json            # 1591 期历史数据
├── lottery-frontend/
│   ├── src/
│   │   ├── App.vue                           # 导航 + 路由
│   │   ├── router/index.js                   # 路由配置
│   │   ├── api/index.js                      # API 封装
│   │   ├── styles/global.scss                # 全局暗色主题
│   │   ├── components/PickBalls.vue          # 选球组件
│   │   └── views/                            # 8 个功能页面
│   ├── vite.config.js                         # 5173 端口 + 代理
│   └── package.json
└── START_GUIDE.md                             # 详细启动指南
```

## 统计分析功能

### 双色球（12 项统计）
号码频次、遗漏值、冷热号 Top10、蓝球分析、奇偶比、大小比、质合比、和值分布、跨度分布、AC 值、三区比 Top10、连号/重号统计

### 快乐8（12 项统计）
80 号频次、遗漏值、冷热号 Top10、四区比 Top10、奇偶比、大小比、质合比、和值分布、跨度分布、AC 值、连号/重号统计

### 界面特色
- 分区标题 + 渐变分隔线（5 个逻辑分区）
- 概览卡片带图标 + 独立配色（橙/蓝/紫/绿）
- 图表标题色点指示器 + 标签徽章
- 悬停上浮动效 + 阴影增强
- 排行卡片彩色左边框区分（热号红/冷号蓝/汇总紫）
- 加载遮罩 + 过渡动画

## 数据同步

| 彩种 | 数据源 | 频率 | 数据量 |
|------|--------|------|--------|
| 双色球 |  每 30 分钟 + 开奖夜 5 分钟 | 1591 期 |
| 快乐8 |  每 30 分钟 + 每日 5 分钟 | 2038 期 |

定时任务配置见 `SsqSyncScheduler.java` 和 `Kl8SyncScheduler.java`。

## License

MIT

<p align="center">
| 微信 | 支付宝 |
|:----:|:------:|
| <img src=".github/assets/wx.jpg" width="200"/> | <img src=".github/assets/alipay.jpg" width="200"/> |
</p>

