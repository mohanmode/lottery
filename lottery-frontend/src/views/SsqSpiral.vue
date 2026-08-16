<template>
  <div class="ssq-spiral">
    <el-row :gutter="24">
      <!-- 左：选号区 -->
      <el-col :span="16">
        <div class="glass-card">
          <div class="panel-head">
            <div class="head-left">
              <h2>双色球 · 号码选择</h2>
              <div class="period-tools">
                <el-select v-model="selIssue" placeholder="选择近30期开奖" filterable
                           style="width:230px" size="default" @change="loadIssue(selIssue)">
                  <el-option v-for="d in recentIssues" :key="d.issue"
                             :label="`${d.issue} · ${d.drawDate}`" :value="d.issue"/>
                </el-select>
                <el-input v-model="issueInput" placeholder="输入期号" size="default"
                          style="width:150px;margin-left:8px" @keyup.enter="loadIssue(issueInput)">
                  <template #append>
                    <el-button @click="loadIssue(issueInput)">加载</el-button>
                  </template>
                </el-input>
                <el-button v-if="highlightedDraw" type="danger" plain
                           style="margin-left:8px" @click="clearHighlight">清除高亮</el-button>
              </div>
            </div>
            <div class="modes">
              <el-radio-group v-model="mode">
                <el-radio-button value="single">单式 (6 红 + 1 蓝)</el-radio-button>
                <el-radio-button value="dantuo">胆拖</el-radio-button>
                <el-radio-button value="compound">复式</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- 开奖结果展示条 -->
          <div v-if="highlightedDraw" class="draw-banner">
            <div class="info">
              <el-tag type="warning" effect="dark" round>{{ highlightedDraw.issue }} 期</el-tag>
              <span class="date">{{ highlightedDraw.drawDate }}</span>
            </div>
            <div class="balls">
              <span v-for="r in highlightedDraw.reds" :key="'dr'+r" class="ball red glow-red">{{ r }}</span>
              <span class="plus">+</span>
              <span class="ball blue glow-blue">{{ highlightedDraw.blue }}</span>
            </div>
            <div class="hit-box">
              当前选号命中
              <b :class="['hit-red', hits.red===6?'max':'']">{{ hits.red }} 红</b>
              <b :class="['hit-blue', hits.blue===1?'max':'']">{{ hits.blue }} 蓝</b>
              <el-tag v-if="hits.level" :type="hits.level==='未中奖'?'info':'danger'" effect="dark" size="small">
                {{ hits.level }}
              </el-tag>
            </div>
          </div>

          <div class="spiral-area">
            <!-- 红球螺旋 SVG -->
            <svg ref="redSvg" :viewBox="`0 0 ${redSize} ${redSize}`" class="spiral-svg red-spiral-svg">
              <defs>
                <radialGradient id="rgGold" cx="50%" cy="50%" r="50%">
                  <stop offset="0%"   stop-color="rgba(255,214,102,0)"/>
                  <stop offset="65%"  stop-color="rgba(255,214,102,0.35)"/>
                  <stop offset="100%" stop-color="rgba(255,214,102,0.95)"/>
                </radialGradient>
                <radialGradient id="rgBlue" cx="50%" cy="50%" r="50%">
                  <stop offset="0%"   stop-color="rgba(74,144,226,0)"/>
                  <stop offset="65%"  stop-color="rgba(74,144,226,0.35)"/>
                  <stop offset="100%" stop-color="rgba(74,144,226,0.95)"/>
                </radialGradient>
              </defs>
              <path :d="redPath" stroke="rgba(255,255,255,0.1)" fill="none" stroke-width="1"/>

              <!-- 开奖号码金光环 (不拦截点击) -->
              <g style="pointer-events:none">
                <g v-for="(code,idx) in hitRedCodes" :key="'hr'+code+idx">
                  <circle :cx="redNode(code).x" :cy="redNode(code).y" r="32"
                          fill="url(#rgGold)" :style="{animation:`halo-pulse 1.8s ease-in-out ${idx*0.08}s infinite`}"/>
                  <circle :cx="redNode(code).x" :cy="redNode(code).y" r="26"
                          fill="none" stroke="#ffd666" stroke-width="3" stroke-dasharray="4 3"
                          :style="{animation:`halo-spin 4s linear infinite`}"/>
                </g>
              </g>

              <!-- 红球节点 -->
              <g v-for="n in 33" :key="'r'+n" class="node-group" @click="toggleRed(n)">
                <circle :cx="redNodes[n-1].x" :cy="redNodes[n-1].y" r="21"
                  :class="['node-circle', redClass(n), isHitRed(n)?'hit-node':'']"/>
                <text :x="redNodes[n-1].x" :y="redNodes[n-1].y+5" text-anchor="middle"
                  class="node-text" :class="redTextClass(n)">{{ pad(n) }}</text>
              </g>
            </svg>

            <!-- 蓝球区：也是阿基米德小螺旋 -->
            <div class="blue-box">
              <div class="blue-title">蓝球区 (1~16)</div>
              <svg ref="blueSvg" :viewBox="`0 0 ${blueSize} ${blueSize}`" class="spiral-svg blue-spiral-svg">
                <path :d="bluePath" stroke="rgba(74,144,226,0.15)" fill="none" stroke-width="1"/>
                <g style="pointer-events:none">
                  <g v-if="isHitBlue(highlightedDraw?.blue)">
                    <circle :cx="blueNode(highlightedDraw.blue).x" :cy="blueNode(highlightedDraw.blue).y" r="28"
                            fill="url(#rgBlue)" style="animation:halo-pulse 1.6s ease-in-out infinite"/>
                    <circle :cx="blueNode(highlightedDraw.blue).x" :cy="blueNode(highlightedDraw.blue).y" r="22"
                            fill="none" stroke="#66ccff" stroke-width="3" stroke-dasharray="4 3"
                            style="animation:halo-spin 4s linear infinite"/>
                  </g>
                </g>
                <g v-for="n in 16" :key="'b'+n" class="node-group" @click="toggleBlue(n)">
                  <circle :cx="blueNodes[n-1].x" :cy="blueNodes[n-1].y" r="18"
                    :class="['node-circle', blueClass(n), isHitBlue(pad(n))?'blue-hit':'']"/>
                  <text :x="blueNodes[n-1].x" :y="blueNodes[n-1].y+4" text-anchor="middle"
                    class="node-text blue-text">{{ pad(n) }}</text>
                </g>
              </svg>
              <div v-if="mode==='dantuo'" class="blue-hint">
                <el-tag type="warning" size="small">左键：选蓝胆</el-tag>
                <el-tag type="primary" size="small" effect="dark">Ctrl + 左键：选蓝拖</el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右：选号面板 -->
      <el-col :span="8">
        <div class="glass-card selection-panel">
          <h3>已选号码</h3>
          <div v-if="mode==='dantuo'" class="picks">
            <div class="pick-row">
              <label>红球胆码 ({{selectedDans.size}}/5)</label>
              <div class="pick-balls">
                <span v-for="d in [...selectedDans]" :key="d" class="ball gold ball-sm">{{ d }}</span>
                <span v-if="selectedDans.size===0" class="empty">Ctrl + 点击红球 = 胆码</span>
              </div>
            </div>
            <div class="pick-row">
              <label>红球拖码 ({{selectedTuos.size}})</label>
              <div class="pick-balls">
                <span v-for="t in [...selectedTuos]" :key="t" class="ball blue ball-sm">{{ t }}</span>
              </div>
            </div>
          </div>
          <div v-else class="picks">
            <div class="pick-row">
              <label>红球 ({{selectedReds.size}}/{{mode==='compound'?'≥6':'=6'}})</label>
              <div class="pick-balls">
                <span v-for="r in [...selectedReds].sort()" :key="r" class="ball red ball-sm">{{ r }}</span>
              </div>
            </div>
          </div>
          <div class="pick-row">
            <label>蓝球 ({{selectedBlues.size}}/{{mode==='single'?'=1':'≥1'}})</label>
            <div class="pick-balls">
              <span v-for="b in [...selectedBlues].sort()" :key="b" class="ball blue ball-sm">{{ b }}</span>
            </div>
          </div>

          <div class="summary">
            <div class="row"><span>组合注数：</span><b class="num">{{ combinationCount }}</b></div>
            <div class="row"><span>投注金额：</span><b class="price">¥ {{ (combinationCount*2).toLocaleString() }}</b></div>
          </div>

          <div class="actions">
            <el-button @click="clearAll">清空</el-button>
            <el-button type="success" @click="doRandom">随机一注</el-button>
            <el-button type="primary" @click="generatePick" :disabled="!canGenerate" style="width:100%;margin-top:10px">
              展开组合 / 计算
            </el-button>
          </div>

          <el-alert v-if="mode!=='single' && combinationCount>0" type="info" :closable="false" show-icon class="mt15">
            <template #title>
              生成 <b>{{ combinationCount }}</b> 注 (¥{{ (combinationCount*2).toLocaleString() }})
              <span v-if="combinationCount>500">（仅预览前 500 注）</span>
            </template>
          </el-alert>
        </div>

        <!-- 展开结果 / 中奖判定 -->
        <div class="glass-card mt20" v-if="generated.length">
          <div class="panel-head no-pad">
            <h3>投注组合 (共 {{ generated.length }} 注)</h3>
            <el-button-group>
              <el-button size="small" type="warning" @click="doMatch('latest')">对比最新开奖判奖</el-button>
              <el-button size="small" @click="saveBets">保存到投注记录</el-button>
            </el-button-group>
          </div>
          <el-table :data="tableData" size="small" max-height="360" class="mt15 dark-table">
            <el-table-column label="#" width="55" prop="idx"/>
            <el-table-column label="红球" min-width="220">
              <template #default="{row}">
                <span v-for="r in row.reds" :key="r" class="ball red ball-sm" style="margin-right:3px">{{ r }}</span>
              </template>
            </el-table-column>
            <el-table-column label="蓝" width="50">
              <template #default="{row}"><span class="ball blue ball-sm">{{ row.blue }}</span></template>
            </el-table-column>
            <el-table-column label="奖项" width="110" v-if="matchResult">
              <template #default="{row}">
                <el-tag v-if="row.levelName==='未中奖'" type="info" size="small">{{ row.levelName }}</el-tag>
                <el-tag v-else type="warning" size="small" effect="dark">{{ row.levelName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="奖金" width="90" v-if="matchResult" prop="prize"/>
          </el-table>
          <div v-if="matchResult" class="match-summary mt15">
            <el-descriptions :column="3" border size="small" class="dark-desc">
              <el-descriptions-item label="总注数">{{ matchResult.totalBets }}</el-descriptions-item>
              <el-descriptions-item label="中奖注数">
                <b style="color:#ffd666">{{ matchResult.wonBets }}</b>
              </el-descriptions-item>
              <el-descriptions-item label="累计奖金">
                <b style="color:#ff6b35">¥ {{ Number(matchResult.totalPrize||0).toLocaleString() }}</b>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ssq, bet } from '@/api'
const route = useRoute()

// ======== 数学参数 ========
const redSize  = 620
const blueSize = 320
const redCenter  = redSize  / 2
const blueCenter = blueSize / 2
const mode = ref('single')
const selectedReds  = reactive(new Set())
const selectedBlues = reactive(new Set())
const selectedDans  = reactive(new Set())
const selectedTuos  = reactive(new Set())

// ======== 开奖高亮 ========
const highlightedDraw = ref(null)
const recentIssues = ref([])
const selIssue = ref('')
const issueInput = ref('')

const hitRedCodes = computed(() => highlightedDraw.value ? highlightedDraw.value.reds : [])
const isHitRed = (n) => hitRedCodes.value.includes(pad(n))
const isHitBlue = (c) => highlightedDraw.value && highlightedDraw.value.blue === c
const allSelectedReds = computed(() => new Set([...selectedReds, ...selectedDans, ...selectedTuos]))
const hits = computed(() => {
  let red = 0
  if (highlightedDraw.value) {
    for (const r of highlightedDraw.value.reds) if (allSelectedReds.value.has(r)) red++
  }
  const blue = highlightedDraw.value && selectedBlues.has(highlightedDraw.value.blue) ? 1 : 0
  let level = null
  if (highlightedDraw.value && allSelectedReds.value.size >= 6 && selectedBlues.size >= 1) {
    level = ssqLevel(red, blue === 1)
  }
  return { red, blue, level }
})
function ssqLevel(r, b) {
  if (r===6 && b) return '一等奖'
  if (r===6 && !b) return '二等奖'
  if (r===5 && b) return '三等奖'
  if ((r===5 && !b) || (r===4 && b)) return '四等奖'
  if ((r===4 && !b) || (r===3 && b)) return '五等奖'
  if (b) return '六等奖'
  return '未中奖'
}
async function loadRecentIssues() {
  try {
    const data = await ssq.list({ pageNum: 1, pageSize: 30 })
    recentIssues.value = data.list || []
  } catch (_) {}
}
async function loadIssue(issue) {
  if (!issue) return ElMessage.warning('请输入或选择期号')
  try {
    const d = await ssq.byIssue(String(issue).trim())
    highlightedDraw.value = d
    selIssue.value = d.issue
    issueInput.value = d.issue
    ElMessage.success(`已加载 ${d.issue} 期`)
  } catch (_) {}
}
function clearHighlight() {
  highlightedDraw.value = null
  selIssue.value = ''
  issueInput.value = ''
}

// ======== 螺旋节点 & 轨迹 ========
const redNodes  = ref([])
const blueNodes = ref([])
let redPath  = ''
let bluePath = ''
function pad(n) { return String(n).padStart(2, '0') }
function redNode(code) { return (redNodes.value[parseInt(code,10)-1]) || { x:redCenter, y:redCenter } }
function blueNode(code) { return (blueNodes.value[parseInt(code,10)-1]) || { x:blueCenter, y:blueCenter } }

// 按弧长均匀分布的阿基米德螺旋节点生成
// 阿基米德螺旋: r = k * θ，其中 k = maxR / (turns * 2π)
// 弧长元素 ds = k * sqrt(1 + θ²) dθ
// 累积弧长 S(θ) = k/2 * [ θ·√(1+θ²) + ln(θ + √(1+θ²)) ]
function arcIntegral(theta) {
  // ∫√(1+u²) du 从 0 到 theta 的解析解
  return 0.5 * (theta * Math.sqrt(1 + theta * theta) + Math.log(theta + Math.sqrt(1 + theta * theta)))
}
function dArcIntegral(theta) {
  // 导数: dS/dθ = √(1+θ²)，用于牛顿迭代
  return Math.sqrt(1 + theta * theta)
}
function genNodes(count, center, maxR, turns) {
  const out = []
  const totalTheta = turns * 2 * Math.PI
  const k = maxR / totalTheta
  const totalArc = k * arcIntegral(totalTheta)

  for (let i = 1; i <= count; i++) {
    // 目标弧长位置（按节点序号等分）
    const targetArc = (i / count) * totalArc

    // 牛顿迭代反解 θ，使 k·arcIntegral(θ) = targetArc
    let theta = Math.sqrt(2 * targetArc / k) // 初始猜测（小角度近似）
    for (let iter = 0; iter < 25; iter++) {
      const currentArc = k * arcIntegral(theta)
      const error = targetArc - currentArc
      const deriv = k * dArcIntegral(theta)
      if (deriv < 1e-9) break
      theta += error / deriv
      if (theta < 0) theta = 0
      if (theta > totalTheta * 1.01) theta = totalTheta
      if (Math.abs(error) < 1e-6) break
    }

    const r = k * theta
    out.push({
      x: center + r * Math.cos(theta),
      y: center + r * Math.sin(theta),
      n: i
    })
  }
  return out
}
function genPath(center, maxR, turns) {
  const pts = []
  const totalTheta = turns * 2 * Math.PI
  const k = maxR / totalTheta
  // 按弧长均匀采样路径点，使轨迹线更平滑
  const arcStep = (k * arcIntegral(totalTheta)) / 500
  let a = 0
  for (let step = 0; step <= 500; step++) {
    // 按弧长步长反解角度
    const targetArc = step * arcStep
    let theta = Math.sqrt(2 * targetArc / Math.max(k, 1e-9))
    for (let iter = 0; iter < 20; iter++) {
      const cur = k * arcIntegral(theta)
      const err = targetArc - cur
      const d = k * dArcIntegral(theta)
      if (d < 1e-9) break
      theta += err / d
      if (theta < 0) theta = 0
      if (Math.abs(err) < 1e-6) break
    }
    a = Math.min(theta, totalTheta)
    const r = k * a
    pts.push(`${step===0?'M':'L'}${(center + r*Math.cos(a)).toFixed(2)},${(center + r*Math.sin(a)).toFixed(2)}`)
  }
  return pts.join(' ')
}
// 同步初始化螺旋节点，避免 onMounted 前首屏 render 访问 undefined.x
// 红球 33 个：2.8 圈，使内圈到外圈间距基本一致
// 蓝球 16 个：2.0 圈，视觉更紧凑均匀
redNodes.value  = genNodes(33, redCenter,  redCenter  - 50, 2.8)
blueNodes.value = genNodes(16, blueCenter, blueCenter - 35, 2.0)
redPath  = genPath(redCenter,  redCenter  - 50, 2.8)
bluePath = genPath(blueCenter, blueCenter - 35, 2.0)

// ======== 选号交互 ========
function toggleRed(n) {
  const code = pad(n)
  const withCtrl = window.event?.ctrlKey || window.event?.metaKey
  if (mode.value === 'dantuo') {
    if (withCtrl) {
      if (selectedDans.has(code)) { selectedDans.delete(code) }
      else {
        if (selectedDans.size >= 5) return ElMessage.warning('胆码最多 5 个')
        selectedDans.add(code)
        selectedTuos.delete(code)
      }
    } else {
      if (selectedDans.has(code)) return
      if (selectedTuos.has(code)) selectedTuos.delete(code)
      else selectedTuos.add(code)
    }
  } else {
    if (selectedReds.has(code)) selectedReds.delete(code)
    else {
      if (mode.value === 'single' && selectedReds.size >= 6) return ElMessage.warning('单式红球只能选 6 个')
      selectedReds.add(code)
    }
  }
}
function redClass(n) {
  const code = pad(n)
  if (mode.value === 'dantuo') {
    if (selectedDans.has(code)) return 'dan'
    if (selectedTuos.has(code)) return 'tuo'
    return 'normal'
  }
  return selectedReds.has(code) ? 'selected' : 'normal'
}
function redTextClass(n) {
  const code = pad(n)
  if (mode.value === 'dantuo') return (selectedDans.has(code) || selectedTuos.has(code)) ? 'sel' : ''
  return selectedReds.has(code) ? 'sel' : ''
}
function toggleBlue(n) {
  const code = pad(n)
  if (selectedBlues.has(code)) selectedBlues.delete(code)
  else {
    if (mode.value === 'single' && selectedBlues.size >= 1) return ElMessage.warning('单式蓝球只能选 1 个')
    selectedBlues.add(code)
  }
}
function blueClass(n) {
  return selectedBlues.has(pad(n)) ? 'blue-selected' : 'blue-normal'
}

function clearAll() {
  selectedReds.clear(); selectedBlues.clear(); selectedDans.clear(); selectedTuos.clear()
  generated.value = []; matchResult.value = null
}
async function doRandom() {
  const r = await ssq.randomSingle()
  clearAll()
  r.reds.forEach(x => selectedReds.add(x))
  selectedBlues.add(r.blue)
  if (mode.value !== 'single') mode.value = 'single'
}

// ======== 组合数 ========
const combinationCount = computed(() => {
  const C = (n, k) => {
    if (k<0 || k>n) return 0
    k = Math.min(k, n-k); let r = 1n
    for (let i=1;i<=k;i++) r = r * BigInt(n-k+i) / BigInt(i)
    return Number(r)
  }
  const bc = Math.max(1, selectedBlues.size)
  if (mode.value === 'single')   return bc * (selectedReds.size === 6 ? 1 : 0)
  if (mode.value === 'compound') return C(selectedReds.size, 6) * bc
  if (mode.value === 'dantuo')   return C(selectedTuos.size, 6 - selectedDans.size) * bc
  return 0
})
const canGenerate = computed(() => combinationCount.value > 0)

// ======== 生成组合 / 判奖 ========
const generated = ref([])
const matchResult = ref(null)
const tableData = computed(() => generated.value.slice(0, 500).map((b, i) => ({
  idx: i + 1,
  reds: b.slice(0, 6), blue: b[6],
  levelName: matchResult.value?.details?.[i]?.levelName || '',
  prize: matchResult.value?.details?.[i]?.prizeAmount || ''
})))

async function generatePick() {
  try {
    let resp
    if (mode.value === 'single') {
      generated.value = [[...[...selectedReds].sort(), [...selectedBlues][0]]]
      matchResult.value = null
      ElMessage.success('已生成 1 注')
      return
    }
    if (mode.value === 'compound') {
      resp = await ssq.compoundPick({ reds: [...selectedReds], blues: [...selectedBlues] })
    } else {
      resp = await ssq.dantuoPick({ dans: [...selectedDans], tuos: [...selectedTuos], blues: [...selectedBlues] })
    }
    generated.value = resp.samples.map(s => [...s.reds, s.blue])
    matchResult.value = null
    ElMessage.success(`共 ${resp.combinationCount} 注，预览前 ${generated.value.length} 注`)
  } catch (_) {}
}
async function doMatch() {
  if (!generated.value.length) return ElMessage.warning('请先生成组合')
  const params = mode.value === 'compound'
    ? { reds: [...selectedReds], blues: [...selectedBlues] }
    : mode.value === 'dantuo'
    ? { dans: [...selectedDans], tuos: [...selectedTuos], blues: [...selectedBlues] }
    : { reds: [...selectedReds], blue: [...selectedBlues][0] }
  const api = mode.value === 'compound' ? ssq.matchCompound
            : mode.value === 'dantuo' ? ssq.matchDantuo : ssq.matchSingle
  matchResult.value = await api(params)
  ElMessage.success(`判奖完成：${matchResult.value.wonBets}/${matchResult.value.totalBets} 注中奖`)
}
async function saveBets() {
  if (!generated.value.length) return ElMessage.warning('请先生成组合')
  const dansStr = mode.value === 'dantuo' ? [...selectedDans].join(',') : ''
  const tuosStr = mode.value === 'dantuo' ? [...selectedTuos].join(',') : ''
  const mainStr = mode.value === 'dantuo' ? '' : [...selectedReds].join(',')
  await bet.save({
    lotteryType: 'SSQ', betType: mode.value.toUpperCase(),
    danNumbers: dansStr, tuoNumbers: tuosStr,
    mainNumbers: mainStr, extraNumbers: [...selectedBlues].join(','),
    combinationCnt: combinationCount.value
  })
  ElMessage.success('投注记录已保存')
}

// ======== 初始化 ========
onMounted(async () => {
  await loadRecentIssues()
  const issue = route.query.issue
  if (issue) {
    issueInput.value = String(issue)
    await loadIssue(issue)
  }
})
watch(() => route.query.issue, (v) => {
  if (v && String(v) !== selIssue.value) {
    issueInput.value = String(v)
    loadIssue(v)
  }
})
</script>

<style lang="scss" scoped>
.ssq-spiral { max-width: 1700px; margin: 0 auto; }
.panel-head {
  display:flex; justify-content:space-between; align-items:flex-end; margin-bottom: 20px;
  flex-wrap: wrap; gap: 16px;
  .head-left { flex: 1; min-width: 520px;
    h2 { margin: 0 0 12px; font-size: 22px; }
  }
  &.no-pad { margin-bottom: 10px; align-items: center;
    h3 { margin: 0; font-size: 17px; }
  }
}
.period-tools { display:flex; align-items: center; flex-wrap: wrap; gap: 4px; }

/* 开奖结果条 */
.draw-banner {
  margin-bottom: 20px; padding: 14px 18px; border-radius: 12px;
  background: linear-gradient(135deg, rgba(255,214,102,0.12), rgba(255,107,53,0.08));
  border: 1px solid rgba(255,214,102,0.25);
  display:flex; align-items:center; justify-content:space-between; flex-wrap: wrap; gap: 14px;
  animation: fadeInUp .35s ease;
  .info { display:flex; align-items:center; gap: 10px;
    .date { color: rgba(255,255,255,0.5); font-size: 13px; }
  }
  .balls { display:flex; align-items:center; gap: 8px;
    .plus { color: rgba(255,255,255,0.35); font-weight: bold; margin: 0 4px; }
  }
  .hit-box {
    padding: 8px 14px; background: rgba(0,0,0,0.2); border-radius: 8px;
    font-size: 14px; color: rgba(255,255,255,0.75);
    b { margin: 0 6px; font-size: 16px; }
    .hit-red { color: #ff6b35; &.max { color: #ffd666; font-size: 22px; } }
    .hit-blue { color: #4a90e2; &.max { color: #66ccff; font-size: 22px; } }
  }
}

/* 螺旋区域 */
.spiral-area {
  display:flex; gap: 24px; align-items:flex-start; justify-content: center;
  flex-wrap: wrap;
}
.spiral-svg {
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 14px; background: rgba(0,0,0,0.18);
  display: block;
}
.red-spiral-svg  { width: 100%; max-width: 620px; height: auto; }
.blue-spiral-svg { width: 320px; height: 320px; }
.blue-box {
  padding: 14px; background: rgba(255,255,255,0.03);
  border-radius: 14px; border: 1px solid rgba(255,255,255,0.08);
  .blue-title { color: #4a90e2; font-weight: bold; margin-bottom: 10px; }
  .blue-hint { margin-top: 10px; display: flex; gap: 6px; flex-wrap: wrap; }
}

/* 红球节点圆圈 */
.node-group { cursor: pointer;
  &:hover { filter: brightness(1.15); }
}
.node-circle {
  transition: all 0.2s;
  &.normal       { fill: rgba(255,255,255,0.06);  stroke: rgba(255,107,53,0.35); stroke-width: 2; }
  &.selected     { fill: #ff6b35; stroke: #ffd666; stroke-width: 3; }
  &.dan          { fill: #ffd666; stroke: #ffffff; stroke-width: 3; }
  &.tuo          { fill: #4a90e2; stroke: #66ccff; stroke-width: 3; }
  &.hit-node     { stroke: #ffd666 !important; stroke-width: 4 !important; }
}
.node-text {
  font-size: 14px; fill: rgba(255,255,255,0.65); pointer-events: none;
  &.sel { fill: #1a1a2e; font-weight: bold; }
  &.blue-text { fill: rgba(255,255,255,0.75); font-size: 13px; }
}
.node-circle.blue-normal   { fill: rgba(255,255,255,0.05);  stroke: rgba(74,144,226,0.4); stroke-width: 2; }
.node-circle.blue-selected { fill: #4a90e2; stroke: #66ccff; stroke-width: 3; }
.node-circle.blue-hit      { stroke: #ffd666 !important; stroke-width: 4 !important; }

/* 光晕关键帧 (不依赖 transform-box，兼容所有浏览器) */
@keyframes halo-pulse {
  0%   { opacity: 0.95; transform: scale(0.82); transform-origin: center; }
  50%  { opacity: 0.35; transform: scale(1.12); transform-origin: center; }
  100% { opacity: 0.95; transform: scale(0.82); transform-origin: center; }
}
@keyframes halo-spin {
  from { stroke-dashoffset: 0; }
  to   { stroke-dashoffset: -42; }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(-8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* 选球发光 */
.glow-red  { box-shadow: 0 0 14px #ff6b35, 0 0 4px #fff inset; }
.glow-blue { box-shadow: 0 0 14px #4a90e2, 0 0 4px #fff inset; }

/* 右侧选号面板 */
.selection-panel { position: sticky; top: 20px;
  h3 { margin: 0 0 18px; font-size: 18px; }
}
.picks { .pick-row { margin-bottom: 14px;
  label { display:block; color: rgba(255,255,255,0.6); font-size: 13px; margin-bottom: 6px; }
  .pick-balls { min-height: 38px; }
  .empty { color: rgba(255,255,255,0.3); font-size: 12px; }
} }
.summary { background: rgba(255,107,53,0.1); padding: 14px 16px; border-radius: 10px;
  margin: 16px 0; border: 1px solid rgba(255,107,53,0.2);
  .row { display:flex; justify-content:space-between; line-height: 2; }
  .num { color: #ffd666; font-size: 18px; }
  .price { color: #ff6b35; font-size: 18px; }
}
.actions { display: flex; flex-wrap: wrap; gap: 8px; }
.mt20 { margin-top: 20px; }
.mt15 { margin-top: 15px; }
.dark-table :deep(.el-table) {
  background: transparent; --el-table-border-color: rgba(255,255,255,0.08);
  --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent;
  --el-table-text-color: rgba(255,255,255,0.9); --el-table-header-text-color: #ffd666;
  td, th { background: transparent !important; }
  tr.el-table__row:hover > td { background: rgba(255,255,255,0.05) !important; }
}
.dark-desc :deep(.el-descriptions) {
  --el-descriptions-bg-color: transparent;
  --el-descriptions-table-border: rgba(255,255,255,0.1);
  --el-descriptions-item-label-bg-color: rgba(255,255,255,0.04);
  .el-descriptions__label, .el-descriptions__content { color: rgba(255,255,255,0.85); }
}
</style>
