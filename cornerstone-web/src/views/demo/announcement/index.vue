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
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
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
        <el-button v-permission="'demo:announcement:edit'" type="primary" :icon="Plus" @click="handleCreate">
          新增公告
        </el-button>
        <span class="tip">公告查询接口为公开 API（无需登录即可访问）；本管理页面的新增/编辑/发布/下线/删除需登录并具备编辑权限。</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }: { row: any }">
            <!-- 状态为后端整数：0草稿 1已发布 2已下线 -->
            <el-tag :type="announcementStatusTagType(row.status)">
              {{ announcementStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }: { row: any }">{{ row.publishTime || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button link type="info" @click="handleView(row)">查看</el-button>
            <!-- 草稿可发布；已发布可下线；已下线无流转按钮 -->
            <el-button
              v-if="row.status === 0"
              v-permission="'demo:announcement:edit'"
              link
              type="primary"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-else-if="row.status === 1"
              v-permission="'demo:announcement:edit'"
              link
              type="warning"
              @click="handleOffline(row)"
            >
              下线
            </el-button>
            <el-button v-permission="'demo:announcement:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'demo:announcement:edit'" link type="danger" @click="handleDelete(row)">删除</el-button>
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
      :title="isEdit ? '编辑公告' : '新增公告'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="20000" show-word-limit placeholder="公告正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗（复用后端 GET /{id}，正文全文展示） -->
    <el-dialog v-model="detailVisible" title="公告详情" width="560px">
      <template v-if="detail">
        <h3 class="detail-title">{{ detail.title }}</h3>
        <div class="detail-meta">
          <el-tag size="small" :type="announcementStatusTagType(detail.status)">
            {{ announcementStatusText(detail.status) }}
          </el-tag>
          <span v-if="detail.author">作者：{{ detail.author }}</span>
          <span v-if="detail.publishTime">发布：{{ detail.publishTime }}</span>
          <span v-if="detail.createTime">创建：{{ detail.createTime }}</span>
        </div>
        <div class="detail-content">{{ detail.content || '（无内容）' }}</div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
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
  getAnnouncementDetail,
  getAnnouncementPage,
  offlineAnnouncement,
  publishAnnouncement,
  updateAnnouncement,
  type Announcement,
  type AnnouncementQuery,
} from '@/api/announcement'
import {
  announcementStatusText,
  announcementStatusTagType,
} from '@/utils/announcement'
import { pageNumAfterDelete } from '@/utils/pagination'

interface AnnouncementForm {
  id?: number
  title: string
  content?: string
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<Announcement[]>([])
const total = ref(0)

// 详情弹窗状态
const detailVisible = ref(false)
const detail = ref<Announcement | null>(null)

async function handleView(row: Announcement) {
  try {
    detail.value = await getAnnouncementDetail(row.id)
    detailVisible.value = true
  } catch {
    // 详情加载失败（如游客态访问非发布内容）由请求拦截器提示，不打开弹窗
  }
}

const query = reactive<AnnouncementQuery>({ pageNum: 1, pageSize: 10 })

/** 每页条数变化时回到第一页（避免停留在越界页码）。 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}
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
const form = reactive<AnnouncementForm>({ title: '' })

const rules: FormRules<AnnouncementForm> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { id: undefined, title: '', content: '' })
  dialogVisible.value = true
}

function handleEdit(row: Announcement) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    title: row.title,
    content: row.content || '',
  })
  dialogVisible.value = true
}

// 状态展示映射已提取到 utils/announcement.ts（announcementStatusText/announcementStatusTagType）

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
  await ElMessageBox.confirm(`确认删除公告「${row.title}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    // 删除当前页最后一条时回退一页，避免停留在空页
    query.pageNum = pageNumAfterDelete(query.pageNum, list.value.length)
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
  }
}

// ---------------- 发布 / 下线 ----------------
async function handlePublish(row: Announcement) {
  await ElMessageBox.confirm(`确认发布公告「${row.title}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await publishAnnouncement(row.id)
    ElMessage.success('已发布')
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
  }
}

async function handleOffline(row: Announcement) {
  await ElMessageBox.confirm(`确认下线公告「${row.title}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await offlineAnnouncement(row.id)
    ElMessage.success('已下线')
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
  color: var(--cs-text-secondary);
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.detail-title {
  margin: 0 0 8px;
  font-size: 16px;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--cs-text-secondary);
}
.detail-content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: var(--cs-text);
  max-height: 320px;
  overflow-y: auto;
}
</style>
