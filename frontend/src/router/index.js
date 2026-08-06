import { createRouter, createWebHistory } from 'vue-router';
import authRoutes from './auth';
import goalRoutes from './goal';
import transactionRoutes from './transaction';
import reportRoutes from './report';
import HomePage from '../pages/HomePage.vue';
import savingsRoutes from './savings';
<<<<<<< HEAD
import mypageRoutes from './mypage';
=======
import notificationRoutes from './notification';
>>>>>>> b8a0baa2200259d79e588ab439f37d9c82f4304f

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 앱 진입점 - 온보딩(처음이면) 또는 로그인으로 자동 분기
    {
      path: '/',
      name: 'onboarding',
      component: () => import('../pages/OnboardingPage.vue'),
    },
    // 온보딩/가입/마이데이터연결/목표설정 완료 후 도착하는 메인 홈
    {
      path: '/home',
      name: 'home',
      component: HomePage,
    },
    ...authRoutes,
    ...goalRoutes,
    ...transactionRoutes,
    ...reportRoutes,
    ...savingsRoutes,
<<<<<<< HEAD
    ...mypageRoutes,
=======
    ...notificationRoutes,
>>>>>>> b8a0baa2200259d79e588ab439f37d9c82f4304f
  ],
});

export default router;
