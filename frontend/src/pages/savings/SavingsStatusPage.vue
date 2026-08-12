<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import savingsApi from '@/api/savingsApi';
import { useAlert } from '@/util/useAlert';

const route = useRoute();
const router = useRouter();

const subscriptionId = route.params.subscriptionId;

const statusData = ref(null);
const loading = ref(true);
import { useAuthStore } from '@/stores/auth';

import CustomAlertModal from '@/components/common/CustomAlertModal.vue'; // 2. 공용 모달 임포트

const { alertState, showAlert, hideAlert } = useAlert();

const authStore = useAuthStore();

const fetchSavingsStatus = async () => {
  try {
    loading.value = true;
    const data = await savingsApi.getSavingsStatus(subscriptionId);
    statusData.value = data;
    console.log('적금 현황 데이터:', data);
  } catch (error) {
    console.error('적금 현황 조회 실패:', error);
    showAlert('적금 현황을 불러오지 못했습니다.');
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  // 새로고침 시 토큰이 세션/로컬스토리지에서 복구될 때까지 미세 대기
  if (!authStore.token) {
    await new Promise((resolve) => setTimeout(resolve, 100));
  }

  if (subscriptionId) {
    fetchSavingsStatus();
  }
});

const goBack = () => router.push('/home');

const formatMoney = (val) => {
  if (val === undefined || val === null) return '0원';
  return val.toLocaleString('ko-KR') + '원';
};

// 월별 납입 내역 중 최대금액 (차트 높이 비율 계산용)
const getMaxPaymentAmount = () => {
  if (!statusData.value?.monthlyPayments?.length) return 100000;
  const max = Math.max(
    ...statusData.value.monthlyPayments.map((p) => p.amount),
  );
  return max > 0 ? max : 100000;
};

// 상품 상세 / 해지 페이지 이동

const goToDetail = () => {
  const pId = statusData.value?.productId || statusData.value?.savingsProductId;

  if (pId) {
    // from=status 쿼리 파라미터 추가
    router.push({
      path: `/savings/products/${pId}`,
      query: { from: 'status' },
    });
  } else {
    showAlert('상품 상세 정보를 불러올 수 없습니다.');
  }
};

const goToCancel = () => {
  const sid =
    statusData.value?.subscriptionId ||
    statusData.value?.id ||
    route.params.subscriptionId;

  if (sid) {
    router.push(`/savings/${sid}/cancel`);
  } else {
    showAlert('적금 가입 정보를 찾을 수 없습니다.');
  }
};
</script>

<template>
  <div
    class="container py-3"
    style="max-width: 480px; background-color: #f8f9fa; min-height: 100vh"
  >
    <!-- 헤더 -->
    <div class="header">
      <button class="back-btn" @click="goBack">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <h1 class="header-title">적금 현황</h1>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="text-center my-5 text-secondary">
      적금 현황을 불러오는 중입니다...
    </div>

    <!-- 메인 컨텐츠 -->
    <div v-else-if="statusData" class="d-flex flex-column gap-3">
      <!-- 1. 상품 기본 정보 카드 -->
      <div class="card border-0 shadow-sm rounded-4 p-3 bg-white">
        <div class="d-flex justify-content-between align-items-start mb-2">
          <span class="badge bg-warning text-dark px-2 py-1 fw-normal"
            >가입 중</span
          >
        </div>

        <h5 class="fw-bold mb-1">{{ statusData.productName }}</h5>
        <div class="text-secondary small mb-3">
          {{ statusData.saveType }} ·
          {{ statusData.companyName || 'KB국민은행' }}
        </div>

        <div class="d-flex gap-2">
          <div class="bg-light p-2 rounded-3 flex-fill text-center">
            <div class="text-secondary micro-text">금리</div>
            <div class="fw-bold text-warning">
              {{ statusData.appliedRate }}%
            </div>
          </div>
          <div class="bg-light p-2 rounded-3 flex-fill text-center">
            <div class="text-secondary micro-text">적립 기간</div>
            <div class="fw-bold">{{ statusData.saveTerm }}개월</div>
          </div>
        </div>
      </div>

      <!-- 2. 주요 금액 & 수치 카드 (줄바꿈 완전히 제거 및 폰트 수치 최적화) -->
      <div class="card border-0 shadow-sm rounded-4 p-3 bg-white">
        <!-- 상단 3개 지표 -->
        <div
          class="row text-center mb-3 pb-3 border-bottom g-0 align-items-center"
        >
          <div class="col-4 px-1">
            <div class="text-secondary micro-text mb-1 text-nowrap">가입일</div>
            <div
              class="fw-bold text-nowrap"
              style="font-size: 12px; letter-spacing: -0.5px"
            >
              {{ statusData.startDate }}
            </div>
          </div>
          <div class="col-4 px-1 border-start border-end">
            <div class="text-secondary micro-text mb-1 text-nowrap">
              월 납입액
            </div>
            <div class="fw-bold text-nowrap" style="font-size: 13px">
              {{ formatMoney(statusData.monthlyAmount) }}
            </div>
          </div>
          <div class="col-4 px-1">
            <div class="text-secondary micro-text mb-1 text-nowrap">
              만기 예상 수령액
            </div>
            <div
              class="fw-bold text-warning text-nowrap"
              style="font-size: 13px"
            >
              {{ formatMoney(statusData.expectedAmount) }}
            </div>
          </div>
        </div>

        <!-- 하단 3개 지표 -->
        <div class="row text-center align-items-center g-0">
          <div class="col-4 px-1">
            <div class="text-secondary micro-text mb-1 text-nowrap">
              총 납입 원금
            </div>
            <div class="fw-bold text-nowrap" style="font-size: 13px">
              {{ formatMoney(statusData.totalPrincipal) }}
            </div>
          </div>
          <div class="col-4 px-1 border-start border-end">
            <div class="text-secondary micro-text mb-1 text-nowrap">
              목표 달성률
            </div>
            <div class="fw-bold text-nowrap" style="font-size: 14px">
              {{ statusData.achievementRate }}%
            </div>
            <div class="progress mt-1 mx-auto" style="height: 4px; width: 75%">
              <div
                class="progress-bar bg-warning"
                :style="{
                  width: Math.min(statusData.achievementRate, 100) + '%',
                }"
              ></div>
            </div>
          </div>
          <div class="col-4 px-1">
            <div class="text-secondary micro-text mb-1 text-nowrap">
              다음 납입 예정일
            </div>
            <div
              class="fw-bold text-nowrap"
              style="font-size: 12px; letter-spacing: -0.5px"
            >
              {{ statusData.nextPaymentDate || '-' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 3. 월별 적립 현황 막대 차트 (가이드선 점선 스타일 선명화) -->
      <div class="card border-0 shadow-sm rounded-4 p-3 bg-white">
        <h6 class="fw-bold mb-1">월별 적립 현황</h6>
        <div class="text-secondary micro-text mb-2">(원)</div>

        <div
          v-if="statusData.monthlyPayments?.length"
          class="position-relative pt-3"
          style="height: 175px"
        >
          <!-- Y축 눈금 + 선명한 점선 가이드라인 레이어 -->
          <div
            class="position-absolute w-100 d-flex flex-column justify-content-between"
            style="
              height: 120px;
              top: 25px;
              left: 0;
              pointer-events: none;
              z-index: 0;
            "
          >
            <div class="d-flex align-items-center w-100">
              <span
                class="text-secondary text-end pe-2"
                style="width: 48px; font-size: 9px; line-height: 1"
                >100,000</span
              >
              <div
                class="flex-fill"
                style="border-bottom: 1px dashed #dee2e6"
              ></div>
            </div>
            <div class="d-flex align-items-center w-100">
              <span
                class="text-secondary text-end pe-2"
                style="width: 48px; font-size: 9px; line-height: 1"
                >80,000</span
              >
              <div
                class="flex-fill"
                style="border-bottom: 1px dashed #dee2e6"
              ></div>
            </div>
            <div class="d-flex align-items-center w-100">
              <span
                class="text-secondary text-end pe-2"
                style="width: 48px; font-size: 9px; line-height: 1"
                >60,000</span
              >
              <div
                class="flex-fill"
                style="border-bottom: 1px dashed #dee2e6"
              ></div>
            </div>
            <div class="d-flex align-items-center w-100">
              <span
                class="text-secondary text-end pe-2"
                style="width: 48px; font-size: 9px; line-height: 1"
                >40,000</span
              >
              <div
                class="flex-fill"
                style="border-bottom: 1px dashed #dee2e6"
              ></div>
            </div>
            <div class="d-flex align-items-center w-100">
              <span
                class="text-secondary text-end pe-2"
                style="width: 48px; font-size: 9px; line-height: 1"
                >20,000</span
              >
              <div
                class="flex-fill"
                style="border-bottom: 1px dashed #dee2e6"
              ></div>
            </div>
            <div class="d-flex align-items-center w-100">
              <span
                class="text-secondary text-end pe-2"
                style="width: 48px; font-size: 9px; line-height: 1"
                >0</span
              >
              <div
                class="flex-fill"
                style="border-bottom: 1px solid #ced4da"
              ></div>
            </div>
          </div>

          <!-- 실제 막대 그래픽 레이어 -->
          <div
            class="d-flex align-items-end justify-content-around h-100 position-relative"
            style="margin-left: 48px; z-index: 1"
          >
            <div
              v-for="(item, idx) in statusData.monthlyPayments"
              :key="idx"
              class="d-flex flex-column align-items-center flex-fill"
            >
              <!-- 막대 위 금액 텍스트 -->
              <span
                class="micro-text text-dark fw-bold mb-1 text-nowrap"
                style="font-size: 9px"
              >
                {{ item.amount.toLocaleString() }}
              </span>

              <!-- 막대 바 -->
              <div
                class="bg-warning rounded-top"
                style="width: 14px; transition: height 0.3s ease"
                :style="{
                  height: Math.min((item.amount / 100000) * 120, 120) + 'px',
                }"
              ></div>

              <!-- 월 표시 -->
              <span class="small text-secondary mt-2" style="font-size: 11px"
                >{{ item.payMonth }}월</span
              >
            </div>
          </div>
        </div>
        <div v-else class="text-center text-muted py-4 small">
          아직 납입된 내역이 없습니다.
        </div>

        <div class="text-muted mt-2" style="font-size: 10px">
          * 최근 12개월 기준으로 표시됩니다.
        </div>
      </div>

      <!-- 4. 하단 버튼 영역 -->
      <div class="row g-2 mt-1 mb-4">
        <div class="col-6">
          <button
            class="btn btn-light w-100 py-3 rounded-4 d-flex justify-content-between align-items-center bg-white border-0 shadow-sm"
            @click="goToDetail"
          >
            <span class="small fw-bold">상품 상세</span>
            <i class="fa-solid fa-chevron-right text-secondary small"></i>
          </button>
        </div>
        <div class="col-6">
          <button
            class="btn btn-light w-100 py-3 rounded-4 d-flex justify-content-between align-items-center bg-white border-0 shadow-sm"
            @click="goToCancel"
          >
            <span class="small fw-bold">상품 해지</span>
            <i class="fa-solid fa-chevron-right text-secondary small"></i>
          </button>
        </div>
        <div class="col-12">
          <button
            class="btn btn-warning w-100 py-3 fw-bold rounded-4 text-dark shadow-sm"
            style="background-color: #ffcc00; border: none"
            @click="router.push(`/savings/${subscriptionId}/deposit`)"
          >
            <i class="fa-solid fa-plus me-1"></i> 추가 납입하기
          </button>
        </div>
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
  font-size: 11px;
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
