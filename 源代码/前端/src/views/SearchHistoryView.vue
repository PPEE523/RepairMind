<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Clock /></el-icon>
            <span>检索历史</span>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="search-bar">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="故障描述">
            <el-input
              v-model="queryParams.keyword"
              placeholder="关键词搜索"
              clearable
              style="width: 180px"
            />
          </el-form-item>
          <el-form-item label="设备型号">
            <el-input
              v-model="queryParams.equipModel"
              placeholder="设备型号"
              clearable
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="检索状态">
            <el-select v-model="queryParams.searchSuccess" placeholder="全部" clearable style="width: 120px">
              <el-option label="成功" :value="true" />
              <el-option label="失败" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchList">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshRight /></el-icon>重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 批量操作 -->
      <div class="batch-bar" v-if="selectedIds.length">
        <span>已选 {{ selectedIds.length }} 项</span>
        <el-button type="danger" size="small" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>

      <!-- 列表 -->
      <el-table
        :data="historyList"
        border
        stripe
        v-loading="loading"
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="queryText" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="equipModel" label="设备型号" width="140" show-overflow-tooltip />
        <el-table-column label="故障文件" width="100" align="center">
          <template #default="{ row }">
            <template v-if="getRowFiles(row).length">
              <template v-for="(file, idx) in getRowFiles(row).slice(0, 2)" :key="idx">
                <el-image
                  v-if="file.fileType === 'image'"
                  :src="file.url"
                  :preview-src-list="getRowImageUrls(row)"
                  :initial-index="getRowImageUrls(row).indexOf(file.url)"
                  fit="cover"
                  style="width: 50px; height: 50px; border-radius: 4px; margin-right: 4px"
                />
                <el-icon
                  v-else
                  :size="24"
                  style="color: var(--el-color-primary); cursor: pointer"
                  @click="playVideo(file.url)"
                ><VideoPlay /></el-icon>
              </template>
              <span v-if="getRowFiles(row).length > 2" style="color: #999; font-size: 12px">+{{ getRowFiles(row).length - 2 }}</span>
            </template>
            <span v-else style="color: #999">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="searchSuccess" label="检索状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.searchSuccess ? 'success' : 'danger'" effect="plain">
              {{ row.searchSuccess ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="检索用户" width="120" align="center">
          <template #default="{ row }">
            {{ row.nickname || row.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="检索时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="检索详情" width="800px" destroy-on-close>
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="故障描述" :span="2">
            {{ detailData.queryText || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="设备型号">
            {{ detailData.equipModel || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="检索状态">
            <el-tag :type="detailData.searchSuccess ? 'success' : 'danger'" effect="plain">
              {{ detailData.searchSuccess ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检索用户">
            {{ detailData.nickname || detailData.username || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="检索时间">
            {{ formatTime(detailData.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 故障文件 -->
        <div v-if="detailAllFiles.length" class="detail-section">
          <h4>故障文件</h4>
          <div class="image-list">
            <template v-for="(file, index) in detailAllFiles" :key="index">
              <div class="file-item">
                <el-image
                  v-if="file.fileType === 'image'"
                  :src="file.url"
                  :preview-src-list="detailImageUrls"
                  :initial-index="detailImageUrls.indexOf(file.url)"
                  fit="cover"
                  referrerpolicy="no-referrer"
                  style="width: 120px; height: 120px; border-radius: 6px"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                      <span>加载失败</span>
                    </div>
                  </template>
                </el-image>
                <div v-else class="video-preview" @click="playVideo(file.url)">
                  <el-icon :size="32" style="color: #fff"><VideoPlay /></el-icon>
                  <span class="video-label">视频</span>
                </div>
                <div class="file-name" :title="file.name">{{ file.name }}</div>
              </div>
            </template>
          </div>
        </div>
        <div v-else-if="detailData.imageUrl" class="detail-section">
          <h4>故障图片</h4>
          <div class="image-list">
            <el-image
              v-for="(url, index) in allImages"
              :key="index"
              :src="url"
              :preview-src-list="allImages"
              :initial-index="index"
              fit="cover"
              referrerpolicy="no-referrer"
              style="width: 120px; height: 120px; border-radius: 6px; margin-right: 8px"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                  <span>加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
        </div>

        <!-- 检索结果 -->
        <div v-if="detailData.resultAnswer" class="detail-section">
          <h4>检索结果</h4>
          <div class="result-content" v-html="formatAnswer(detailData.resultAnswer)"></div>
        </div>

        <!-- 错误信息 -->
        <div v-if="!detailData.searchSuccess && detailData.errorMessage" class="detail-section">
          <h4>错误信息</h4>
          <el-alert :title="detailData.errorMessage" type="error" :closable="false" show-icon />
        </div>
      </template>
    </el-dialog>

    <!-- 视频播放弹窗 -->
    <el-dialog v-model="videoDialogVisible" title="视频播放" width="700px" destroy-on-close>
      <video v-if="videoDialogVisible" :src="currentVideoUrl" controls style="width: 100%" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import { getSearchHistoryList, getSearchHistoryDetail, deleteSearchHistory, batchDeleteSearchHistory } from '@/api'

const loading = ref(false)
const historyList = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const detailVisible = ref(false)
const detailData = ref<any>(null)
const dateRange = ref<string[] | null>(null)
const videoDialogVisible = ref(false)
const currentVideoUrl = ref('')

interface DetailFile {
  url: string
  fileType: string
  name: string
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  equipModel: '',
  searchSuccess: undefined as boolean | undefined
})

const parsedImageUrls = computed(() => {
  if (!detailData.value?.imageUrls) return []
  try {
    return JSON.parse(detailData.value.imageUrls)
  } catch {
    return []
  }
})

const allImages = computed(() => {
  const images: string[] = []
  if (detailData.value?.imageUrl) {
    images.push(detailData.value.imageUrl)
  }
  if (parsedImageUrls.value.length) {
    parsedImageUrls.value.forEach((url: string) => {
      if (!images.includes(url)) images.push(url)
    })
  }
  return images
})

// 详情弹窗中的文件列表（支持图片和视频）
const detailAllFiles = computed(() => {
  if (!detailData.value) return []
  const files: DetailFile[] = []
  const urls: string[] = []
  const types: string[] = []
  const names: string[] = []

  if (detailData.value.imageUrls) {
    try {
      const parsed = JSON.parse(detailData.value.imageUrls)
      if (Array.isArray(parsed)) urls.push(...parsed)
    } catch { /* ignore */ }
  }
  if (detailData.value.fileTypes) {
    try {
      const parsed = JSON.parse(detailData.value.fileTypes)
      if (Array.isArray(parsed)) types.push(...parsed)
    } catch { /* ignore */ }
  }
  if (detailData.value.fileNames) {
    try {
      const parsed = JSON.parse(detailData.value.fileNames)
      if (Array.isArray(parsed)) names.push(...parsed)
    } catch { /* ignore */ }
  }

  if (urls.length > 0) {
    for (let i = 0; i < urls.length; i++) {
      files.push({
        url: urls[i],
        fileType: types[i] || 'image',
        name: names[i] || urls[i].split('/').pop() || ''
      })
    }
  } else if (detailData.value.imageUrl) {
    files.push({ url: detailData.value.imageUrl, fileType: 'image', name: '' })
  }
  return files
})

const detailImageUrls = computed(() => detailAllFiles.value.filter(f => f.fileType === 'image').map(f => f.url))

const hasVideo = (row: any) => {
  if (row.fileTypes) {
    try {
      const types = JSON.parse(row.fileTypes)
      return Array.isArray(types) && types.includes('video')
    } catch { /* ignore */ }
  }
  return false
}

// 获取列表行中的文件列表（支持多文件）
const getRowFiles = (row: any): DetailFile[] => {
  const files: DetailFile[] = []
  const urls: string[] = []
  const types: string[] = []
  const names: string[] = []

  if (row.imageUrls) {
    try {
      const parsed = JSON.parse(row.imageUrls)
      if (Array.isArray(parsed)) urls.push(...parsed)
    } catch { /* ignore */ }
  }
  if (row.fileTypes) {
    try {
      const parsed = JSON.parse(row.fileTypes)
      if (Array.isArray(parsed)) types.push(...parsed)
    } catch { /* ignore */ }
  }
  if (row.fileNames) {
    try {
      const parsed = JSON.parse(row.fileNames)
      if (Array.isArray(parsed)) names.push(...parsed)
    } catch { /* ignore */ }
  }

  if (urls.length > 0) {
    for (let i = 0; i < urls.length; i++) {
      files.push({ url: urls[i], fileType: types[i] || 'image', name: names[i] || urls[i].split('/').pop() || '' })
    }
  } else if (row.imageUrl) {
    files.push({ url: row.imageUrl, fileType: 'image', name: '' })
  }
  return files
}

const getRowImageUrls = (row: any): string[] => {
  return getRowFiles(row).filter(f => f.fileType === 'image').map(f => f.url)
}

const playVideo = (url: string) => {
  currentVideoUrl.value = url
  videoDialogVisible.value = true
}

const fetchList = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize
    }
    if (queryParams.keyword) params.keyword = queryParams.keyword
    if (queryParams.equipModel) params.equipModel = queryParams.equipModel
    if (queryParams.searchSuccess !== undefined) params.searchSuccess = queryParams.searchSuccess
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const res: any = await getSearchHistoryList(params)
    historyList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取检索历史失败', error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.equipModel = ''
  queryParams.searchSuccess = undefined
  dateRange.value = null
  queryParams.pageNum = 1
  fetchList()
}

const handleSelectionChange = (rows: any[]) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleDetail = async (row: any) => {
  try {
    const res: any = await getSearchHistoryDetail(row.id)
    detailData.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error('获取详情失败', error)
    ElMessage.error('获取详情失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定删除该检索记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSearchHistory(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // 取消删除
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条记录吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await batchDeleteSearchHistory(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchList()
  } catch {
    // 取消删除
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  return time.substring(0, 19).replace('T', ' ')
}

const formatAnswer = (answer: string) => {
  if (!answer) return ''
  return marked.parse(answer)
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.main-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.search-bar {
  margin-bottom: 16px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--el-color-primary-light-9);
  border-radius: 4px;
  font-size: 14px;
  color: var(--el-color-primary);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 14px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.file-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.file-name {
  font-size: 12px;
  color: var(--text-secondary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.video-preview {
  width: 120px;
  height: 120px;
  border-radius: 6px;
  background: #1e1e1e;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity 0.3s;
}

.video-preview:hover {
  opacity: 0.8;
}

.video-label {
  color: #fff;
  font-size: 12px;
  margin-top: 4px;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
}

.image-error span {
  margin-top: 4px;
}

.result-content {
  padding: 16px;
  background: var(--bg-color, #f5f7fa);
  border-radius: 8px;
  line-height: 1.8;
  color: var(--text-regular);
  max-height: 400px;
  overflow-y: auto;
}

.result-content :deep(h1),
.result-content :deep(h2),
.result-content :deep(h3),
.result-content :deep(h4) {
  margin: 16px 0 8px;
  color: var(--text-primary);
}

.result-content :deep(p) {
  margin: 8px 0;
  line-height: 1.8;
}

.result-content :deep(ul),
.result-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.result-content :deep(li) {
  margin: 4px 0;
  line-height: 1.8;
}

.result-content :deep(code) {
  background: #e8eaed;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.result-content :deep(pre) {
  background: #e8eaed;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.result-content :deep(pre code) {
  background: none;
  padding: 0;
}

.result-content :deep(blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding-left: 12px;
  margin: 8px 0;
  color: var(--text-secondary);
}

.result-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.result-content :deep(th),
.result-content :deep(td) {
  border: 1px solid var(--border-color, #dcdfe6);
  padding: 8px 12px;
  text-align: left;
}

.result-content :deep(th) {
  background: #e8eaed;
  font-weight: 600;
}
</style>
