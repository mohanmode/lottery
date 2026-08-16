<template>
  <div class="kl8">
    <div class="glass-card">
      <div class="panel-head">
        <h2>快乐8 选号 (选 1~10 个号码，共 80 选 20)</h2>
        <div class="actions">
          <el-select v-model="pickType" placeholder="玩法类型" style="width:140px">
            <el-option v-for="k in pickTypes" :key="k" :label="`选${k}`" :value="k"/>
          </el-select>
          <el-button @click="doRandom(pickType)">随机选号</el-button>
          <el-button type="primary" @click="generateCompound" :disabled="selected.size<pickType">展开复式</el-button>
          <el-button type="warning" @click="clearAll">清空</el-button>
        </div>
      </div>

      <div class="hint">
        <el-tag type="info">当前选 <b>{{ pickType }}</b> 玩法：命中越多，奖金越高。从 80 个号码中选择 {{ pickType }} 个，开奖开出 20 个号码。</el-tag>
        <span class="picked">已选 <b>{{ selected.size }}</b> / {{ pickType }} 个</span>
      </div>

      <div class="kl8-grid">
        <div v-for="row in rows" :key="row" class="kl8-row">
          <span v-for="n in cols" :key="n" @click="toggle(row, n)"
                :class="['ball', cellCls(row,n)]">
            {{ String(cellNum(row,n)).padStart(2,'0') }}
          </span>
        </div>
      </div>

      <div class="summary" v-if="selected.size">
        <span>已选号码：</span>
        <span v-for="n in [...selected].sort((a,b)=>a-b)" :key="n" class="ball green ball-sm" style="margin-right:4px">
          {{ String(n).padStart(2,'0') }}
        </span>
        <span style="margin-left:20px">复式组合数：
          <b style="color:#ffd666">{{ compoundCount }}</b> 注 · ¥{{ (compoundCount*2).toLocaleString() }}
        </span>
      </div>
    </div>

    <div class="glass-card mt20" v-if="generated.length">
      <div class="panel-head">
        <h3>展开投注（共 {{ generated.length }} 注）</h3>
        <el-button type="warning" size="small" @click="doMatchLocal">模拟判奖</el-button>
      </div>
      <el-table :data="tableView" size="small" max-height="360" class="dark-table">
        <el-table-column label="#" width="50" type="index"/>
        <el-table-column label="号码">
          <template #default="{row}">
            <span v-for="n in row.nums" :key="n" class="ball green ball-sm" style="margin-right:3px">
              {{ String(n).padStart(2,'0') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="命中" width="90" v-if="matchDone">
          <template #default="{row}"><b style="color:#ffd666">{{ row.hit }}/{{ pickType }}</b></template>
        </el-table-column>
      </el-table>
      <div v-if="matchDone" class="mt15">
        <el-alert type="success" show-icon :closable="false">
          <template #title>
            模拟开奖 20 个号码：
            <span v-for="n in drawn" :key="n" class="ball gold ball-sm" style="margin:0 3px">{{ String(n).padStart(2,'0') }}</span>
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'

const pickType = ref(8)
const pickTypes = [1,2,3,4,5,6,7,8,9,10]
const rows = 8
const cols = 10
const cellNum = (r, c) => (r-1)*cols + c
const selected = reactive(new Set())
const generated = ref([])
const drawn = ref([])
const matchDone = ref(false)

function cellCls(r, n) {
  const num = cellNum(r,n)
  if (num > 80) return 'gray'
  return selected.has(num) ? 'green selected' : 'gray'
}
function toggle(r, n) {
  const num = cellNum(r,n); if (num > 80) return
  if (selected.has(num)) selected.delete(num)
  else {
    if (selected.size >= pickType.value && pickType.value < 10)
      return ElMessage.warning(`选${pickType.value}玩法最多选择${pickType.value}个号码`)
    selected.add(num)
  }
}
function clearAll() { selected.clear(); generated.value=[]; matchDone.value=false }
function C(n, k) {
  if (k<0||k>n) return 0
  k = Math.min(k, n-k); let r = 1n
  for (let i=1;i<=k;i++) r = r * BigInt(n-k+i) / BigInt(i)
  return Number(r)
}
const compoundCount = computed(() => C(selected.size, pickType.value))

function doRandom(k) {
  clearAll()
  const pool = Array.from({length:80},(_,i)=>i+1)
  for (let i=pool.length-1;i>0;i--) { const j=Math.floor(Math.random()*(i+1)); [pool[i],pool[j]]=[pool[j],pool[i]] }
  pool.slice(0, k).forEach(n => selected.add(n))
}

function combinations(arr, k) {
  const res = []
  const go = (start, cur) => {
    if (cur.length === k) { res.push([...cur]); return }
    if (res.length > 500) return
    for (let i=start; i<arr.length; i++) {
      cur.push(arr[i]); go(i+1, cur); cur.pop()
      if (res.length > 500) return
    }
  }
  go(0, [])
  return res
}

function generateCompound() {
  if (selected.size < pickType.value) return ElMessage.warning('选号不足')
  const nums = [...selected].sort((a,b)=>a-b)
  generated.value = combinations(nums, pickType.value).map(ns => ({ nums: ns, hit: 0 }))
  matchDone.value = false
  ElMessage.success(`已生成 ${generated.value.length} 注`)
}
function doMatchLocal() {
  const pool = Array.from({length:80},(_,i)=>i+1)
  for (let i=pool.length-1;i>0;i--) { const j=Math.floor(Math.random()*(i+1)); [pool[i],pool[j]]=[pool[j],pool[i]] }
  drawn.value = pool.slice(0, 20).sort((a,b)=>a-b)
  const ds = new Set(drawn.value)
  generated.value.forEach(b => b.hit = b.nums.filter(x => ds.has(x)).length)
  matchDone.value = true
}
const tableView = computed(() => generated.value.slice(0,500))
</script>

<style lang="scss" scoped>
.kl8 { max-width: 1500px; margin: 0 auto; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; flex-wrap: wrap; gap: 12px;
  h2 { margin: 0; font-size: 20px; }
  .actions { display: flex; gap: 10px; flex-wrap: wrap; }
}
.hint { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; flex-wrap: wrap; gap: 10px;
  .picked { color: rgba(255,255,255,0.7); b { color: #10b981; font-size: 16px; } }
}
.kl8-grid {
  padding: 16px; background: rgba(0,0,0,0.2); border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.08);
  .kl8-row { display: grid; grid-template-columns: repeat(10, 1fr); gap: 8px; margin-bottom: 8px; }
  .ball { width: 100%; height: 42px; font-size: 15px; }
}
.summary { margin-top: 18px; padding: 14px 18px; background: rgba(16,185,129,0.1);
  border-radius: 10px; border: 1px solid rgba(16,185,129,0.25); }
.mt20 { margin-top: 20px; } .mt15 { margin-top: 15px; }
.dark-table :deep(.el-table) { --el-table-header-text-color: #ffd666; --el-table-text-color: #eee;
  --el-table-border-color: rgba(255,255,255,0.08); background: transparent;
  td,th { background: transparent !important; } tr:hover > td { background: rgba(255,255,255,0.04) !important; } }
</style>
