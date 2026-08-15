<template>
  <div>
    <!-- 搜索栏 -->
    <el-card class="toolbar" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="字典名称">
          <el-input v-model="query.dictName" placeholder="请输入字典名称" clearable />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="query.dictType" placeholder="请输入字典类型" clearable />
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
        <el-button v-permission="'system:dict:add'" type="primary" :icon="Plus" @click="handleCreate">
          新增字典
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="dictId" label="ID" width="70" />
        <el-table-column prop="dictName" label="字典名称" min-width="150" />
        <el-table-column prop="dictType" label="字典类型" min-width="150" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleData(row)">数据项</el-button>
            <el-button v-permission="'system:dict:edit'" link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:dict:remove'" link type="danger" @click="handleDelete(row)">
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
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 字典类型新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑字典' : '新增字典'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="form.dictName" placeholder="字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" :disabled="isEdit" placeholder="如：sys_normal_disable" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
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

    <!-- 数据项管理弹窗：维护当前字典类型下的数据项 -->
    <el-dialog v-model="dataVisible" :title="`数据项 - ${currentDictType || ''}`" width="860px" destroy-on-close>
      <div class="data-toolbar">
        <el-button v-permission="'system:dict:add'" type="primary" :icon="Plus" @click="handleDataCreate">
          新增数据项
        </el-button>
      </div>
      <el-table v-loading="dataLoading" :data="dataList" border stripe>
        <el-table-column prop="dictCode" label="ID" width="70" />
        <el-table-column prop="dictLabel" label="标签" min-width="120" />
        <el-table-column prop="dictValue" label="键值" min-width="120" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:dict:edit'" link type="primary" @click="handleDataEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:dict:remove'" link type="danger" @click="handleDataDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="dataQuery.pageNum"
        v-model:page-size="dataQuery.pageSize"
        :total="dataTotal"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="loadDataList"
        @current-change="loadDataList"
      />
    </el-dialog>

    <!-- 数据项新增/编辑弹窗 -->
    <el-dialog
      v-model="dataDialogVisible"
      :title="isDataEdit ? '编辑数据项' : '新增数据项'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典类型">
          <el-input :model-value="currentDictType" disabled />
        </el-form-item>
        <el-form-item label="标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="显示标签" />
        </el-form-item>
        <el-form-item label="键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="存储键值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dataSubmitting" @click="handleDataSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createDictData,
  createDictType,
  deleteDictData,
  deleteDictType,
  getDictDataPage,
  getDictTypePage,
  updateDictData,
  updateDictType,
} from '@/api/system'
import type { DictData, DictType, DictTypeQuery } from '@/types/system'

// ---------------- 字典类型 ----------------
const loading = ref(false)
const submitting = ref(false)
const list = ref<DictType[]>([])
const total = ref(0)

const query = reactive<DictTypeQuery>({ pageNum: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await getDictTypePage(query)
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
  query.dictName = undefined
  query.dictType = undefined
  query.pageNum = 1
  loadData()
}

// ---------------- 字典类型 新增/编辑 ----------------
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<DictType>>({ status: '0' })

const rules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { dictId: undefined, dictName: '', dictType: '', status: '0', remark: '' })
  dialogVisible.value = true
}

function handleEdit(row: DictType) {
  isEdit.value = true
  Object.assign(form, {
    dictId: row.dictId,
    dictName: row.dictName,
    dictType: row.dictType,
    status: row.status || '0',
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
      await updateDictType(form)
      ElMessage.success('编辑成功')
    } else {
      await createDictType(form)
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

async function handleDelete(row: DictType) {
  await ElMessageBox.confirm(`确认删除字典「${row.dictName}」吗？`, '提示', { type: 'warning' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteDictType(row.dictId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
  }
}

// ---------------- 数据项管理 ----------------
const dataVisible = ref(false)
const dataLoading = ref(false)
const dataList = ref<DictData[]>([])
const dataTotal = ref(0)
const currentDictType = ref('')
const currentDictId = ref<number | undefined>(undefined)

const dataQuery = reactive({ pageNum: 1, pageSize: 10, dictType: '' })

async function handleData(row: DictType) {
  currentDictId.value = row.dictId
  currentDictType.value = row.dictType
  dataQuery.dictType = row.dictType
  dataQuery.pageNum = 1
  dataVisible.value = true
  await loadDataList()
}

async function loadDataList() {
  dataLoading.value = true
  try {
    const res = await getDictDataPage(dataQuery)
    dataList.value = res.records || []
    dataTotal.value = res.total || 0
  } catch {
    dataList.value = []
    dataTotal.value = 0
  } finally {
    dataLoading.value = false
  }
}

// ---------------- 数据项 新增/编辑 ----------------
const dataDialogVisible = ref(false)
const isDataEdit = ref(false)
const dataSubmitting = ref(false)
const dataFormRef = ref<FormInstance>()
const dataForm = reactive<Partial<DictData>>({ status: '0' })

const dataRules: FormRules = {
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入键值', trigger: 'blur' }],
}

function handleDataCreate() {
  isDataEdit.value = false
  Object.assign(dataForm, {
    dictCode: undefined,
    dictType: currentDictType.value,
    dictLabel: '',
    dictValue: '',
    sort: 0,
    status: '0',
    remark: '',
  })
  dataDialogVisible.value = true
}

function handleDataEdit(row: DictData) {
  isDataEdit.value = true
  Object.assign(dataForm, {
    dictCode: row.dictCode,
    dictType: currentDictType.value,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    sort: row.sort ?? 0,
    status: row.status || '0',
    remark: row.remark || '',
  })
  dataDialogVisible.value = true
}

async function handleDataSubmit() {
  if (!dataFormRef.value) return
  const valid = await dataFormRef.value.validate().catch(() => false)
  if (!valid) return
  dataSubmitting.value = true
  try {
    if (isDataEdit.value) {
      await updateDictData(dataForm)
      ElMessage.success('编辑成功')
    } else {
      await createDictData(dataForm)
      ElMessage.success('新增成功')
    }
    dataDialogVisible.value = false
    await loadDataList()
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    dataSubmitting.value = false
  }
}

async function handleDataDelete(row: DictData) {
  await ElMessageBox.confirm(`确认删除数据项「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteDictData(row.dictCode)
    ElMessage.success('删除成功')
    loadDataList()
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
.table-toolbar,
.data-toolbar {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
