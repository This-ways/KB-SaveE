export default [
  {
    path: '/notifications',
    name: 'notification-list',
    component: () => import('@/pages/notification/NotificationListPage.vue'),
  },
  
    {
    path: '/mypage/notification',
    name: 'notification/setting',
    component: () => import('@/pages/notification/NotificationSettingPage.vue'),
  },
];