// src/api/notificationApi.js

import api from '@/api'; // 토큰 자동 첨부되는 axios 인스턴스

const BASE_URL = '/api/notifications';

export default {
  // 기기토큰 등록
  // 같은 토큰을 다시 보내면 서버에서 ON DUPLICATE KEY UPDATE 로 처리됨
  // 한 사용자가 여러 기기를 등록할 수 있음 (멀티 디바이스 발송)
  async registerDeviceToken(fcmToken) {
    const { data } = await api.post(`${BASE_URL}/device-token`, { fcmToken });
    return data;
  },

  // 기기토큰 삭제 (로그아웃 / 알림 끄기 시)
  async deleteDeviceToken(fcmToken) {
    const { data } = await api.delete(`${BASE_URL}/device-token`, {
      params: { fcmToken },
    });
    return data;
  },

  // 알림함 목록 조회
  // title / body 는 서버 DTO 에서 조립돼 내려오므로 그대로 출력하면 됨
  // 응답: [{ notificationId, categoryId, typeCode, thresholdRate,
  //          targetMonth, title, body, sentAt }, ...]
  // 예산 알림 : typeCode = 'BUDGET', categoryId 있음, thresholdRate 는 50/70/90
  // 적금 알림 : typeCode = 'PAY_UPCOMING' | 'PAY_SUCCESS' | 'PAY_FAIL', categoryId 는 null
  async getNotifications() {
    const { data } = await api.get(BASE_URL);
    return data;
  },

  // 목표가 설정된 전체 카테고리의 소진율을 체크해 임계값(50/70/90) 초과분 알림 생성
  // 거래 저장 시점 훅이 없어 화면 진입 시 호출하는 방식
  // 중복 발송은 DB UNIQUE 제약으로 차단되므로 여러 번 호출해도 안전함
  // targetMonth 형식: '2026-08'
  async checkAllCategories(targetMonth) {
    const { data } = await api.post(`${BASE_URL}/check-all`, null, {
      params: { targetMonth },
    });
    return data;
  },

  // 기준월 이전 알림 일괄 삭제 (알림함 무한 누적 방지)
  async deleteOldNotifications(currentMonth) {
    const { data } = await api.delete(`${BASE_URL}/old`, {
      params: { currentMonth },
    });
    return data;
  },

    // 푸시 수신 여부 조회 (user.push_enabled)
  async getPushEnabled() {
    const { data } = await api.get('/api/users/push-enabled');
    return data;
  },

  // 푸시 수신 여부 변경
  async updatePushEnabled(enabled) {
    const { data } = await api.patch('/api/users/push-enabled', null, {
      params: { enabled },
    });
    return data;
  },
};