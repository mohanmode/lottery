<template>
  <div class="kl8-history">
    <div class="glass-card">
      <div class="panel-head">
        <h2>快乐8 开奖历史（共 {{ total }} 期，10年数据）</h2>
        <div class="filters">
          <el-input v-model="filters.issue" placeholder="期号" clearable style="width:150px"
                    @keyup.enter="loadPage(1)" :prefix-icon="Search"/>
          <el-input v-model="filters.numbers" placeholder="号码 (多个用逗号分隔)" clearable style="width:240px"
                    @keyup.enter="loadPage(1)" :prefix-icon="Search"/>
          <el-date-picker v-model="filters.range" type="daterange" range-separator="至"
                          start-placeholder="开始日期" end-placeholder="结束日期" style="width:300px"
                          value-format="YYYY-MM-DD"/>
          <el-button type="primary" @click="loadPage(1)">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-table :data="list" size="default" class="dark-table" v-loading="loading" border stripe>
        <el-table-column label="期号" prop="issue" width="100" align="center" fixed />
        <el-table-column label="开奖日期" prop="drawDate" width="120" align="center" />
        <el-table-column label="开奖号码（20个）" min-width="640">
          <template #default="{row}">
            <span v-for="n in row.numbers" :key="n"
                  :class="['ball', 'ball-sm', numberCls(n)]" style="margin:2px">{{ n }}</span>
          </template>
        </el-table-column>
        <el-table-column label="和值" width="70" align="center">
          <template #default="{row}">
            <b style="color:#ffd666">{{ row.numbers.reduce((s,n)=>s+Number(n),0) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="奇偶" width="80" align="center">
          <template #default="{row}">
            <span style="color:#9ad">{{ oddEven(row.numbers) }}</span>
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
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { kl8 } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ pageNum: 1, pageSize: 20, issue: '', numbers: '', range: [] })

async function loadPage(p) {
  if (p) filters.pageNum = p
  loading.value = true
  try {
    const data = await kl8.list({
      pageNum: filters.pageNum, pageSize: filters.pageSize,
      issue: filters.issue || undefined,
      numbers: filters.numbers || undefined,
      startDate: filters.range?.[0] || undefined,
      endDate:   filters.range?.[1] || undefined
    })
    list.value = data.list; total.value = data.total
  } finally { loading.value = false }
}
function resetFilters() {
  filters.issue = ''; filters.numbers = ''; filters.range = []; loadPage(1)
}

// 号码着色：按四区分配色，更易区分
function numberCls(n) {
  const num = Number(n)
  if (num <= 20) return 'zone1'
  if (num <= 40) return 'zone2'
  if (num <= 60) return 'zone3'
  return 'zone4'
}
// 奇偶比
function oddEven(nums) {
  const odd = nums.filter(n => Number(n) % 2 === 1).length
  return `${odd}:${20 - odd}`
}

onMounted(() => loadPage(1))
</script>

<style lang="scss" scoped>
.kl8-history { max-width: 1500px; margin: 0 auto; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 16px;
  h2 { margin: 0; font-size: 20px; }
  .filters { display: flex; gap: 10px; flex-wrap: wrap; }
}
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.dark-table :deep(.el-table) { --el-table-header-bg-color: rgba(102,204,255,0.12);
  --el-table-header-text-color: #ffd666; --el-table-text-color: #eee;
  --el-table-row-hover-bg-color: rgba(255,255,255,0.05); --el-table-border-color: rgba(255,255,255,0.08);
  background: transparent;
  ::v-deep(.el-table__body tr) { background: transparent !important; }
  ::v-deep(.el-table__body tr:hover > td) { background: rgba(255,255,255,0.04) !important; }
  td, th { background: transparent !important; }
}
// 四区号码球配色
.ball.zone1 { background: linear-gradient(135deg,#4a90e2,#1976d2); color:#fff; }
.ball.zone2 { background: linear-gradient(135deg,#10b981,#059669); color:#fff; }
.ball.zone3 { background: linear-gradient(135deg,#f59e0b,#d97706); color:#fff; }
.ball.zone4 { background: linear-gradient(135deg,#ec4899,#db2777); color:#fff; }
</style>
