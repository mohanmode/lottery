<template>
  <div class="ssq-match">
    <el-row :gutter="24">
      <!-- 左：冷热号频率分析 -->
      <el-col :span="12">
        <div class="glass-card">
          <div class="panel-head">
            <h2>冷热号码频率分析</h2>
            <el-input-number v-model="recent" :min="10" :max="1500" :step="10" size="default" @change="loadFreq"/>
          </div>
          <div ref="chartRef" style="width:100%;height:420px"></div>
          <div class="freq-summary" v-if="freq">
            <div>
              <h4>最热红球 (出现次数)</h4>
              <span v-for="r in topReds" :key="r.n" class="ball red"
                    :style="{opacity: 0.5 + 0.5*(r.c/maxRedFreq)}">{{ r.n }}<sub>{{ r.c }}</sub></span>
            </div>
            <div style="margin-top:12px">
              <h4>最冷红球 (出现次数)</h4>
              <span v-for="r in bottomReds" :key="r.n" class="ball gray"
                    :style="{opacity: 0.4 + 0.6*(r.c/minRedFreq+0.2)}">{{ r.n }}<sub>{{ r.c }}</sub></span>
            </div>
            <div style="margin-top:12px">
              <h4>蓝球频率</h4>
              <span v-for="b in sortedBlues" :key="b.n" class="ball blue"
                    :style="{opacity: 0.5 + 0.5*(b.c/maxBlueFreq)}">{{ b.n }}<sub>{{ b.c }}</sub></span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右：批量判奖入口 -->
      <el-col :span="12">
        <div class="glass-card">
          <div class="panel-head">
            <h2>选号中奖判定</h2>
            <el-select v-model="targetIssue" placeholder="选择开奖期号" size="default" filterable style="width:180px">
              <el-option v-for="d in issueList" :key="d.issue"
                         :label="`${d.issue} (${d.drawDate})`" :value="d.issue" />
              <el-option label="使用最新开奖" value="LATEST" />
            </el-select>
          </div>

          <el-tabs v-model="tab" class="dark-tabs">
            <el-tab-pane label="单式判奖" name="single">
              <PickBalls v-model:reds="sReds" v-model:blues="sBlues" :red-limit="6" :blue-limit="1" />
              <el-button type="primary" size="large" style="width:100%;margin-top:12px" @click="runMatch('single')" :loading="loading">判奖</el-button>
            </el-tab-pane>
            <el-tab-pane label="复式判奖" name="compound">
              <PickBalls v-model:reds="cReds" v-model:blues="cBlues" :red-min="6" :blue-min="1" />
              <div class="cc" v-if="cCombo>0">
                组合数 <b>{{ cCombo }}</b> · 投注 <b>¥{{ (cCombo*2).toLocaleString() }}</b>
                <span v-if="cCombo>500" style="color:#ff6b35">(仅计算前500注，详情接口返回展开)</span>
              </div>
              <el-button type="primary" size="large" style="width:100%;margin-top:12px" @click="runMatch('compound')" :loading="loading">判奖</el-button>
            </el-tab-pane>
            <el-tab-pane label="胆拖判奖" name="dantuo">
              <div class="pick-section">
                <label>红球胆码 ({{dDans.size}}/5)</label>
                <PickBalls v-model:reds="dDans" v-model:blues="fakeB" :red-limit="5" :blue-limit="0" :hide-blue="true"/>
              </div>
              <div class="pick-section">
                <label>红球拖码 ({{dTuos.size}})</label>
                <PickBalls v-model:reds="dTuos" v-model:blues="fakeB" :red-min="1" :blue-limit="0" :hide-blue="true"
                           :exclude-reds="dDansSet"/>
              </div>
              <div class="pick-section">
                <label>蓝球 ({{dBlues.size}})</label>
                <PickBalls v-model:reds="fakeR" v-model:blues="dBlues" :red-limit="0" :hide-red="true"/>
              </div>
              <div class="cc" v-if="dCombo>0">
                组合数 <b>{{ dCombo }}</b> · 投注 <b>¥{{ (dCombo*2).toLocaleString() }}</b>
              </div>
              <el-button type="primary" size="large" style="width:100%;margin-top:12px" @click="runMatch('dantuo')" :loading="loading">判奖</el-button>
            </el-tab-pane>
          </el-tabs>

          <el-divider v-if="result">判奖结果</el-divider>
          <div v-if="result" class="result-box">
            <el-descriptions :column="2" border size="default" class="dark-desc">
              <el-descriptions-item label="开奖期号">{{ targetIssue==='LATEST' ? '(最新)' : targetIssue }}</el-descriptions-item>
              <el-descriptions-item label="总注数">{{ result.totalBets }}</el-descriptions-item>
              <el-descriptions-item label="中奖等级">
                <el-tag :type="result.won?'danger':'info'" size="default" effect="dark">{{ result.levelName }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="中奖注数">
                <b style="color:#ffd666;font-size:16px">{{ result.wonBets ?? (result.won?1:0) }}</b>
              </el-descriptions-item>
              <el-descriptions-item label="单注命中" v-if="result.totalBets===1">
                红 <b>{{ result.redHit }}</b> · 蓝 <b :style="{color:result.blueHit?'#4a90e2':'#aaa'}">{{ result.blueHit?'✓':'✗' }}</b>
              </el-descriptions-item>
              <el-descriptions-item label="累计奖金" :span="2">
                <b style="color:#ff6b35;font-size:20px">¥ {{ Number(result.totalPrize ?? result.prizeAmount ?? 0).toLocaleString() }}</b>
              </el-descriptions-item>
            </el-descriptions>
            <div v-if="result.details && result.details.length" class="mt20">
              <h4>明细 (预览前50条)</h4>
              <el-table :data="result.details.slice(0,50)" size="small" max-height="280" class="dark-table">
                <el-table-column label="#" type="index" width="50"/>
                <el-table-column label="红球">
                  <template #default="{row}">
                    <span v-for="r in row.reds" :key="r" class="ball red ball-sm" style="margin-right:3px">{{ r }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="蓝" width="50"><template #default="{row}"><span class="ball blue ball-sm">{{row.blue}}</span></template></el-table-column>
                <el-table-column label="命中" width="90">
                  <template #default="{row}">{{row.redHit}}红 + {{row.blueHit?'1':'0'}}蓝</template>
                </el-table-column>
                <el-table-column label="奖项">
                  <template #default="{row}">
                    <el-tag v-if="row.levelName!=='未中奖'" type="warning" effect="dark">{{ row.levelName }}</el-tag>
                    <el-tag v-else size="small">{{ row.levelName }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="奖金" prop="prizeAmount" width="100"/>
              </el-table>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { ssq } from '@/api'
import PickBalls from '@/components/PickBalls.vue'
import { ElMessage } from 'element-plus'

// ================= 频率图 =================
const recent = ref(100)
const freq = ref(null)
const chartRef = ref(null)
let chart = null
const issueList = ref([])

const topReds    = computed(() => freq.value ? Object.entries(freq.value.red).map(([n,c])=>({n,c})).sort((a,b)=>b.c-a.c).slice(0,6) : [])
const bottomReds = computed(() => freq.value ? Object.entries(freq.value.red).map(([n,c])=>({n,c})).sort((a,b)=>a.c-b.c).slice(0,6) : [])
const sortedBlues = computed(() => freq.value ? Object.entries(freq.value.blue).map(([n,c])=>({n,c})).sort((a,b)=>b.c-a.c) : [])
const maxRedFreq  = computed(() => Math.max(1, ...topReds.value.map(x=>x.c)))
const minRedFreq  = computed(() => Math.max(1, ...bottomReds.value.map(x=>x.c)))
const maxBlueFreq = computed(() => Math.max(1, ...sortedBlues.value.map(x=>x.c)))

async function loadFreq() {
  freq.value = await ssq.frequency(recent.value)
  await nextTick()
  renderChart()
}
function renderChart() {
  if (!chartRef.value) return
  chart = chart || echarts.init(chartRef.value)
  const redNames = Object.keys(freq.value.red).sort()
  const redVals = redNames.map(n => freq.value.red[n])
  const blueNames = Object.keys(freq.value.blue).sort()
  const blueVals = blueNames.map(n => freq.value.blue[n])
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { data: ['红球出现频次','蓝球出现频次'], textStyle: { color: '#ddd' }, top: 0 },
    grid: { left: 50, right: 30, top: 40, bottom: 40 },
    xAxis: [
      { type: 'category', data: redNames, axisLabel: { color: '#fff' }, name: '红球号码', nameTextStyle: {color:'#ffd666'} },
      { type: 'category', data: blueNames, axisLabel: { color: '#fff' }, gridIndex: 1, name: '蓝球号码', nameTextStyle: {color:'#4a90e2'} }
    ],
    yAxis: [
      { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } } },
      { type: 'value', gridIndex: 1, axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } } }
    ],
    series: [
      { name: '红球出现频次', type: 'bar', data: redVals, barWidth: '70%',
        itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[
          {offset:0,color:'#ff6b35'},{offset:1,color:'#e63946'}]) },
        markLine: { silent: true, data: [{ type: 'average', name: '平均值', lineStyle: { color: '#ffd666' } }],
                  label: { color: '#333' } }
      },
      { name: '蓝球出现频次', type: 'line', xAxisIndex: 1, yAxisIndex: 1, smooth: true,
        data: blueVals, lineStyle: { color: '#4a90e2', width: 3 },
        symbolSize: 10, itemStyle: { color: '#4a90e2', borderColor: '#fff', borderWidth: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[
          {offset:0,color:'rgba(74,144,226,0.5)'},{offset:1,color:'rgba(74,144,226,0)'}]) }
      }
    ],
    grid: [
      { left: 50, right: 30, top: 40, height: '36%' },
      { left: 50, right: 30, top: '60%', height: '30%' }
    ]
  }, true)
  chart.resize()
  window.addEventListener('resize', () => chart && chart.resize())
}

// ================= 判奖 =================
const targetIssue = ref('LATEST')
const tab = ref('single')
const loading = ref(false)
const result = ref(null)
// 单式
const sReds = reactive(new Set()); const sBlues = reactive(new Set())
// 复式
const cReds = reactive(new Set()); const cBlues = reactive(new Set())
// 胆拖
const dDans = reactive(new Set()); const dTuos = reactive(new Set()); const dBlues = reactive(new Set())
const fakeR = reactive(new Set()); const fakeB = reactive(new Set())
const dDansSet = computed(() => new Set(dDans))

function C(n, k) {
  if (k < 0 || k > n) return 0
  k = Math.min(k, n - k)
  let r = 1n; for (let i = 1; i <= k; i++) r = r * BigInt(n - k + i) / BigInt(i)
  return Number(r)
}
const cCombo = computed(() => C(cReds.size, 6) * Math.max(1, cBlues.size))
const dCombo = computed(() => C(dTuos.size, 6 - dDans.size) * Math.max(1, dBlues.size))

async function runMatch(type) {
  loading.value = true; result.value = null
  try {
    const issue = targetIssue.value === 'LATEST' ? null : targetIssue.value
    if (type === 'single') {
      if (sReds.size !== 6 || sBlues.size < 1) return ElMessage.warning('请选6个红球 + 1个蓝球')
      result.value = await ssq.matchSingle({ reds: [...sReds], blue: [...sBlues][0] }, issue)
    } else if (type === 'compound') {
      if (cCombo.value <= 0) return ElMessage.warning('复式选号不足 (≥6红 + ≥1蓝)')
      result.value = await ssq.matchCompound({ reds: [...cReds], blues: [...cBlues] }, issue)
    } else {
      if (dCombo.value <= 0) return ElMessage.warning('胆拖选号不足')
      result.value = await ssq.matchDantuo({ dans: [...dDans], tuos: [...dTuos], blues: [...dBlues] }, issue)
    }
  } finally { loading.value = false }
}

// 拉取期号列表供下拉
async function loadIssues() {
  try {
    const data = await ssq.list({ pageNum: 1, pageSize: 100 })
    issueList.value = [{ issue: 'LATEST', drawDate: '' }, ...data.list]
  } catch(e) {}
}

onMounted(async () => {
  await loadFreq(); await loadIssues()
})
watch(recent, loadFreq)
</script>

<style lang="scss" scoped>
.ssq-match { max-width: 1700px; margin: 0 auto; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h2 { margin: 0; font-size: 20px; }
}
.freq-summary {
  margin-top: 20px; padding: 16px; border-radius: 10px;
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06);
  h4 { margin: 0 0 8px; color: rgba(255,255,255,0.8); font-size: 14px; }
  .ball { margin-right: 5px; margin-bottom: 5px; position: relative;
    sub { position:absolute; bottom:-6px; right:-1px; font-size: 10px; color:#fff; opacity:0.8; background:rgba(0,0,0,0.6); border-radius:3px; padding:0 3px;}
  }
}
.pick-section { margin-bottom: 14px; label { display:block; margin-bottom:6px; color:rgba(255,255,255,0.7); font-size:13px; } }
.cc { margin-top: 10px; padding: 10px 14px; background: rgba(255,214,102,0.08); border-radius: 8px;
  border: 1px dashed rgba(255,214,102,0.3);
  b { color: #ffd666; }
}
.mt20 { margin-top: 18px; }
.result-box { padding: 10px 0; }
.dark-tabs :deep(.el-tabs) { --el-tabs-header-border-color: rgba(255,255,255,0.1);
  .el-tabs__item { color: rgba(255,255,255,0.65); }
  .el-tabs__item.is-active { color: #ff6b35; }
  .el-tabs__active-bar { background-color: #ff6b35; }
}
.dark-desc :deep(.el-descriptions) {
  --el-descriptions-bg-color: transparent; --el-descriptions-table-border: rgba(255,255,255,0.1);
  --el-descriptions-item-label-bg-color: rgba(255,255,255,0.04);
  .el-descriptions__label, .el-descriptions__content { color: #fff; }
}
.dark-table :deep(.el-table) { --el-table-header-bg-color: rgba(255,107,53,0.15);
  --el-table-text-color: #eee; --el-table-border-color: rgba(255,255,255,0.08);
  background: transparent; td,th { background: transparent !important; }
  tr:hover > td { background: rgba(255,255,255,0.04) !important; }
}
</style>
