<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import savingsApi from '@/api/savingsApi';
import { useAlert } from '@/util/useAlert';

import CustomAlertModal from '@/components/common/CustomAlertModal.vue'; // 2. 공용 모달 임포트

const route = useRoute();
const router = useRouter();

const { alertState, showAlert, hideAlert } = useAlert();

const subscriptionId = ref(Number(route.params.subscriptionId) || null);

// 단계 관리: 1 = 해지 명세서, 2 = 해지 완료
const step = ref(1);

const loading = ref(true);
const cancelling = ref(false);
const previewData = ref(null);

const isMaturityCancel = computed(() => {
  if (!previewData.value) return false;

  const type = previewData.value.cancelType || '';
  const label = previewData.value.rateLabel || '';

  // cancelType이 '중도해지'가 아니거나, 텍스트에 '만기'가 들어가면 표 노출
  return type.includes('만기') || label.includes('만기') || type !== '중도해지';
});

//  해지 명세서 데이터 조회
const fetchCancelPreview = async () => {
  // subscriptionId가 유효하지 않으면 이전 페이지로 복귀
  if (!subscriptionId.value) {
    showAlert('올바르지 않은 접근입니다.');
    router.back();
    return;
  }

  try {
    loading.value = true;
    const res = await savingsApi.getCancelPreview(subscriptionId.value);
    previewData.value = res;
  } catch (error) {
    console.error('해지 명세서 조회 실패:', error);
    showAlert(
      error.response?.data?.message ||
        '해지 정보를 불러오는데 실패했거나 이미 해지된 계좌입니다.',
    );
    router.back();
  } finally {
    loading.value = false;
  }
};

// 해지 실행
const handleCancel = async () => {
  if (!subscriptionId.value) return;

  try {
    cancelling.value = true;
    await savingsApi.cancelSubscription(subscriptionId.value);
    step.value = 2; // 해지 완료 화면(STEP 2)으로 전환
  } catch (error) {
    console.error('해지 처리 실패:', error);
    showAlert(
      error.response?.data?.message ||
        error.response?.data ||
        '해지 처리 중 오류가 발생했습니다.',
    );
  } finally {
    cancelling.value = false;
  }
};

// 메인 페이지로 이동
const goToHome = () => {
  router.push('/home');
};

onMounted(() => {
  fetchCancelPreview();
});
</script>

<template>
  <div
    class="container py-3 pb-5 position-relative"
    style="max-width: 420px; background-color: #fff; min-height: 100vh; padding-top: 76px !important"
  >
    <!-- 로딩 상태 -->
    <div v-if="loading" class="text-center py-5 text-secondary micro-text">
      해지 명세서를 계산하는 중입니다...
    </div>

    <!-- ========================================== -->
    <!-- STEP 1: 해지 명세서 화면 -->
    <!-- ========================================== -->
    <div v-else-if="step === 1" class="d-flex flex-column gap-3 mb-5">
      <!-- 헤더 -->
      <div class="header">
        <button class="back-btn" @click="router.back()">
          <i class="fa-solid fa-chevron-left"></i>
        </button>
        <h1 class="header-title">해지예상조회</h1>
      </div>

      <p class="text-secondary micro-text mb-1">해지 시 아래와 같이 지급돼요</p>

      <!-- 카드 박스 (지급 내역 요약) -->
      <div class="card border border-light-subtle rounded-4 p-3 shadow-sm">
        <div class="d-flex flex-column gap-2">
          <!-- 납입 원금 -->
          <div class="d-flex justify-content-between align-items-center">
            <span class="text-secondary detail-text">납입 원금</span>
            <span class="fw-bold detail-text text-dark">
              {{ previewData?.totalPrincipal?.toLocaleString() }}원
            </span>
          </div>

          <!-- 세전 이자 -->
          <div class="d-flex justify-content-between align-items-start">
            <div class="d-flex flex-column">
              <span class="text-secondary detail-text">
                {{
                  previewData?.rateLabel ||
                  (isMaturityCancel ? '약정 이율' : '중도해지 이율')
                }}
                <span v-if="previewData?.appliedCancelRate !== undefined">
                  (연 {{ previewData?.appliedCancelRate }}%)
                </span>
                세전이자
              </span>

              <!-- 만기 후 이율 적용 시 약정이율 기반 산출식 안내 문구 추가 -->
              <span
                v-if="
                  previewData?.cancelType?.includes('만기후') ||
                  previewData?.rateLabel?.includes('만기 후')
                "
                class="text-primary micro-text mt-0-5"
                style="font-size: 11px"
              >
                * 약정이율({{
                  previewData?.baseRate || previewData?.appliedRate || '2.50'
                }}%) × 만기후 경과기간율
              </span>
            </div>

            <span class="fw-bold detail-text text-dark ms-2 text-nowrap">
              {{ previewData?.preTaxInterest?.toLocaleString() }}원
            </span>
          </div>

          <!-- 이자소득세 -->
          <div class="d-flex justify-content-between align-items-center">
            <span class="text-secondary detail-text">이자소득세 (15.4%)</span>
            <span class="fw-bold detail-text text-dark">
              -{{ previewData?.taxAmount?.toLocaleString() }}원
            </span>
          </div>

          <hr class="my-1 border-dashed" />

          <!-- 실 수령액 -->
          <div class="d-flex justify-content-between align-items-center pt-1">
            <span class="fw-bold detail-text text-dark">실 수령액</span>
            <span
              class="fw-bold fs-5 text-warning"
              style="color: #f3ac12 !important"
            >
              {{ previewData?.actualReceiveAmount?.toLocaleString() }}원
            </span>
          </div>
        </div>
      </div>

      <!-- 만기해지 시 표출되는 만기 후 방치 시 적용 이율 표 -->
      <div v-if="isMaturityCancel" class="mt-2">
        <h6 class="fw-bold text-secondary mb-2 table-title-text">
          만기 후 방치 시 적용 이율
        </h6>
        <div class="table-responsive rounded-3 border">
          <table
            class="table table-bordered text-center align-middle mb-0 cancel-table"
          >
            <thead class="table-light">
              <tr>
                <th class="py-1-5 text-secondary fw-semibold">경과 기간</th>
                <th class="py-1-5 text-secondary fw-semibold">적용 이율</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="py-1-5">만기 후 1개월 이내</td>
                <td class="py-1-5 fw-bold">기본이율 × 50%</td>
              </tr>
              <tr>
                <td class="py-1-5">1개월 초과 ~ 3개월 이내</td>
                <td class="py-1-5 fw-bold">기본이율 × 30%</td>
              </tr>
              <tr>
                <td class="py-1-5">3개월 초과</td>
                <td class="py-1-5 fw-bold">0.1%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 하단 고정 해지 버튼 영역 -->
      <div class="fixed-bottom-wrapper">
        <button
          class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark"
          style="background-color: #ffcc00; border: none"
          :disabled="cancelling"
          @click="handleCancel"
        >
          <span
            v-if="cancelling"
            class="spinner-border spinner-border-sm me-2"
          ></span>
          {{ cancelling ? '해지 처리 중...' : '해지' }}
        </button>
      </div>
    </div>

    <!-- ========================================== -->
    <!-- STEP 2: 해지 완료 화면 -->
    <!-- ========================================== -->
    <div v-else-if="step === 2" class="text-center py-5 mb-5">
      <!-- 완료 아이콘 -->
      <div class="mb-3 d-inline-block">
        <div
          class="bg-warning bg-opacity-20 rounded-circle p-4 d-flex align-items-center justify-content-center"
          style="width: 90px; height: 90px; margin: 0 auto"
        >
          <i class="fa-solid fa-check text-warning display-4"></i>
        </div>
      </div>

      <h4 class="fw-bold text-dark mb-2">적금 해지가 완료되었습니다.</h4>
      <p class="text-secondary small mb-4">
        해지 금액은 연결된 계좌로 안전하게 입금되었습니다.
      </p>

      <!-- 최종 입금 내역 카드 -->
      <div class="card border-0 bg-light rounded-4 p-4 text-start mb-4">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <span class="text-secondary micro-text">해지 유형</span>
          <span class="fw-bold text-dark micro-text">{{
            previewData?.cancelType
          }}</span>
        </div>
        <div class="d-flex justify-content-between align-items-center">
          <span class="text-secondary micro-text">최종 입금액</span>
          <span
            class="fw-bold text-warning fs-5"
            style="color: #f3ac12 !important"
          >
            {{ previewData?.actualReceiveAmount?.toLocaleString() }}원
          </span>
        </div>
      </div>

      <!-- 하단 고정 확인 버튼 (메인으로 이동) -->
      <div class="fixed-bottom-wrapper">
        <button
          class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark"
          style="background-color: #ffcc00; border: none"
          @click="goToHome"
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
  font-size: 13.5px;
}

.table-title-text {
  font-size: 12.5px;
}

/* 표 내부 글자 크기 및 여백 감축 */
.cancel-table {
  font-size: 12px;
}

.py-1-5 {
  padding-top: 6px !important;
  padding-bottom: 6px !important;
}

.border-dashed {
  border-top: 1px dashed #ccc;
}

.fixed-bottom-wrapper {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  padding: 16px 20px;
  background-color: #ffffff;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.05);
  z-index: 100;
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
  background: #fff;
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