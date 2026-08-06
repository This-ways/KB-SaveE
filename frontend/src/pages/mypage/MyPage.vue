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
    <button class="profile" @click="notReady('내 정보')">
      <span class="avatar"><i class="fa-solid fa-user"></i></span>
      <span class="profile-text">
        <strong>{{ userName }}님</strong>
        <span class="sub">{{ loginId }}</span>
      </span>
      <i class="fa-solid fa-chevron-right arrow"></i>
    </button>

    <!-- 계정 -->
    <p class="section-label">계정</p>
    <div class="group">
      <button class="row" @click="notReady('내 정보')">
        <i class="fa-solid fa-user row-icon"></i>
        <span class="row-label">내 정보</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
      <button class="row" @click="router.push('/mydata/connect')">
        <i class="fa-solid fa-building-columns row-icon"></i>
        <span class="row-label">계좌 연결</span>
        <span class="row-value">
          {{ auth.isMydataConnected ? '연결됨' : '미연결' }}
        </span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
    </div>

    <!-- 앱 설정 -->
    <p class="section-label">앱 설정</p>
    <div class="group">
      <button class="row" @click="notReady('알림 설정')">
        <i class="fa-solid fa-bell row-icon"></i>
        <span class="row-label">알림 설정</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
    </div>

    <!-- 지원 및 정보 -->
    <p class="section-label">지원 및 정보</p>
    <div class="group">
      <button class="row" @click="notReady('고객센터')">
        <i class="fa-solid fa-headset row-icon"></i>
        <span class="row-label">고객센터</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
      <button class="row" @click="router.push('/mypage/legal')">
        <i class="fa-solid fa-file-lines row-icon"></i>
        <span class="row-label">법적 정보</span>
        <span class="row-value">개인정보 처리방침 등</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
      <button class="row" @click="router.push('/mypage/help')">
        <i class="fa-solid fa-circle-question row-icon"></i>
        <span class="row-label">도움말</span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
      <button class="row" @click="logout">
        <i class="fa-solid fa-right-from-bracket row-icon"></i>
        <span class="row-label">로그아웃</span>
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
  padding: 16px 16px 40px;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 4px 16px;
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
  border: none;
  border-radius: 14px;
  text-align: left;
  cursor: pointer;
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
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 16px;
  background: none;
  border: none;
  border-bottom: 1px solid #f4f5f7;
  text-align: left;
  cursor: pointer;
}
.row:last-child {
  border-bottom: none;
}
.row:hover {
  background: #fafafa;
}
.row-icon {
  width: 20px;
  text-align: center;
  font-size: 15px;
  color: #ffbc00;
  flex: 0 0 20px;
}
.row-label {
  flex: 1 1 0;
  font-size: 14px;
  color: #111;
}
.row-value {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
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
