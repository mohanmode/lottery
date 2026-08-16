<template>
  <div class="home">
    <div class="hero glass-card">
      <div>
        <h1>智能选号与历史数据分析</h1>
        <p class="subtitle">基于 Spring Boot 3 + MyBatis + MySQL + Vue 3 技术栈，提供专业的双色球与快乐8选号、开奖历史、中奖判定服务</p>
      </div>
      <div class="stats" v-if="stats">
        <div class="stat"><div class="num">{{ stats.totalDraws }}</div><div class="lbl">累计开奖期数</div></div>
        <div class="stat"><div class="num">{{ stats.dateRange }}</div><div class="lbl">数据跨度</div></div>
        <div class="stat"><div class="num">{{ stats.latest ? stats.latest.issue : '-' }}</div><div class="lbl">最新开奖期号</div></div>
      </div>
    </div>

    <el-row :gutter="24" class="quick-cards">
      <el-col :span="8">
        <div class="card glass-card" @click="$router.push('/ssq-spiral')">
          <div class="icon"></div>
          <h3>双色球螺旋选号</h3>
          <p>阿基米德螺旋图交互式选号，支持随机/复式/胆拖，并实时计算组合数与成本</p>
          <el-button type="primary">立即选号 →</el-button>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card glass-card" @click="$router.push('/ssq-history')">
          <div class="icon"></div>
          <h3>开奖历史查询</h3>
          <p>10年历史开奖数据（1591期，2016-2026），支持期号/号码/日期多维检索</p>
          <el-button type="success">查看历史 →</el-button>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card glass-card" @click="$router.push('/ssq-match')">
          <div class="icon"></div>
          <h3>中奖判定 & 冷热分析</h3>
          <p>复式/胆拖/单式 完整判奖，冷热号码频率分析 ECharts 可视化</p>
          <el-button type="warning">开始分析 →</el-button>
        </div>
      </el-col>
    </el-row>

    <div class="latest-section glass-card mt30" v-if="latest">
      <div class="section-head">
        <h2>最新开奖结果 · {{ latest.issue }} 期</h2>
        <div class="head-right">
          <span class="date">{{ latest.drawDate }}</span>
          <el-button size="default" :loading="syncing" type="primary" @click="doSync">同步最新开奖</el-button>
          <el-button size="default" :loading="kl8Syncing" type="warning" @click="doSyncKl8">同步快乐8</el-button>
        </div>
      </div>
      <div class="balls">
        <span v-for="r in latest.reds" :key="r" class="ball red ball-lg">{{ r }}</span>
        <span class="plus">+</span>
        <span class="ball blue ball-lg">{{ latest.blue }}</span>
      </div>
      <div class="kl8-latest" v-if="kl8Latest">
        <div class="kl8-head">
          <span class="kl8-title">快乐8 · {{ kl8Latest.issue }} 期</span>
          <span class="kl8-date">{{ kl8Latest.drawDate }}</span>
        </div>
        <div class="balls kl8-balls">
          <span v-for="n in kl8Latest.numbers" :key="n" class="ball blue ball-sm">{{ n }}</span>
        </div>
      </div>
      <div class="sync-info" v-if="sync">
        <!-- 双色球同步状态 -->
        <div class="sync-section" v-if="sync.ssq">
          <div class="sync-row">
            <div>
              <span class="sync-dot" :class="sync.ssq.running?'running':(sync.ssq.lastResult?.success?'ok':'err')"></span>
              <span class="sync-label">双色球同步：{{ sync.ssq.lastAttemptTime || '暂无' }}</span>
            </div>
            <div class="sync-sched">{{ sync.ssq.scheduleDesc }}</div>
          </div>
          <div class="sync-result" v-if="sync.ssq.lastResult">
            <span>状态：</span>
            <span :class="sync.ssq.lastResult.success?'tag ok':'tag err'">{{ sync.ssq.lastResult.success ? '成功' : '失败' }}</span>
            <span class="sync-msg">{{ sync.ssq.lastResult.message }}</span>
            <span class="sync-latest" v-if="sync.ssq.lastResult.latestIssue">
              最新：期{{ sync.ssq.lastResult.latestIssue }} · {{ sync.ssq.lastResult.latestDrawDate }} ·
              <span class="mini-ball red" v-for="r in (sync.ssq.lastResult.latestRed||'').split(',')" :key="r">{{ r }}</span>
              <span class="mini-ball blue">{{ sync.ssq.lastResult.latestBlue }}</span>
            </span>
          </div>
        </div>
        <!-- 快乐8同步状态 -->
        <div class="sync-section" v-if="sync.kl8" style="margin-top:10px">
          <div class="sync-row">
            <div>
              <span class="sync-dot" :class="sync.kl8.running?'running':(sync.kl8.lastResult?.success?'ok':'err')"></span>
              <span class="sync-label">快乐8同步：{{ sync.kl8.lastAttemptTime || '暂无' }}</span>
            </div>
            <div class="sync-sched">{{ sync.kl8.scheduleDesc }}</div>
          </div>
          <div class="sync-result" v-if="sync.kl8.lastResult">
            <span>状态：</span>
            <span :class="sync.kl8.lastResult.success?'tag ok':'tag err'">{{ sync.kl8.lastResult.success ? '成功' : '失败' }}</span>
            <span class="sync-msg">{{ sync.kl8.lastResult.message }}</span>
            <span class="sync-latest" v-if="sync.kl8.lastResult.latestIssue">
              最新：期{{ sync.kl8.lastResult.latestIssue }} · {{ sync.kl8.lastResult.latestDrawDate }} ·
              <span class="mini-ball blue" v-for="n in (sync.kl8.lastResult.latestNumbers||'').split(',')" :key="n">{{ n }}</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { ssq, kl8, sys } from '@/api'
import { ElMessage } from 'element-plus'

const latest = ref(null)
const stats  = reactive({ totalDraws: 0, dateRange: '-', latest: null })
const sync = ref(null)
const syncing = ref(false)
const kl8Latest = ref(null)
const kl8Syncing = ref(false)

async function doSync() {
  syncing.value = true
  try {
    const r = await sys.syncSsq()
    ElMessage.success(r.message || '同步完成')
    latest.value = await ssq.latest()
    stats.latest = latest.value
    sync.value = await sys.syncStatus()
  } catch (e) {
    ElMessage.error('同步失败')
  } finally {
    syncing.value = false
  }
}

async function doSyncKl8() {
  kl8Syncing.value = true
  try {
    const r = await sys.syncKl8()
    ElMessage.success(r.message || '同步完成')
    kl8Latest.value = await kl8.latest()
    sync.value = await sys.syncStatus()
  } catch (e) {
    ElMessage.error('同步失败')
  } finally {
    kl8Syncing.value = false
  }
}

onMounted(async () => {
  latest.value = await ssq.latest()
  stats.latest = latest.value
  try {
    const total = await ssq.count()
    stats.totalDraws = total
    const firstPage = await ssq.list({ pageNum: 1, pageSize: 1 })
    stats.dateRange = `${firstPage.list[firstPage.list.length-1]?.drawDate || '-'} ~ ${latest.value?.drawDate || '-'}`
  } catch (e) { /* ignore */ }
  try { sync.value = await sys.syncStatus() } catch (e) { /* ignore */ }
  try { kl8Latest.value = await kl8.latest() } catch (e) { /* ignore */ }
})
</script>

<style lang="scss" scoped>
.home { max-width: 1400px; margin: 0 auto; }
.hero {
  display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(135deg, rgba(255,107,53,0.15), rgba(255,214,102,0.08));
  margin-bottom: 30px;
  h1 { font-size: 32px; margin: 0 0 10px;
       background: linear-gradient(45deg, #fff, #ffd666);
       -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
  .subtitle { color: rgba(255,255,255,0.7); line-height: 1.8; max-width: 620px; }
}
.stats { display: flex; gap: 40px;
  .stat { text-align: center;
    .num { font-size: 34px; font-weight: bold; color: #ff6b35; }
    .lbl { color: rgba(255,255,255,0.5); margin-top: 6px; }
  }
}
.quick-cards .card {
  text-align: center; cursor: pointer; transition: all 0.3s;
  &:hover { transform: translateY(-6px); border-color: #ff6b35; box-shadow: 0 10px 30px rgba(255,107,53,0.2); }
  .icon { font-size: 54px; margin-bottom: 12px; }
  h3 { font-size: 20px; margin: 0 0 10px; color: #fff; }
  p { color: rgba(255,255,255,0.6); line-height: 1.8; min-height: 54px; }
}
.mt30 { margin-top: 30px; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h2 { margin: 0; font-size: 22px; }
  .head-right { display: flex; gap: 14px; align-items: center; }
  .date { color: rgba(255,255,255,0.6); font-size: 15px; }
}
.balls { display: flex; align-items: center; gap: 14px; flex-wrap: wrap;
  .plus { font-size: 28px; font-weight: bold; color: rgba(255,255,255,0.3); margin: 0 8px; }
}
.sync-info {
  margin-top: 24px; padding-top: 18px;
  border-top: 1px dashed rgba(255,255,255,0.12);
}
.sync-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.sync-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; margin-right: 8px; vertical-align: middle;
  &.ok { background: #4caf50; box-shadow: 0 0 6px #4caf50; }
  &.err { background: #f44336; box-shadow: 0 0 6px #f44336; }
  &.running { background: #2196f3; animation: pulse 1s infinite; }
}
@keyframes pulse { 0%{opacity:.3} 50%{opacity:1} 100%{opacity:.3} }
.sync-label { color: rgba(255,255,255,0.75); font-size: 14px; }
.sync-sched { color: rgba(255,255,255,0.45); font-size: 12px; max-width: 680px; text-align: right; }
.sync-result { margin-top: 12px; color: rgba(255,255,255,0.7); font-size: 14px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.tag { padding: 2px 10px; border-radius: 999px; font-size: 12px;
  &.ok { background: rgba(76,175,80,0.15); color: #81c784; border: 1px solid rgba(76,175,80,0.3); }
  &.err { background: rgba(244,67,54,0.15); color: #ef9a9a; border: 1px solid rgba(244,67,54,0.3); }
}
.sync-msg { color: rgba(255,255,255,0.55); }
.sync-latest { color: rgba(255,255,255,0.65); display: flex; gap: 4px; align-items: center; }
.mini-ball { width: 22px; height: 22px; border-radius: 50%; display: inline-flex; justify-content: center; align-items: center; font-size: 11px; font-weight: bold; color: #fff;
  &.red { background: linear-gradient(135deg,#ff6b35,#e53935); }
  &.blue { background: linear-gradient(135deg,#4a90e2,#1976d2); }
}
.kl8-latest {
  margin-top: 18px; padding-top: 18px;
  border-top: 1px dashed rgba(255,255,255,0.12);
}
.kl8-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;
  .kl8-title { font-size: 16px; font-weight: bold; color: #66ccff; }
  .kl8-date { color: rgba(255,255,255,0.6); font-size: 14px; }
}
.kl8-balls { gap: 6px; }
</style>
