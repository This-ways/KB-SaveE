// public/firebase-messaging-sw.js
// 브라우저가 백그라운드일 때 푸시를 받는 서비스워커
// public 폴더에 있어 Vite 빌드를 타지 않으므로 import.meta.env 사용 불가
// Firebase 웹 설정값은 브라우저에 노출되는 게 정상이라 직접 적어도 무방함

importScripts('https://www.gstatic.com/firebasejs/12.17.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/12.17.1/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: 'AIzaSyDC0dt37oZLQqjMrd7HrxzEVj0BaMBI1CM',
  authDomain: 'savee-9e15d.firebaseapp.com',
  projectId: 'savee-9e15d',
  storageBucket: 'savee-9e15d.firebasestorage.app',
  messagingSenderId: '987563284638',
  appId: '1:987563284638:web:b9f14eba7a547e1a6a438f',
});

const messaging = firebase.messaging();

// onBackgroundMessage 는 등록하지 않는다
// 서버가 notification 페이로드를 보내므로 백그라운드 알림은 브라우저가 자동으로 띄우고,
// 핸들러를 등록해두면 포그라운드 메시지까지 SW 가 가져가버려 인앱 토스트가 뜨지 않는다

// 알림 클릭 시 알림함으로 이동
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      for (const client of list) {
        if ('focus' in client) {
          client.navigate('/notifications');
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow('/notifications');
      }
    }),
  );
});