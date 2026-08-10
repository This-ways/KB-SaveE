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
    hasGoals: false, // 목표(카테고리/예산)를 한 번이라도 설정한 적 있는지 - 온보딩 완료 여부 판단용
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

  // 계좌는 연결했지만(mydataConnected=true) 카테고리/예산 설정을 아직 한 번도 안 끝냈으면
  // "온보딩 중간에 이탈한" 상태 - 로그인 라우팅에서 /home이 아니라 /goal/category로 보내야 함
  const hasGoals = computed(() => state.value.user.hasGoals);

  const login = async (member) => {
    // member = { username, password }  (백엔드 필터가 username 필드명을 사용)
    const { data } = await axios.post('/api/auth/login', member);
    state.value = { ...data };
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  const logout = () => {
    // 서버에도 로그아웃을 알려서 token_version을 올려야, 다른 탭/기기에 남아있는 토큰도
    // 만료 전에 즉시 무효 처리된다. state를 지우기 전에 토큰을 직접 헤더에 실어서 보내야 함
    // (여기서 쓰는 axios는 인터셉터 없는 순수 인스턴스라 토큰을 자동으로 안 붙여줌 - login()과 동일 패턴).
    // 실패해도(네트워크 문제 등) 로컬 로그아웃 자체는 그대로 진행한다.
    const token = state.value.token;
    if (token) {
      axios
        .post('/api/auth/logout', null, { headers: { Authorization: `Bearer ${token}` } })
        .catch((e) => console.warn('로그아웃 서버 반영 실패 (로컬 로그아웃은 정상 진행)', e));
    }
    localStorage.removeItem('auth'); // 온보딩 봤음 플래그는 남겨둬야 하므로 clear() 대신 removeItem
    state.value = { ...initState };
  };

  const getToken = () => state.value.token;

  // 마이데이터 연결 완료 후 재로그인 없이 상태만 갱신
  const setMydataConnected = () => {
    state.value.user.mydataConnected = true;
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  // 카테고리/예산 설정(온보딩) 완료 후 재로그인 없이 상태만 갱신
  const setHasGoals = () => {
    state.value.user.hasGoals = true;
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
    hasGoals,
    login,
    logout,
    getToken,
    setMydataConnected,
    setHasGoals,
  };
});
