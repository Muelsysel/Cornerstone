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
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格（只读） -->
    <el-card shadow="never">
      <div class="table-toolbar">操作日志</div>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="operId" label="ID" width="70" />
        <el-table-column prop="title" label="系统模块" min-width="130" />
        <el-table-column label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ businessTypeText(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="110" />
        <el-table-column prop="operIp" label="操作IP" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="请求方法" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operTime" label="操作时间" min-width="170" />
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getOperLogPage } from '@/api/system'
import type { OperLog, OperLogQuery } from '@/types/system'

const loading = ref(false)
const list = ref<OperLog[]>([])
const total = ref(0)

const query = reactive<OperLogQuery>({ pageNum: 1, pageSize: 10 })

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
  query.pageNum = 1
  loadData()
}

// 业务类型枚举映射（后端 businessType 为数字）。未知值宽容显示为「其他」。
function businessTypeText(type: number | undefined): string {
  const map: Record<number, string> = {
    1: '新增',
    2: '修改',
    3: '删除',
    5: '导出',
    6: '导入',
    7: '强退',
    8: '清空',
    9: '查询',
  }
  return type !== undefined && type in map ? map[type] : '其他'
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.table-toolbar {
  margin-bottom: 12px;
  color: #606266;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
