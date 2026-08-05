<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import userApi from '@/api/userApi';

const router = useRouter();

const form = ref({
  loginId: '',
  password: '',
  passwordCheck: '',
  userName: '',
  birthDate: '',
});
const agree = ref(false);
const error = ref('');
const submitting = ref(false);

const doJoin = async () => {
  if (submitting.value) return;
  error.value = '';

  if (!form.value.loginId || !form.value.password || !form.value.userName || !form.value.birthDate) {
    error.value = '모든 항목을 입력해 주세요.';
    return;
  }
  if (form.value.password !== form.value.passwordCheck) {
    error.value = '비밀번호가 일치하지 않습니다.';
    return;
  }
  if (!agree.value) {
    error.value = '약관에 동의해 주세요.';
    return;
  }

  submitting.value = true;
  try {
    await userApi.signup({
      loginId: form.value.loginId,
      password: form.value.password,
      userName: form.value.userName,
      birthDate: form.value.birthDate,
    });
    alert('회원가입이 완료되었습니다. 로그인해 주세요.');
    router.push('/auth/login');
  } catch (e) {
    // 백엔드: 아이디 중복 시 400 (IllegalStateException 핸들러)
    error.value = e?.response?.data || '회원가입에 실패했습니다.';
  } finally {
    submitting.value = false;
  }
};

const goLogin = () => router.push('/auth/login');
</script>

<template>
  <div class="join-page">
    <div class="tabs">
      <button class="tab" @click="goLogin">로그인</button>
      <button class="tab active">회원가입</button>
    </div>

    <div class="form">
      <label class="field">
        <span class="label">아이디</span>
        <input v-model="form.loginId" type="text" placeholder="아이디를 입력해 주세요" />
      </label>

      <label class="field">
        <span class="label">비밀번호</span>
        <input v-model="form.password" type="password" placeholder="비밀번호를 입력해 주세요" />
      </label>

      <label class="field">
        <span class="label">비밀번호 확인</span>
        <input v-model="form.passwordCheck" type="password" placeholder="비밀번호를 다시 입력해 주세요" />
      </label>

      <label class="field">
        <span class="label">이름</span>
        <input v-model="form.userName" type="text" placeholder="이름을 입력해 주세요" />
      </label>

      <label class="field">
        <span class="label">생년월일</span>
        <input v-model="form.birthDate" type="date" />
      </label>

      <label class="agree">
        <input v-model="agree" type="checkbox" />
        <span>서비스 이용약관 및 개인정보처리방침에 동의합니다</span>
      </label>

      <p v-if="error" class="error">{{ error }}</p>

      <button class="join-btn" :disabled="submitting" @click="doJoin">
        {{ submitting ? '가입 중...' : '가입하기' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.join-page {
  padding: 20px;
  min-height: 100vh;
  background: #fff;
}
.tabs {
  display: flex;
  border-bottom: 1px solid #f3f4f6;
  margin: 20px 0 28px;
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
  gap: 18px;
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
.agree {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
}
.error {
  color: #ef4444;
  font-size: 13px;
}
.join-btn {
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
.join-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
}
</style>
