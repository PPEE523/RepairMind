import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  // ==============================================
  // 用户端独立路由
  // ==============================================
  // 用户登录页（独立布局，不在 UserLayout 内）
  {
    path: '/user/login',
    name: 'UserLogin',
    component: () => import('@/views/user/UserLogin.vue'),
    meta: { title: '登录', userRoute: true, public: true }
  },
  // 用户端主入口（UserHome 公开，其余子页面需登录）
  {
    path: '/user',
    name: 'UserLayout',
    component: () => import('@/views/UserLayout.vue'),
    redirect: '/user/home',
    children: [
      {
        path: 'home',
        name: 'UserHome',
        component: () => import('@/views/user/UserHome.vue'),
        meta: { title: '首页', userRoute: true, public: true }
      },
      {
        path: 'search',
        name: 'UserSearch',
        component: () => import('@/views/user/UserSearch.vue'),
        meta: { title: '智能检索', userRoute: true }
      },
      {
        path: 'sop',
        name: 'UserSop',
        component: () => import('@/views/user/UserSop.vue'),
        meta: { title: 'SOP问答', userRoute: true }
      },
      {
        path: 'case',
        name: 'UserCase',
        component: () => import('@/views/user/UserCase.vue'),
        meta: { title: '案例查看', userRoute: true }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/UserProfile.vue'),
        meta: { title: '个人中心', userRoute: true }
      }
    ]
  },
  // ==============================================
  // 管理端路由（现有 /）
  // ==============================================
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/Search.vue'),
        meta: { title: '多模态检索' }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/ChatView.vue'),
        meta: { title: 'AI智能问答' }
      },
      {
        path: 'search-history',
        name: 'SearchHistory',
        component: () => import('@/views/SearchHistoryView.vue'),
        meta: { title: '检索历史' }
      },
      {
        path: 'workflow',
        name: 'WorkFlow',
        component: () => import('@/views/WorkFlow.vue'),
        meta: { title: '检修作业指引' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/Knowledge.vue'),
        meta: { title: '知识库管理' }
      },
      {
        path: 'case',
        name: 'Case',
        component: () => import('@/views/CaseManagement.vue'),
        meta: { title: '知识案例' }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/Audit.vue'),
        meta: { title: '案例审核' }
      },
      {
        path: 'ai-feedback',
        name: 'AiFeedback',
        component: () => import('@/views/AiFeedbackDashboard.vue'),
        meta: { title: 'AI修正统计' }
      },
      
      {
        path: 'user-management',
        name: 'User',
        component: () => import('@/views/UserManagement.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/RoleManagement.vue'),
        meta: { title: '角色管理', requiresAdmin: true }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/System.vue'),
        meta: { title: '系统配置', requiresAdmin: true }
      },
      {
        path: 'menu',
        name: 'MenuManagement',
        component: () => import('@/views/MenuManagement.vue'),
        meta: { title: '菜单管理', requiresAdmin: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 检查用户是否有权限访问菜单
function hasPermission(path: string): boolean {
  const menusStr = localStorage.getItem('menus')
  if (!menusStr) return false
  
  try {
    const menus = JSON.parse(menusStr)
    return checkMenuPermission(menus, path)
  } catch {
    return false
  }
}

// 递归检查菜单权限
function checkMenuPermission(menus: any[], path: string): boolean {
  for (const menu of menus) {
    if (menu.path === path) {
      return true
    }
    if (menu.children && menu.children.length > 0) {
      if (checkMenuPermission(menu.children, path)) {
        return true
      }
    }
  }
  return false
}

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // ======== 管理端登录页 ========
  if (to.path === '/login') {
    if (token) { next('/'); return }
    next()
    return
  }

  // ======== 用户端登录页 ========
  if (to.path === '/user/login') {
    if (token) { next('/user/home'); return }
    next()
    return
  }

  // ======== 用户端公开页面（首页） ========
  if (to.meta.userRoute && to.meta.public) {
    next()
    return
  }

  // ======== 用户端需登录页面 ========
  if (to.path.startsWith('/user/') || to.path === '/user') {
    if (!token) { next('/user/login'); return }
    next()
    return
  }

  // ======== 管理端（需登录） ========
  if (!token) { next('/login'); return }

  // 首页和仪表盘不需要权限检查
  if (to.path === '/' || to.path === '/dashboard') {
    next()
    return
  }

  // 检查权限（需要菜单已加载）
  const menusStr = localStorage.getItem('menus')
  if (menusStr) {
    if (!hasPermission(to.path)) {
      next('/dashboard')
      return
    }

    // 检查是否需要管理员权限
    if (to.meta.requiresAdmin) {
      const userStore = useUserStore()
      if (!userStore.isAdmin()) {
        ElMessage.error('无权限访问该页面')
        next('/dashboard')
        return
      }
    }
    // 菜单未加载时允许访问，由页面自行处理
  }

  next()
})

export default router
