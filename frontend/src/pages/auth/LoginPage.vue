<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

// 백엔드 로그인 필터가 "username" 필드명을 사용하므로 그대로 맞춤
const form = ref({ username: '', password: '' });
const error = ref('');
const submitting = ref(false);

const doLogin = async () => {
  if (submitting.value) return;
  error.value = '';

  if (!form.value.username || !form.value.password) {
    error.value = '아이디와 비밀번호를 입력해 주세요.';
    return;
  }

  submitting.value = true;
  try {
    await auth.login(form.value);

    // 마이데이터 연결 여부로 진입 화면 분기
    if (auth.isMydataConnected) {
      router.push('/home'); // 이미 자산 연결된 기존 사용자는 메인으로
    } else {
      router.push('/mydata/connect');
    }
  } catch (e) {
    error.value = '아이디 또는 비밀번호가 일치하지 않습니다.';
  } finally {
    submitting.value = false;
  }
};

const goJoin = () => router.push('/auth/join');
</script>

<template>
  <div class="login-page">
    <div class="logo-area">
      <h1 class="logo"><span class="kb">KB</span> SaveE</h1>
      <p class="tagline">
        작은 절약이 모여 만드는 습관<br />
        지금 바로 시작해보세요
      </p>
    </div>

    <div class="tabs">
      <button class="tab active">로그인</button>
      <button class="tab" @click="goJoin">회원가입</button>
    </div>

    <div class="form">
      <label class="field">
        <span class="label">아이디</span>
        <input
          v-model="form.username"
          type="text"
          placeholder="아이디를 입력해 주세요"
          autocomplete="username"
          @keyup.enter="doLogin"
        />
      </label>

      <label class="field">
        <span class="label">비밀번호</span>
        <input
          v-model="form.password"
          type="password"
          placeholder="비밀번호를 입력해 주세요"
          autocomplete="current-password"
          @keyup.enter="doLogin"
        />
      </label>

      <p v-if="error" class="error">{{ error }}</p>

      <button class="login-btn" :disabled="submitting" @click="doLogin">
        {{ submitting ? '로그인 중...' : '로그인' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  padding: 40px 24px;
  min-height: 100vh;
  background: #fff;
}
.logo-area {
  text-align: center;
  padding: 40px 0 36px;
}
.logo {
  font-size: 30px;
  font-weight: 800;
  margin: 0;
}
.kb {
  color: #ffbc00;
}
.tagline {
  margin-top: 12px;
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.6;
}
.tabs {
  display: flex;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 32px;
}
.tab {
  flex: 1;
  padding: 12px 0;
  background: none;
  border: none;
  font-size: 15px;
  color: #9ca3af;
  cursor: pointer;
}
.tab.active {
  color: #111;
  font-weight: 600;
  border-bottom: 2px solid #ffbc00;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.field input {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 15px;
}
.field input:focus {
  outline: none;
  border-color: #ffbc00;
}
.error {
  color: #ef4444;
  font-size: 13px;
  margin: 0;
}
.login-btn {
  margin-top: 8px;
  padding: 16px;
  border: none;
  border-radius: 12px;
  background: #ffbc00;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
.login-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
}
</style>
