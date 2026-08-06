import api from '@/api';
import { useAuthStore } from '@/stores/auth';

const BASE_URL = '/savings';

// 헤더에 Authorization 토큰을 담는 공통 함수
const getAuthHeader = () => {
  const authStore = useAuthStore();
  return {
    headers: {
      Authorization: `Bearer ${authStore.token}`,
    },
  };
};

export default {
  // 1. 내 가입 적금 ID 조회
  async getMySubscriptionId() {
    const { data } = await api.get(
      `${BASE_URL}/my-subscription-id`,
      getAuthHeader(),
    );
    return data;
  },

  // 2. 적금 현황 상세 조회
  async getSavingsStatus(subscriptionId) {
    const { data } = await api.get(
      `${BASE_URL}/status/${subscriptionId}`,
      getAuthHeader(),
    );
    return data;
  },

  // 3. 적금 추천 목록 조회
  async getRecommendedSavings(reqData) {
    const { data } = await api.post(
      `${BASE_URL}/recommend`,
      reqData,
      getAuthHeader(),
    );
    return data;
  },

  // 4. 상품 상세 정보 조회
  async getSavingsDetail(productId) {
    const { data } = await api.get(
      `${BASE_URL}/products/${productId}`,
      getAuthHeader(),
    );
    return data;
  },

  // 5. 자동이체 금액 수정
  async updateMonthlyAmount(subscriptionId, newAmount) {
    const { data } = await api.put(
      `${BASE_URL}/auto-transfer/${subscriptionId}/amount`,
      { newAmount },
      getAuthHeader(),
    );
    return data;
  },
};
