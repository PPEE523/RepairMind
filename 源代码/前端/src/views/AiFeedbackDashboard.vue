<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff">
              <el-icon size="32"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalFeedback || 0 }}</div>
              <div class="stat-label">总反馈数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon size="32"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.correctCount || 0 }}</div>
              <div class="stat-label">正确反馈</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon size="32"><CircleClose /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.errorCount || 0 }}</div>
              <div class="stat-label">错误反馈</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff">
              <el-icon size="32"><EditPen /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.processedCount || 0 }}</div>
              <div class="stat-label">已处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-title">
              <el-icon><PieChart /></el-icon>
              <span>反馈状态分布</span>
            </div>
          </template>
          <div ref="typeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-title">
              <el-icon><DataLine /></el-icon>
              <span>反馈趋势（近7天）</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 反馈列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><List /></el-icon>
            <span>反馈列表</span>
          </div>
          <el-radio-group v-model="filterStatus" @change="fetchFeedbackList">
            <el-radio-button :value="''">全部</el-radio-button>
            <el-radio-button value="correct">正确</el-radio-button>
            <el-radio-button value="incorrect">错误</el-radio-button>
            <el-radio-button value="processed">已处理</el-radio-button>
            <el-radio-button value="pending">待处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="feedbackList" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userQuestion" label="用户问题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceModel" label="设备型号" width="120" show-overflow-tooltip />
        <el-table-column prop="answerContent" label="AI回答" min-width="200" show-overflow-tooltip />
        <el-table-column prop="feedbackStatus" label="反馈状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.feedbackStatus)" size="small">
              {{ statusLabel(row.feedbackStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="命中文档" width="90" align="center">
          <template #default="{ row }">
            {{ row.documentResources ? row.documentResources.length : 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="feedbackTime" label="反馈时间" width="170" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.feedbackStatus === 'incorrect'" type="warning" link size="small" @click="handleProcess(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="showDetailDrawer" title="反馈详情" size="500px">
      <div v-if="detailData" class="detail-content">
        <div class="detail-section">
          <div class="detail-label">用户问题</div>
          <div class="detail-text">{{ detailData.userQuestion }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">AI回答</div>
          <div class="detail-text markdown-body" v-html="renderMarkdown(detailData.answerContent || '')"></div>
        </div>
        <div class="detail-section">
          <div class="detail-label">反馈状态</div>
          <el-tag :type="statusTagType(detailData.feedbackStatus)">{{ statusLabel(detailData.feedbackStatus) }}</el-tag>
        </div>
        <div class="detail-section" v-if="detailData.deviceModel">
          <div class="detail-label">设备型号</div>
          <div class="detail-text">{{ detailData.deviceModel }}</div>
        </div>
        <!-- 命中文档列表 -->
        <div class="detail-section">
          <div class="detail-label">命中文档</div>
          <div v-if="detailData.documentResources && detailData.documentResources.length > 0">
            <div v-for="(doc, dIdx) in detailData.documentResources" :key="dIdx" class="doc-card">
              <div class="doc-header">
                <span class="doc-name">{{ doc.documentName }}</span>
                <el-tag size="small" type="info">{{ doc.datasetName }}</el-tag>
              </div>
              <div v-for="(seg, sIdx) in doc.segments" :key="sIdx" class="seg-item">
                <div class="seg-meta">
                  <el-tag size="small" type="success">{{ (seg.score * 100).toFixed(1) }}%</el-tag>
                </div>
                <div class="seg-content markdown-body" v-html="renderMarkdown(seg.content || '')"></div>
              </div>
            </div>
          </div>
          <el-empty v-else description="无命中文档" :image-size="60" />
        </div>
      </div>
    </el-drawer>

    <!-- 处理弹窗（全屏） -->
    <el-dialog v-model="showProcessDialog" title="处理错误反馈" fullscreen>
      <template #header>
        <div class="process-dialog-header">
          <span>处理错误反馈</span>
          <el-button @click="showProcessDialog = false">
            <el-icon><ArrowLeft /></el-icon> 返回列表
          </el-button>
        </div>
      </template>
      <div v-if="processData" class="process-layout">
        <!-- 左侧：对话信息 -->
        <div class="process-left">
          <h4>对话信息</h4>
          <div class="detail-section">
            <div class="detail-label">用户问题</div>
            <div class="detail-text">{{ processData.userQuestion }}</div>
          </div>
          <div class="detail-section">
            <div class="detail-label">AI回答</div>
            <div class="detail-text markdown-body" v-html="renderMarkdown(processData.answerContent || '')"></div>
          </div>
          <div class="detail-section">
            <div class="detail-label">反馈状态</div>
            <el-tag :type="statusTagType(processData.feedbackStatus)">{{ statusLabel(processData.feedbackStatus) }}</el-tag>
          </div>
        </div>
        <!-- 右侧：文档分段处理 -->
        <div class="process-right">
          <h4>命中文档与分段</h4>
          <el-tabs v-if="processData.documentResources && processData.documentResources.length > 0"
                   v-model="activeDocTab" type="border-card">
            <el-tab-pane v-for="(doc, dIdx) in processData.documentResources"
                         :key="dIdx"
                         :label="doc.documentName"
                         :name="String(dIdx)">
              <div class="doc-info-row">
                <span>知识库：{{ doc.datasetName }}</span>
                <span style="margin-left: 16px">文档ID：{{ doc.documentId }}</span>
              </div>
              <div class="seg-list">
                <div v-for="(seg, sIdx) in doc.segments" :key="sIdx"
                     class="seg-process-item highlighted">
                  <div class="seg-process-header">
                    <span>分段 {{ sIdx + 1 }}</span>
                    <div class="seg-process-meta">
                      <el-tag size="small" type="success">{{ (seg.score * 100).toFixed(1) }}%</el-tag>
                      <el-tag size="small" type="warning">命中</el-tag>
                    </div>
                    <el-button type="primary" link size="small" @click="handleEditSegment(seg, doc)">
                      {{ editingSegmentId === seg.segmentId ? '取消编辑' : '编辑' }}
                    </el-button>
                  </div>
                  <div v-if="editingSegmentId === seg.segmentId" class="seg-edit-area">
                    <el-input v-model="editingContent" type="textarea" :rows="8" placeholder="编辑分段内容" />
                    <div class="seg-edit-actions">
                      <el-button type="primary" size="small" :loading="savingSegment" @click="handleSaveSegment(seg, doc)">
                        保存分段
                      </el-button>
                    </div>
                  </div>
                  <div v-else class="seg-content markdown-body" v-html="renderMarkdown(seg.content || '')"></div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
          <el-empty v-else description="无命中文档，可直接标记为已处理" />
          <div class="process-actions">
            <el-button type="success" @click="handleMarkProcessed" :loading="markingProcessed">
              标记为已处理
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'
import { getChatFeedbackStats, getChatFeedbackList, getChatFeedbackDetail, processChatFeedback } from '@/api/index'

const stats = ref<any>({})
const feedbackList = ref<any[]>([])
const filterStatus = ref('')

const typeChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let typeChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

// 详情抽屉
const showDetailDrawer = ref(false)
const detailData = ref<any>(null)

// 处理弹窗
const showProcessDialog = ref(false)
const processData = ref<any>(null)
const activeDocTab = ref('0')
const editingSegmentId = ref('')
const editingContent = ref('')
const savingSegment = ref(false)
const markingProcessed = ref(false)

const correctRate = computed(() => {
  const total = (stats.value.correctCount || 0) + (stats.value.errorCount || 0) + (stats.value.processedCount || 0)
  if (!total) return 0
  return (((stats.value.correctCount || 0) + (stats.value.processedCount || 0)) / total * 100).toFixed(1)
})

const statusTagType = (status: string) => {
  if (status === 'correct') return 'success'
  if (status === 'incorrect') return 'danger'
  if (status === 'processed') return 'primary'
  if (status === 'pending') return 'warning'
  return 'info'
}

const statusLabel = (status: string) => {
  if (status === 'correct') return '正确'
  if (status === 'incorrect') return '错误'
  if (status === 'processed') return '已处理'
  if (status === 'pending') return '待处理'
  return '未知'
}

const fetchStats = async () => {
  try {
    const res: any = await getChatFeedbackStats()
    stats.value = res.data || {}
    initCharts()
  } catch (error) {
    console.error('获取统计失败', error)
  }
}

const fetchFeedbackList = async () => {
  try {
    const params: any = { pageNum: 1, pageSize: 50 }
    if (filterStatus.value) {
      params.feedbackStatus = filterStatus.value
    }
    const res: any = await getChatFeedbackList(params)
    feedbackList.value = res.data || []
  } catch (error) {
    console.error('获取反馈列表失败', error)
  }
}

const initCharts = () => {
  // 饼图
  if (typeChartRef.value) {
    // 复用已有实例，避免重复 init
    typeChart = echarts.getInstanceByDom(typeChartRef.value) || echarts.init(typeChartRef.value)
    typeChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        name: '反馈状态',
        type: 'pie',
        radius: '50%',
        data: [
          { value: stats.value.correctCount || 0, name: '正确', itemStyle: { color: '#67c23a' } },
          { value: stats.value.errorCount || 0, name: '错误', itemStyle: { color: '#f56c6c' } },
          { value: stats.value.processedCount || 0, name: '已处理', itemStyle: { color: '#409eff' } }
        ],
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' }
        }
      }]
    })
  }

  // 趋势图 - 使用真实数据
  if (trendChartRef.value) {
    // 复用已有实例，避免重复 init
    trendChart = echarts.getInstanceByDom(trendChartRef.value) || echarts.init(trendChartRef.value)
    const dailyStats = stats.value.dailyStats || []
    const days = dailyStats.map((d: any) => d.date)
    const correctData = dailyStats.map((d: any) => d.correctCount || 0)
    const errorData = dailyStats.map((d: any) => d.errorCount || 0)
    const processedData = dailyStats.map((d: any) => d.processedCount || 0)

    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['正确', '错误', '已处理'] },
      xAxis: { type: 'category', data: days },
      yAxis: { type: 'value' },
      series: [
        { name: '正确', type: 'line', smooth: true, data: correctData, itemStyle: { color: '#67c23a' } },
        { name: '错误', type: 'line', smooth: true, data: errorData, itemStyle: { color: '#f56c6c' } },
        { name: '已处理', type: 'line', smooth: true, data: processedData, itemStyle: { color: '#409eff' } }
      ]
    })
  }
}

/** 查看详情 */
const handleViewDetail = async (row: any) => {
  try {
    const res: any = await getChatFeedbackDetail(row.id)
    detailData.value = res.data || row
    showDetailDrawer.value = true
  } catch (error) {
    console.error('获取详情失败', error)
    detailData.value = row
    showDetailDrawer.value = true
  }
}

/** 处理错误反馈 */
const handleProcess = async (row: any) => {
  try {
    const res: any = await getChatFeedbackDetail(row.id)
    processData.value = res.data || row
  } catch (error) {
    processData.value = row
  }
  activeDocTab.value = '0'
  editingSegmentId.value = ''
  editingContent.value = ''
  showProcessDialog.value = true
}

/** 编辑分段 */
const handleEditSegment = (seg: any, doc: any) => {
  if (editingSegmentId.value === seg.segmentId) {
    editingSegmentId.value = ''
    editingContent.value = ''
  } else {
    editingSegmentId.value = seg.segmentId
    editingContent.value = seg.content || ''
  }
}

/** 保存分段 */
const handleSaveSegment = async (seg: any, doc: any) => {
  if (!editingContent.value.trim()) {
    ElMessage.warning('分段内容不能为空')
    return
  }
  savingSegment.value = true
  try {
    await processChatFeedback({
      chatFeedbackId: processData.value.id,
      segmentId: seg.segmentId,
      datasetId: doc.datasetId,
      documentId: doc.documentId,
      content: editingContent.value
    })
    ElMessage.success('分段内容已更新')
    // 更新本地数据
    seg.content = editingContent.value
    editingSegmentId.value = ''
    editingContent.value = ''
    // 刷新列表
    fetchFeedbackList()
  } catch (error) {
    console.error('保存分段失败', error)
    ElMessage.error('保存分段失败')
  } finally {
    savingSegment.value = false
  }
}

/** 标记已处理 */
const handleMarkProcessed = async () => {
  markingProcessed.value = true
  try {
    // 有命中文档时携带分段信息；无命中文档时只传反馈ID，由后端仅更新状态
    const firstDoc = processData.value.documentResources?.[0]
    const firstSeg = firstDoc?.segments?.[0]
    await processChatFeedback({
      chatFeedbackId: processData.value.id,
      segmentId: firstSeg?.segmentId,
      datasetId: firstDoc?.datasetId,
      documentId: firstDoc?.documentId,
      content: firstSeg?.content || ''
    })
    ElMessage.success('已标记为已处理')
    showProcessDialog.value = false
    fetchStats()
    fetchFeedbackList()
  } catch (error) {
    console.error('标记已处理失败', error)
    ElMessage.error('标记已处理失败')
  } finally {
    markingProcessed.value = false
  }
}

const handleResize = () => {
  typeChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  fetchStats()
  fetchFeedbackList()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  typeChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.stat-card {
  cursor: pointer;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 300px;
}

/* 详情抽屉 */
.detail-content {
  padding: 0 8px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 500;
}

.detail-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  word-break: break-all;
}

.doc-card {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.doc-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.doc-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.seg-item {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px;
  margin-top: 8px;
}

.seg-meta {
  margin-bottom: 6px;
}

.seg-content {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}

/* 处理弹窗 */
.process-layout {
  display: flex;
  gap: 24px;
  height: calc(100vh - 120px);
}

.process-left {
  width: 35%;
  overflow-y: auto;
  padding-right: 16px;
  border-right: 1px solid #ebeef5;
}

.process-right {
  flex: 1;
  overflow-y: auto;
}

.process-left h4,
.process-right h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
}

.doc-info-row {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.seg-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.seg-process-item {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.seg-process-item.highlighted {
  border: 2px solid #e6a23c;
  background: #fdf6ec;
}

.seg-process-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.seg-process-meta {
  display: flex;
  gap: 4px;
}

.seg-edit-area {
  margin-top: 8px;
}

.seg-edit-actions {
  margin-top: 8px;
  text-align: right;
}

.process-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  text-align: center;
}

.process-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Markdown渲染样式 */
.markdown-body {
  font-size: 14px;
  line-height: 1.8;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3), .markdown-body :deep(h4) {
  margin: 12px 0 8px;
  color: #303133;
}

.markdown-body :deep(p) {
  margin: 6px 0;
  line-height: 1.8;
}

.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.markdown-body :deep(li) {
  margin: 3px 0;
  line-height: 1.8;
}

.markdown-body :deep(strong) {
  font-weight: 700;
  color: #303133;
}

.markdown-body :deep(code) {
  background: #f0f2f5;
  color: #c7254e;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #1e1e1e;
  padding: 14px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 10px 0;
}

.markdown-body :deep(pre code) {
  background: none;
  color: #d4d4d4;
  padding: 0;
  font-size: 13px;
  line-height: 1.6;
  border-radius: 0;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding: 6px 14px;
  margin: 10px 0;
  background: rgba(64, 158, 258, 0.04);
  border-radius: 0 6px 6px 0;
  color: #606266;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
  font-size: 13px;
}

.markdown-body :deep(th), .markdown-body :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 6px 0;
}
</style>
