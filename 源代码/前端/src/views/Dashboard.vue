<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent: #3b82f6; --accent-soft: rgba(59,130,246,0.12)">
          <el-icon size="26"><Document /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.totalDocuments || 0 }}</span>
          <span class="stat-label">知识案例</span>
        </div>
        <div class="stat-badge up"><el-icon><Top /></el-icon>12%</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent: #22c55e; --accent-soft: rgba(34,197,94,0.12)">
          <el-icon size="26"><Search /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.todaySearchCount || 0 }}</span>
          <span class="stat-label">今日检索</span>
        </div>
        <div class="stat-badge up"><el-icon><Top /></el-icon>8%</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent: #f97316; --accent-soft: rgba(249,115,22,0.12)">
          <el-icon size="26"><Edit /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.todayNewCases || 0 }}</span>
          <span class="stat-label">今日案例</span>
        </div>
        <div class="stat-badge down"><el-icon><Bottom /></el-icon>3%</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="--accent: #ef4444; --accent-soft: rgba(239,68,68,0.12)">
          <el-icon size="26"><Warning /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stats.pendingAuditCount || 0 }}</span>
          <span class="stat-label">待审核</span>
        </div>
        <div class="stat-badge warn" v-if="stats.pendingAuditCount > 5">需处理</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-section">
      <div class="section-header">
        <h3>检索趋势分析（近7天）</h3>
      </div>
      <div ref="trendChartRef" class="chart-container"></div>
    </div>

    <!-- 底部区域 -->
    <div class="bottom-grid">
      <!-- AI反馈统计 -->
      <div class="panel feedback-panel">
        <div class="section-header">
          <h3>AI反馈统计</h3>
          <span class="correct-rate">正确率 {{ stats.aiFeedbackStats?.correctRate?.toFixed(1) || 0 }}%</span>
        </div>
        <div class="feedback-list">
          <div class="feedback-row">
            <div class="feedback-dot" style="--dot-color: #3b82f6"></div>
            <span class="feedback-name">总反馈数</span>
            <span class="feedback-num">{{ stats.aiFeedbackStats?.totalFeedback || 0 }}</span>
          </div>
          <div class="feedback-row">
            <div class="feedback-dot" style="--dot-color: #22c55e"></div>
            <span class="feedback-name">正确反馈</span>
            <span class="feedback-num green">{{ stats.aiFeedbackStats?.correctCount || 0 }}</span>
          </div>
          <div class="feedback-row">
            <div class="feedback-dot" style="--dot-color: #ef4444"></div>
            <span class="feedback-name">错误反馈</span>
            <span class="feedback-num red">{{ stats.aiFeedbackStats?.errorCount || 0 }}</span>
          </div>
        </div>
        <!-- 进度条可视化 -->
        <div class="feedback-bar">
          <div class="bar-fill bar-correct" :style="{ width: (stats.aiFeedbackStats?.correctRate || 0) + '%' }"></div>
          <div class="bar-fill bar-error" :style="{ width: (100 - (stats.aiFeedbackStats?.correctRate || 0)) + '%' }"></div>
        </div>
      </div>

      <!-- 快速操作 -->
      <div class="panel actions-panel">
        <div class="section-header">
          <h3>快速操作</h3>
        </div>
        <div class="action-grid">
          <div class="action-card" @click="$router.push('/chat')">
            <div class="action-icon" style="--action-accent: #3b82f6">
              <el-icon size="22"><Search /></el-icon>
            </div>
            <span class="action-name">开始检索</span>
          </div>
          <div class="action-card" @click="$router.push('/workflow')">
            <div class="action-icon" style="--action-accent: #22c55e">
              <el-icon size="22"><Document /></el-icon>
            </div>
            <span class="action-name">检修作业指引</span>
          </div>
          <div class="action-card" v-if="userStore.canAudit()" @click="$router.push('/audit')">
            <div class="action-icon" style="--action-accent: #f97316">
              <el-icon size="22"><Checked /></el-icon>
            </div>
            <span class="action-name">审核案例</span>
          </div>
          <div class="action-card" @click="$router.push('/profile')">
            <div class="action-icon" style="--action-accent: #8b5cf6">
              <el-icon size="22"><User /></el-icon>
            </div>
            <span class="action-name">个人中心</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const stats = ref<any>({})

const fetchStats = async () => {
  try {
    const res: any = await request.get('/system/dashboard')
    stats.value = res.data || {}
    initCharts()
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

const initCharts = () => {
  if (trendChartRef.value) {
    // 复用已有实例，避免重复 init 导致实例泄漏和控制台警告
    trendChart = echarts.getInstanceByDom(trendChartRef.value) || echarts.init(trendChartRef.value)

    // 使用后端返回的检索趋势数据
    const searchTrend = stats.value.searchTrend || []
    // 后端返回 yyyy-MM-dd，直接拆分字符串，避免 new Date 按 UTC 解析造成时区错位
    const days = searchTrend.map((item: any) => {
      const parts = String(item.date).split('-')
      return `${Number(parts[1])}/${Number(parts[2])}`
    })
    const searchData = searchTrend.map((item: any) => item.count)

    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'transparent',
        textStyle: { color: '#fff' }
      },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: 'category',
        data: days,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b' }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#64748b' }
      },
      series: [
        {
          name: '检索次数',
          type: 'line',
          smooth: true,
          data: searchData,
          lineStyle: { width: 3, color: '#3b82f6' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.25)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0)' }
            ])
          },
          itemStyle: { color: '#3b82f6' },
          symbol: 'circle',
          symbolSize: 6
        }
      ]
    })
  }
}

const handleResize = () => {
  trendChart?.resize()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========== 统计卡片 ========== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: default;
  min-height: 100px;
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  background: var(--accent-soft);
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

.stat-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 20px;
  white-space: nowrap;
  align-self: flex-start;
}

.stat-badge.up {
  color: #16a34a;
  background: #f0fdf4;
}

.stat-badge.down {
  color: #dc2626;
  background: #fef2f2;
}

.stat-badge.warn {
  color: #ef4444;
  background: #fef2f2;
}

/* ========== 图表区域 ========== */
.chart-section {
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.correct-rate {
  font-size: 13px;
  font-weight: 600;
  color: #22c55e;
  background: #f0fdf4;
  padding: 3px 10px;
  border-radius: 20px;
}

.chart-container {
  height: 300px;
}

/* ========== 底部区域 ========== */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
}

/* AI反馈 */
.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.feedback-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 10px;
}

.feedback-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dot-color);
  flex-shrink: 0;
}

.feedback-name {
  flex: 1;
  font-size: 13px;
  color: #64748b;
}

.feedback-num {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.feedback-num.green { color: #22c55e; }
.feedback-num.red { color: #ef4444; }

.feedback-bar {
  display: flex;
  height: 6px;
  border-radius: 3px;
  overflow: hidden;
  background: #f1f5f9;
  margin-top: 16px;
}

.bar-correct { background: #22c55e; }
.bar-error { background: #ef4444; }

/* 快速操作 */
.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  flex: 1;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 12px;
  border-radius: 12px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;
  border: 1.5px solid transparent;
}

.action-card:hover {
  background: #fff;
  border-color: var(--action-accent);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--action-accent);
  background: color-mix(in srgb, var(--action-accent) 12%, white);
  transition: transform 0.2s;
}

.action-card:hover .action-icon {
  transform: scale(1.1);
}

.action-name {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

/* ========== 响应式 ========== */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
