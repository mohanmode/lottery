<template>
  <div class="ssq-history">
    <div class="glass-card">
      <div class="panel-head">
        <h2>双色球开奖历史（共 {{ total }} 期，10年数据）</h2>
        <div class="filters">
          <el-input v-model="filters.keyword" placeholder="期号 / 红球 / 蓝球" clearable style="width:200px"
                    @keyup.enter="loadPage(1)" :prefix-icon="Search"/>
          <el-date-picker v-model="filters.range" type="daterange" range-separator="至"
                          start-placeholder="开始日期" end-placeholder="结束日期" style="width:300px"
                          value-format="YYYY-MM-DD"/>
          <el-button type="primary" @click="loadPage(1)">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-table :data="list" size="default" class="dark-table" v-loading="loading" border stripe>
        <el-table-column label="期号" prop="issue" width="90" align="center" fixed />
        <el-table-column label="开奖日期" prop="drawDate" width="115" align="center" />
        <el-table-column label="红球" min-width="380">
          <template #default="{row}">
            <span v-for="r in row.reds" :key="r" class="ball red ball-sm" style="margin-right:5px">{{ r }}</span>
          </template>
        </el-table-column>
        <el-table-column label="蓝球" width="80" align="center">
          <template #default="{row}"><span class="ball blue ball-sm">{{ row.blue }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{row}">
            <el-button size="small" link type="warning" @click="openSpiral(row)">生成螺旋图</el-button>
            <el-button size="small" link type="primary" @click="checkPrize(row)">模拟判奖</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background layout="total, sizes, prev, pager, next, jumper"
          :total="total" :page-sizes="[10, 20, 50, 100]"
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          @current-change="loadPage" @size-change="loadPage(1)" />
      </div>
    </div>

    <!-- 模拟判奖弹窗 -->
    <el-dialog v-model="prizeDlg" title="模拟投注判奖" width="560px" class="dark-dlg">
      <el-form label-width="90px">
        <el-form-item label="选号方式">
          <el-radio-group v-model="matchForm.mode" :disabled="matching">
            <el-radio value="single">单式</el-radio>
            <el-radio value="compound">复式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="红球">
          <div style="display:flex;gap:6px;flex-wrap:wrap">
            <span v-for="n in 33" :key="n"
                  :class="['ball', matchReds.has(pad(n))?'red':'gray', matchReds.has(pad(n))?'selected':'']"
                  @click="!matching && toggleMatchRed(n)">{{ pad(n) }}</span>
          </div>
          <div style="margin-top:6px;color:rgba(255,255,255,0.5);font-size:12px">
            已选 {{ matchReds.size }} 个 {{ matchForm.mode==='single'?'(需要6)':'(≥6)' }}
          </div>
        </el-form-item>
        <el-form-item label="蓝球">
          <div style="display:flex;gap:6px;flex-wrap:wrap">
            <span v-for="n in 16" :key="n"
                  :class="['ball', matchBlues.has(pad(n))?'blue':'gray', matchBlues.has(pad(n))?'selected':'']"
                  @click="!matching && toggleMatchBlue(n)">{{ pad(n) }}</span>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="matching" @click="runMatch">开始判奖</el-button>
          <el-button @click="clearMatchForm">清空选号</el-button>
          <span v-if="matchResult" style="margin-left:12px">
            组合数: <b style="color:#ffd666">{{ combCount }}</b> 注
          </span>
        </el-form-item>
      </el-form>
      <el-divider v-if="matchResult">判奖结果</el-divider>
      <el-alert v-if="matchResult" :type="matchResult.wonBets>0?'success':'info'" show-icon :closable="false">
        <template #title>
          <span v-if="matchResult.totalBets===1">
            命中红 <b>{{ matchResult.details[0].redHit }}</b> / 蓝 <b>{{ matchResult.details[0].blueHit?'是':'否' }}</b>
            → <b style="color:#ff6b35;font-size:16px">{{ matchResult.levelName }}</b>
            <span v-if="matchResult.prizeAmount">，奖金 ¥{{ Number(matchResult.prizeAmount).toLocaleString() }}</span>
          </span>
          <span v-else>
            共 <b>{{ matchResult.totalBets }}</b> 注，中奖 <b style="color:#ffd666">{{ matchResult.wonBets }}</b> 注
            ，累计奖金 <b style="color:#ff6b35">¥{{ Number(matchResult.totalPrize||0).toLocaleString() }}</b>
          </span>
        </template>
      </el-alert>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ssq } from '@/api'
import { ElMessage } from 'element-plus'
const router = useRouter()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ pageNum: 1, pageSize: 20, keyword: '', range: [] })

async function loadPage(p) {
  if (p) filters.pageNum = p
  loading.value = true
  try {
    const data = await ssq.list({
      pageNum: filters.pageNum, pageSize: filters.pageSize,
      keyword: filters.keyword || undefined,
      startDate: filters.range?.[0] || undefined,
      endDate:   filters.range?.[1] || undefined
    })
    list.value = data.list; total.value = data.total
  } finally { loading.value = false }
}
function resetFilters() {
  filters.keyword = ''; filters.range = []; loadPage(1)
}
const pad = (n) => String(n).padStart(2, '0')

// ====== 判奖弹窗 ======
const prizeDlg = ref(false)
const matching = ref(false)
const matchForm = reactive({ mode: 'single' })
const matchReds = reactive(new Set())
const matchBlues = reactive(new Set())
const matchResult = ref(null)
const currentRow = ref(null)

const combCount = computed(() => {
  const C = (n, k) => {
    if (k < 0 || k > n) return 0
    k = Math.min(k, n - k)
    let r = 1n; for (let i = 1; i <= k; i++) r = r * BigInt(n - k + i) / BigInt(i)
    return Number(r)
  }
  const b = Math.max(1, matchBlues.size)
  return matchForm.mode === 'single' ? (matchReds.size === 6 && matchBlues.size >= 1 ? b : 0)
       : C(matchReds.size, 6) * b
})

function toggleMatchRed(n) {
  const c = pad(n)
  if (matchReds.has(c)) matchReds.delete(c)
  else {
    if (matchForm.mode === 'single' && matchReds.size >= 6) return ElMessage.warning('单式红球需恰好6个')
    matchReds.add(c)
  }
}
function toggleMatchBlue(n) {
  const c = pad(n)
  if (matchBlues.has(c)) matchBlues.delete(c)
  else {
    if (matchForm.mode === 'single' && matchBlues.size >= 1) {
      matchBlues.clear()
    }
    matchBlues.add(c)
  }
}
function clearMatchForm() { matchReds.clear(); matchBlues.clear(); matchResult.value = null }
function checkPrize(row) {
  currentRow.value = row
  clearMatchForm()
  prizeDlg.value = true
}
function openSpiral(row) {
  router.push({ path: '/ssq-spiral', query: { issue: row.issue } })
}
async function runMatch() {
  if (combCount.value <= 0) return ElMessage.warning('选号数量不足')
  matching.value = true; matchResult.value = null
  try {
    const issue = currentRow.value?.issue
    if (matchForm.mode === 'single') {
      matchResult.value = await ssq.matchSingle(
        { reds: [...matchReds], blue: [...matchBlues][0] }, issue)
    } else {
      matchResult.value = await ssq.matchCompound(
        { reds: [...matchReds], blues: [...matchBlues] }, issue)
    }
  } finally { matching.value = false }
}
onMounted(() => loadPage(1))
</script>

<style lang="scss" scoped>
.ssq-history { max-width: 1400px; margin: 0 auto; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 16px;
  h2 { margin: 0; font-size: 20px; }
  .filters { display: flex; gap: 10px; flex-wrap: wrap; }
}
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.dark-table :deep(.el-table) { --el-table-header-bg-color: rgba(255,107,53,0.12);
  --el-table-header-text-color: #ffd666; --el-table-text-color: #eee;
  --el-table-row-hover-bg-color: rgba(255,255,255,0.05); --el-table-border-color: rgba(255,255,255,0.08);
  background: transparent;
  ::v-deep(.el-table__body tr) { background: transparent !important; }
  ::v-deep(.el-table__body tr:hover > td) { background: rgba(255,255,255,0.04) !important; }
  td, th { background: transparent !important; }
}
.dark-dlg :deep(.el-dialog) { background: #232b48 !important; color: #fff; }
</style>
