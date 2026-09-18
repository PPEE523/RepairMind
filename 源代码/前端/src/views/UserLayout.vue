<template>
  <div class="user-layout">
    <!-- 移动端遮罩 -->
    <Transition name="overlay-fade">
      <div v-if="sidebarExpanded && isMobile" class="sidebar-overlay" @click="sidebarExpanded = false"></div>
    </Transition>

    <!-- ============================================================
         左侧边栏 — Kimi 风格
         ============================================================ -->
    <aside class="sidebar" :class="{ collapsed: !sidebarExpanded && isMobile, expanded: sidebarExpanded || !isMobile }">
      <!-- Logo 区 -->
      <div class="sidebar-brand">
        <router-link to="/user/home" class="brand-link" @click="closeOnMobile">
          <div class="brand-logo">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
            </svg>
          </div>
          <span class="brand-name">智能设备检修系统</span>
        </router-link>
        <button class="sidebar-toggle" @click="sidebarExpanded = !sidebarExpanded" aria-label="切换侧边栏">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m11 17-5-5 5-5"/><path d="m18 17-5-5 5-5"/></svg>
        </button>
      </div>

      <!-- 移动端收起按钮 -->
      <button class="mobile-close" @click="sidebarExpanded = false">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m15 18-6-6 6-6"/></svg>
      </button>

      <!-- 新建会话按钮 -->
      <button class="new-chat-btn" @click="startNewChat" v-if="userStore.token">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        <span>新建会话</span>
        <kbd class="shortcut-hint" v-if="!isMobile">⌘N</kbd>
      </button>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon" v-html="item.icon"></span>
          <span class="nav-text">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-divider"></div>

      <!-- 历史会话 -->
      <div class="history-section" v-if="userStore.token">
        <h3 class="history-title" @click="historyExpanded = !historyExpanded">
          <svg class="history-chevron" :class="{ open: historyExpanded }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m9 18 6-6-6-6"/></svg>
          历史会话
        </h3>
        <div class="history-list" v-if="historyExpanded">
          <div
            v-for="conv in conversations"
            :key="conv.conversationId"
            class="history-item"
            :class="{ active: route.query.conversationId === conv.conversationId }"
            @click="openConversation(conv)"
            :title="conv.userQuestion"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
            <span class="history-text">{{ conv.userQuestion }}</span>
            <span class="history-time">{{ formatConvTime(conv.createTime) }}</span>
            <button
              class="history-delete-btn"
              @click.stop="handleDeleteConv(conv)"
              :title="'删除会话: ' + conv.userQuestion"
              aria-label="删除会话"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                <line x1="10" y1="11" x2="10" y2="17"/>
                <line x1="14" y1="11" x2="14" y2="17"/>
              </svg>
            </button>
          </div>
          <p v-if="conversations.length === 0" class="history-empty">暂无历史会话</p>
        </div>
      </div>

      <!-- 底部用户区 -->
      <div class="sidebar-footer">
        <template v-if="userStore.token">
          <!-- 通知 -->
          <button class="footer-action" aria-label="通知">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </button>
          <!-- 设置 -->
          <button class="footer-action" aria-label="设置" @click="goProfile">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          </button>
          <!-- 用户头像 -->
          <div class="user-info">
            <div class="user-avatar-small">{{ (userStore.nickname || userStore.username || '?').charAt(0) }}</div>
            <span class="user-name-text" v-if="!isMobile">{{ userStore.nickname || userStore.username }}</span>
          </div>
        </template>
        <template v-else>
          <router-link to="/user/login" class="sidebar-login-btn">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
            <span v-if="!isMobile">登录</span>
          </router-link>
        </template>
      </div>
    </aside>

    <!-- ============================================================
         主内容区
         ============================================================ -->
    <main class="main-area">
      <!-- 移动端汉堡按钮 -->
      <button v-if="isMobile && !sidebarExpanded" class="hamburger-btn" @click="sidebarExpanded = true" aria-label="打开菜单">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </button>

      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getChatConversations, deleteChatConversation } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const sidebarExpanded = ref(true)
const historyExpanded = ref(true)
const isMobile = ref(false)

// 导航菜单
const navItems = [
  {
    path: '/user/home',
    label: '首页',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`
  },
  {
    path: '/user/search',
    label: '多模态知识检索',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>`
  },
  {
    path: '/user/sop',
    label: '标准化作业指引',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>`
  },
  {
    path: '/user/case',
    label: '知识案例库',
    icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>`
  }
]

const isActive = (path: string) => {
  return route.path.startsWith(path)
}

// 真实对话历史（从后端 API 加载）
interface ConvItem {
  conversationId: string
  userQuestion: string
  deviceModel: string
  createTime: string
  roundCount: number
}
const conversations = ref<ConvItem[]>([])

const loadConversations = async () => {
  if (!userStore.token) return
  try {
    const res: any = await getChatConversations({ pageNum: 1, pageSize: 50 })
    const data = res.data
    conversations.value = data?.records || data || []
  } catch { /* silent */ }
}

// 2 分钟定时轮询刷新历史会话列表
let conversationPollTimer: ReturnType<typeof setInterval> | null = null
const startConversationPolling = () => {
  stopConversationPolling()
  conversationPollTimer = setInterval(() => {
    loadConversations()
  }, 120000) // 2 分钟
}
const stopConversationPolling = () => {
  if (conversationPollTimer) {
    clearInterval(conversationPollTimer)
    conversationPollTimer = null
  }
}

const openConversation = (conv: ConvItem) => {
  closeOnMobile()
  router.push(`/user/search?conversationId=${conv.conversationId}`)
}

// 删除对话（悬停显示删除图标）
const handleDeleteConv = async (conv: ConvItem) => {
  if (!confirm(`确定删除对话「${conv.userQuestion?.substring(0, 20)}...」吗？`)) return
  try {
    await deleteChatConversation(conv.conversationId)
    // 如果当前正在查看被删除的对话，跳回新会话
    if (route.query.conversationId === conv.conversationId) {
      router.push('/user/search')
    }
    loadConversations()
  } catch { /* silent */ }
}

// 格式化时间
const formatConvTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const closeOnMobile = () => {
  if (isMobile.value) sidebarExpanded.value = false
}

const startNewChat = () => {
  closeOnMobile()
  router.push('/user/search')
}

const goProfile = () => {
  closeOnMobile()
  router.push('/user/profile')
}

// 监听路由变化，刷新对话列表
watch(() => route.fullPath, () => {
  closeOnMobile()
  loadConversations()
})

// 检测移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    sidebarExpanded.value = false
  } else {
    sidebarExpanded.value = true
  }
}

onMounted(() => {
  loadConversations()
  startConversationPolling()
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  stopConversationPolling()
  window.removeEventListener('resize', checkMobile)
})

</script>

<style scoped>
/* ============================================================
   Kimi 风格布局 — 左侧边栏 + 右侧内容
   ============================================================ */
.user-layout {
  display: flex;
  height: 100vh;
  height: 100dvh;
  background: var(--surface-page);
  overflow: hidden;
  position: relative;
}

/* ---- 移动端遮罩 ---- */
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  z-index: 190;
}

/* ---- 移动端汉堡按钮 ---- */
.hamburger-btn {
  display: flex;
  position: fixed;
  top: var(--space-3);
  left: var(--space-3);
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-secondary);
  cursor: pointer;
  z-index: 40;
  box-shadow: var(--shadow-sm);
}

.hamburger-btn:hover {
  color: var(--text-primary);
  border-color: var(--primary-300);
}

/* ---- 移动端收起按钮 ---- */
.mobile-close {
  display: none;
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  width: 32px;
  height: 32px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-secondary);
  cursor: pointer;
  align-items: center;
  justify-content: center;
}

/* ============================================================
   左侧边栏
   ============================================================ */
.sidebar {
  width: 260px;
  min-width: 260px;
  height: 100%;
  background: #f9fafb;
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  z-index: 191;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.sidebar.collapsed {
  transform: translateX(-100%);
}

/* ---- Brand ---- */
.sidebar-brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-3);
  flex-shrink: 0;
}

.brand-link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  overflow: hidden;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--primary-600), var(--primary-800));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-name {
  font-size: 15px;
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  white-space: nowrap;
}

.sidebar-toggle {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-base);
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  background: var(--color-neutral-100);
  color: var(--text-primary);
}

/* ---- 新建会话按钮 ---- */
.new-chat-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: 0 var(--space-3) var(--space-3);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
  white-space: nowrap;
  overflow: hidden;
}

.new-chat-btn:hover {
  border-color: var(--primary-300);
  color: var(--primary-600);
  background: var(--primary-50);
}

.shortcut-hint {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-tertiary);
  background: var(--color-neutral-100);
  padding: 1px 5px;
  border-radius: 3px;
  font-family: var(--font-mono);
  letter-spacing: 0;
}

/* ---- 导航菜单 ---- */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 var(--space-2);
  flex-shrink: 0;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: var(--text-sm);
  transition: all var(--transition-base);
  white-space: nowrap;
  overflow: hidden;
}

.nav-link:hover {
  background: var(--color-neutral-100);
  color: var(--text-primary);
}

.nav-link.active {
  background: var(--primary-50);
  color: var(--primary-600);
  font-weight: var(--weight-medium);
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-text {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- 分隔线 ---- */
.sidebar-divider {
  height: 1px;
  background: var(--border-light);
  margin: var(--space-3) var(--space-3);
  flex-shrink: 0;
}

/* ---- 历史会话 ---- */
.history-section {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.history-title {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: 0 var(--space-3);
  margin-bottom: var(--space-1);
  font-size: 11px;
  font-weight: var(--weight-medium);
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.history-chevron {
  transition: transform var(--transition-base);
}
.history-chevron.open {
  transform: rotate(90deg);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 var(--space-2);
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all var(--transition-base);
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
}

.history-item:hover {
  background: var(--color-neutral-100);
}

.history-item.active {
  background: var(--primary-50);
  color: var(--primary-600);
}

.history-item svg {
  flex-shrink: 0;
  color: var(--text-tertiary);
}

.history-text {
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.history-time {
  font-size: 10px;
  color: var(--text-tertiary);
  flex-shrink: 0;
  opacity: 1;
  transition: opacity var(--transition-base);
}

.history-item:hover .history-time {
  opacity: 0;
}

.history-delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: var(--radius-xs);
  color: var(--text-tertiary);
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
  padding: 0;
  opacity: 0;
  pointer-events: none;
}

.history-item:hover .history-delete-btn {
  opacity: 1;
  pointer-events: auto;
}

.history-delete-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.history-empty {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  padding: var(--space-2) var(--space-3);
  margin: 0;
}

/* ---- 底部用户区 ---- */
.sidebar-footer {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-2);
  border-top: 1px solid var(--border-light);
  flex-shrink: 0;
}

.footer-action {
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-base);
  flex-shrink: 0;
}

.footer-action:hover {
  background: var(--color-neutral-100);
  color: var(--text-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
  overflow: hidden;
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-sm);
  transition: background var(--transition-base);
}

.user-info:hover {
  background: var(--color-neutral-100);
}

.user-avatar-small {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary-500), var(--primary-700));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: var(--weight-semibold);
  flex-shrink: 0;
}

.user-name-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
}

.sidebar-login-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--primary-600);
  color: #fff;
  text-decoration: none;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  transition: all var(--transition-base);
  justify-content: center;
}

.sidebar-login-btn:hover {
  background: var(--primary-700);
}

/* ============================================================
   主内容区
   ============================================================ */
.main-area {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--surface-page);
  scrollbar-gutter: stable;
  scroll-behavior: smooth;
}

/* 美化滚动条（Kimi 风格细滚动条） */
.main-area::-webkit-scrollbar {
  width: 6px;
}
.main-area::-webkit-scrollbar-thumb {
  background: var(--gray-300);
  border-radius: 3px;
}
.main-area::-webkit-scrollbar-track {
  background: transparent;
}

/* 内容居中容器 */
.content-wrapper {
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-6) var(--space-4);
  height: 100%;
}

/* ---- 过渡动画 ---- */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity var(--duration-200) var(--ease-out-quart),
              transform var(--duration-200) var(--ease-out-quart);
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 191;
    box-shadow: var(--shadow-xl);
    width: 280px;
    min-width: 280px;
  }

  .sidebar.collapsed {
    transform: translateX(-100%);
  }

  .mobile-close {
    display: flex;
  }

  .main-area {
    padding-top: 52px;
  }

  .hamburger-btn {
    display: flex;
  }
}

@media (min-width: 769px) {
  .hamburger-btn {
    display: none;
  }

  .mobile-close {
    display: none;
  }

  .sidebar-overlay {
    display: none;
  }
}
</style>
