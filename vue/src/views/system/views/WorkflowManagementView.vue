<template>
  <div class="workflow-management-view">
    <el-card shadow="never" class="content-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('system.workflow.title') }}</span>
          <div style="display: flex; gap: 8px; align-items: center;">
            <el-button class="secondary-btn" @click="openCategoryDialog">{{ t('system.workflow.categoryManagement') }}</el-button>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              {{ t('system.workflow.create') }}
            </el-button>
          </div>
        </div>
      </template>
      <div class="content">
        <div style="width: 100%; padding: 12px; display: flex; flex-direction: column; gap: 12px; min-height: 0;">
          <div class="search-bar" style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
            <el-input v-model="query.keyword" :placeholder="t('system.workflow.search.keyword')" clearable style="width: 260px" @clear="handleReset" @keyup.enter="handleSearch">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="query.categoryId" clearable :placeholder="t('system.workflow.search.category')" style="width: 220px" @change="handleSearch">
              <el-option :value="undefined" :label="t('system.workflow.search.allCategories')" />
              <el-option v-for="c in categoryList" :key="c.categoryId" :label="c.name" :value="c.categoryId" />
            </el-select>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              {{ t('system.workflow.search.query') }}
            </el-button>
            <el-button class="reset-btn" @click="handleReset">
              <el-icon><RefreshRight /></el-icon>
              {{ t('system.workflow.search.reset') }}
            </el-button>
          </div>

          <div class="table-wrapper">
          <el-table :data="table.items" border stripe style="width: 100%" height="100%" class="workflow-table">
            <el-table-column :label="t('system.workflow.table.cover')" width="84">
              <template #default="{ row }">
                <div
                  v-if="row.url"
                  class="workflow-cover-thumb"
                >
                  <CoverMedia :src="row.url" :alt="t('system.workflow.table.cover')" fit="cover" />
                </div>
                <div v-else style="width: 56px; height: 56px; border-radius: 4px; background: var(--el-fill-color);"></div>
              </template>
            </el-table-column>
            <el-table-column prop="workflowId" :label="t('system.workflow.table.id')" width="90" />
            <el-table-column prop="name" :label="t('system.workflow.table.name')" min-width="200" show-overflow-tooltip />
            <el-table-column prop="description" :label="t('system.workflow.table.description')" min-width="260" show-overflow-tooltip />
            <el-table-column prop="categoryName" :label="t('system.workflow.table.category')" width="160" show-overflow-tooltip />
            <el-table-column prop="creditsDeducted" :label="t('system.workflow.table.credits')" width="110" />
            <el-table-column :label="t('system.workflow.table.published')" width="100">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.published"
                  @change="(val: boolean) => togglePublish(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.workflow.table.requiredLevel')" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.requiredLevel === Role.ADMIN" type="danger" size="small">{{ t('system.workflow.levels.admin') }}</el-tag>
                <el-tag v-else-if="row.requiredLevel === Role.VIP" type="warning" size="small">{{ t('system.workflow.levels.vip') }}</el-tag>
                <el-tag v-else type="info" size="small">{{ t('system.workflow.levels.user') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('system.workflow.table.actions')" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :loading="loadingDetailId === row.workflowId" @click="openEditDialog(row)">{{ t('system.workflow.table.edit') }}</el-button>
                <el-button link type="danger" @click="confirmDelete(row)">{{ t('system.workflow.table.delete') }}</el-button>
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
        </div>
      </div>
    </el-card>
    <!-- 新建工作流对话框 -->
    <el-dialog v-model="createDialogVisible" :title="dialogMode === 'edit' ? t('system.workflow.dialog.editTitle') : t('system.workflow.dialog.createTitle')" width="960px" class="workflow-dialog" @close="resetAll">
      <div class="dialog-content">
        <!-- 基本信息 -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-header">{{ t('system.workflow.dialog.basicInfo') }}</div>
          </template>
          <div class="base-form">
            <el-form ref="createFormRef" :model="baseForm" :rules="createFormRules" label-width="100px" label-position="left">
              <el-form-item :label="t('system.workflow.dialog.name')" prop="name">
                <el-input v-model="baseForm.name" :placeholder="t('system.workflow.dialog.namePlaceholder')" />
              </el-form-item>
              <div class="base-form-grid">
                <el-form-item :label="t('system.workflow.dialog.category')" prop="workflowCategoryId">
                  <el-select
                    v-model="baseForm.workflowCategoryId"
                    :placeholder="t('system.workflow.dialog.categoryPlaceholder')"
                    style="width: 100%"
                  >
                    <el-option v-for="c in categoryList" :key="c.categoryId" :label="c.name" :value="c.categoryId" />
                  </el-select>
                </el-form-item>
                <el-form-item :label="t('system.workflow.dialog.credits')" prop="creditsDeducted">
                  <el-input-number v-model="baseForm.creditsDeducted" :min="0" :max="100000" style="width: 100%" />
                </el-form-item>
                <el-form-item :label="t('system.workflow.dialog.requiredLevel')">
                  <el-select v-model="baseForm.requiredLevel" style="width: 100%">
                    <el-option :label="t('system.workflow.levels.user')" :value="Role.USER" />
                    <el-option :label="t('system.workflow.levels.vip')" :value="Role.VIP" />
                    <el-option :label="t('system.workflow.levels.admin')" :value="Role.ADMIN" />
                  </el-select>
                </el-form-item>
                <el-form-item :label="t('system.workflow.dialog.published')">
                  <el-switch v-model="baseForm.published" />
                </el-form-item>
              </div>
              <el-form-item :label="t('system.workflow.dialog.cover')">
                <div class="cover-field">
                  <div class="cover-field__main">
                    <el-input v-model="baseForm.url" :placeholder="t('system.workflow.dialog.coverPlaceholder')" />
                    <el-upload
                      :show-file-list="false"
                      :http-request="handleCoverUpload"
                      accept="image/*,video/*,.gif"
                      :disabled="coverUploading"
                    >
                      <el-button class="secondary-btn" :loading="coverUploading">
                        {{ coverUploading ? t('system.workflow.dialog.coverUploading') : t('system.workflow.dialog.upload') }}
                      </el-button>
                    </el-upload>
                  </div>
                  <div v-if="baseForm.url" class="cover-preview">
                    <CoverMedia
                      :key="baseForm.url"
                      :src="baseForm.url"
                      fit="cover"
                      :alt="t('system.workflow.dialog.cover')"
                      :error-text="t('system.workflow.dialog.coverPreviewFailed')"
                    />
                  </div>
                </div>
              </el-form-item>
              <el-form-item :label="t('system.workflow.dialog.description')">
                <el-input v-model="baseForm.description" type="textarea" :rows="2" :placeholder="t('system.workflow.dialog.descriptionPlaceholder')" />
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <!-- 解析工作流 -->
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-header">{{ t('system.workflow.dialog.parseWorkflow') }}</div>
          </template>
          <div class="parse-row">
            <input ref="jsonFileRef" type="file" accept="application/json,.json" @change="onWorkflowFileChange" v-show="false" />
            <el-button class="secondary-btn" :loading="parsing" @click="triggerChooseFile">{{ t('system.workflow.dialog.chooseFile') }}</el-button>
            <el-text type="info" v-if="parseResult.json">{{ dialogMode === 'edit' ? t('system.workflow.dialog.loaded') : t('system.workflow.dialog.parsed') }}</el-text>
          </div>
          <div class="parse-summary" v-if="parseResult.formNodeList.length || parseResult.allNodeList.length">
            <el-alert type="success" show-icon :closable="false">
              <template #title>
                {{ t('system.workflow.dialog.parseSummary', { formNodes: parseResult.formNodeList.length, allNodes: parseResult.allNodeList.length }) }}
              </template>
            </el-alert>
          </div>
        </el-card>

        <!-- 配置输入表单节点 -->
        <el-card shadow="never" class="section-card" v-if="parseResult.formNodeList.length">
          <template #header>
            <div class="section-header-row">
              <div class="section-header-main">
                <span class="section-header">{{ t('system.workflow.dialog.inputConfig') }}</span>
                <el-text type="info" size="small">{{ t('system.workflow.dialog.inputConfigHint') }}</el-text>
              </div>
              <div class="section-header-actions">
                <el-tag type="info" size="small">{{ t('system.workflow.dialog.configuredInputs', { count: configFormNodes.length }) }}</el-tag>
                <el-tag v-if="availableFormFieldOptions.length" type="success" size="small">{{ t('system.workflow.dialog.availableInputs', { count: availableFormFieldOptions.length }) }}</el-tag>
                <el-select
                  v-model="pendingInputFieldKey"
                  :placeholder="t('system.workflow.dialog.selectInputFieldToAdd')"
                  class="input-field-picker"
                  clearable
                  :disabled="!availableFormFieldOptions.length"
                >
                  <el-option
                    v-for="opt in availableFormFieldOptions"
                    :key="opt.fieldKey"
                    :label="opt.label"
                    :value="opt.fieldKey"
                  />
                </el-select>
                <el-button
                  type="primary"
                  link
                  :disabled="!pendingInputFieldKey"
                  @click="confirmAddFormConfig"
                >
                  {{ t('system.workflow.dialog.addInput') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-empty
            v-if="!configFormNodes.length"
            :description="availableFormFieldOptions.length ? t('system.workflow.dialog.inputConfigEmpty') : t('system.workflow.dialog.noAvailableInputFields')"
            :image-size="72"
          />
          <div v-else class="form-nodes">
            <div class="form-node" v-for="(item, idx) in configFormNodes" :key="item.fieldKey">
              <div class="form-node__toolbar">
                <el-select
                  v-model="item.fieldKey"
                  :placeholder="t('system.workflow.dialog.selectInputField')"
                  class="form-node__field-select"
                  @change="onFormFieldChange(item)"
                >
                  <el-option
                    v-for="opt in getFormFieldOptions(item)"
                    :key="opt.fieldKey"
                    :label="opt.label"
                    :value="opt.fieldKey"
                  />
                </el-select>
                <el-tag type="info" size="small">{{ item.inputs }}</el-tag>
                <el-button link type="danger" @click="removeFormConfig(idx)">{{ t('system.workflow.dialog.removeInput') }}</el-button>
              </div>

              <div class="form-node__grid">
                <div class="form-node__control">
                  <span class="form-node__label">{{ t('system.workflow.dialog.enableForm') }}</span>
                  <el-select v-model="item.type" style="width: 100%">
                    <el-option v-for="type in getAvailableTypes(item)" :key="type" :label="t('system.workflow.formTypes.' + type)" :value="type" />
                  </el-select>
                </div>
                <div class="form-node__control">
                  <span class="form-node__label">{{ t('system.workflow.dialog.required') }} / {{ t('system.workflow.dialog.hidden') }}</span>
                  <div class="form-node__switches">
                    <el-switch v-model="item.required" :disabled="item.hidden" :active-text="t('system.workflow.dialog.required')" :inactive-text="t('system.workflow.dialog.optional')" />
                    <el-switch v-model="item.hidden" :active-text="t('system.workflow.dialog.hidden')" :inactive-text="t('system.workflow.dialog.visible')" />
                  </div>
                </div>
                <div class="form-node__control span-2">
                  <span class="form-node__label">{{ t('system.workflow.dialog.formLabel') }}</span>
                  <el-input v-model="item.tips" :placeholder="t('system.workflow.dialog.formLabel')" />
                </div>
                <div class="form-node__control">
                  <span class="form-node__label">{{ item.hidden ? t('system.workflow.dialog.hiddenTemplateRequired') : t('system.workflow.dialog.defaultTemplate') }}</span>
                  <el-input v-model="item.template" :placeholder="item.hidden ? t('system.workflow.dialog.hiddenTemplateRequired') : t('system.workflow.dialog.defaultTemplate')" />
                </div>
                <div class="form-node__control">
                  <span class="form-node__label">{{ t('system.workflow.dialog.sizeLength') }}</span>
                  <el-input-number v-model="item.size" :min="0" :max="100000" :controls="false" style="width: 100%" :placeholder="t('system.workflow.dialog.sizeLength')" />
                </div>
              </div>

              <div class="form-node__extra" v-if="(item.type === WorkflowFormTypeEnum.RADIO_SELECTOR || item.type === WorkflowFormTypeEnum.CHECKBOX_SELECTOR)">
                <span class="form-node__label">{{ t('system.workflow.dialog.options') }}</span>
                <el-input type="textarea" v-model="item.options" :rows="3" :placeholder="t('system.workflow.dialog.optionsPlaceholder')" />
              </div>

              <div class="form-node__extra form-node__prompt-assist" v-if="item.type === WorkflowFormTypeEnum.TEXT_PROMPT">
                <el-select
                  v-model="item.promptStyle"
                  style="width: 220px"
                  :placeholder="t('system.workflow.dialog.promptStylePlaceholder')"
                  @change="onPromptStyleChange(item)"
                >
                  <el-option
                    v-for="style in PROMPT_STYLE_OPTIONS"
                    :key="style"
                    :label="t('system.workflow.promptStyles.' + style)"
                    :value="style"
                  />
                </el-select>
                <el-select
                  v-if="item.promptStyle && item.promptStyle !== PromptStyleEnum.NONE"
                  v-model="item.promptImageRefs"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  style="flex: 1; min-width: 240px"
                  :placeholder="t('system.workflow.dialog.promptImageRefsPlaceholder')"
                >
                  <el-option
                    v-for="opt in getImageFieldOptions(item)"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 配置输出节点 -->
        <el-card shadow="never" class="section-card" v-if="parseResult.json">
          <template #header>
            <div class="section-header-row">
              <div class="section-header-main">
                <span class="section-header">{{ t('system.workflow.dialog.outputConfig') }}</span>
                <el-text type="info" size="small">{{ t('system.workflow.dialog.outputConfigHint') }}</el-text>
              </div>
              <el-button v-if="outputNodes.length < 1" type="primary" link @click="addOutputConfig">{{ t('system.workflow.dialog.addOutput') }}</el-button>
            </div>
          </template>
          <el-empty v-if="!outputNodes.length" :description="t('system.workflow.dialog.outputConfigEmpty')" :image-size="72">
            <el-button type="primary" @click="addOutputConfig">{{ t('system.workflow.dialog.addOutput') }}</el-button>
          </el-empty>
          <div v-else class="output-config">
            <div class="output-row" v-for="(o, i) in outputNodes" :key="i">
              <el-select v-model="o.nodeKey" :placeholder="t('system.workflow.dialog.selectNode')" class="output-row__node">
                <el-option v-for="n in parseResult.allNodeList" :key="n.nodeKey" :label="formatNodeLabel(n)" :value="n.nodeKey" />
              </el-select>
              <el-select v-model="o.type" :placeholder="t('system.workflow.dialog.selectType')" class="output-row__type">
                <el-option v-for="(label, value) in outputTypeLabelMap" :key="value" :label="label" :value="value" />
              </el-select>
              <el-button link type="danger" @click="removeOutputConfig(i)">{{ t('system.workflow.dialog.removeOutput') }}</el-button>
            </div>
          </div>
        </el-card>
      </div>
      <template #footer>
        <el-button class="cancel-btn" @click="createDialogVisible = false">{{ t('system.workflow.dialog.cancel') }}</el-button>
        <el-button type="primary" :disabled="!canSubmit" :loading="saving" @click="handleSave">{{ t('system.workflow.dialog.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 类别管理对话框 -->
    <el-dialog v-model="categoryDialogVisible" :title="t('system.workflow.dialog.categoryTitle')" width="620px" class="workflow-category-dialog">
      <div style="display: flex; justify-content: flex-end; margin-bottom: 12px;">
        <el-button type="primary" @click="openCreateCategoryDialog">{{ t('system.workflow.dialog.createCategory') }}</el-button>
      </div>
      <el-table :data="categoryList" border size="small" class="workflow-category-table">
        <el-table-column prop="categoryId" :label="t('system.workflow.table.id')" width="90" />
        <el-table-column :label="t('system.workflow.dialog.name')">
          <template #default="{ row }">
            <el-input v-model="row.name" />
          </template>
        </el-table-column>
        <el-table-column :label="t('system.workflow.table.actions')" width="160">
          <template #default="{ row }">
            <el-button link type="primary" :loading="row.__updating" @click="updateCategory(row)">{{ t('system.workflow.dialog.save') }}</el-button>
            <el-button link type="danger" :loading="row.__deleting" @click="deleteCategory(row)">{{ t('system.workflow.table.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新增类别对话框 -->
    <el-dialog v-model="createCategoryDialogVisible" :title="t('system.workflow.dialog.createCategoryTitle')" width="400px" class="workflow-category-create-dialog">
      <el-form :model="categoryForm" :rules="categoryFormRules" ref="categoryFormRef" label-width="80px">
        <el-form-item :label="t('system.workflow.dialog.categoryName')" prop="name">
          <el-input v-model="categoryForm.name" :placeholder="t('system.workflow.dialog.categoryNamePlaceholder')" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="createCategoryDialogVisible = false">{{ t('system.workflow.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="creatingCategory" @click="submitCreateCategory">{{ t('system.workflow.dialog.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Plus, Search, RefreshRight } from '@element-plus/icons-vue';
import { ElNotification, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { workflowApi } from '@/api/system-workflow/system-workflow'
import type { ParsingWorkflowVo, FormNodeConfig, OutputNodeConfig, WorkflowListItem, WorkflowDetailVo } from '@/api/system-workflow/types'
import { ossApi } from '@/api/oss/oss'
import CoverMedia from '@/components/common/CoverMedia.vue'
import { WorkflowFormTypeEnum, WorkflowResultModelTypeEnum, WorkflowResultModelDigitalEnum } from '@/enums/workflow'
import { PromptStyleEnum, PROMPT_STYLE_OPTIONS } from '@/enums/promptStyle'
import { Role } from '@/enums/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const createDialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingWorkflowId = ref<number | null>(null)
const loadingDetailId = ref<number | null>(null)
const parsing = ref(false)
const saving = ref(false)
const coverUploading = ref(false)

const jsonFileRef = ref<HTMLInputElement | null>(null)
const createFormRef = ref<FormInstance>()

const baseForm = reactive<{
  name: string
  description: string
  url: string
  workflowCategoryId?: number
  creditsDeducted: number
  published: boolean
  requiredLevel: Role
}>({
  name: '',
  description: '',
  url: '',
  workflowCategoryId: undefined,
  creditsDeducted: 0,
  published: false,
  requiredLevel: Role.USER
})

// 创建工作流表单验证规则
const createFormRules = computed<FormRules>(() => ({
  name: [
    { required: true, message: t('system.workflow.validation.nameRequired'), trigger: 'blur' },
    { min: 1, max: 100, message: t('system.workflow.validation.nameLength'), trigger: 'blur' }
  ],
  workflowCategoryId: [
    { required: true, message: t('system.workflow.validation.categoryRequired'), trigger: 'change' }
  ],
  creditsDeducted: [
    { required: true, message: t('system.workflow.validation.creditsRequired'), trigger: 'blur' },
    { type: 'number', min: 0, message: t('system.workflow.validation.creditsMin'), trigger: 'blur' }
  ]
}))

const parseResult = reactive<ParsingWorkflowVo>({
  json: '',
  allNodeList: [],
  formNodeList: []
})

type ConfigFormNode = {
  fieldKey: string
  nodeKey: string
  type: WorkflowFormTypeEnum
  inputs: WorkflowResultModelDigitalEnum
  tips: string
  options?: string
  template?: string
  required: boolean
  size?: number
  hidden: boolean
  promptStyle: PromptStyleEnum | string
  promptImageRefs: string[]
}

type ParsedFormNode = ParsingWorkflowVo['formNodeList'][number]

const formFieldKey = (nodeKey: string, inputs: string) => `${nodeKey}_${inputs}`

const configFormNodes = ref<ConfigFormNode[]>([])
const pendingInputFieldKey = ref('')
const outputNodes = ref<OutputNodeConfig[]>([])

const formatFormFieldLabel = (n: ParsedFormNode) => {
  return `${n.tips || t('system.workflow.dialog.unnamedNode')} (${n.nodeKey}) · ${n.nodeDigital}`
}

const buildFormFieldOptionPool = () =>
  parseResult.formNodeList.map(n => ({
    fieldKey: formFieldKey(n.nodeKey, n.nodeDigital),
    label: formatFormFieldLabel(n),
    parseNode: n
  }))

const availableFormFieldOptions = computed(() => {
  const used = new Set(configFormNodes.value.map(n => n.fieldKey))
  return buildFormFieldOptionPool().filter(opt => !used.has(opt.fieldKey))
})

watch(availableFormFieldOptions, opts => {
  if (pendingInputFieldKey.value && !opts.some(o => o.fieldKey === pendingInputFieldKey.value)) {
    pendingInputFieldKey.value = ''
  }
})

const getFormFieldOptions = (current: ConfigFormNode) => {
  const used = new Set(
    configFormNodes.value
      .filter(n => n.fieldKey !== current.fieldKey)
      .map(n => n.fieldKey)
  )
  return buildFormFieldOptionPool().filter(opt => !used.has(opt.fieldKey))
}

const outputTypeLabelMap = computed(() => ({
  [WorkflowResultModelTypeEnum.IMAGE]: t('system.workflow.outputTypes.image'),
  [WorkflowResultModelTypeEnum.VIDEO]: t('system.workflow.outputTypes.video'),
  [WorkflowResultModelTypeEnum.AUDIO]: t('system.workflow.outputTypes.audio')
}))

const openCreateDialog = () => {
  resetAll()
  dialogMode.value = 'create'
  editingWorkflowId.value = null
  createDialogVisible.value = true
}

// ---------- 列表、查询、分页 ----------
const query = reactive<{ page: number; size: number; keyword?: string; categoryId?: number | undefined }>({ page: 1, size: 10, keyword: '', categoryId: undefined })
const table = reactive<{ total: number; items: WorkflowListItem[] }>({ total: 0, items: [] })

const categoryList = ref<{ categoryId: number; name: string; url?: string; __updating?: boolean; __deleting?: boolean }[]>([])

const loadCategories = async () => {
  try {
    const list = await workflowApi.getCategoryList()
    categoryList.value = list.map((c: any) => ({ ...c }))
  } catch (e) {
    console.error(e)
  }
}

const loadPage = async (toPage?: number) => {
  if (typeof toPage === 'number') query.page = toPage
  const params: any = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  if (query.categoryId) params.categoryId = query.categoryId
  try {
    const data = await workflowApi.getWorkflowPage(params)
    table.total = data.total
    table.items = data.items
  } catch (e) {
    console.error(e)
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

// 与用户管理一致的查询/重置
const handleSearch = () => {
  query.page = 1
  loadPage(1)
}
const handleReset = () => {
  query.keyword = ''
  query.categoryId = undefined
  query.page = 1
  loadPage(1)
}

onMounted(() => {
  loadCategories()
  loadPage(1)
})

// ---------- 编辑、删除 ----------
type SavedFormNode = WorkflowDetailVo['savedFormNodeList'][number]

const parsePromptImageRefs = (raw?: string | null): string[] => {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter((x): x is string => typeof x === 'string') : []
  } catch {
    return []
  }
}

const getImageFieldOptions = (current: ConfigFormNode) => {
  return configFormNodes.value
    .filter(n =>
      (n.type === WorkflowFormTypeEnum.IMAGE_UPLOAD || n.type === WorkflowFormTypeEnum.IMAGE_SCRIBBLE)
      && n.fieldKey !== current.fieldKey
    )
    .map(n => ({
      value: n.fieldKey,
      label: `${n.tips || t('system.workflow.dialog.unnamedNode')} (${n.nodeKey})`
    }))
}

const onPromptStyleChange = (item: ConfigFormNode) => {
  if (!item.promptStyle || item.promptStyle === PromptStyleEnum.NONE) {
    item.promptImageRefs = []
  }
}

const mapParseNodeToDefaultConfig = (n: ParsedFormNode): ConfigFormNode => {
  const inputs = n.nodeDigital as WorkflowResultModelDigitalEnum
  return {
    fieldKey: formFieldKey(n.nodeKey, inputs),
    nodeKey: n.nodeKey,
    type: n.type === WorkflowFormTypeEnum.TEXT_CONFIGURABLE
      ? WorkflowFormTypeEnum.TEXT_PROMPT
      : n.type === WorkflowFormTypeEnum.IMAGE_CONFIGURABLE
      ? WorkflowFormTypeEnum.IMAGE_UPLOAD
      : (n.type as WorkflowFormTypeEnum),
    inputs,
    tips: n.tips || '',
    required: true,
    size: n.type === WorkflowFormTypeEnum.IMAGE_UPLOAD
      || n.type === WorkflowFormTypeEnum.IMAGE_CONFIGURABLE
      || n.type === WorkflowFormTypeEnum.IMAGE_SCRIBBLE
      || n.type === WorkflowFormTypeEnum.VIDEO_UPLOAD
      || n.type === WorkflowFormTypeEnum.AUDIO_UPLOAD ? 10 : 500,
    template: '',
    options: n.type === WorkflowFormTypeEnum.TEXT_CONFIGURABLE ? '' : undefined,
    hidden: false,
    promptStyle: PromptStyleEnum.NONE,
    promptImageRefs: []
  }
}

const mapSavedNodeToConfig = (saved: SavedFormNode): ConfigFormNode => ({
  fieldKey: formFieldKey(saved.nodeKey, saved.inputs),
  nodeKey: saved.nodeKey,
  type: saved.type as WorkflowFormTypeEnum,
  inputs: saved.inputs as WorkflowResultModelDigitalEnum,
  tips: saved.tips || '',
  options: saved.options,
  template: saved.template || '',
  required: saved.required === 1,
  size: saved.size,
  hidden: saved.hidden === 1,
  promptStyle: saved.promptStyle || PromptStyleEnum.NONE,
  promptImageRefs: parsePromptImageRefs(saved.promptImageRefs)
})

const applyParsedWorkflow = (data: ParsingWorkflowVo, savedList?: SavedFormNode[]) => {
  parseResult.json = data.json
  parseResult.allNodeList = data.allNodeList
  parseResult.formNodeList = data.formNodeList

  const parseMap = new Map(data.formNodeList.map(n => [formFieldKey(n.nodeKey, n.nodeDigital), n]))

  if (savedList?.length) {
    configFormNodes.value = savedList.map(saved => {
      const parsed = parseMap.get(formFieldKey(saved.nodeKey, saved.inputs))
      if (!parsed) {
        return mapSavedNodeToConfig(saved)
      }
      const merged = mapParseNodeToDefaultConfig(parsed)
      const savedConfig = mapSavedNodeToConfig(saved)
      return { ...merged, ...savedConfig, fieldKey: savedConfig.fieldKey }
    })
    return
  }

  configFormNodes.value = configFormNodes.value
    .filter(item => parseMap.has(item.fieldKey))
    .map(item => {
      const parsed = parseMap.get(item.fieldKey)!
      const defaults = mapParseNodeToDefaultConfig(parsed)
      return {
        ...defaults,
        tips: item.tips,
        type: item.type,
        template: item.template,
        options: item.options,
        required: item.required,
        size: item.size,
        hidden: item.hidden,
        promptStyle: item.promptStyle,
        promptImageRefs: item.promptImageRefs
      }
    })
  ensureDefaultOutputConfig()
}

const guessDefaultOutputNodeKey = () => {
  const saveImage = parseResult.allNodeList.find(n => /save\s*image|保存图像/i.test(n.tips || ''))
  return saveImage?.nodeKey || parseResult.allNodeList[0]?.nodeKey || ''
}

const ensureDefaultOutputConfig = () => {
  if (!parseResult.json || outputNodes.value.length > 0) return
  outputNodes.value.push({
    nodeKey: guessDefaultOutputNodeKey(),
    type: WorkflowResultModelTypeEnum.IMAGE
  })
}

const onFormFieldChange = (item: ConfigFormNode) => {
  const option = buildFormFieldOptionPool().find(opt => opt.fieldKey === item.fieldKey)
  if (!option) return
  const next = mapParseNodeToDefaultConfig(option.parseNode)
  Object.assign(item, next)
}

const confirmAddFormConfig = () => {
  const option = availableFormFieldOptions.value.find(opt => opt.fieldKey === pendingInputFieldKey.value)
  if (!option) return
  configFormNodes.value.push(mapParseNodeToDefaultConfig(option.parseNode))
  pendingInputFieldKey.value = ''
}

const removeFormConfig = (idx: number) => {
  configFormNodes.value.splice(idx, 1)
}

const openEditDialog = async (row: WorkflowListItem) => {
  try {
    loadingDetailId.value = row.workflowId
    const detail = await workflowApi.getWorkflowDetail(row.workflowId)
    resetAll()
    dialogMode.value = 'edit'
    editingWorkflowId.value = row.workflowId

    baseForm.name = detail.name
    baseForm.description = detail.description || ''
    baseForm.url = detail.url || ''
    baseForm.workflowCategoryId = detail.workflowCategoryId ?? undefined
    baseForm.creditsDeducted = detail.creditsDeducted
    baseForm.published = detail.published ?? false
    baseForm.requiredLevel = (detail.requiredLevel as Role) || Role.USER

    applyParsedWorkflow(
      {
        json: detail.json,
        allNodeList: detail.allNodeList,
        formNodeList: detail.formNodeList
      },
      detail.savedFormNodeList
    )
    outputNodes.value = detail.outputNodeList.map(item => ({ ...item }))
    ensureDefaultOutputConfig()
    createDialogVisible.value = true
  } catch (e) {
    console.error(e)
    ElNotification.error(t('system.workflow.messages.loadDetailFailed'))
  } finally {
    loadingDetailId.value = null
  }
}

const togglePublish = async (row: WorkflowListItem, published: boolean) => {
  if (row.workflowCategoryId == null) return
  try {
    await workflowApi.updateWorkflow({
      workflowId: row.workflowId,
      name: row.name,
      workflowCategoryId: row.workflowCategoryId,
      published
    })
    row.published = published
    ElNotification.success(t('system.workflow.messages.publishUpdateSuccess'))
  } catch (e) {
    console.error(e)
  }
}

const confirmDelete = async (row: { workflowId: number; name: string }) => {
  try {
    await ElMessageBox.confirm(
      t('system.workflow.messages.deleteConfirm', { name: row.name }),
      t('system.workflow.messages.deleteTitle'),
      { type: 'warning', confirmButtonText: t('system.workflow.table.delete'), cancelButtonText: t('system.workflow.messages.cancel') }
    )
    await workflowApi.deleteWorkflow({ workflowId: row.workflowId })
    ElNotification.success(t('system.workflow.messages.deleteSuccess'))
    loadPage()
  } catch (e) {
    // 用户取消或请求异常
    if (e) console.error(e)
  }
}

// ---------- 类别管理 ----------
const categoryDialogVisible = ref(false)
const createCategoryDialogVisible = ref(false)
const categoryForm = reactive<{ name: string }>({ name: '' })
const categoryFormRef = ref<FormInstance>()
const categoryFormRules = computed<FormRules>(() => ({
  name: [
    { required: true, message: t('system.workflow.validation.categoryNameRequired'), trigger: 'blur' },
    { min: 1, max: 50, message: t('system.workflow.validation.categoryNameLength'), trigger: 'blur' }
  ]
}))
const creatingCategory = ref(false)

const openCategoryDialog = () => {
  categoryDialogVisible.value = true
}

const openCreateCategoryDialog = () => {
  categoryForm.name = ''
  createCategoryDialogVisible.value = true
}

const submitCreateCategory = async () => {
  if (!categoryFormRef.value) return
  await categoryFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      creatingCategory.value = true
      await workflowApi.createCategory({ name: categoryForm.name })
      ElNotification.success(t('system.workflow.messages.categoryCreateSuccess'))
      createCategoryDialogVisible.value = false
      categoryForm.name = ''
      await loadCategories()
    } catch (e) {
      console.error(e)
    } finally {
      creatingCategory.value = false
    }
  })
}

const updateCategory = async (row: any) => {
  try {
    row.__updating = true
    await workflowApi.updateCategory({ categoryId: row.categoryId, name: row.name })
    ElNotification.success(t('system.workflow.messages.categorySaveSuccess'))
    await loadCategories()
    // 刷新工作流列表
    await loadPage(query.page)
  } catch (e) {
    console.error(e)
  } finally {
    row.__updating = false
  }
}

const deleteCategory = async (row: any) => {
  try {
    await ElMessageBox.confirm(t('system.workflow.messages.categoryDeleteConfirm', { name: row.name }), t('system.workflow.messages.deleteTitle'), { type: 'warning' })
    row.__deleting = true
    await workflowApi.deleteCategory({ categoryId: row.categoryId })
    ElNotification.success(t('system.workflow.messages.categoryDeleteSuccess'))
    await loadCategories()
    // 清除类别筛选
    if (query.categoryId === row.categoryId) {
      query.categoryId = undefined
      await loadPage(1)
    } else {
      // 刷新工作流列表
      await loadPage(query.page)
    }
  } catch (e) {
    if (e) console.error(e)
  } finally {
    row.__deleting = false
  }
}

const triggerChooseFile = () => {
  jsonFileRef.value?.click()
}

const onWorkflowFileChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const file = input.files[0]
  if (!file.name.endsWith('.json')) {
    ElNotification.error(t('system.workflow.validation.fileFormatError'))
    input.value = ''
    return
  }
  try {
    parsing.value = true
    const data = await workflowApi.parseWorkflow(file)
    applyParsedWorkflow(data)
    ElNotification.success(t('system.workflow.dialog.parseSuccess'))
  } catch (err: any) {
    console.error(err)
  } finally {
    parsing.value = false
    input.value = ''
  }
}

const TEXT_INPUT_FIELDS = new Set<WorkflowResultModelDigitalEnum>([
  WorkflowResultModelDigitalEnum.TEXT,
  WorkflowResultModelDigitalEnum.MULTI_LINE_PROMPT,
  WorkflowResultModelDigitalEnum.RESOLUTION,
  WorkflowResultModelDigitalEnum.PROMPT,
  WorkflowResultModelDigitalEnum.VALUE
])

const getAvailableTypes = (item: ConfigFormNode) => {
  // 文本类可切换三种
  if (TEXT_INPUT_FIELDS.has(item.inputs)) {
    return [WorkflowFormTypeEnum.TEXT_PROMPT, WorkflowFormTypeEnum.RADIO_SELECTOR, WorkflowFormTypeEnum.CHECKBOX_SELECTOR]
  }
  // 图片类可切换两种：IMAGE_UPLOAD / IMAGE_SCRIBBLE
  if (item.inputs === WorkflowResultModelDigitalEnum.IMAGE) {
    return [WorkflowFormTypeEnum.IMAGE_UPLOAD, WorkflowFormTypeEnum.IMAGE_SCRIBBLE]
  }
  // 其他文件类固定
  if (item.inputs === WorkflowResultModelDigitalEnum.VIDEO) return [WorkflowFormTypeEnum.VIDEO_UPLOAD]
  if (item.inputs === WorkflowResultModelDigitalEnum.AUDIO) return [WorkflowFormTypeEnum.AUDIO_UPLOAD]
  return [WorkflowFormTypeEnum.TEXT_PROMPT]
}

const addOutputConfig = () => {
  if (outputNodes.value.length > 0) return
  outputNodes.value.push({ nodeKey: '', type: WorkflowResultModelTypeEnum.IMAGE })
}
const removeOutputConfig = (idx: number) => {
  outputNodes.value.splice(idx, 1)
}
const formatNodeLabel = (n: { nodeKey: string; tips: string | null }) => {
  return t('system.workflow.dialog.nodeLabel', { tips: n.tips || t('system.workflow.dialog.unnamedNode'), key: n.nodeKey })
}

const canSubmit = computed(() => {
  return !!baseForm.name && !!parseResult.json && configFormNodes.value.length > 0 && outputNodes.value.length > 0 && outputNodes.value.every(o => o.nodeKey && o.type)
})

const handleSave = async () => {
  if (!createFormRef.value) return
  
  // 先验证基础表单
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  if (!canSubmit.value) return
  
  // 选择器需要校验 options
  for (const item of configFormNodes.value) {
    if (item.hidden && !item.template?.trim()) {
      ElNotification.error(t('system.workflow.validation.hiddenTemplateRequired', { node: item.nodeKey }))
      return
    }
    if ((item.type === WorkflowFormTypeEnum.RADIO_SELECTOR || item.type === WorkflowFormTypeEnum.CHECKBOX_SELECTOR)) {
      if (!item.options) {
        ElNotification.error(t('system.workflow.validation.optionsRequired'))
        return
      }
      try {
        const obj = JSON.parse(item.options)
        if (!obj || typeof obj !== 'object' || Array.isArray(obj) || Object.keys(obj).length === 0) {
          ElNotification.error(t('system.workflow.validation.optionsInvalidFormat'))
          return
        }
      } catch {
        ElNotification.error(t('system.workflow.validation.optionsJsonError'))
        return
      }
    }
  }

  const payload = {
    name: baseForm.name,
    description: baseForm.description || undefined,
    url: baseForm.url || undefined,
    json: parseResult.json,
    workflowCategoryId: baseForm.workflowCategoryId != null ? String(baseForm.workflowCategoryId) : '1',
    creditsDeducted: baseForm.creditsDeducted,
    published: baseForm.published,
    requiredLevel: baseForm.requiredLevel,
    formNodeList: configFormNodes.value.map<FormNodeConfig>(i => ({
      nodeKey: i.nodeKey,
      type: i.type as WorkflowFormTypeEnum,
      inputs: i.inputs,
      tips: i.tips,
      options: i.options,
      template: i.template,
      hidden: i.hidden ? 1 : 0,
      required: i.hidden ? 0 : (i.required ? 1 : 0),
      size: i.size,
      promptStyle: i.promptStyle && i.promptStyle !== PromptStyleEnum.NONE ? i.promptStyle : undefined,
      promptImageRefs:
        i.promptStyle && i.promptStyle !== PromptStyleEnum.NONE && i.promptImageRefs.length
          ? JSON.stringify(i.promptImageRefs)
          : undefined
    })),
    outputNodeList: outputNodes.value
  }

  try {
    saving.value = true
    if (dialogMode.value === 'edit' && editingWorkflowId.value != null) {
      await workflowApi.updateWorkflowConfig({
        ...payload,
        workflowId: editingWorkflowId.value
      })
      ElNotification.success(t('system.workflow.messages.updateSuccess'))
    } else {
      await workflowApi.saveWorkflowConfig(payload)
      ElNotification.success(t('system.workflow.messages.saveSuccess'))
    }
    createDialogVisible.value = false
    await loadPage()
  } catch (err: any) {
    console.error(err)
  } finally {
    saving.value = false
  }
}

const handleCoverUpload = async (req: any) => {
  try {
    coverUploading.value = true
    const url = await ossApi.uploadCover({ file: req.file })
    baseForm.url = url
    ElNotification.success(t('system.workflow.dialog.coverUploadSuccess'))
  } catch (e) {
    console.error(e)
  } finally {
    coverUploading.value = false
  }
}

const resetAll = () => {
  dialogMode.value = 'create'
  editingWorkflowId.value = null
  baseForm.name = ''
  baseForm.description = ''
  baseForm.url = ''
  baseForm.workflowCategoryId = undefined
  baseForm.creditsDeducted = 0
  baseForm.published = false
  baseForm.requiredLevel = Role.USER
  parseResult.json = ''
  parseResult.allNodeList = []
  parseResult.formNodeList = []
  configFormNodes.value = []
  pendingInputFieldKey.value = ''
  outputNodes.value = []
  createFormRef.value?.clearValidate()
}
</script>

<style scoped>
.workflow-management-view {
  height: 100%;
}

.content-card {
  height: 100%;
  background-color: var(--el-bg-color-overlay);
}

.content-card :deep(.el-card__header) {
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  color: var(--el-text-color-primary);
}

.content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: calc(100% - 52px);
}

.table-wrapper {
  flex: 1;
  min-height: 0;
  margin-bottom: 16px;
}

.workflow-table {
  margin-bottom: 0;
  /* unify table tokens in dark theme */
  --el-table-bg-color: var(--el-bg-color);
  --el-table-header-bg-color: var(--el-bg-color);
  --el-table-tr-bg-color: var(--el-bg-color);
  --el-table-row-hover-bg-color: var(--el-fill-color-light);
}

.workflow-table :deep(.el-table),
.workflow-table :deep(.el-table__inner-wrapper),
.workflow-table :deep(.el-table__body-wrapper),
.workflow-table :deep(.el-scrollbar__wrap),
.workflow-table :deep(.el-scrollbar__view) {
  background-color: var(--el-bg-color);
}

.workflow-table :deep(.el-table__empty-block),
.workflow-table :deep(.el-table__header) {
  background-color: var(--el-bg-color);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}

.pagination-container :deep(.el-pagination) {
  --el-pagination-bg-color: var(--el-bg-color);
  --el-pagination-text-color: var(--el-text-color-primary);
  --el-pagination-button-disabled-bg-color: var(--el-fill-color-lighter);
}

.pagination-container :deep(.el-pagination .btn-prev),
.pagination-container :deep(.el-pagination .btn-next),
.pagination-container :deep(.el-pager li) {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
}

.pagination-container :deep(.el-pager li.is-active) {
  background-color: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
}

/* 
  对话框样式修正（参考 UserManagementView.vue）
  使用 :deep() 穿透 scoped 样式，确保对 teleport 到 body 的 el-dialog 生效 
*/
:deep(.workflow-dialog .el-dialog),
:deep(.workflow-edit-dialog .el-dialog) {
  background-color: var(--el-bg-color-overlay);
  color: var(--el-text-color-primary);
  display: flex;
  flex-direction: column;
  max-height: 88vh;
  margin: 6vh auto !important;
}

:deep(.workflow-category-dialog .el-dialog),
:deep(.workflow-category-create-dialog .el-dialog) {
  background-color: var(--el-bg-color-overlay);
  color: var(--el-text-color-primary);
}

:deep(.workflow-dialog .el-dialog__header),
:deep(.workflow-dialog .el-dialog__footer),
:deep(.workflow-edit-dialog .el-dialog__header),
:deep(.workflow-edit-dialog .el-dialog__footer),
:deep(.workflow-category-dialog .el-dialog__header),
:deep(.workflow-category-dialog .el-dialog__footer),
:deep(.workflow-category-create-dialog .el-dialog__header),
:deep(.workflow-category-create-dialog .el-dialog__footer) {
  background-color: var(--el-bg-color-overlay);
}

:deep(.workflow-dialog .el-dialog__body),
:deep(.workflow-edit-dialog .el-dialog__body) {
  background-color: var(--el-bg-color-overlay);
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-top: 8px;
  padding-bottom: 8px;
}

:deep(.workflow-category-dialog .el-dialog__body),
:deep(.workflow-category-create-dialog .el-dialog__body) {
  background-color: var(--el-bg-color-overlay);
}

:deep(.workflow-dialog .el-input__wrapper),
:deep(.workflow-dialog .el-select__wrapper),
:deep(.workflow-edit-dialog .el-input__wrapper),
:deep(.workflow-edit-dialog .el-select__wrapper),
:deep(.workflow-category-dialog .el-input__wrapper),
:deep(.workflow-category-dialog .el-select__wrapper),
:deep(.workflow-category-create-dialog .el-input__wrapper) {
  background-color: var(--el-bg-color) !important; /* 强制覆盖 */
  border: 1px solid var(--el-border-color);
  transition: all 0.2s ease;
}

:deep(.workflow-dialog .el-input__wrapper:hover),
:deep(.workflow-dialog .el-select__wrapper:hover),
:deep(.workflow-edit-dialog .el-input__wrapper:hover),
:deep(.workflow-edit-dialog .el-select__wrapper:hover),
:deep(.workflow-category-dialog .el-input__wrapper:hover),
:deep(.workflow-category-dialog .el-select__wrapper:hover),
:deep(.workflow-category-create-dialog .el-input__wrapper:hover) {
  border-color: var(--el-border-color-hover);
}

:deep(.workflow-dialog .el-input__wrapper.is-focus),
:deep(.workflow-dialog .el-select__wrapper.is-focused),
:deep(.workflow-edit-dialog .el-input__wrapper.is-focus),
:deep(.workflow-edit-dialog .el-select__wrapper.is-focused),
:deep(.workflow-category-dialog .el-input__wrapper.is-focus),
:deep(.workflow-category-dialog .el-select__wrapper.is-focused),
:deep(.workflow-category-create-dialog .el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

/* 对话框中的文本域样式 */
:deep(.workflow-dialog .el-textarea__inner),
:deep(.workflow-edit-dialog .el-textarea__inner),
:deep(.workflow-category-dialog .el-textarea__inner),
:deep(.workflow-category-create-dialog .el-textarea__inner) {
  background-color: var(--el-bg-color) !important;
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-primary);
  transition: all 0.2s ease;
  box-shadow: none; /* 移除默认的 focus shadow */
}

:deep(.workflow-dialog .el-textarea__inner:hover),
:deep(.workflow-edit-dialog .el-textarea__inner:hover),
:deep(.workflow-category-dialog .el-textarea__inner:hover),
:deep(.workflow-category-create-dialog .el-textarea__inner:hover) {
  border-color: var(--el-border-color-hover);
}

:deep(.workflow-dialog .el-textarea__inner:focus),
:deep(.workflow-edit-dialog .el-textarea__inner:focus),
:deep(.workflow-category-dialog .el-textarea__inner:focus),
:deep(.workflow-category-create-dialog .el-textarea__inner:focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}


/* 统一取消按钮样式和用户管理一致 */
:deep(.cancel-btn) {
  background-color: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  border-color: var(--el-border-color);
}

:deep(.cancel-btn:hover) {
  background-color: var(--el-fill-color-light);
}

/* 次级按钮与用户管理一致（例如：类别管理、上传、选择文件） */
.secondary-btn {
  background-color: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  border-color: var(--el-border-color);
}

.secondary-btn:hover {
  background-color: var(--el-fill-color-light);
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.section-card :deep(.el-card__header) {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.section-card :deep(.el-card__body) {
  padding: 16px;
}

.section-header {
  font-weight: 600;
}

.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.section-header-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.input-field-picker {
  width: min(360px, 100%);
}

.base-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.base-form-grid :deep(.el-form-item) {
  margin-bottom: 18px;
}

.base-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

@media (max-width: 720px) {
  .base-form-grid {
    grid-template-columns: 1fr;
  }

  .base-form .cover-field {
    flex-direction: column;
  }
}

.base-form .cover-field {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
}

.base-form .cover-field__main {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 8px;
  align-items: center;
}

.base-form .cover-preview {
  width: 120px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  background: var(--el-fill-color);
  flex-shrink: 0;
}

.base-form .cover-preview :deep(.cover-media-root) {
  width: 100%;
  height: 100%;
}

.workflow-cover-thumb {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  background: var(--el-fill-color);
  overflow: hidden;
}

.workflow-cover-thumb :deep(.cover-media-root) {
  width: 100%;
  height: 100%;
}

.parse-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.form-nodes {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-node {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.form-node__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.form-node__field-select {
  flex: 1;
  min-width: 280px;
}

.form-node__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-node__grid .span-2 {
  grid-column: span 2;
}

.form-node__control {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.form-node__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.form-node__switches {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  align-items: center;
  min-height: 32px;
}

.form-node__extra {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-node__prompt-assist {
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

@media (max-width: 720px) {
  .form-node__grid {
    grid-template-columns: 1fr;
  }

  .form-node__grid .span-2 {
    grid-column: span 1;
  }
}

/* 搜索栏控件暗色样式 */
.search-bar :deep(.el-input__wrapper),
.search-bar :deep(.el-select__wrapper) {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  transition: all 0.2s ease;
}

.search-bar :deep(.el-input__wrapper:hover),
.search-bar :deep(.el-select__wrapper:hover) {
  border-color: var(--el-border-color-hover);
}

.search-bar :deep(.el-input__wrapper.is-focus),
.search-bar :deep(.el-select__wrapper.is-focused) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

/* 重置按钮与用户管理一致 */
.reset-btn {
  background-color: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  border-color: var(--el-border-color);
}

.reset-btn:hover {
  background-color: var(--el-fill-color-light);
}

/* 类别管理对话框中的表格暗色样式 */
.workflow-category-table {
  --el-table-bg-color: var(--el-bg-color);
  --el-table-header-bg-color: var(--el-bg-color);
  --el-table-tr-bg-color: var(--el-bg-color);
  --el-table-row-hover-bg-color: var(--el-fill-color-light);
}

.workflow-category-table :deep(.el-table),
.workflow-category-table :deep(.el-table__inner-wrapper),
.workflow-category-table :deep(.el-table__body-wrapper),
.workflow-category-table :deep(.el-scrollbar__wrap),
.workflow-category-table :deep(.el-scrollbar__view),
.workflow-category-table :deep(.el-table__empty-block),
.workflow-category-table :deep(.el-table__header) {
  background-color: var(--el-bg-color);
}

/* 分页中的下拉与输入框（sizes/jumper）与用户管理一致 */
.pagination-container :deep(.el-select__wrapper),
.pagination-container :deep(.el-input__wrapper) {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-primary);
  transition: all 0.2s ease;
}

.pagination-container :deep(.el-select__wrapper:hover),
.pagination-container :deep(.el-input__wrapper:hover) {
  border-color: var(--el-border-color-hover);
}

.pagination-container :deep(.el-select__wrapper.is-focused),
.pagination-container :deep(.el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

/* Select 下拉弹层暗色（与用户管理同步） */
:deep(.el-select__popper) {
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
}

:deep(.el-select__popper .el-select-dropdown) {
  background-color: var(--el-bg-color-overlay);
}

:deep(.el-select__popper .el-select-dropdown__item) {
  color: var(--el-text-color-regular);
  transition: background-color 0.15s ease, color 0.15s ease;
}

:deep(.el-select__popper .el-select-dropdown__item.is-hovering) {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}

:deep(.el-select__popper .el-select-dropdown__item.is-selected) {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

/* 虚拟化 SelectV2 兼容 */
:deep(.el-select__popper .el-select-dropdown__item.selected),
:deep(.el-select__popper .el-select-dropdown__item.hover) {
  background-color: var(--el-fill-color-light);
}

.output-config {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.output-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.output-row__node {
  flex: 1;
  min-width: 280px;
}

.output-row__type {
  width: 160px;
}

.parse-summary {
  padding-top: 12px;
}
</style>

