<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import userApi from '@/api/userApi';
import { useAlert } from '@/util/useAlert';
import CustomAlertModal from '@/components/common/CustomAlertModal.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const loading = ref(false);
const loadingText = ref('계좌 정보를 불러오는 중이에요');

// "계좌 연결" / "카드 연결" 클릭
// 실제로는 mydata_connected 플래그만 true로 바꾸고, 로딩은 연출용 타이머
const connect = async () => {
  if (loading.value) return;

  loading.value = true;
  try {
    await userApi.connectMydata();
    auth.setMydataConnected(); // 재로그인 없이 store 상태만 갱신

    // 연출: 단계별 문구를 보여주다가 카테고리 선택 화면으로 이동
    setTimeout(() => {
      loadingText.value = '소비 내역을 분석하는 중이에요';
    }, 1200);

    setTimeout(() => {
      loading.value = false;
      router.push('/goal/analysis');
    }, 2400);
  } catch (e) {
    loading.value = false;
    showAlert('연결에 실패했어요. 잠시 후 다시 시도해 주세요.');
  }
};

const skip = () => router.push('/home'); // 연결 없이 넘어가면 메인으로
const { alertState, showAlert, hideAlert } = useAlert();

</script>

<template>
  <div class="connect-page">
    <button class="back-btn" @click="router.back()">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <h1 class="title">
      소비 분석을 위한<br />
      데이터를 연결해 주세요
    </h1>
    <p class="subtitle">더 정확한 분석을 제공할 수 있어요</p>

    <button class="connect-card" @click="connect">
      <div class="card-icon">
        <i class="fa-solid fa-building-columns"></i>
      </div>
      <div class="card-text">
        <strong>계좌 연결</strong>
        <span>은행 계좌를 연결하여 지출 내역을 분석해요</span>
      </div>
      <i class="fa-solid fa-chevron-right arrow"></i>
    </button>

    <div class="divider"><span>또는</span></div>

    <button class="connect-card" @click="connect">
      <div class="card-icon">
        <i class="fa-solid fa-credit-card"></i>
      </div>
      <div class="card-text">
        <strong>카드 연결</strong>
        <span>카드 사용 내역을 연결하여 소비를 분석해요</span>
      </div>
      <i class="fa-solid fa-chevron-right arrow"></i>
    </button>

    <button class="skip-btn" @click="skip">나중에 하기</button>

    <!-- 로딩 연출 오버레이 -->
    <div v-if="loading" class="overlay">
      <div class="spinner"></div>
      <p class="loading-text">{{ loadingText }}</p>
      <p class="loading-sub">잠시만 기다려 주세요</p>
    </div>
  </div>
    <CustomAlertModal
      :show="alertState.show"
      :message="alertState.message"
      @close="hideAlert"
    />
</template>

<style scoped>
.connect-page {
  padding: 76px 20px 20px;
  min-height: 100vh;
  background: #fff;
}
.back-btn {
  background: #fff;
  border: none;
  font-size: 20px;
  padding: 20px 20px 12px;
  color: #111;
  /* 스크롤해도 화면에 그대로 남아있게 - 다른 화면들과 동일한 방식 */
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  z-index: 100;
  text-align: left;
}
.title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
  margin-top: 24px;
}
.subtitle {
  color: #6b7280;
  font-size: 14px;
  margin-top: 8px;
  margin-bottom: 32px;
}
.connect-card {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s;
}
.connect-card:hover {
  border-color: #ffbc00;
}
.card-icon {
  font-size: 24px;
  color: #4b5563;
  width: 32px;
  text-align: center;
}
.card-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.card-text strong {
  font-size: 15px;
  font-weight: 600;
}
.card-text span {
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.4;
}
.arrow {
  color: #d1d5db;
  font-size: 14px;
}
.divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
  color: #d1d5db;
  font-size: 13px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #f3f4f6;
}
.divider span {
  padding: 0 12px;
}
.skip-btn {
  display: block;
  margin: 40px auto 0;
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 14px;
  cursor: pointer;
}
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.96);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 100;
}
.spinner {
  width: 44px;
  height: 44px;
  border: 4px solid #f3f4f6;
  border-top-color: #ffbc00;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
  margin-bottom: 8px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.loading-text {
  font-size: 16px;
  font-weight: 600;
}
.loading-sub {
  font-size: 13px;
  color: #9ca3af;
}
</style>
