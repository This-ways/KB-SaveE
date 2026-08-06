import api from '@/api'; // 토큰 자동 첨부되는 axios 인스턴스

const BASE_URL = '/api/goals';

export default {
  // 카테고리 목록 (13종) - 인증 불필요하지만 같은 인스턴스로 호출
  // 응답: [{ categoryId, name }, ...]
  async getCategories() {
    const { data } = await api.get('/api/categories');
    return data;
  },

  // 카테고리별 최근 N개월 평균 지출
  // mydata_connected = false 인 사용자는 빈 배열이 내려옴
  // 응답: [{ categoryId, avgAmount }, ...]
  async getCategoryAverages(months = 3) {
    const { data } = await api.get(`${BASE_URL}/category-averages`, {
      params: { months },
    });
    return data;
  },

  // 이번 달 내 목표 목록
  // yearMonth 생략 시 서버가 현재월로 처리 (없으면 지난달 목표가 자동 이월됨)
  // 응답: [{ goalId, categoryId, yearMonth, targetAmount }, ...]
  async getMyGoals(yearMonth) {
    const { data } = await api.get(BASE_URL, {
      params: yearMonth ? { yearMonth } : {},
    });
    return data;
  },

  // 목표 여러 개 한 번에 저장 (카테고리 선택 -> 금액 입력 -> 확인 화면용)
  // goals = [{ categoryId, targetAmount }, ...]
  // 하나라도 실패하면 전체 롤백됨
  async saveAll(goals) {
    const { data } = await api.post(`${BASE_URL}/batch`, goals);
    return data;
  },

  // 목표 1건 저장/수정
  async save(goal) {
    const { data } = await api.post(BASE_URL, goal);
    return data;
  },

  // 예상 절약액 = Σ(카테고리별 평균 지출 - 목표금액), 음수는 0 처리
  // 응답: { expectedSaving: 숫자 }
  async getExpectedSaving(yearMonth, months = 3) {
    const { data } = await api.get(`${BASE_URL}/expected-saving`, {
      params: yearMonth ? { yearMonth, months } : { months },
    });
    return data;
  },
};
