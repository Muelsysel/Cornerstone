import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'

// 路由表：登录页公开，管理页面挂载在 Layout 之下并标记 requireAuth。
// 路由守卫统一拦截：未登录访问受限页面时跳转 /login。
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    // 已登录再访问登录页则直接回首页
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/system/user',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' },
      },
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar' },
      },
      {
        path: 'system/menu',
        name: 'MenuManage',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu' },
      },
      {
        path: 'system/dept',
        name: 'DeptManage',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'demo/announcement',
        name: 'AnnouncementManage',
        component: () => import('@/views/demo/announcement/index.vue'),
        meta: { title: '公告管理', icon: 'Bell' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫：未标记 public 的路由均需登录态。
router.beforeEach((to) => {
  const hasToken = !!getToken()
  const isPublic = to.meta.public === true
  // 未登录访问受限页面 -> 登录页（带 redirect，登录后回跳）
  if (!isPublic && !hasToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录访问公开页（登录页）-> 回首页
  if (isPublic && hasToken) {
    return '/'
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${String(to.meta.title)} - Cornerstone` : 'Cornerstone 管理后台'
})

export default router
