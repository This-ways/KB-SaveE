import api from '@/api'; // 토큰 자동 첨부되는 axios 인스턴스

const BASE_URL = '/api/users';

export default {
  // 회원가입
  // member = { loginId, password, userName, birthDate: 'YYYY-MM-DD' }
  // 응답: 생성된 userId (숫자)
  async signup(member) {
    const { data } = await api.post(`${BASE_URL}/signup`, member);
    return data;
  },

  // 아이디 중복확인 버튼 - true면 사용 가능
  async checkLoginId(loginId) {
    const { data } = await api.get(`${BASE_URL}/check-id`, { params: { loginId } });
    return data;
  },

  // 마이데이터(계좌/카드) 연결 - "계좌 연결" 버튼 클릭 시 호출
  // userId는 JWT에서 추출하므로 별도 파라미터 없음
  async connectMydata() {
    await api.post(`${BASE_URL}/mydata/connect`);
  },
};
