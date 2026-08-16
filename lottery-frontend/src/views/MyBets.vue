<template>
  <div class="my-bets">
    <div class="glass-card">
      <div class="panel-head">
        <h2>我的投注记录</h2>
        <div class="actions">
          <el-select v-model="filterType" placeholder="彩种" style="width:120px" clearable>
            <el-option label="双色球" value="SSQ" />
            <el-option label="快乐8" value="KL8" />
          </el-select>
          <el-button type="warning" @click="matchAll">批量判奖(最新开奖)</el-button>
          <el-button type="primary" @click="loadList">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" size="default" border stripe class="dark-table" v-loading="loading">
        <el-table-column label="ID" prop="id" width="70" align="center" />
        <el-table-column label="彩种" prop="lotteryType" width="90" align="center">
          <template #default="{row}">
            <el-tag :type="row.lotteryType==='SSQ'?'danger':'success'" size="small">{{ row.lotteryType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="betType" width="100">
          <template #default="{row}">
            <el-tag size="small">{{ {SINGLE:'单式',COMPOUND:'复式',DANTUO:'胆拖'}[row.betType] || row.betType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="480">
          <template #default="{row}">
            <template v-if="row.betType==='DANTUO'">
              <span style="color:#ffd666">胆：</span>
              <span v-for="d in split(row.danNumbers)" :key="d" class="ball gold ball-sm" style="margin-right:3px">{{ d }}</span>
              <span style="color:#4a90e2;margin-left:8px">拖：</span>
              <span v-for="t in split(row.tuoNumbers)" :key="t" class="ball blue ball-sm" style="margin-right:3px">{{ t }}</span>
            </template>
            <span v-else>
              <span v-for="r in split(row.mainNumbers)" :key="r"
                    :class="['ball', 'ball-sm', row.lotteryType==='KL8'?'green':'red']"
                    style="margin-right:3px">{{ r }}</span>
            </span>
            <span v-if="row.extraNumbers" style="margin-left:10px">
              <span style="color:rgba(255,255,255,0.5);margin-right:4px">蓝:</span>
              <span v-for="b in split(row.extraNumbers)" :key="b" class="ball blue ball-sm" style="margin-right:3px">{{ b }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="注数" prop="combinationCnt" width="70" align="center"/>
        <el-table-column label="创建时间" prop="createdAt" width="160" align="center"/>
        <el-table-column label="最近判奖" width="240">
          <template #default="{row}">
            <template v-if="row._match">
              <el-tag v-if="row._match.won" type="warning" effect="dark">{{ row._match.levelName }}</el-tag>
              <el-tag v-else type="info" size="small">{{ row._match.levelName }}</el-tag>
              <span style="margin-left:8px;color:#ffd666" v-if="row._match.prizeAmount||row._match.totalPrize">
                ¥{{ Number(row._match.prizeAmount||row._match.totalPrize||0).toLocaleString() }}
              </span>
            </template>
            <el-tag v-else type="info" effect="plain" size="small">未判奖</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{row}">
            <el-button size="small" type="warning" link @click="matchOne(row)">判奖</el-button>
            <el-button size="small" type="danger" link @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination background layout="total, prev, pager, next"
          :total="total" :current-page="pageNum" :page-size="pageSize"
          @current-change="loadList"/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { bet as betApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const filterType = ref('')

const split = (s) => !s ? [] : s.split(',').filter(Boolean)

async function loadList() {
  loading.value = true
  try {
    const r = await betApi.list({
      pageNum: pageNum.value, pageSize: pageSize.value,
      lotteryType: filterType.value || undefined
    })
    list.value = r.list || []; total.value = r.total || 0
  } finally { loading.value = false }
}

async function matchOne(row) {
  try {
    const r = await betApi.match(row.id)
    row._match = r
    ElMessage.success(`判奖完成：${r.levelName} ${r.totalPrize?('¥'+Number(r.totalPrize).toLocaleString()):''}`)
  } catch (e) {}
}
async function matchAll() {
  try {
    await ElMessageBox.confirm(`将对所有双色球投注记录按最新开奖批量判奖，继续？`, '批量判奖', { type: 'warning' })
    const r = await betApi.matchAll()
    ElMessage.success(`已对 ${r.matched||'?'} 条投注判奖，中奖 ${r.wonCount||0} 条`)
    loadList()
  } catch {}
}
async function del(row) {
  try {
    await ElMessageBox.confirm(`确认删除投注 ID=${row.id}？`, '删除', { type: 'error' })
    await betApi.delete(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch {}
}

onMounted(loadList)
</script>

<style lang="scss" scoped>
.my-bets { max-width: 1600px; margin: 0 auto; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px;
  h2 { margin: 0; font-size: 20px; }
  .actions { display: flex; gap: 10px; flex-wrap: wrap; }
}
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.dark-table :deep(.el-table) {
  --el-table-header-bg-color: rgba(255,107,53,0.12);
  --el-table-header-text-color: #ffd666; --el-table-text-color: #eee;
  --el-table-row-hover-bg-color: rgba(255,255,255,0.04);
  --el-table-border-color: rgba(255,255,255,0.08);
  background: transparent;
  td, th { background: transparent !important; }
}
</style>
