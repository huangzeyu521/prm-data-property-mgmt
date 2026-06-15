<template>
  <div class="prm-page">
    <!-- 查询条 -->
    <div class="prm-query-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="登录名">
          <el-input v-model="query.username" placeholder="登录名" clearable style="width: 160px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="姓名" clearable style="width: 160px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="r in ROLES" :key="r.key" :label="r.label" :value="r.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表卡 -->
    <div class="prm-table-card">
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="onCreate">新增用户</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="username" label="登录名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column label="角色" min-width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="provinceCode" label="省公司" min-width="90" align="center">
          <template #default="{ row }">{{ row.provinceCode || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === '启用' ? 'success' : 'info'">{{ row.status || '启用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-button link type="warning" @click="onResetPassword(row)">重置密码</el-button>
            <el-button link :type="row.status === '启用' ? 'info' : 'success'" @click="onToggle(row)">
              {{ row.status === '启用' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="prm-table-note">注:新增用户初始密码为 Prm@1234;角色变更即时生效;登录名创建后不可修改。</div>

      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        :page-sizes="[10, 20, 50, 100]"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录名(创建后不可改)" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in ROLES" :key="r.key" :label="r.label" :value="r.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="省公司">
          <el-input v-model="form.provinceCode" placeholder="如 GD(可空)" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="启用">启用</el-radio>
            <el-radio label="停用">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert v-if="!isEdit" type="info" :closable="false" show-icon style="margin-top: 4px"
          title="新建用户初始密码为 Prm@1234,请提醒用户首次登录后修改。" />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ROLES } from '@/lib/roles'
import { pageUser, createUser, updateUser, deleteUser, resetPassword, toggleUserStatus } from '@/api/system'

const query = reactive({ current: 1, size: 10, username: '', realName: '', role: '', status: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))
const formRef = ref()
const form = reactive({ userId: '', username: '', realName: '', role: '', provinceCode: '', status: '启用' })
const rules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function roleLabel(code) {
  const r = ROLES.find((x) => x.key === code)
  return r ? r.label : code
}
function roleTagType(code) {
  return { admin: 'danger', all: 'danger', review: 'warning', apply: 'primary', view: 'info' }[code] || ''
}

async function load() {
  loading.value = true
  try {
    const res = await pageUser({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.current = 1
  load()
}
function onReset() {
  query.username = ''
  query.realName = ''
  query.role = ''
  query.status = ''
  onSearch()
}
function onPageChange(p) {
  query.current = p
  load()
}
function onSizeChange(s) {
  query.size = s
  query.current = 1
  load()
}

function onCreate() {
  isEdit.value = false
  Object.assign(form, { userId: '', username: '', realName: '', role: '', provinceCode: '', status: '启用' })
  dialogVisible.value = true
}
function onEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    userId: row.userId,
    username: row.username,
    realName: row.realName,
    role: row.role,
    provinceCode: row.provinceCode || '',
    status: row.status || '启用',
  })
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateUser({ ...form })
      ElMessage.success('保存成功')
    } else {
      await createUser({ ...form })
      ElMessage.success('新增成功,初始密码 Prm@1234')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function onResetPassword(row) {
  ElMessageBox.confirm(`确认将「${row.realName}(${row.username})」的密码重置为默认密码 Prm@1234?`, '重置密码', { type: 'warning' })
    .then(() => resetPassword(row.userId))
    .then(() => ElMessage.success('已重置为默认密码 Prm@1234'))
    .catch(() => {})
}
function onToggle(row) {
  const next = row.status === '启用' ? '停用' : '启用'
  ElMessageBox.confirm(`确认${next}「${row.realName}(${row.username})」?`, `${next}用户`, { type: 'warning' })
    .then(() => toggleUserStatus(row.userId))
    .then(() => {
      ElMessage.success(`已${next}`)
      load()
    })
    .catch(() => {})
}
function onDelete(row) {
  ElMessageBox.confirm(`确认删除用户「${row.realName}(${row.username})」?该操作不可恢复。`, '删除用户', { type: 'warning' })
    .then(() => deleteUser(row.userId))
    .then(() => {
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.prm-page { padding: 16px; }
.prm-query-bar { margin-bottom: 16px; }
.prm-table-card { background: #fff; padding: 16px; border-radius: 4px; }
.prm-table-note { margin-top: 12px; font-size: 12px; color: #8c8c8c; }
</style>
