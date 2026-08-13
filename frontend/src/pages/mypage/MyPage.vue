<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const userName = computed(() => auth.userName || '사용자');
const loginId = computed(() => auth.loginId || '-');

const logout = () => {
  auth.logout();
  router.push('/auth/login');
};

// 아직 화면이 없는 항목은 자리만 잡아둠
const notReady = (label) => alert(`${label} 화면은 준비 중이에요.`);
</script>

<template>
  <div class="mypage">
    <!-- 헤더 -->
    <div class="header">
      <button class="back-btn" @click="router.push('/home')">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <h1 class="header-title">마이페이지</h1>
    </div>

    <!-- 프로필 -->
    <div class="profile">
      <span class="avatar"><i class="fa-solid fa-user"></i></span>
      <span class="profile-text">
        <strong>{{ userName }}님</strong>
        <span class="sub">{{ loginId }}</span>
      </span>
    </div>

    <!-- 계정 -->
    <p class="section-label">계정</p>
    <div class="group">
      <button class="mypage-row" @click="router.push('/mydata/connect')">
        <i class="fa-solid fa-building-columns mypage-row-icon"></i>
        <span class="mypage-row-label">
          계좌 연결
          <span class="status-badge" :class="{ connected: auth.isMydataConnected }">
            ({{ auth.isMydataConnected ? '연결됨' : '미연결' }})
          </span>
        </span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
    </div>

    <!-- 앱 설정 -->
    <p class="section-label">앱 설정</p>
    <div class="group">
      <button class="mypage-row" @click="router.push('/mypage/notification')">
        <i class="fa-solid fa-bell mypage-row-icon"></i>
        <span class="mypage-row-label">알림 설정</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
    </div>

    <!-- 지원 및 정보 -->
    <p class="section-label">지원 및 정보</p>
    <div class="group">
      <button class="mypage-row" @click="router.push('/mypage/support')">
        <i class="fa-solid fa-headset mypage-row-icon"></i>
        <span class="mypage-row-label">고객센터</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
      <button class="mypage-row" @click="router.push('/mypage/legal')">
        <i class="fa-solid fa-file-lines mypage-row-icon"></i>
        <span class="mypage-row-label">법적 정보</span>
        <span class="mypage-row-value">개인정보 처리방침 등</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
      <button class="mypage-row" @click="router.push('/mypage/help')">
        <i class="fa-solid fa-circle-question mypage-row-icon"></i>
        <span class="mypage-row-label">도움말</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
      <button class="mypage-row" @click="logout">
        <i class="fa-solid fa-right-from-bracket mypage-row-icon"></i>
        <span class="mypage-row-label">로그아웃</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
    </div>

    <p class="version">KB SaveE v1.0.0</p>
  </div>
</template>

<style scoped>
.mypage {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 76px 16px 40px;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  z-index: 100;
  background: #f7f8fa;
}
.back-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #111;
  padding: 0;
}
.header-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
}

.profile {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 18px 16px;
  background: #fff;
  border-radius: 14px;
  text-align: left;
  margin-bottom: 24px;
}
.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #ffbc00;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex: 0 0 48px;
}
.profile-text {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.profile-text strong {
  font-size: 16px;
  font-weight: 700;
  color: #111;
}
.sub {
  font-size: 12px;
  color: #9ca3af;
}

.section-label {
  font-size: 12px;
  color: #9ca3af;
  margin: 0 0 8px 4px;
}
.group {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 24px;
}
.mypage-row {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 12px;
  width: 100%;
  padding: 16px;
  background: none;
  border: none;
  border-bottom: 1px solid #f4f5f7;
  text-align: left;
  cursor: pointer;
}
.mypage-row:last-child {
  border-bottom: none;
}
.mypage-row:hover {
  background: #fafafa;
}
.mypage-row-icon {
  width: 20px;
  text-align: center;
  font-size: 15px;
  color: #ffbc00;
  flex: 0 0 20px;
}
.mypage-row-label {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 16px;
  color: #111;
}
.status-badge {
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af;
  margin-left: 4px;
  white-space: nowrap;
}
.status-badge.connected {
  color: #d99a00;
}
.mypage-row-value {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  flex: 0 0 auto;
}
.arrow {
  font-size: 12px;
  color: #d1d5db;
  flex: 0 0 auto;
}

.version {
  text-align: center;
  font-size: 12px;
  color: #c8ccd1;
  margin-top: 8px;
}
</style>