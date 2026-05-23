import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { useUserStore } from '@/stores'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/views/ResetPasswordView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/dreams',
      name: 'dreams',
      component: () => import('@/views/DreamListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('@/views/ChangePasswordView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dream-stats',
      name: 'dream-stats',
      component: () => import('@/views/DreamStatsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/AdminView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/record-dream',
      name: 'record-dream',
      component: () => import('@/views/RecordDreamView.vue')
    },
    {
      path: '/learn-more',
      name: 'learn-more',
      component: () => import('@/views/LearnMoreView.vue')
    },
    {
      path: '/error/:code',
      name: 'error',
      component: () => import('@/views/ErrorView.vue')
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/ErrorView.vue'),
      props: { code: '404' }
    }
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (userStore.isLoggedIn && userStore.isSessionExpired()) {
    userStore.logout()
  }

  if (userStore.isLoggedIn) {
    userStore.refreshSession()
  }

  const isLoggedIn = userStore.isLoggedIn
  const role = userStore.role

  if (to.meta.requiresAuth && !isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && isLoggedIn) {
    return '/'
  }

  if (to.meta.requiresAdmin && role !== 'ADMIN') {
    return '/error/403'
  }
})

export default router
