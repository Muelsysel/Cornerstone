<template>
  <div>
    <!-- 搜索栏 -->
    <el-card class="toolbar" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="请输入标题" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
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
        <el-button type="primary" :icon="Plus" @click="handleCreate">新增公告</el-button>
        <span class="tip">公开查询无需登录；新增/编辑/删除需要登录后的授权。</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '新增公告'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="PUBLISHED">发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="公告正文" />
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
import {
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncementPage,
  updateAnnouncement,
  type Announcement,
  type AnnouncementQuery,
} from '@/api/announcement'

interface AnnouncementForm {
  id?: number
  title: string
  content?: string
  status: string
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<Announcement[]>([])
const total = ref(0)

const query = reactive<AnnouncementQuery>({ pageNum: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await getAnnouncementPage(query)
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
  query.status = undefined
  query.pageNum = 1
  loadData()
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<AnnouncementForm>({ title: '', status: 'DRAFT' })

const rules: FormRules<AnnouncementForm> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { id: undefined, title: '', content: '', status: 'DRAFT' })
  dialogVisible.value = true
}

function handleEdit(row: Announcement) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    title: row.title,
    content: row.content || '',
    status: row.status || 'DRAFT',
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
      await updateAnnouncement(form)
      ElMessage.success('编辑成功')
    } else {
      await createAnnouncement(form)
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

// ---------------- 删除 ----------------
async function handleDelete(row: Announcement) {
  await ElMessageBox.confirm(`确认删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
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
  display: flex;
  align-items: center;
  gap: 12px;
}
.tip {
  font-size: 12px;
  color: #909399;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
