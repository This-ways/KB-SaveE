// 마이페이지 도메인 라우트 (강두형 담당)
export default [
  {
    path: '/mypage',
    name: 'mypage',
    component: () => import('../pages/mypage/MyPage.vue'),
  },
  {
    path: '/mypage/legal',
    name: 'mypageLegal',
    component: () => import('../pages/mypage/LegalPage.vue'),
  },
  {
    path: '/mypage/help',
    name: 'mypageHelp',
    component: () => import('../pages/mypage/HelpPage.vue'),
  },
];
