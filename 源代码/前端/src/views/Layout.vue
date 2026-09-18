<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '72px' : '240px'" class="aside">
      <!-- Logo区域 -->
      <div class="logo-section" :class="{ collapsed: isCollapse }">
        <div class="logo-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
          </svg>
        </div>
        <transition name="fade">
          <div v-show="!isCollapse" class="logo-text">
            <h1>智能设备检修系统</h1>
            <span>Intelligent equipment  system</span>
          </div>
        </transition>
      </div>

      <!-- 导航菜单 -->
      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          router
          background-color="transparent"
          text-color="rgba(255, 255, 255, 0.7)"
          active-text-color="#ffffff"
          :collapse-transition="false"
        >
          <template v-for="menu in menuList" :key="menu.id">
            <!-- 无子菜单 -->
            <el-menu-item 
              v-if="!menu.children || menu.children.length === 0" 
              :index="menu.path"
              class="menu-item"
            >
              <el-icon><component :is="menu.icon || 'Document'" /></el-icon>
              <template #title>{{ menu.title }}</template>
            </el-menu-item>
            <!-- 有子菜单 -->
            <el-sub-menu v-else :index="menu.path || menu.id + ''" class="sub-menu">
              <template #title>
                <el-icon><component :is="menu.icon || 'Folder'" /></el-icon>
                <span>{{ menu.title }}</span>
              </template>
              <el-menu-item 
                v-for="child in menu.children" 
                :key="child.id" 
                :index="child.path"
                class="sub-menu-item"
              >
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>

      <!-- 底部折叠按钮 -->
      <div class="collapse-btn" @click="isCollapse = !isCollapse">
        <el-icon>
          <Fold v-if="!isCollapse" />
          <Expand v-else />
        </el-icon>
      </div>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 通知铃铛 -->
          <el-popover
            placement="bottom-end"
            :width="360"
            trigger="click"
            class="notification-popover"
            @show="handleNotificationShow"
          >
            <template #reference>
              <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0" class="notification-badge">
                <el-button :icon="Bell" circle size="small" />
              </el-badge>
            </template>
            <div class="notification-panel">
              <div class="notification-header">
                <span class="notification-title">
                  {{ userStore.canAudit() ? '审核待办' : '案例通知' }}
                </span>
                <div class="notification-header-actions">
                  <el-button v-if="unreadCount > 0" type="primary" link size="small" @click="markAsRead">全部已读</el-button>
                  <el-button type="primary" link size="small" @click="goToLink(notifications[0]?.link)">查看全部</el-button>
                </div>
              </div>
              <div class="notification-list" v-if="notifications.length">
                <div
                  v-for="item in notifications"
                  :key="item.id"
                  class="notification-item"
                  :class="{ 'is-unread': isNew(item) }"
                  @click="goToLink(item.link)"
                >
                  <div class="notification-item-header">
                    <el-tag :type="notificationTagType(item.type)" size="small" effect="plain" class="oper-type-tag">
                      {{ item.title }}
                    </el-tag>
                    <span class="notification-time">{{ formatTime(item.time) }}</span>
                  </div>
                  <div class="notification-content" :title="item.content">{{ item.content }}</div>
                </div>
              </div>
              <div v-else class="notification-empty">暂无通知</div>
            </div>
          </el-popover>
          
          <!-- 用户信息 -->
          <el-dropdown @command="handleCommand" class="user-dropdown">
            <div class="user-info">
              <el-avatar :size="36" class="user-avatar">
                {{ userStore.nickname?.charAt(0) || userStore.username?.charAt(0) }}
              </el-avatar>
              <div class="user-details">
                <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
                <span class="user-role">{{ getRoleName(userStore.roleId) }}</span>
              </div>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings" v-if="userStore.isAdmin()">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Bell } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const menuList = ref<any[]>([])

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta?.title as string) || '')

const getRoleName = (roleId: number | null) => {
  const map: Record<number, string> = { 1: '系统管理员', 2: '资深工程师', 3: '实习人员' }
  return roleId ? map[roleId] || '' : ''
}

const fetchMenu = async () => {
  try {
    const res: any = await request.get('/auth/menu')
    menuList.value = res.data || []
    localStorage.setItem('menus', JSON.stringify(res.data || []))
  } catch (error) {
    console.error('获取菜单失败', error)
  }
}

// 通知铃铛 - 根据角色显示不同通知
const notifications = ref<any[]>([])
const unreadCount = ref(0)
const LAST_VIEW_KEY = 'notification_last_view_time'

const getLastViewTime = (): number => {
  const stored = localStorage.getItem(LAST_VIEW_KEY)
  return stored ? new Date(stored).getTime() : 0
}

const markAsRead = () => {
  localStorage.setItem(LAST_VIEW_KEY, new Date().toISOString())
  unreadCount.value = 0
}

const isNew = (item: any) => {
  if (!item.time) return false
  return new Date(item.time).getTime() > getLastViewTime()
}

const fetchNotifications = async () => {
  try {
    const res: any = await request.get('/system/notification/list')
    if (res.data && res.data.items) {
      notifications.value = res.data.items
      // 计算未读数：上次查看时间之后的新通知
      const lastView = getLastViewTime()
      unreadCount.value = lastView === 0
        ? res.data.items.length
        : res.data.items.filter((item: any) => item.time && new Date(item.time).getTime() > lastView).length
    }
  } catch (error) {
    notifications.value = []
    unreadCount.value = 0
  }
}

const handleNotificationShow = () => {
  markAsRead()
}

const notificationTagType = (type: string) => {
  const map: Record<string, string> = {
    audit_timeout: 'danger',
    audit_passed: 'success',
    audit_rejected: 'warning'
  }
  return map[type] || 'info'
}

const goToLink = (link?: string) => {
  if (link) router.push(link)
}

const formatTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'logout':
      userStore.logout()
      localStorage.removeItem('menus')
      router.push('/login')
      break
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/system')
      break
  }
}

onMounted(() => {
  fetchMenu()
  fetchNotifications()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 侧边栏 */
.aside {
  background: linear-gradient(180deg, var(--gray-900) 0%, var(--gray-800) 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  z-index: 10;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
}

/* Logo区域 */
.logo-section {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.logo-section.collapsed {
  justify-content: center;
  padding: 0;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-700) 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.logo-text {
  margin-left: 12px;
  white-space: nowrap;
}

.logo-text h1 {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin: 0;
  line-height: 1.2;
}

.logo-text span {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.5px;
}

/* 菜单滚动区 */
.menu-scrollbar {
  flex: 1;
  overflow: hidden;
}

.menu-item,
.sub-menu-item {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

.menu-item:hover,
.sub-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.menu-item.is-active {
  background: var(--primary-600) !important;
  color: white !important;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

/* 折叠按钮 */
.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.2s;
}

.collapse-btn:hover {
  color: white;
  background: rgba(255, 255, 255, 0.05);
}

/* 主容器 */
.main-container {
  background: var(--bg-page);
}

/* 顶部导航 */
.header {
  height: 64px;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 5;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-badge {
  cursor: pointer;
}

/* 通知面板 */
.notification-panel {
  margin: -12px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.notification-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.notification-list {
  max-height: 320px;
  overflow-y: auto;
}

.notification-item {
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  transition: background 0.15s;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--gray-50);
}

.notification-item.is-unread {
  background: rgba(37, 99, 235, 0.04);
  border-left: 3px solid var(--primary-500);
  padding-left: 13px;
}

.notification-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.oper-type-tag {
  font-family: var(--font-mono);
  font-size: 11px;
}

.notification-time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.notification-content {
  font-size: 12px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 13px;
}

/* 用户信息 */
.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.user-info:hover {
  background: var(--gray-100);
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-700) 100%);
  color: white;
  font-weight: 600;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.2;
}

.user-role {
  font-size: 11px;
  color: var(--text-tertiary);
}

/* 主内容区 */
.main {
  padding: 24px;
  overflow-y: auto;
}

/* 页面过渡动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>

<!-- 全局样式：修复侧边栏折叠时子菜单弹出面板白板问题 -->
<style>
.el-menu--popup {
  background: var(--gray-800) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 8px !important;
  padding: 4px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3) !important;
}

.el-menu--popup .el-menu-item {
  color: rgba(255, 255, 255, 0.7) !important;
  background: transparent !important;
  border-radius: 6px !important;
  height: 40px !important;
  line-height: 40px !important;
  margin: 2px 0 !important;
}

.el-menu--popup .el-menu-item:hover {
  color: #ffffff !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

.el-menu--popup .el-menu-item.is-active {
  color: #ffffff !important;
  background: var(--primary-600) !important;
  font-weight: 500;
}

.el-menu--popup .el-sub-menu__title {
  color: rgba(255, 255, 255, 0.7) !important;
  background: transparent !important;
  border-radius: 6px !important;
  height: 40px !important;
  line-height: 40px !important;
}

.el-menu--popup .el-sub-menu__title:hover {
  color: #ffffff !important;
  background: rgba(255, 255, 255, 0.08) !important;
}
</style>
