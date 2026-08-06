import api from '@/api'; // 토큰 자동 첨부되는 axios 인스턴스

const BASE_URL = '/savings';

export default {
  // 내 가입 적금 ID 조회 (미가입 시 subscriptionId: null)
  async getMySubscriptionId() {
    const { data } = await api.get(`${BASE_URL}/my-subscription-id`);
    return data.subscriptionId;
  },

  // 적금 상세 현황 (상품명/금리/누적납입액/달성률 등)
  async getStatus(subscriptionId) {
    const { data } = await api.get(`${BASE_URL}/status/${subscriptionId}`);
    return data;
  },
};
