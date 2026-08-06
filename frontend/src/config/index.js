export default {
  title: 'SaveE', // 메인 타이틀
  subtitle: '절약이 적금이 되는 가장 쉬운 방법', // 서브 타이틀
  menus: [
    // 메인 메뉴 구성 정보 (실제 만들어진 화면만 등록)
    {
      title: '홈',
      url: '/',
      icon: 'fa-solid fa-house',
    },
    {
      title: '소비분석',
      url: '/report',
      icon: 'fa-solid fa-chart-pie',
    },
    // TODO: 적금/마이페이지 화면 완성되면 여기에 추가 (다른 팀원 담당)
  ],
  accountMenus: {
    // 인증 관련 메뉴 정보
    login: {
      url: '/auth/login',
      title: '로그인',
      icon: 'fa-solid fa-right-to-bracket',
    },

    join: {
      url: '/auth/join',
      title: '회원가입',
      icon: 'fa-solid fa-user-plus',
    },
  },
};