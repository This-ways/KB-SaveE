// src/util/usePushSetup.js
// 권한 요청 -> 토큰 발급 -> 서버 등록 을 묶은 컴포저블
// 로그인 직후나 알림 설정 화면에서 호출
// 로그아웃 시 기기토큰 삭제는 서버(AuthController)에서 user_id 기준으로 일괄 처리한다

import { ref } from 'vue';
import { requestFcmToken } from '@/util/firebase';
import notificationApi from '@/api/notificationApi';

export function usePushSetup() {
  const isLoading = ref(false);
  const isEnabled = ref(false);
  const errorMessage = ref('');

  // ---------------------------------------------------------------
  // 소진율 체크
  //
  // 서버가 "목표가 설정된 카테고리"만 훑어서 임계값 초과분을 기록/발송한다.
  // 즉 예산이 없는 카테고리는 애초에 대상이 아니므로, 예산이 확정된 뒤에 불러야 의미가 있다.
  //
  // 재호출해도 안전하다 : notification 테이블에
  // UNIQUE(user_id, category_id, target_month, threshold_rate) 가 걸려 있어
  // 이미 기록된 알림은 INSERT 단계에서 걸러지고 푸시도 나가지 않는다.
  // 그래서 아래 두 진입점에서 각각 부담 없이 호출한다.
  // ---------------------------------------------------------------
  async function checkBudgetUsage() {
    const now = new Date();
    const targetMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;

    try {
      await notificationApi.checkAllCategories(targetMonth);
    } catch (e) {
      // 체크가 실패해도 앱 사용에는 지장이 없으므로 로그만 남긴다
      console.warn('[알림] 소진율 체크 실패', e);
    }
  }

  async function setupPush() {
    isLoading.value = true;
    errorMessage.value = '';

    try {
      const token = await requestFcmToken();

      // 권한 거부 / 미지원 브라우저 -> 여기서 종료
      // 푸시를 못 받아도 알림함(DB 기록)은 정상 동작하므로 앱에는 문제없음
      if (!token) {
        errorMessage.value = '알림 권한이 허용되지 않았습니다.';
        isEnabled.value = false;
        return false;
      }

      await notificationApi.registerDeviceToken(token);

      isEnabled.value = true;
      return true;
    } catch (e) {
      console.error('[푸시 설정 실패]', e);
      errorMessage.value = '알림 설정 중 오류가 발생했습니다.';
      isEnabled.value = false;
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  return { isLoading, isEnabled, errorMessage, setupPush, checkBudgetUsage };
}