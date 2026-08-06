// src/util/firebase.js
// Firebase 초기화 + FCM 토큰 발급 / 포그라운드 수신

import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

// Firebase 콘솔 > 프로젝트 설정 > 일반 > 내 앱(웹) 에서 복사
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

// Firebase 콘솔 > 클라우드 메시징 > 웹 푸시 인증서
const VAPID_KEY = import.meta.env.VITE_FIREBASE_VAPID_KEY;

// .env 가 없는 팀원 환경에서도 앱이 죽지 않도록 조건부 초기화
// 푸시만 비활성화되고 나머지 기능은 정상 동작함
const isConfigured = Boolean(firebaseConfig.apiKey);

let messaging = null;

if (isConfigured) {
  const app = initializeApp(firebaseConfig);
  messaging = getMessaging(app);
} else {
  console.warn('[FCM] Firebase 설정이 없어 푸시 기능이 비활성화됩니다.');
}

// 푸시 사용 가능 여부 (설정 화면 안내 문구 분기용)
export function isPushAvailable() {
  return isConfigured;
}

// 알림 권한 요청 후 FCM 토큰 발급
// 거부 / 미지원 / 실패 시 null 반환 (호출부에서 분기)
export async function requestFcmToken() {
  if (!messaging) {
    return null;
  }

  // iOS Safari 등 웹 푸시 미지원 브라우저 대비
  if (!('Notification' in window) || !('serviceWorker' in navigator)) {
    console.warn('[FCM] 이 브라우저는 웹 푸시를 지원하지 않습니다.');
    return null;
  }

  // 이미 차단된 상태면 재요청해도 팝업이 뜨지 않으므로 바로 종료
  if (Notification.permission === 'denied') {
    console.warn(
      '[FCM] 알림 권한이 차단돼 있습니다. 브라우저 설정에서 해제 필요.',
    );
    return null;
  }

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    console.warn('[FCM] 사용자가 알림 권한을 허용하지 않았습니다.');
    return null;
  }

  try {
    // 서비스워커를 직접 등록해 넘겨줌
    // 자동 등록에 맡기면 경로를 못 찾는 경우가 있어 명시적으로 처리
    const registration = await navigator.serviceWorker.register(
      '/firebase-messaging-sw.js',
    );

      // register() 는 등록만 하고 활성화를 기다리지 않는다
  // 활성화 전에 토큰을 요청하면 no active Service Worker 에러가 나므로 대기
     await navigator.serviceWorker.ready;

    const token = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration: registration,
    });

    // register() 는 등록만 하고 활성화를 기다리지 않는다
    // 활성화 전에 토큰을 요청하면 no active Service Worker 에러가 나므로 대기
   

    if (!token) {
      console.warn('[FCM] 토큰 발급 실패');
      return null;
    }

    console.log('[FCM] 토큰 발급 성공:', token);
    return token;
  } catch (e) {
    console.error('[FCM] 토큰 발급 중 오류', e);
    return null;
  }
}

// 앱이 화면에 떠 있을 때 도착한 푸시 수신
// 백그라운드는 서비스워커가 처리하므로 여기서는 잡히지 않음
export function onForegroundMessage(handler) {
  if (!messaging) {
    return null; // 초기화 안 된 상태 방어
  }

  try {
    // 수신 로그를 남기기 위해 한번 래핑해서 handler로 전달하도록 구성
    return onMessage(messaging, (payload) => {
      console.log('[FCM] 포그라운드 메시지 수신', payload);
      handler(payload);
    });
  } catch (e) {
    console.warn('[FCM] 포그라운드 수신 등록 실패', e);
    return null;
  }
}
