<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import userApi from '@/api/userApi';
import { useAlert } from '@/util/useAlert';
import CustomAlertModal from '@/components/common/CustomAlertModal.vue';

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

// 아이디 중복확인 버튼 상태: null(아직 확인 안 함) / 'checking' / 'available' / 'unavailable'
const idCheckState = ref(null);
const idCheckMessage = ref('');
const checkedLoginId = ref(''); // 마지막으로 확인 통과한 아이디 - 이후 값이 바뀌면 재확인 필요

// 아이디 입력값이 바뀌면 예전 확인 결과는 무효 (다른 아이디인데 "사용가능"이 남아있으면 안 되니까)
const onLoginIdInput = () => {
  idCheckState.value = null;
  idCheckMessage.value = '';
};

const checkLoginId = async () => {
  const id = form.value.loginId;
  if (!id || id.length < 4 || id.length > 20) {
    idCheckState.value = 'unavailable';
    idCheckMessage.value = '아이디는 4~20자로 입력해 주세요.';
    return;
  }
  idCheckState.value = 'checking';
  try {
    const available = await userApi.checkLoginId(id);
    if (available) {
      idCheckState.value = 'available';
      idCheckMessage.value = '사용 가능한 아이디예요.';
      checkedLoginId.value = id;
    } else {
      idCheckState.value = 'unavailable';
      idCheckMessage.value = '이미 사용 중인 아이디예요.';
    }
  } catch (e) {
    idCheckState.value = 'unavailable';
    idCheckMessage.value = '확인에 실패했어요. 잠시 후 다시 시도해 주세요.';
  }
};

const doJoin = async () => {
  if (submitting.value) return;
  error.value = '';

  if (!form.value.loginId || !form.value.password || !form.value.userName || !form.value.birthDate) {
    error.value = '모든 항목을 입력해 주세요.';
    return;
  }
  if (form.value.loginId.length < 4 || form.value.loginId.length > 20) {
    error.value = '아이디는 4~20자로 입력해 주세요.';
    return;
  }
  if (form.value.password.length < 4 || form.value.password.length > 20) {
    error.value = '비밀번호는 4~20자로 입력해 주세요.';
    return;
  }
  if (form.value.password !== form.value.passwordCheck) {
    error.value = '비밀번호가 일치하지 않습니다.';
    return;
  }
  // 중복확인을 아예 안 했거나, 확인 이후 아이디를 바꿔놓고 다시 확인 안 한 경우
  if (idCheckState.value !== 'available' || checkedLoginId.value !== form.value.loginId) {
    error.value = '아이디 중복확인을 먼저 해 주세요.';
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
    await showAlert('회원가입이 완료되었습니다. 로그인해 주세요.');
    router.push('/auth/login');
  } catch (e) {
    // 백엔드: 아이디 중복 시 400 (IllegalStateException 핸들러)
    error.value = e?.response?.data || '회원가입에 실패했습니다.';
  } finally {
    submitting.value = false;
  }
};

const goLogin = () => router.push('/auth/login');
const { alertState, showAlert, hideAlert } = useAlert();

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
        <div class="id-row">
          <input
            v-model="form.loginId"
            type="text"
            maxlength="20"
            placeholder="아이디를 입력해 주세요 (4~20자)"
            @input="onLoginIdInput"
          />
          <button
            type="button"
            class="check-btn"
            :disabled="idCheckState === 'checking'"
            @click="checkLoginId"
          >
            중복확인
          </button>
        </div>
        <span
          v-if="idCheckMessage"
          class="id-check-msg"
          :class="idCheckState"
        >
          {{ idCheckMessage }}
        </span>
      </label>

      <label class="field">
        <span class="label">비밀번호</span>
        <input v-model="form.password" type="password" maxlength="20" placeholder="비밀번호를 입력해 주세요 (4~20자)" />
      </label>

      <label class="field">
        <span class="label">비밀번호 확인</span>
        <input v-model="form.passwordCheck" type="password" maxlength="20" placeholder="비밀번호를 다시 입력해 주세요" />
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
    <CustomAlertModal
      :show="alertState.show"
      :message="alertState.message"
      @close="hideAlert"
    />
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
.id-row {
  display: flex;
  gap: 8px;
}
.id-row input {
  flex: 1;
}
.check-btn {
  flex-shrink: 0;
  padding: 0 14px;
  border: 1px solid #ffbc00;
  border-radius: 10px;
  background: #fff;
  color: #b8860b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.check-btn:disabled {
  border-color: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}
.id-check-msg {
  font-size: 12px;
}
.id-check-msg.available {
  color: #1f7a4d;
}
.id-check-msg.unavailable {
  color: #ef4444;
}
.id-check-msg.checking {
  color: #9ca3af;
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
