// SAVINGS 도메인 화면 라우트
// router/index.js 에서 import 해서 스프레드로 합침
export default [
  // 1. 내 적금 현황 페이지 (subscriptionId 파라미터 수신)
  {
    path: '/savings/status/:subscriptionId',
    name: 'savingsStatus',
    component: () => import('../pages/savings/SavingsStatusPage.vue'),
    props: true,
  },
  // 2. 적금 추천 페이지 (미가입 시 이동용)
  {
    path: '/savings/recommend',
    name: 'savingsRecommend',
    component: () => import('../pages/savings/SavingsRecommendPage.vue'),
  },
  //   // 3. 적금 상품 상세 페이지
  {
    path: '/savings/products/:productId',
    name: 'savingsDetail',
    component: () => import('../pages/savings/SavingsDetailPage.vue'),
    props: true,
  },
  {
    path: '/savings/subscribe/:productId',
    name: 'savings-subscribe',
    component: () => import('@/views/savings/SavingsSubscribePage.vue'),
  },
];
