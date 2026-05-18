import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

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
      component: () => import('@/views/RegisterView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/views/ResetPasswordView.vue')
    },
    {
      path: '/dreams',
      name: 'dreams',
      component: () => import('@/views/DreamListView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue')
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('@/views/ChangePasswordView.vue')
    },
    {
      path: '/dream-stats',
      name: 'dream-stats',
      component: () => import('@/views/DreamStatsView.vue')
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

export default router
