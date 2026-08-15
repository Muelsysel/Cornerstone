import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'
import { hasPermission } from '@/utils/permission'

// 路由表：登录页与错误页公开，管理页面挂载在 Layout 之下并标记 requireAuth。
// 路由守卫统一拦截：未登录访问受限页面跳转 /login；
// 已登录但缺少页面权限（meta.permission）时跳转 /403。
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    // 已登录再访问登录页则直接回首页
    meta: { title: '登录', public: true },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', public: true },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
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
        meta: { title: '用户管理', icon: 'User', group: '系统管理', permission: 'system:user:list' },
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar', group: '系统管理', permission: 'system:role:list' },
      },
      {
        path: 'system/menu',
        name: 'MenuManage',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu', group: '系统管理', permission: 'system:menu:list' },
      },
      {
        path: 'system/dept',
        name: 'DeptManage',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', group: '系统管理', permission: 'system:dept:list' },
      },
      {
        path: 'system/dict',
        name: 'DictManage',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', icon: 'Collection', group: '系统管理', permission: 'system:dict:list' },
      },
      {
        path: 'system/config',
        name: 'ConfigManage',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '参数设置', icon: 'Setting', group: '系统管理', permission: 'system:config:list' },
      },
      {
        path: 'system/operlog',
        name: 'OperLogManage',
        component: () => import('@/views/system/operlog/index.vue'),
        meta: { title: '操作日志', icon: 'Document', group: '系统管理', permission: 'system:log:list' },
      },
      {
        path: 'system/loginlog',
        name: 'LoginLogManage',
        component: () => import('@/views/system/loginlog/index.vue'),
        meta: { title: '登录日志', icon: 'Finished', group: '系统管理', permission: 'system:log:list' },
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
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', public: true },
  },
]

const router = createRouter({
  // hash 模式：SPA 路由在 # 后（如 #/system/user），nginx 静态托管只见 /，
  // 避免与 API 前缀 /system/、/demo/、/auth/ 冲突（history 模式刷新会被 nginx 反代到网关）
  history: createWebHashHistory(),
  routes,
})

// 全局前置守卫：
// 1. 未标记 public 的路由均需登录态，否则跳登录页；
// 2. 已登录访问公开页（登录/403/404）放行或对登录页回首页；
// 3. 路由声明了 meta.permission 时校验权限点，不足则跳 /403。
router.beforeEach((to) => {
  const hasToken = !!getToken()
  const isPublic = to.meta.public === true
  if (!isPublic && !hasToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && hasToken) {
    return '/'
  }
  // 权限校验：路由声明了权限点则由 store 判断
  const required = to.meta.permission as string | undefined
  if (hasToken && required && !hasPermission(required)) {
    return { path: '/403', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${String(to.meta.title)} - Cornerstone` : 'Cornerstone 管理后台'
})

export default router
