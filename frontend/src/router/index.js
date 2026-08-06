import { createRouter, createWebHistory } from 'vue-router';
import authRoutes from './auth';
import transactionRoutes from './transaction';
import reportRoutes from './report';
import goalRoutes from './goal';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 앱 진입점 - 온보딩(처음이면) 또는 로그인으로 자동 분기
    {
      path: '/',
      name: 'onboarding',
      component: () => import('../pages/OnboardingPage.vue'),
    },
    ...authRoutes,
    ...transactionRoutes,
    ...reportRoutes,
    ...goalRoutes,
  ],
});

export default router;