<template>
  <div>
    <!-- 搜索栏 -->
    <el-card class="toolbar" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="系统模块">
          <el-input v-model="query.title" placeholder="请输入操作标题" clearable />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operName" placeholder="请输入操作人" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" :value="0" />
            <el-option label="失败" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格（只读 + 清空） -->
    <el-card shadow="never">
      <div class="table-toolbar">
        <span>操作日志</span>
        <el-button
          v-permission="'system:log:remove'"
          link
          type="danger"
          :icon="Delete"
          @click="handleClean"
        >
          清空
        </el-button>
      </div>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="operId" label="ID" width="70" />
        <el-table-column prop="title" label="系统模块" min-width="130" />
        <el-table-column label="操作类型" width="100">
          <template #default="{ row }: { row: any }">
            <el-tag>{{ businessTypeText(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="110" />
        <el-table-column prop="operIp" label="操作IP" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }: { row: any }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="请求方法" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operTime" label="操作时间" min-width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button v-permission="'system:log:remove'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="640px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="ID">{{ detail.operId }}</el-descriptions-item>
        <el-descriptions-item label="系统模块">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ businessTypeText(detail.businessType) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operName }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ detail.operIp }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 0 ? 'success' : 'danger'">
            {{ detail.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.operTime }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求方法" :span="2">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="detail-pre">{{ detail.operParam }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2">
          <pre class="detail-pre">{{ detail.jsonResult }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="异常信息" :span="2">
          <pre class="detail-pre error">{{ detail.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, Search } from '@element-plus/icons-vue'
import { clearOperLog, deleteOperLog, getOperLogPage } from '@/api/system'
import { businessTypeText } from '@/utils/operlog'
import type { OperLog, OperLogQuery } from '@/types/system'
import { pageNumAfterDelete } from '@/utils/pagination'

const loading = ref(false)
const list = ref<OperLog[]>([])
const total = ref(0)

const query = reactive<OperLogQuery>({ pageNum: 1, pageSize: 10 })

/** 每页条数变化时回到第一页（避免停留在越界页码）。 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}
async function loadData() {
  loading.value = true
  try {
    const res = await getOperLogPage(query)
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.title = undefined
  query.operName = undefined
  query.status = undefined
  query.pageNum = 1
  loadData()
}

// ---------------- 详情 ----------------
const detailVisible = ref(false)
const detail = ref<OperLog | null>(null)

function handleDetail(row: OperLog) {
  detail.value = row
  detailVisible.value = true
}

// businessTypeText 已提取到 utils/operlog.ts（业务类型 → 中文文本）

async function handleDelete(row: OperLog) {
  await ElMessageBox.confirm('确认删除该条操作日志吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  }).catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteOperLog(row.operId)
    ElMessage.success('删除成功')
    // 删除当前页最后一条时回退一页，避免停留在空页
    query.pageNum = pageNumAfterDelete(query.pageNum, list.value.length)
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
  }
}

async function handleClean() {
  await ElMessageBox.confirm('确认清空全部操作日志吗？此操作不可恢复。', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  }).catch(() => Promise.reject(new Error('canceled')))
  try {
    await clearOperLog()
    ElMessage.success('已清空')
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.table-toolbar {
  margin-bottom: 12px;
  color: var(--cs-text-secondary);
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.detail-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  color: var(--cs-text-secondary);
  max-height: 160px;
  overflow: auto;
  background: var(--cs-log-bg);
  padding: 8px;
  border-radius: 4px;
}
.detail-pre.error {
  color: var(--cs-danger);
}
</style>
