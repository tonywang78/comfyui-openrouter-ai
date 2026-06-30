<template>
  <div class="api-key-container">
    <el-card shadow="never" class="content-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>{{ t('system.apiKeys.title') }}</span>
          <div class="header-actions">
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              {{ t('system.apiKeys.create') }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
          v-model="query.keyword"
          :placeholder="t('system.apiKeys.search.keyword')"
          clearable
          style="width: 240px"
          @keyup.enter="loadPage(1)"
          @clear="onSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="query.status"
          :placeholder="t('system.apiKeys.search.status')"
          clearable
          style="width: 160px"
          @change="onSearch"
        >
          <el-option :value="1" :label="t('system.apiKeys.status.enabled')" />
          <el-option :value="0" :label="t('system.apiKeys.status.disabled')" />
        </el-select>
        <el-input
          v-model="query.userIdInput"
          :placeholder="t('system.apiKeys.search.userId')"
          clearable
          style="width: 160px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-button type="primary" @click="onSearch">
          <el-icon><Search /></el-icon>
          {{ t('system.apiKeys.search.query') }}
        </el-button>
        <el-button class="reset-btn" @click="onReset">
          <el-icon><RefreshRight /></el-icon>
          {{ t('system.apiKeys.search.reset') }}
        </el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="table.items" class="ak-table" height="100%" border stripe>
          <el-table-column prop="id" :label="t('system.apiKeys.table.id')" width="80" />
          <el-table-column prop="name" :label="t('system.apiKeys.table.name')" min-width="140" show-overflow-tooltip />
          <el-table-column :label="t('system.apiKeys.table.keyPrefix')" min-width="140">
            <template #default="{ row }">
              <el-text style="font-family: monospace;">{{ row.keyPrefix }}****</el-text>
            </template>
          </el-table-column>
          <el-table-column :label="t('system.apiKeys.table.user')" min-width="160">
            <template #default="{ row }">
              {{ row.userNickname || '-' }} ({{ row.userId }})
            </template>
          </el-table-column>
          <el-table-column :label="t('system.apiKeys.table.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expiresAt" :label="t('system.apiKeys.table.expiresAt')" min-width="160">
            <template #default="{ row }">{{ formatTime(row.expiresAt) }}</template>
          </el-table-column>
          <el-table-column prop="lastUsedAt" :label="t('system.apiKeys.table.lastUsedAt')" min-width="160">
            <template #default="{ row }">{{ formatTime(row.lastUsedAt) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('system.apiKeys.table.createTime')" min-width="160">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column :label="t('system.apiKeys.table.actions')" fixed="right" width="240">
            <template #default="{ row }">
              <el-button size="small" @click="openEditDialog(row)">{{ t('system.apiKeys.table.edit') }}</el-button>
              <el-button size="small" type="warning" @click="onRotate(row)">{{ t('system.apiKeys.table.rotate') }}</el-button>
              <el-popconfirm :title="t('system.apiKeys.messages.deleteConfirm')" @confirm="onDelete(row)">
                <template #reference>
                  <el-button size="small" type="danger">{{ t('system.apiKeys.table.delete') }}</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="table.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="createDialog.visible"
      :title="t('system.apiKeys.dialog.createTitle')"
      width="520px"
      @close="onCreateDialogClose"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item :label="t('system.apiKeys.dialog.user')" prop="userId">
          <el-select
            v-model="createForm.userId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            :placeholder="t('system.apiKeys.dialog.userPlaceholder')"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="`${user.nickname || user.email || user.phone || user.id} (${user.id})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('system.apiKeys.dialog.name')" prop="name">
          <el-input v-model="createForm.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('system.apiKeys.dialog.expiresAt')" prop="expiresAt">
          <el-date-picker
            v-model="createForm.expiresAt"
            type="datetime"
            :placeholder="t('system.apiKeys.dialog.expiresAtPlaceholder')"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="createSubmitting" @click="createDialog.visible = false">
          {{ t('system.apiKeys.dialog.cancel') }}
        </el-button>
        <el-button type="primary" :loading="createSubmitting" @click="onSubmitCreate">
          {{ t('system.apiKeys.dialog.createBtn') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editDialog.visible"
      :title="t('system.apiKeys.dialog.editTitle')"
      width="520px"
      @close="onEditDialogClose"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item :label="t('system.apiKeys.table.keyPrefix')">
          <el-text type="info" style="font-family: monospace;">{{ editDialog.record?.keyPrefix }}****</el-text>
        </el-form-item>
        <el-form-item :label="t('system.apiKeys.dialog.name')" prop="name">
          <el-input v-model="editForm.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('system.apiKeys.dialog.status')" prop="status">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option :value="1" :label="t('system.apiKeys.status.enabled')" />
            <el-option :value="0" :label="t('system.apiKeys.status.disabled')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('system.apiKeys.dialog.expiresAt')" prop="expiresAt">
          <el-date-picker
            v-model="editForm.expiresAt"
            type="datetime"
            :placeholder="t('system.apiKeys.dialog.expiresAtPlaceholder')"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="editSubmitting" @click="editDialog.visible = false">
          {{ t('system.apiKeys.dialog.cancel') }}
        </el-button>
        <el-button type="primary" :loading="editSubmitting" @click="onSubmitEdit">
          {{ t('system.apiKeys.dialog.saveBtn') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="plainKeyDialog.visible"
      :title="t('system.apiKeys.plainKey.title')"
      width="560px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-alert type="warning" :closable="false" show-icon :title="t('system.apiKeys.plainKey.warning')" />
      <div class="plain-key-box">
        <el-input :model-value="plainKeyDialog.plainKey" readonly>
          <template #append>
            <el-button @click="copyPlainKey">{{ t('system.apiKeys.plainKey.copy') }}</el-button>
          </template>
        </el-input>
      </div>
      <template #footer>
        <el-button type="primary" @click="plainKeyDialog.visible = false">
          {{ t('system.apiKeys.plainKey.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { SystemApiKeyApi } from '@/api/system-api-key/types'
import { systemApiKeyApi } from '@/api/system-api-key/system-api-key'
import { systemUserApi } from '@/api/system-user/system-user'
import type { GetUserPageApi } from '@/api/system-user/types'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loading = ref(false)
const userSearchLoading = ref(false)
const userOptions = ref<GetUserPageApi.SystemUser[]>([])

const query = reactive<{
  page: number
  size: number
  keyword: string
  status?: 0 | 1
  userIdInput: string
}>({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined,
  userIdInput: ''
})

const table = reactive<{ total: number; items: SystemApiKeyApi.SystemApiKeyVo[] }>({
  total: 0,
  items: []
})

const parseUserId = () => {
  const value = query.userIdInput.trim()
  if (!value) return undefined
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : undefined
}

const loadPage = async (toPage?: number) => {
  if (typeof toPage === 'number') query.page = toPage
  loading.value = true
  try {
    const data = await systemApiKeyApi.fetchKeys({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
      userId: parseUserId()
    })
    table.total = data.total
    table.items = data.items
  } catch (e: any) {
    console.error(e)
    ElMessage.error(e?.message || t('system.apiKeys.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onPageChange = (p: number) => {
  query.page = p
  loadPage()
}

const onSizeChange = (s: number) => {
  query.size = s
  loadPage(1)
}

const onSearch = () => loadPage(1)

const onReset = () => {
  query.keyword = ''
  query.status = undefined
  query.userIdInput = ''
  loadPage(1)
}

const formatTime = (iso?: string | null) => {
  if (!iso) return '-'
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch {
    return iso
  }
}

const statusText = (status: 0 | 1) =>
  status === 1 ? t('system.apiKeys.status.enabled') : t('system.apiKeys.status.disabled')

const statusTagType = (status: 0 | 1): 'success' | 'info' =>
  status === 1 ? 'success' : 'info'

const searchUsers = async (keyword: string) => {
  userSearchLoading.value = true
  try {
    const data = await systemUserApi.reqGetUserPage({
      page: 1,
      size: 20,
      keyword: keyword || undefined
    })
    userOptions.value = data.items
  } catch (e) {
    console.error(e)
  } finally {
    userSearchLoading.value = false
  }
}

const createDialog = reactive({ visible: false })
const editDialog = reactive<{ visible: boolean; record: SystemApiKeyApi.SystemApiKeyVo | null }>({
  visible: false,
  record: null
})
const plainKeyDialog = reactive({ visible: false, plainKey: '' })

const showPlainKey = (plainKey: string) => {
  plainKeyDialog.plainKey = plainKey
  plainKeyDialog.visible = true
}

const copyPlainKey = async () => {
  try {
    await navigator.clipboard.writeText(plainKeyDialog.plainKey)
    ElMessage.success(t('system.apiKeys.plainKey.copySuccess'))
  } catch {
    ElMessage.error(t('system.apiKeys.plainKey.copyFailed'))
  }
}

const openCreateDialog = async () => {
  createDialog.visible = true
  await searchUsers('')
}

const openEditDialog = (row: SystemApiKeyApi.SystemApiKeyVo) => {
  editDialog.record = row
  editDialog.visible = true
  editForm.id = row.id
  editForm.name = row.name
  editForm.status = row.status
  editForm.expiresAt = row.expiresAt || null
}

const onDelete = async (row: SystemApiKeyApi.SystemApiKeyVo) => {
  try {
    await systemApiKeyApi.deleteKey(row.id)
    ElNotification.success(t('system.apiKeys.messages.deleteSuccess'))
    loadPage()
  } catch (e: any) {
    ElMessage.error(e?.message || t('system.apiKeys.messages.deleteFailed'))
  }
}

const onRotate = async (row: SystemApiKeyApi.SystemApiKeyVo) => {
  try {
    const result = await systemApiKeyApi.rotateKey(row.id)
    ElNotification.success(t('system.apiKeys.messages.rotateSuccess'))
    showPlainKey(result.plainKey)
    loadPage()
  } catch (e: any) {
    ElMessage.error(e?.message || t('system.apiKeys.messages.rotateFailed'))
  }
}

const createForm = reactive<{ userId?: number; name: string; expiresAt?: string | null }>({
  userId: undefined,
  name: '',
  expiresAt: null
})

const createFormRef = ref<FormInstance>()
const createSubmitting = ref(false)

const createRules = computed<FormRules>(() => ({
  userId: [{ required: true, message: t('system.apiKeys.validation.userRequired'), trigger: 'change' }],
  name: [{ required: true, message: t('system.apiKeys.validation.nameRequired'), trigger: 'blur' }]
}))

const onCreateDialogClose = () => {
  createForm.userId = undefined
  createForm.name = ''
  createForm.expiresAt = null
  createFormRef.value?.clearValidate()
}

const onSubmitCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid || createForm.userId == null) return
    createSubmitting.value = true
    try {
      const result = await systemApiKeyApi.createKey({
        userId: createForm.userId,
        name: createForm.name.trim(),
        expiresAt: createForm.expiresAt || undefined
      })
      createDialog.visible = false
      ElNotification.success(t('system.apiKeys.messages.createSuccess'))
      showPlainKey(result.plainKey)
      loadPage(1)
    } catch (e: any) {
      ElMessage.error(e?.message || t('system.apiKeys.messages.createFailed'))
    } finally {
      createSubmitting.value = false
    }
  })
}

const editForm = reactive<{ id: number; name: string; status: 0 | 1; expiresAt?: string | null }>({
  id: 0,
  name: '',
  status: 1,
  expiresAt: null
})

const editFormRef = ref<FormInstance>()
const editSubmitting = ref(false)

const editRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('system.apiKeys.validation.nameRequired'), trigger: 'blur' }]
}))

const onEditDialogClose = () => {
  editDialog.record = null
  editFormRef.value?.clearValidate()
}

const onSubmitEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      await systemApiKeyApi.updateKey({
        id: editForm.id,
        name: editForm.name.trim(),
        status: editForm.status,
        expiresAt: editForm.expiresAt
      })
      editDialog.visible = false
      ElNotification.success(t('system.apiKeys.messages.updateSuccess'))
      loadPage()
    } catch (e: any) {
      ElMessage.error(e?.message || t('system.apiKeys.messages.updateFailed'))
    } finally {
      editSubmitting.value = false
    }
  })
}

onMounted(() => {
  loadPage(1)
})
</script>

<style scoped>
.api-key-container {
  height: 100%;
}

.content-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.table-wrapper {
  flex: 1;
  min-height: 400px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.plain-key-box {
  margin-top: 16px;
}
</style>
