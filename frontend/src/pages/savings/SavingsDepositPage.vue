<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import savingsApi from '@/api/savingsApi';
import { useAlert } from '@/util/useAlert';

import CustomAlertModal from '@/components/common/CustomAlertModal.vue'; // 2. 공용 모달 임포트

const { alertState, showAlert, hideAlert } = useAlert();

const route = useRoute();
const router = useRouter();

const subscriptionId = ref(Number(route.params.subscriptionId) || null);
const currentStep = ref(1);

const loading = ref(true);
const isSubmitting = ref(false);

const statusData = ref(null);
const resultData = ref(null);

const depositAmount = ref(10000);
const amountError = ref('');

// 세자리 콤마 포맷팅
const formattedDepositAmount = computed(() => {
  return depositAmount.value ? depositAmount.value.toLocaleString() : '';
});

// 최대 납입 한도
const remainingLimit = computed(() => {
  if (
    statusData.value?.remainingMonthlyLimit !== undefined &&
    statusData.value?.remainingMonthlyLimit !== null
  ) {
    return statusData.value.remainingMonthlyLimit;
  }
  // 기본 백업 한도 (응답받기 전 또는 미설정 시)
  return 1000000;
});

// 금액 검증 공통 함수
const validateAmount = (amount) => {
  // remainingLimit.value (현재 남아있는 한도: 80만원)를 직접 비교
  if (amount > remainingLimit.value) {
    return `이번 달 추가 납입 가능 한도는 ${remainingLimit.value.toLocaleString()}원입니다.`;
  } else if (amount > 0 && amount < 1000) {
    return '최소 납입 금액은 1,000원 이상입니다.';
  }
  return '';
};

// 금액 직접 입력 처리
const handleAmountInput = (e) => {
  const val = e.target.value.replace(/[^0-9]/g, '');
  const num = val ? parseInt(val, 10) : 0;

  depositAmount.value = num;
  amountError.value = validateAmount(num);
};

// 빠른 금액 칩 클릭 처리 (+1만, +3만 등)
const addAmount = (val) => {
  const nextVal = (depositAmount.value || 0) + val;

  depositAmount.value = nextVal;
  // 100만 원이 넘어갈 때 바로 에러 문구 세팅 -> input 보더 및 텍스트가 빨갛게 변함
  amountError.value = validateAmount(nextVal);
};

// 금액 초기화
const resetAmount = () => {
  depositAmount.value = 0;
  amountError.value = '';
};

// 적금 정보 조회
const fetchStatus = async () => {
  if (!subscriptionId.value) return;
  try {
    loading.value = true;
    const res = await savingsApi.getSavingsStatus(subscriptionId.value);
    statusData.value = res;
  } catch (error) {
    console.error('적금 정보 조회 실패:', error);
    showAlert('적금 정보를 불러오는데 실패했습니다.');
    router.back();
  } finally {
    loading.value = false;
  }
};

// 최종 추가 납입 요청
const handleFinalDeposit = async () => {
  try {
    isSubmitting.value = true;
    const res = await savingsApi.depositSavings(
      subscriptionId.value,
      depositAmount.value,
    );
    resultData.value = res;
    currentStep.value = 3; // 완료 단계로 이동
  } catch (error) {
    console.error('추가 납입 처리 실패:', error);
    showAlert(
      error.response?.data?.message || '추가 납입 처리 중 오류가 발생했습니다.',
    );
  } finally {
    isSubmitting.value = false;
  }
};

const goToStatus = () => {
  const targetId = resultData.value?.subscriptionId || subscriptionId.value;

  if (targetId) {
    router.push(`/savings/status/${targetId}`);
  } else {
    //alert('적금 정보(ID)를 찾을 수 없어 홈으로 이동합니다.');
    router.push('/home');
  }
};

const goToMain = () => {
  router.push('/home');
};

onMounted(() => {
  fetchStatus();
});
</script>

<template>
  <div
    class="container py-3 pb-5 position-relative"
    style="max-width: 480px; background-color: #fff; min-height: 100vh"
  >
    <!-- 로딩 화면 -->
    <div v-if="loading" class="text-center py-5 text-secondary micro-text">
      적금 정보를 불러오는 중입니다...
    </div>

    <!-- ========================================== -->
    <!-- STEP 1: 추가 납입 금액 입력 화면 -->
    <!-- ========================================== -->
    <div v-else-if="currentStep === 1" class="d-flex flex-column gap-3 mb-5">
      <!-- 헤더 -->
      <div class="header">
        <button class="back-btn" @click="router.back()">
          <i class="fa-solid fa-chevron-left"></i>
        </button>
        <h1 class="header-title">추가 납입</h1>
      </div>

      <!-- 적금 계좌 요약 카드 -->
      <div class="card border-0 bg-light rounded-4 p-3 shadow-sm">
        <span
          class="badge bg-warning text-dark px-2 py-1 rounded-2 fw-bold mb-2 align-self-start"
          style="background-color: #ffcc00 !important"
        >
          자유적립식
        </span>
        <h5 class="fw-bold text-dark mb-1">{{ statusData?.productName }}</h5>

        <div
          class="d-flex justify-content-between align-items-center pt-2 border-top"
        >
          <span class="text-secondary micro-text">현재 납입 원금</span>
          <span class="fw-bold text-dark detail-text"
            >{{ statusData?.totalPrincipal?.toLocaleString() }}원</span
          >
        </div>
      </div>

      <!-- 입력 폼 영역 -->
      <div class="d-flex flex-column gap-3 mt-2">
        <!-- 납입 금액 입력 -->

        <div>
          <!-- 상단 잔여 한도 안내 문구 -->
          <div class="d-flex justify-content-between align-items-center mb-1">
            <label class="form-label micro-text text-secondary mb-0"></label>
            <span class="micro-text text-warning fw-bold">
              이달 잔여 한도: {{ remainingLimit.toLocaleString() }}원
            </span>
          </div>
          <div class="d-flex justify-content-between align-items-center mb-1">
            <label class="form-label micro-text text-secondary mb-0"
              >납입 금액 (원)</label
            >
            <span class="micro-text text-secondary">최소 1,000원 이상</span>
          </div>

          <div class="input-group">
            <input
              :value="formattedDepositAmount"
              type="text"
              inputmode="numeric"
              class="form-control form-control-lg fw-bold text-end pe-3"
              :class="{ 'border-danger text-danger': amountError }"
              placeholder="0"
              @keydown="preventNonNumeric"
              @input="handleAmountInput"
            />
            <span class="input-group-text bg-light fw-bold">원</span>
          </div>

          <!-- 납입 한도 에러 메시지
          <div
            v-if="amountError"
            class="text-danger micro-text mt-1 text-end fw-bold"
          >
            {{ amountError }}
          </div> -->
          <!-- 금액 에러 메시지 -->
          <div
            v-if="amountError"
            class="text-danger micro-text mt-1 text-end fw-bold"
          >
            {{ amountError }}
          </div>

          <!-- 빠른 금액 추가 버튼 칩 -->
          <div class="d-flex gap-1 mt-2">
            <button
              v-for="addVal in [10000, 30000, 50000, 100000]"
              :key="addVal"
              type="button"
              class="btn btn-sm btn-outline-secondary flex-fill rounded-3 micro-text py-1-5"
              @click="addAmount(addVal)"
            >
              +{{ addVal / 10000 }}만
            </button>
            <button
              type="button"
              class="btn btn-sm btn-light border text-secondary rounded-3 micro-text px-2"
              @click="depositAmount = 0"
            >
              초기화
            </button>
          </div>
        </div>
      </div>

      <!-- 하단 고정 다음 버튼 -->
      <div class="fixed-bottom-wrapper">
        <button
          class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark"
          style="background-color: #ffcc00; border: none"
          :disabled="!depositAmount || depositAmount < 1000 || !!amountError"
          @click="currentStep = 2"
        >
          다음 (납입 정보 확인)
        </button>
      </div>
    </div>

    <!-- ========================================== -->
    <!-- STEP 2: 납입 정보 확인 화면 -->
    <!-- ========================================== -->
    <div v-else-if="currentStep === 2" class="d-flex flex-column gap-3 mb-5">
      <!-- 헤더 -->
      <div class="d-flex justify-content-between align-items-center mb-1">
        <i
          class="fa-solid fa-chevron-left fs-5"
          style="cursor: pointer"
          @click="currentStep = 1"
        ></i>
        <span class="fw-bold fs-5">추가 납입 확인</span>
        <span
          class="text-secondary small"
          style="cursor: pointer"
          @click="router.back()"
          >취소</span
        >
      </div>

      <h5 class="fw-bold my-1">납입 정보를 확인해 주세요</h5>

      <!-- 상세 내역 카드 -->
      <div
        class="card border-0 bg-light rounded-4 p-3 d-flex flex-column gap-3"
      >
        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">적금 상품명</span>
          <span class="fw-bold text-dark micro-text">{{
            statusData?.productName
          }}</span>
        </div>

        <div
          class="d-flex justify-content-between align-items-center border-bottom pb-2"
        >
          <span class="text-secondary micro-text">추가 납입 금액</span>
          <span
            class="fw-bold text-warning micro-text fs-6"
            style="color: #f3ac12 !important"
          >
            {{ depositAmount.toLocaleString() }}원
          </span>
        </div>

        <div class="d-flex justify-content-between align-items-center">
          <span class="text-secondary micro-text">납입 후 예상 원금</span>
          <span class="fw-bold text-dark micro-text">
            {{
              (
                (statusData?.totalPrincipal || 0) + depositAmount
              ).toLocaleString()
            }}원
          </span>
        </div>
      </div>

      <!-- 하단 안내 문구 -->
      <div class="p-3 bg-light rounded-3 micro-text text-secondary lh-base">
        <p class="mb-0">
          * 입력하신 추가 납입 금액은 연결된 출금 계좌에서 즉시 출금되어 해당
          적금 계좌로 이체됩니다.
        </p>
      </div>

      <!-- 하단 고정 최종 납입 버튼 -->
      <div class="fixed-bottom-wrapper">
        <button
          class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark"
          style="background-color: #ffcc00; border: none"
          :disabled="isSubmitting"
          @click="handleFinalDeposit"
        >
          <span
            v-if="isSubmitting"
            class="spinner-border spinner-border-sm me-2"
          ></span>
          {{ isSubmitting ? '납입 처리 중...' : '납입하기' }}
        </button>
      </div>
    </div>

    <!-- ========================================== -->
    <!-- STEP 3: 납입 완료 화면 -->
    <!-- ========================================== -->
    <div v-else-if="currentStep === 3" class="text-center py-5 mb-5">
      <!-- 완료 아이콘 -->
      <div class="mb-3 d-inline-block">
        <div
          class="bg-warning bg-opacity-20 rounded-circle p-4 d-flex align-items-center justify-content-center"
          style="width: 90px; height: 90px; margin: 0 auto"
        >
          <i class="fa-solid fa-check text-warning display-4"></i>
        </div>
      </div>

      <h4 class="fw-bold text-dark mb-2">추가 납입이 완료되었습니다!</h4>
      <p class="text-secondary small mb-4">
        성공적으로 적금 계좌에 금액이 입금되었습니다.
      </p>

      <!-- 최종 결과 카드 -->
      <div
        class="card border-0 bg-light rounded-4 p-4 text-start mb-4 d-flex flex-column gap-2"
      >
        <div class="d-flex justify-content-between align-items-center">
          <span class="text-secondary micro-text">납입 회차</span>
          <span class="fw-bold text-dark micro-text"
            >{{ resultData?.paymentTurn }}회차</span
          >
        </div>
        <div class="d-flex justify-content-between align-items-center">
          <span class="text-secondary micro-text">금회 납입액</span>
          <span
            class="fw-bold text-warning micro-text fs-6"
            style="color: #f3ac12 !important"
          >
            {{ resultData?.depositAmount?.toLocaleString() }}원
          </span>
        </div>
        <hr class="my-1 border-dashed" />
        <div class="d-flex justify-content-between align-items-center pt-1">
          <span class="text-secondary micro-text">총 납입 원금</span>
          <span class="fw-bold text-dark detail-text">
            {{ resultData?.totalPrincipal?.toLocaleString() }}원
          </span>
        </div>
      </div>

      <!-- 하단 고정 확인 버튼 (현황으로 돌아가기) -->
      <div class="fixed-bottom-wrapper">
        <button
          class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark"
          style="background-color: #ffcc00; border: none"
          @click="goToStatus"
        >
          확인
        </button>
      </div>
    </div>
    <CustomAlertModal
      :show="alertState.show"
      :message="alertState.message"
      @close="hideAlert"
    />
  </div>
</template>

<style scoped>
.micro-text {
  font-size: 13px;
}

.detail-text {
  font-size: 14px;
}

.py-1-5 {
  padding-top: 6px;
  padding-bottom: 6px;
}

.border-dashed {
  border-style: dashed !important;
}

.fixed-bottom-wrapper {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  padding: 16px 20px;
  background-color: #ffffff;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.05);
  z-index: 100;
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
  cursor: pointer;
}
.header-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
}
</style>
