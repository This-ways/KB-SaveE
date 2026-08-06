import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import axios from 'axios';

// 백엔드 로그인 응답 구조에 맞춘 초기값
// POST /api/auth/login 응답:
// { token, user: { userId, loginId, userName, joinMonth, trial, mydataConnected } }
const initState = {
  token: '', // 접근 토큰(JWT)
  user: {
    userId: null, // 사용자 PK
    loginId: '', // 로그인 아이디
    userName: '', // 이름
    joinMonth: '', // 가입월 "YYYY-MM"
    trial: false, // 가입월 == 현재월이면 트라이얼
    mydataConnected: false, // 마이데이터(계좌/카드) 연결 여부
  },
};

export const useAuthStore = defineStore('auth', () => {
  const state = ref({ ...initState });

  const isLogin = computed(() => !!state.value.user.loginId); // 로그인 여부

  const userId = computed(() => state.value.user.userId);

  const loginId = computed(() => state.value.user.loginId);

  const userName = computed(() => state.value.user.userName);

  const isTrial = computed(() => state.value.user.trial);

  // 로그인 직후 라우팅 분기에 사용
  // true  -> 메인으로 바로
  // false -> "데이터를 연결해 주세요" 화면부터
  const isMydataConnected = computed(() => state.value.user.mydataConnected);

  const login = async (member) => {
    // member = { username, password }  (백엔드 필터가 username 필드명을 사용)
    const { data } = await axios.post('/api/auth/login', member);
    state.value = { ...data };
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  const logout = () => {
    localStorage.removeItem('auth'); // 온보딩 봤음 플래그는 남겨둬야 하므로 clear() 대신 removeItem
    state.value = { ...initState };
  };

  const getToken = () => state.value.token;

  // 마이데이터 연결 완료 후 재로그인 없이 상태만 갱신
  const setMydataConnected = () => {
    state.value.user.mydataConnected = true;
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  const load = () => {
    const auth = localStorage.getItem('auth');
    if (auth != null) {
      state.value = JSON.parse(auth);
    }
  };

  load();

  return {
    state,
    isLogin,
    userId,
    loginId,
    userName,
    isTrial,
    isMydataConnected,
    login,
    logout,
    getToken,
    setMydataConnected,
  };
});
