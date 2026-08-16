# 彩票选号系统 - 快速启动指南

## 技术栈
- **后端**: Spring Boot 3.2.8 + MyBatis 3.0.3 + Lombok 1.18.44 + JDK 17 (兼容 JDK 24)
- **数据库**: MySQL 8 (生产) / H2 内存数据库 (开发快速启动, MySQL 兼容模式)
- **前端**: Vue 3.4 + Vite 5.4 + Vue Router 4 + Element Plus 2.8 + Axios + ECharts 5 + Sass
- **数据**: 双色球 10 年历史开奖数据 (1591 期, 2016.01 - 2026.08)

---

## 目录结构
```
d:\traeworkspace
├── lottery-backend\                       # Spring Boot 后端
│   ├── pom.xml                            # Maven 配置 (含 H2 依赖)
│   ├── start-backend.bat                  # 后端启动脚本 (MySQL 模式)
│   ├── start-backend-h2.bat               # 后端启动脚本 (H2 内存模式, 无需 MySQL)
│   └── src\main\
│       ├── java\com\lottery\
│       │   ├── LotteryApplication.java    # 启动类
│       │   ├── controller\                # SsqController / UserBetController / SystemController
│       │   ├── service\                   # SsqService + UserBetService (接口 + impl)
│       │   ├── mapper\                    # SsqDrawMapper / UserBetMapper / SsqPrizeRuleMapper
│       │   ├── entity\                    # SsqDraw / UserBet / SsqPrizeRule
│       │   ├── dto\                       # ApiResult / PageResult / PrizeMatchResult / SsqDrawVO
│       │   ├── config\                    # CorsConfig / GlobalExceptionHandler
│       │   └── runner\                    # SsqHistoryImportRunner (启动自动导入 1591 期)
│       └── resources\
│           ├── application.yml            # MySQL 配置 (默认)
│           ├── application-h2.yml         # H2 内存数据库配置 (开发用)
│           ├── sql\schema.sql             # MySQL DDL
│           ├── sql\schema-h2.sql          # H2 兼容 DDL
│           ├── mapper\*.xml               # MyBatis XML 映射
│           └── data\ssq_history.json      # 1591 期历史数据
├── lottery-frontend\                      # Vue3 前端
│   ├── package.json                       # vue 3.4 / vue-router 4 / axios / element-plus 2.8 / echarts 5
│   ├── vite.config.js                     # Vite 配置 (port 5173, proxy /api -> localhost:8080)
│   ├── start-frontend.bat                 # 前端启动脚本
│   └── src\
│       ├── main.js / App.vue              # 入口 + 顶栏导航 + 路由过渡
│       ├── router\index.js               # 6 个路由 (hash 模式)
│       ├── styles\global.scss             # 号码球 / 玻璃卡片 / 暗色主题
│       ├── utils\request.js              # Axios 统一封装 + ElMessage 错误提示
│       ├── api\index.js                   # ssq / bet / sys 三大模块 API
│       ├── components\PickBalls.vue        # 通用选球组件
│       └── views\ (6 个功能页)
│           ├── Home.vue                   # 首页 Dashboard
│           ├── SsqSpiral.vue              # 双色球螺旋选号 (核心)
│           ├── SsqHistory.vue             # 历史开奖查询
│           ├── SsqMatch.vue              # 中奖分析
│           ├── Kl8Pick.vue               # 快乐8 选号
│           └── MyBets.vue                # 投注记录
├── node-portable\                         # 便携版 Node.js v20.17.0 (无需系统安装)
└── ssq_history.json                       # 原始历史数据备份 (1591 条)
```

---

## 启动步骤 (Windows)

### 方式一: H2 内存数据库 (推荐, 无需安装 MySQL)

> 最快启动方式, 适合开发调试和功能验证。数据存在内存中, 重启后清空 (但每次启动会自动从 JSON 重新导入 1591 期)。

**1. 启动后端**
```bat
cd lottery-backend
set SPRING_PROFILES_ACTIVE=h2
mvn spring-boot:run
```
或双击 `start-backend-h2.bat`

- 首次启动约 20~40 秒 (编译 + 下载依赖 + 导入 1591 期数据)
- 控制台看到 `Started LotteryApplication` 即成功
- 验证: 浏览器访问 `http://localhost:8080/api/system/health` 返回 `{"code":0,"data":{"status":"UP"}}`

**2. 启动前端**
```bat
cd lottery-frontend
npm install
npm run dev
```
- 打开 **http://localhost:5173** 即可访问
- `/api` 路径会被 Vite 反向代理到 `http://localhost:8080`

### 方式二: MySQL 数据库 (生产环境)

**1. 准备数据库**
```sql
CREATE DATABASE IF NOT EXISTS lottery_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 也可让 Spring Boot 首次启动时自动执行 schema.sql
```

`application.yml` 默认连接配置:
```yaml
url: jdbc:mysql://localhost:3306/lottery_db
username: root
password: 123456
```
请按实际环境修改。

**2. 启动后端** (不加 h2 profile 即走默认 MySQL 配置)
```bat
cd lottery-backend
mvn spring-boot:run
```

**3. 启动前端** (同方式一)

---

## 功能清单

| 模块 | 功能 | 路由 |
|------|------|------|
| 首页 Dashboard | 统计信息、最新开奖、快捷入口卡片 | `/` |
| **双色球螺旋选号** | 阿基米德螺旋图 SVG 交互式选号 (红球 3.2 圈 + 蓝球 1.8 圈)；支持 单式/胆拖/复式；组合数与投注金额实时计算；随机选号；展开组合预览；对比最新开奖判奖；保存投注；**支持按指定期号加载开奖结果并高亮** —— 近 30 期下拉选择 / 手动输入期号 / URL 传参，命中红球金色脉动光环 + 虚线旋转环，蓝球金色描边 + 徽章，顶部实时显示命中 N 红 N 蓝及奖级 | `/ssq-spiral` |
| **双色球开奖历史** | 分页查询 1591 期历史；期号/号码/日期多维搜索；每行可弹窗模拟判奖；**每行有「生成螺旋图」按钮，点击跳转到螺旋页并自动加载该期开奖号码高亮** | `/ssq-history` |
| 中奖分析 | ECharts 双 Grid 布局: 红球频次柱状图 + 蓝球频次折线面积图；冷热号 Top6/Bottom6 动态透明度；单式/复式/胆拖 3 Tab 判奖带展开明细表格 | `/ssq-match` |
| 快乐8 选号 | 80 选 N (1~10) 玩法切换；复式展开 C(已选, pickType) 前 500 预览；本地模拟 20 开奖号判奖 | `/kl8` |
| 我的投注 | 投注 CRUD、按彩种类型筛选、单条/批量判奖 (按最新开奖) | `/my-bets` |

### 历史开奖 → 螺旋图跳转链路
1. 在 `/ssq-history` 页面表格中，每行末尾有「生成螺旋图」按钮
2. 点击后通过 `router.push({ path: '/ssq-spiral', query: { issue: row.issue } })` 跳转
3. 螺旋页 `onMounted` 读取 `route.query.issue`，调用 `ssq.byIssue(issue)` API 获取该期开奖数据
4. 命中的红球在螺旋图上显示金色脉动光环 + 虚线旋转环，蓝球显示金色描边 + 徽章
5. 顶部横幅实时计算当前选号与该期开奖的命中数和奖级

---

## 后端核心 API

| Method | 路径 | 说明 |
|--------|------|------|
| GET | `/api/system/health` | 健康检查 |
| GET | `/api/ssq/draws/latest` | 最新开奖 |
| GET | `/api/ssq/draws/issue/{issue}` | 按期号查询开奖 |
| GET | `/api/ssq/draws?pageNum=1&pageSize=20&keyword=&startDate=&endDate=` | 分页查询开奖 |
| GET | `/api/ssq/draws/count` | 统计总期数 |
| GET | `/api/ssq/pick/random` | 随机 1 注 |
| GET | `/api/ssq/pick/random/{count}` | 随机 N 注 |
| POST | `/api/ssq/pick/compound` | 复式选号 `{reds:[], blues:[]}` |
| POST | `/api/ssq/pick/dantuo` | 胆拖选号 `{dans:[], tuos:[], blues:[]}` |
| POST | `/api/ssq/match/single?issue=xxx` | 单式判奖 (不带 issue 则用最新) |
| POST | `/api/ssq/match/compound?issue=xxx` | 复式判奖 |
| POST | `/api/ssq/match/dantuo?issue=xxx` | 胆拖判奖 |
| GET | `/api/ssq/analysis/frequency?recent=100` | 最近 N 期号码出现频次 |
| POST | `/api/bet` | 保存投注 |
| PUT | `/api/bet` | 更新投注 |
| DELETE | `/api/bet/{id}` | 删除投注 |
| GET | `/api/bet/{id}` | 查询投注详情 |
| GET | `/api/bet?pageNum=1&pageSize=20` | 分页查询投注 |
| POST | `/api/bet/{id}/match?issue=xxx` | 指定投注判奖 (issue 为空=最新) |
| POST | `/api/bet/match-all-ssq` | 批量判奖 |

### API 统一返回格式
```json
{
  "code": 0,        // 0=成功, 非0=错误
  "msg": "success",
  "data": { ... }   // 业务数据
}
```

### 判奖返回结构 (PrizeMatchResult)
```json
{
  "levelName": "一等奖",
  "levelCode": "1",
  "redHit": 6,
  "blueHit": true,
  "prizeAmount": null,    // 一/二等奖为浮动奖金, 返回 null
  "won": true,
  "totalBets": 1,
  "wonBets": 1,
  "totalPrize": 0,
  "details": [           // 每注明细 (复式/胆拖会返回多注)
    { "reds": [...], "blue": "09", "redHit": 6, "blueHit": true, "levelName": "一等奖", "prizeAmount": 0 }
  ]
}
```

---

## 前端特性
- **暗色玻璃质感 UI**: SCSS 变量 + 渐变 + `backdrop-filter` 实现现代风格
- **阿基米德螺旋选号图**: 纯 SVG + 数学公式 `x = center + r * cos(θ), y = center + r * sin(θ)` 动态生成节点
  - 红球: 33 个节点沿 3.2 圈螺旋分布, 620×620 响应式 SVG
  - 蓝球: 16 个节点沿 1.8 圈螺旋分布, 320×320 独立小螺旋
- **开奖高亮动画**: 命中红球金色脉动光环 (radialGradient + scale 呼吸) + 虚线旋转环 (stroke-dashoffset 动画), 命中蓝球金色描边 + 徽章
- **ECharts 5**: 冷热号频率柱状图 + 折线面积曲线 dual-grid 布局
- **Axios 统一封装**: 自动解包 `{code, data, msg}`, 错误消息统一走 Element Plus `ElMessage`
- **Element Plus 中文本地化**: 全局 `zh-cn`

---

## 常见问题

### 端口冲突
- 后端默认 8080, 前端默认 5173
- 后端: 修改 `application.yml` 中 `server.port`
- 前端: 修改 `vite.config.js` 中 `server.port`, 同时更新 proxy target

### Lombok / JDK 版本兼容
- `pom.xml` 已锁定 Lombok 1.18.44, 兼容 JDK 17/21/24
- 若切换到 JDK 17/21 可恢复 Spring Boot 默认管理的 Lombok 版本

### MySQL 连接失败
- 确认 mysqld 服务已启动
- 确认 `application.yml` 中用户名密码正确
- 确认已执行 `CREATE DATABASE lottery_db`
- 或改用 H2 模式: `set SPRING_PROFILES_ACTIVE=h2`

### H2 模式数据丢失
- H2 内存数据库重启后数据清空, 但每次启动会自动从 `data/ssq_history.json` 重新导入 1591 期
- 如需持久化, 请使用 MySQL 模式

### 首次启动数据未导入
- 检查 `target/classes/data/ssq_history.json` 是否存在 (Maven 编译后会复制到 target)
- 检查数据库是否已有同期号记录 (唯一键冲突会自动跳过)
- 查看 `SsqHistoryImportRunner` 的日志输出

### Vite HMR 报错
- 热更新时偶发 `Cannot read properties of null (reading 'nextSibling')` 是 Vue runtime 在模块替换时 reconcile 阶段的已知行为
- 按 F5 冷刷新页面即可解决, 不影响代码功能

### Node.js 未安装
- 项目根目录下 `node-portable\` 包含便携版 Node.js v20.17.0
- 启动前端时将路径加入 PATH: `set PATH=D:\traeworkspace\node-portable\node-v20.17.0-win-x64;%PATH%`
- 或系统安装 Node.js LTS: `winget install OpenJS.NodeJS.LTS`
