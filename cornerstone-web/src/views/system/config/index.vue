<template>
  <div>
    <!-- 搜索栏 -->
    <el-card class="toolbar" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="参数键名">
          <el-input v-model="query.configKey" placeholder="请输入参数键名" clearable />
        </el-form-item>
        <el-form-item label="参数名称">
          <el-input v-model="query.configName" placeholder="请输入参数名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button v-permission="'system:config:add'" type="primary" :icon="Plus" @click="handleCreate">
          新增参数
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="configId" label="ID" width="70" />
        <el-table-column prop="configName" label="参数名称" min-width="150" />
        <el-table-column prop="configKey" label="参数键名" min-width="160" />
        <el-table-column prop="configValue" label="参数键值" min-width="180" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:config:edit'" link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:config:remove'" link type="danger" @click="handleDelete(row)">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑参数' : '新增参数'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" :disabled="isEdit" placeholder="如：sys.user.initPassword" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="参数键值" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createConfig, deleteConfig, getConfigPage, updateConfig } from '@/api/system'
import type { Config, ConfigQuery } from '@/types/system'
import { pageNumAfterDelete } from '@/utils/pagination'

interface ConfigForm {
  configId?: number
  configName: string
  configKey: string
  configValue: string
  remark?: string
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<Config[]>([])
const total = ref(0)

const query = reactive<ConfigQuery>({ pageNum: 1, pageSize: 10 })

/** 每页条数变化时回到第一页（避免停留在越界页码）。 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}
async function loadData() {
  loading.value = true
  try {
    const res = await getConfigPage(query)
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
  query.configKey = undefined
  query.configName = undefined
  query.pageNum = 1
  loadData()
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ConfigForm>({ configName: '', configKey: '', configValue: '' })

const rules: FormRules<ConfigForm> = {
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }],
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { configId: undefined, configName: '', configKey: '', configValue: '', remark: '' })
  dialogVisible.value = true
}

function handleEdit(row: Config) {
  isEdit.value = true
  Object.assign(form, {
    configId: row.configId,
    configName: row.configName || '',
    configKey: row.configKey,
    configValue: row.configValue || '',
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateConfig(form)
      ElMessage.success('编辑成功')
    } else {
      await createConfig(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Config) {
  await ElMessageBox.confirm(`确认删除参数「${row.configName || row.configKey}」吗？`, '提示', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }).catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteConfig(row.configId)
    ElMessage.success('删除成功')
    // 删除当前页最后一条时回退一页，避免停留在空页
    query.pageNum = pageNumAfterDelete(query.pageNum, list.value.length)
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
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
