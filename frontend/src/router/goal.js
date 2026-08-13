// GOAL 도메인 화면 라우트 (강두형 담당)
// router/index.js 에서 import 해서 스프레드로 합침
export default [
  {
    path: '/mydata/connect',
    name: 'mydataConnect',
    component: () => import('../pages/goal/MydataConnectPage.vue'),
  },
  {
    path: '/goal/analysis',
    name: 'goalAnalysis',
    component: () => import('../pages/goal/AnalysisResultPage.vue'),
  },
  {
    path: '/goal/category',
    name: 'categorySelect',
    component: () => import('../pages/goal/CategorySelectPage.vue'),
  },
  {
    path: '/goal/budget',
    name: 'budgetSetting',
    component: () => import('../pages/goal/BudgetSettingPage.vue'),
  },
  {
    path: '/goal/complete',
    name: 'goalComplete',
    component: () => import('../pages/goal/CompletePage.vue'),
  },
];
