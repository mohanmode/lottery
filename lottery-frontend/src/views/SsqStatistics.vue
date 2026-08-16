<template>
  <div class="ssq-statistics" v-loading="loading" element-loading-text="正在分析数据..." element-loading-background="rgba(10,10,20,0.8)">
    <!-- 顶部控制条 -->
    <div class="glass-card control-bar">
      <div class="bar-left">
        <h2>双色球综合统计分析</h2>
        <span class="sub" v-if="stats">基于近 {{ stats.totalDraws }} 期数据 (请求 {{ stats.sampleSize }} 期)</span>
      </div>
      <div class="bar-right">
        <el-select v-model="recentPeriods" @change="loadStats" style="width:160px">
          <el-option label="近 30 期" :value="30"/>
          <el-option label="近 50 期" :value="50"/>
          <el-option label="近 100 期" :value="100"/>
          <el-option label="近 200 期" :value="200"/>
          <el-option label="近 500 期" :value="500"/>
          <el-option label="全部" :value="2000"/>
        </el-select>
        <el-button @click="loadStats" :loading="loading" type="primary">刷新</el-button>
      </div>
    </div>

    <!-- 核心指标 -->
    <el-row :gutter="16" v-if="stats" class="overview-row">
      <el-col :span="6">
        <div class="glass-card stat-card stat-orange">
          <div class="stat-icon">&#x1F4CA;</div>
          <div class="stat-body">
            <div class="stat-label">和值</div>
            <div class="stat-main">
              <span class="big">{{ stats.sumStats.avg }}</span>
              <span class="unit">平均</span>
            </div>
            <div class="stat-sub">{{ stats.sumStats.min }} ~ {{ stats.sumStats.max }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="glass-card stat-card stat-blue">
          <div class="stat-icon">&#x1F4CF;</div>
          <div class="stat-body">
            <div class="stat-label">跨度</div>
            <div class="stat-main">
              <span class="big">{{ stats.spanStats.avg }}</span>
              <span class="unit">平均</span>
            </div>
            <div class="stat-sub">{{ stats.spanStats.min }} ~ {{ stats.spanStats.max }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="glass-card stat-card stat-purple">
          <div class="stat-icon">&#x1F9EE;</div>
          <div class="stat-body">
            <div class="stat-label">AC 值</div>
            <div class="stat-main">
              <span class="big">{{ stats.acStats.avg }}</span>
              <span class="unit">平均</span>
            </div>
            <div class="stat-sub">分散程度指标</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="glass-card stat-card stat-green">
          <div class="stat-icon">&#x1F504;</div>
          <div class="stat-body">
            <div class="stat-label">连号概率</div>
            <div class="stat-main">
              <span class="big">{{ stats.consecutiveStats.consecutiveRate }}</span>
              <span class="unit">%</span>
            </div>
            <div class="stat-sub">{{ stats.consecutiveStats.hasConsecutiveCount }}/{{ stats.totalDraws }} 期含连号</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 分区1: 号码频次分析 -->
    <div class="section-header" v-if="stats"><span class="section-line"></span><h4>号码频次分析</h4><span class="section-line"></span></div>
    <el-row :gutter="16" v-if="stats" class="chart-row">
      <el-col :span="12">
        <div class="glass-card chart-card chart-important">
          <div class="chart-title"><span class="title-dot dot-red"></span><h3>红球出现频次</h3><span class="chart-badge red">33选6</span></div>
          <div ref="redFreqRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="glass-card chart-card chart-important">
          <div class="chart-title"><span class="title-dot dot-orange"></span><h3>红球遗漏值 (当前未出现期数)</h3></div>
          <div ref="redOmitRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 分区2: 蓝球分析 -->
    <div class="section-header" v-if="stats"><span class="section-line"></span><h4>蓝球分析</h4><span class="section-line"></span></div>
    <el-row :gutter="16" v-if="stats" class="chart-row">
      <el-col :span="12">
        <div class="glass-card chart-card">
          <div class="chart-title"><span class="title-dot dot-blue"></span><h3>蓝球频次 &amp; 遗漏值</h3><span class="chart-badge blue">16选1</span></div>
          <div ref="blueRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="glass-card chart-card">
          <div class="chart-title"><span class="title-dot dot-cyan"></span><h3>蓝球奇偶 / 大小分布</h3></div>
          <div ref="bluePieRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 分区3: 形态分布 -->
    <div class="section-header" v-if="stats"><span class="section-line"></span><h4>红球形态分布</h4><span class="section-line"></span></div>
    <el-row :gutter="16" v-if="stats" class="chart-row">
      <el-col :span="8">
        <div class="glass-card chart-card chart-pie">
          <div class="chart-title"><span class="title-dot dot-red"></span><h3>奇偶比分布</h3></div>
          <div ref="oddEvenRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="glass-card chart-card chart-pie">
          <div class="chart-title"><span class="title-dot dot-orange"></span><h3>大小比分布</h3></div>
          <div ref="bigSmallRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="glass-card chart-card chart-pie">
          <div class="chart-title"><span class="title-dot dot-purple"></span><h3>质合比分布</h3></div>
          <div ref="primeCompRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 分区4: 数值统计 -->
    <div class="section-header" v-if="stats"><span class="section-line"></span><h4>数值统计</h4><span class="section-line"></span></div>
    <el-row :gutter="16" v-if="stats" class="chart-row">
      <el-col :span="12">
        <div class="glass-card chart-card">
          <div class="chart-title"><span class="title-dot dot-blue"></span><h3>和值分布</h3></div>
          <div ref="sumRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="glass-card chart-card">
          <div class="chart-title"><span class="title-dot dot-purple"></span><h3>跨度分布</h3></div>
          <div ref="spanRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="16" v-if="stats" class="chart-row">
      <el-col :span="12">
        <div class="glass-card chart-card">
          <div class="chart-title"><span class="title-dot dot-orange"></span><h3>三区比 Top10</h3><span class="chart-badge gray">1-11 / 12-22 / 23-33</span></div>
          <div ref="zoneRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="glass-card chart-card">
          <div class="chart-title"><span class="title-dot dot-red"></span><h3>AC 值分布</h3></div>
          <div ref="acRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 分区5: 冷热排行与汇总 -->
    <div class="section-header" v-if="stats"><span class="section-line"></span><h4>冷热排行 &amp; 汇总</h4><span class="section-line"></span></div>
    <el-row :gutter="16" v-if="stats" class="chart-row">
      <el-col :span="6">
        <div class="glass-card rank-card rank-hot">
          <div class="chart-title"><span class="title-dot dot-red"></span><h3>红球热号 Top10</h3></div>
          <el-table :data="stats.redHotTop10" size="small" class="dark-table">
            <el-table-column label="号码" width="70"><template #default="{row}"><span class="ball red ball-sm">{{ row.code }}</span></template></el-table-column>
            <el-table-column label="频次" prop="freq" width="60"/>
            <el-table-column label="遗漏" prop="omit" width="60"/>
          </el-table>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="glass-card rank-card rank-cold">
          <div class="chart-title"><span class="title-dot dot-blue"></span><h3>红球冷号 Top10</h3></div>
          <el-table :data="stats.redColdTop10" size="small" class="dark-table">
            <el-table-column label="号码" width="70"><template #default="{row}"><span class="ball blue ball-sm">{{ row.code }}</span></template></el-table-column>
            <el-table-column label="频次" prop="freq" width="60"/>
            <el-table-column label="遗漏" prop="omit" width="60"/>
          </el-table>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="glass-card rank-card">
          <div class="chart-title"><span class="title-dot dot-cyan"></span><h3>蓝球热号 Top5</h3></div>
          <el-table :data="stats.blueHotTop5" size="small" class="dark-table">
            <el-table-column label="号码" width="70"><template #default="{row}"><span class="ball blue ball-sm">{{ row.code }}</span></template></el-table-column>
            <el-table-column label="频次" prop="freq" width="60"/>
            <el-table-column label="遗漏" prop="omit" width="60"/>
          </el-table>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="glass-card rank-card rank-summary">
          <div class="chart-title"><span class="title-dot dot-purple"></span><h3>重号 / 连号统计</h3></div>
          <div class="repeat-info">
            <div class="repeat-row"><span>含重号期数</span><b>{{ stats.repeatStats.hasRepeatCount }}</b></div>
            <div class="repeat-row"><span>重号总数</span><b>{{ stats.repeatStats.totalRepeatNums }}</b></div>
            <div class="repeat-row"><span>平均重号</span><b>{{ stats.repeatStats.avgRepeat }}</b></div>
            <div class="repeat-row"><span>连号组数</span><b>{{ stats.consecutiveStats.totalGroups }}</b></div>
            <div class="repeat-row"><span>无连号期数</span><b>{{ stats.consecutiveStats.noConsecutiveCount }}</b></div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { ssq } from '@/api'

const loading = ref(false)
const recentPeriods = ref(100)
const stats = ref(null)

const redFreqRef = ref(null)
const redOmitRef = ref(null)
const blueRef = ref(null)
const bluePieRef = ref(null)
const oddEvenRef = ref(null)
const bigSmallRef = ref(null)
const primeCompRef = ref(null)
const sumRef = ref(null)
const spanRef = ref(null)
const zoneRef = ref(null)
const acRef = ref(null)

const charts = []

const darkTheme = {
  textColor: 'rgba(255,255,255,0.75)',
  axisLineColor: 'rgba(255,255,255,0.15)',
  splitLineColor: 'rgba(255,255,255,0.06)',
  tooltipBg: 'rgba(20,20,35,0.95)',
}

function initCharts() {
  // 先销毁旧实例，避免重复
  charts.forEach(c => c && c.dispose())
  charts.length = 0
  const refs = [redFreqRef, redOmitRef, blueRef, bluePieRef, oddEvenRef, bigSmallRef, primeCompRef, sumRef, spanRef, zoneRef, acRef]
  refs.forEach(r => {
    if (r.value) charts.push(echarts.init(r.value))
  })
  if (!window.__resizeBound) {
    window.addEventListener('resize', resizeAll)
    window.__resizeBound = true
  }
}

function resizeAll() { charts.forEach(c => c && c.resize()) }

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  window.__resizeBound = false
  charts.forEach(c => c && c.dispose())
})

async function loadStats() {
  loading.value = true
  try {
    const data = await ssq.statistics(recentPeriods.value)
    stats.value = data
    await nextTick()
    // v-if="stats" 渲染后，DOM 才存在 chart-box，此时才能初始化 ECharts
    initCharts()
    await nextTick()
    renderCharts()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 饼图数据辅助：过滤0值、按value降序
function toPieData(mapObj) {
  if (!mapObj) return []
  return Object.entries(mapObj)
    .filter(([k,v]) => Number(v) > 0)
    .sort((a,b) => Number(b[1]) - Number(a[1]))
    .map(([k,v]) => ({ name: k, value: Number(v) }))
}
// 直方图辅助：过滤0值条目
function toHistData(arr, keyField='range', countField='count') {
  if (!Array.isArray(arr)) return []
  return arr.filter(h => Number(h[countField]) > 0)
}
// 排序号码映射：01~33 或 01~80 按数字升序
function sortNumberMap(mapObj) {
  if (!mapObj) return []
  return Object.entries(mapObj)
    .sort((a,b) => Number(a[0]) - Number(b[0]))
}

function renderCharts() {
  if (!stats.value) return
  const d = stats.value
  const pieColors = ['#ff4444','#ff8800','#ffcc00','#4a90e2','#66ccff','#9966ff','#aaaaaa','#ff9966','#cc99ff']

  // 红球频次 - 按号码排序
  const redFreqEntries = sortNumberMap(d.redFreq)
  setChart(0, {
    tooltip: { trigger: 'axis', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: redFreqEntries.map(e=>e[0]), axisLabel: { color: darkTheme.textColor, fontSize: 10, rotate: 45 }, axisLine: { lineStyle: { color: darkTheme.axisLineColor } } },
    yAxis: { type: 'value', axisLabel: { color: darkTheme.textColor }, splitLine: { lineStyle: { color: darkTheme.splitLineColor } } },
    series: [{ name: '频次', type: 'bar', data: redFreqEntries.map(e=>Number(e[1])),
      itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:'#ff6b35'},{offset:1,color:'#ff9966'}]) },
      markLine: { data: [{ type: 'average', name: '平均' }], lineStyle: { color: '#ffd666' }, label: { color: '#ffd666' } } }]
  })

  // 红球遗漏值 - 按号码排序
  const redOmitEntries = sortNumberMap(d.redOmit)
  setChart(1, {
    tooltip: { trigger: 'axis', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: redOmitEntries.map(e=>e[0]), axisLabel: { color: darkTheme.textColor, fontSize: 10, rotate: 45 }, axisLine: { lineStyle: { color: darkTheme.axisLineColor } } },
    yAxis: { type: 'value', axisLabel: { color: darkTheme.textColor }, splitLine: { lineStyle: { color: darkTheme.splitLineColor } } },
    series: [{ name: '遗漏期数', type: 'bar', data: redOmitEntries.map(e=>Number(e[1])),
      itemStyle: { color: function(p) {
        const v = p.value
        if (v >= 30) return '#ff4444'
        if (v >= 15) return '#ff8800'
        if (v >= 5) return '#ffcc00'
        return '#4a90e2'
      } } }]
  })

  // 蓝球频次 + 遗漏
  const blueFreqEntries = sortNumberMap(d.blueFreq)
  const blueOmitEntries = sortNumberMap(d.blueOmit)
  setChart(2, {
    tooltip: { trigger: 'axis', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    legend: { data: ['频次', '遗漏值'], textStyle: { color: darkTheme.textColor }, top: 0 },
    grid: { left: 50, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: blueFreqEntries.map(e=>e[0]), axisLabel: { color: darkTheme.textColor }, axisLine: { lineStyle: { color: darkTheme.axisLineColor } } },
    yAxis: [
      { type: 'value', name: '频次', axisLabel: { color: darkTheme.textColor }, splitLine: { lineStyle: { color: darkTheme.splitLineColor } } },
      { type: 'value', name: '遗漏', axisLabel: { color: darkTheme.textColor }, splitLine: { show: false } }
    ],
    series: [
      { name: '频次', type: 'bar', data: blueFreqEntries.map(e=>Number(e[1])), itemStyle: { color: '#4a90e2' } },
      { name: '遗漏值', type: 'line', yAxisIndex: 1, data: blueOmitEntries.map(e=>Number(e[1])), itemStyle: { color: '#ff6b35' }, smooth: true, areaStyle: { color: 'rgba(255,107,53,0.1)' } }
    ]
  })

  // 蓝球奇偶/大小 - 空值保护
  const ba = d.blueAnalysis || {}
  setChart(3, {
    tooltip: { trigger: 'item', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    legend: { textStyle: { color: darkTheme.textColor }, bottom: 0 },
    series: [
      { name: '奇偶', type: 'pie', radius: ['20%','40%'], center: ['25%','45%'],
        data: [
          {name:'奇数', value:Number(ba.oddCount)||0, itemStyle:{color:'#ff6b35'}},
          {name:'偶数', value:Number(ba.evenCount)||0, itemStyle:{color:'#4a90e2'}}
        ].filter(x=>x.value>0),
        label: { color: darkTheme.textColor } },
      { name: '大小', type: 'pie', radius: ['20%','40%'], center: ['75%','45%'],
        data: [
          {name:'大号(9-16)', value:Number(ba.bigCount)||0, itemStyle:{color:'#ffd666'}},
          {name:'小号(1-8)', value:Number(ba.smallCount)||0, itemStyle:{color:'#66ccff'}}
        ].filter(x=>x.value>0),
        label: { color: darkTheme.textColor } }
    ]
  })

  // 奇偶比 - 过滤0值+排序
  const oddEvenData = toPieData(d.oddEvenDist)
  setChart(4, {
    tooltip: { trigger: 'item', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    legend: { orient: 'vertical', right: 0, top: 'center', textStyle: { color: darkTheme.textColor, fontSize: 11 } },
    series: [{ name: '奇偶比', type: 'pie', radius: ['30%','60%'], center: ['40%','50%'],
      data: oddEvenData,
      label: { color: darkTheme.textColor },
      itemStyle: { color: function(p) { return pieColors[p.dataIndex % pieColors.length] } } }]
  })

  // 大小比
  const bigSmallData = toPieData(d.bigSmallDist)
  setChart(5, {
    tooltip: { trigger: 'item', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    legend: { orient: 'vertical', right: 0, top: 'center', textStyle: { color: darkTheme.textColor, fontSize: 11 } },
    series: [{ name: '大小比', type: 'pie', radius: ['30%','60%'], center: ['40%','50%'],
      data: bigSmallData,
      label: { color: darkTheme.textColor },
      itemStyle: { color: function(p) { return pieColors[p.dataIndex % pieColors.length] } } }]
  })

  // 质合比
  const primeCompData = toPieData(d.primeCompDist)
  setChart(6, {
    tooltip: { trigger: 'item', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    legend: { orient: 'vertical', right: 0, top: 'center', textStyle: { color: darkTheme.textColor, fontSize: 11 } },
    series: [{ name: '质合比', type: 'pie', radius: ['30%','60%'], center: ['40%','50%'],
      data: primeCompData,
      label: { color: darkTheme.textColor },
      itemStyle: { color: function(p) { return pieColors[p.dataIndex % pieColors.length] } } }]
  })

  // 和值分布 - 过滤0值
  const sumHist = toHistData(d.sumStats && d.sumStats.histogram, 'range', 'count')
  setChart(7, {
    tooltip: { trigger: 'axis', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: sumHist.map(h=>h.range), axisLabel: { color: darkTheme.textColor, rotate: 30 }, axisLine: { lineStyle: { color: darkTheme.axisLineColor } } },
    yAxis: { type: 'value', axisLabel: { color: darkTheme.textColor }, splitLine: { lineStyle: { color: darkTheme.splitLineColor } } },
    series: [{ name: '期数', type: 'bar', data: sumHist.map(h=>Number(h.count)),
      itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:'#4a90e2'},{offset:1,color:'#66ccff'}]) },
      markLine: { data: [{ type: 'average', name: '平均' }], lineStyle: { color: '#ffd666' } } }]
  })

  // 跨度分布 - 过滤0值
  const spanHist = toHistData(d.spanStats && d.spanStats.histogram, 'range', 'count')
  setChart(8, {
    tooltip: { trigger: 'axis', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: spanHist.map(h=>h.range), axisLabel: { color: darkTheme.textColor, rotate: 30 }, axisLine: { lineStyle: { color: darkTheme.axisLineColor } } },
    yAxis: { type: 'value', axisLabel: { color: darkTheme.textColor }, splitLine: { lineStyle: { color: darkTheme.splitLineColor } } },
    series: [{ name: '期数', type: 'bar', data: spanHist.map(h=>Number(h.count)),
      itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:'#9966ff'},{offset:1,color:'#cc99ff'}]) },
      markLine: { data: [{ type: 'average', name: '平均' }], lineStyle: { color: '#ffd666' } } }]
  })

  // 三区比 Top10 - 过滤count=0
  const zoneData = Array.isArray(d.zoneDistTop10)
    ? d.zoneDistTop10.filter(z => Number(z.count) > 0).slice(0,10)
    : []
  setChart(9, {
    tooltip: { trigger: 'axis', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    grid: { left: 60, right: 30, top: 30, bottom: 40 },
    xAxis: { type: 'value', axisLabel: { color: darkTheme.textColor }, splitLine: { lineStyle: { color: darkTheme.splitLineColor } } },
    yAxis: { type: 'category', data: zoneData.map(z=>z.ratio), axisLabel: { color: darkTheme.textColor }, axisLine: { lineStyle: { color: darkTheme.axisLineColor } } },
    series: [{ name: '出现次数', type: 'bar', data: zoneData.map(z=>Number(z.count)),
      itemStyle: { color: new echarts.graphic.LinearGradient(1,0,0,0, [{offset:0,color:'#ff6b35'},{offset:1,color:'#ffd666'}]) },
      label: { show: true, position: 'right', color: darkTheme.textColor } }]
  })

  // AC 值 - 过滤0值
  const acHist = toHistData(d.acStats && d.acStats.histogram, 'ac', 'count')
  setChart(10, {
    tooltip: { trigger: 'axis', backgroundColor: darkTheme.tooltipBg, textStyle: { color: '#fff' } },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: acHist.map(h=>h.ac), axisLabel: { color: darkTheme.textColor }, axisLine: { lineStyle: { color: darkTheme.axisLineColor } } },
    yAxis: { type: 'value', axisLabel: { color: darkTheme.textColor }, splitLine: { lineStyle: { color: darkTheme.splitLineColor } } },
    series: [{ name: '期数', type: 'bar', data: acHist.map(h=>Number(h.count)),
      itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:'#ff4444'},{offset:1,color:'#ff8866'}]) },
      markLine: { data: [{ type: 'average', name: '平均' }], lineStyle: { color: '#ffd666' } } }]
  })
}

function setChart(idx, option) {
  if (charts[idx]) charts[idx].setOption(option, true)
}

onMounted(async () => {
  await loadStats()
})
</script>

<style lang="scss" scoped>
.ssq-statistics { max-width: 1700px; margin: 0 auto; min-height: 200px; }

/* 控制条 */
.control-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; padding: 18px 28px; flex-wrap: wrap; gap: 12px;
  background: linear-gradient(135deg, rgba(30,30,55,0.7), rgba(20,20,40,0.5));
  border: 1px solid rgba(255,255,255,0.08);
  .bar-left { h2 { margin: 0 0 4px; font-size: 24px; font-weight: 700;
    background: linear-gradient(90deg, #fff, #ffd666); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    .sub { color: rgba(255,255,255,0.45); font-size: 13px; } }
  .bar-right { display: flex; gap: 8px; }
}

/* 概览卡片 */
.overview-row { margin-bottom: 20px; }
.stat-card {
  display: flex; align-items: center; gap: 16px; padding: 20px 24px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  &:hover { transform: translateY(-4px); box-shadow: 0 8px 32px rgba(0,0,0,0.3); }
  .stat-icon { font-size: 28px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center;
    border-radius: 12px; flex-shrink: 0; }
  .stat-body { flex: 1; }
  .stat-label { color: rgba(255,255,255,0.5); font-size: 13px; margin-bottom: 6px; letter-spacing: 1px; }
  .stat-main { display: flex; align-items: baseline; gap: 6px;
    .big { font-size: 36px; font-weight: 700; line-height: 1; }
    .unit { color: rgba(255,255,255,0.4); font-size: 13px; } }
  .stat-sub { color: rgba(255,255,255,0.35); font-size: 12px; margin-top: 6px; }
}
.stat-orange { .stat-icon { background: rgba(255,107,53,0.15); } .big { color: #ff8c42; } }
.stat-blue   { .stat-icon { background: rgba(74,144,226,0.15); } .big { color: #4a90e2; } }
.stat-purple { .stat-icon { background: rgba(153,102,255,0.15); } .big { color: #9966ff; } }
.stat-green  { .stat-icon { background: rgba(102,204,102,0.15); } .big { color: #66cc66; } }

/* 分区标题 */
.section-header {
  display: flex; align-items: center; gap: 16px; margin: 24px 0 16px;
  h4 { margin: 0; font-size: 15px; color: rgba(255,255,255,0.6); white-space: nowrap; letter-spacing: 2px; }
  .section-line { flex: 1; height: 1px;
    background: linear-gradient(90deg, rgba(255,255,255,0.15), rgba(255,255,255,0.02)); }
}

/* 图表卡片 */
.chart-row { margin-bottom: 16px; }
.chart-card {
  padding: 18px 22px; transition: transform 0.3s ease, box-shadow 0.3s ease;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 24px rgba(0,0,0,0.2); }
  .chart-title { display: flex; align-items: center; gap: 8px; margin-bottom: 14px;
    h3 { margin: 0; font-size: 16px; color: rgba(255,255,255,0.9); } }
  .chart-box { width: 100%; height: 320px; }
}
.chart-important {
  border: 1px solid rgba(255,107,53,0.12);
  background: linear-gradient(135deg, rgba(30,25,45,0.6), rgba(20,18,35,0.4));
}
.chart-pie .chart-box { height: 280px; }

/* 标题色点 */
.title-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot-red { background: #ff4444; box-shadow: 0 0 8px rgba(255,68,68,0.5); }
.dot-orange { background: #ff8800; box-shadow: 0 0 8px rgba(255,136,0,0.5); }
.dot-blue { background: #4a90e2; box-shadow: 0 0 8px rgba(74,144,226,0.5); }
.dot-cyan { background: #66ccff; box-shadow: 0 0 8px rgba(102,204,255,0.5); }
.dot-purple { background: #9966ff; box-shadow: 0 0 8px rgba(153,102,255,0.5); }

/* 图表标签 */
.chart-badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; margin-left: auto;
  background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.4); }
.chart-badge.red { background: rgba(255,68,68,0.12); color: #ff6666; }
.chart-badge.blue { background: rgba(74,144,226,0.12); color: #5aa3f0; }
.chart-badge.gray { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.35); }

/* 排行卡片 */
.rank-card {
  padding: 18px 22px;
  .chart-title { display: flex; align-items: center; gap: 8px; margin-bottom: 14px;
    h3 { margin: 0; font-size: 16px; color: rgba(255,255,255,0.9); } }
}
.rank-hot { border-left: 3px solid rgba(255,68,68,0.4); }
.rank-cold { border-left: 3px solid rgba(74,144,226,0.4); }
.rank-summary { border-left: 3px solid rgba(153,102,255,0.4); }

/* 重号统计 */
.repeat-info {
  .repeat-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0;
    border-bottom: 1px solid rgba(255,255,255,0.06);
    &:last-child { border-bottom: none; }
    span { color: rgba(255,255,255,0.55); font-size: 14px; }
    b { color: #ffd666; font-size: 18px; font-weight: 600; }
  }
}

/* 暗色表格 */
.dark-table :deep(.el-table) {
  background: transparent; --el-table-border-color: rgba(255,255,255,0.08);
  --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent;
  --el-table-text-color: rgba(255,255,255,0.9); --el-table-header-text-color: #ffd666;
  td, th { background: transparent !important; border-bottom: 1px solid rgba(255,255,255,0.04) !important; }
  tr.el-table__row:hover > td { background: rgba(255,255,255,0.05) !important; }
  .el-table__header th { font-weight: 600; font-size: 13px; }
}
</style>
