<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import savingsApi from '@/api/savingsApi';
import reportApi from '@/api/reportApi';
import moment from 'moment';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// URL Query에서 step, monthlyAmount, saveTerm 복원
const step = ref(Number(route.query.step) || 1);
const saveAmount = ref(0); // 현재 납입 가능 금액 (이달 잔액)

// 🟢 디폴트 월 납입액을 0원으로 설정 (쿼리에 금액 값이 전달된 경우만 해당 값 사용)
const monthlyAmount = ref(
  route.query.monthlyAmount !== undefined
    ? Number(route.query.monthlyAmount)
    : 0,
);
const saveTerm = ref(Number(route.query.saveTerm) || 12);

const yearMonth = ref(moment().format('YYYY-MM'));
const userId = computed(() => authStore.userId);
const loadingSaveAmount = ref(true);

// URL 쿼리 동기화 함수
const updateQueryParams = (newStep) => {
  step.value = newStep;
  router.push({
    query: {
      ...route.query,
      step: newStep,
      monthlyAmount: monthlyAmount.value,
      saveTerm: saveTerm.value,
    },
  });
};

// 브라우저 백/포워드 시 쿼리 변경 감지
watch(
  () => route.query,
  (query) => {
    if (query.step) step.value = Number(query.step);
    if (query.monthlyAmount !== undefined)
      monthlyAmount.value = Number(query.monthlyAmount);
    if (query.saveTerm) saveTerm.value = Number(query.saveTerm);

    if (step.value === 3 && recommendedList.value.length === 0) {
      fetchRecommendations(false);
    }
  },
  { deep: true },
);

// 🟢 소비 리포트 API로부터 '이달 잔액' 조회
const fetchSaveAmount = async () => {
  try {
    loadingSaveAmount.value = true;

    const reportData = await reportApi.get({
      userId: userId.value,
      yearMonth: yearMonth.value,
    });

    if (reportData && reportData.cashFlow) {
      const currentBalance = reportData.cashFlow.currentBalance ?? 0;
      saveAmount.value = Math.max(currentBalance, 0);
    } else {
      saveAmount.value = 0;
    }

    // URL 쿼리에 지정된 monthlyAmount가 있다면 해당 값 적용
    if (route.query.monthlyAmount !== undefined) {
      monthlyAmount.value = Number(route.query.monthlyAmount);
    }
  } catch (error) {
    console.error('이달 잔액 조회 실패:', error);
    saveAmount.value = 0;
  } finally {
    loadingSaveAmount.value = false;
  }
};

// 🟢 콤마 포맷팅 (0원일 때도 '0'으로 리턴)
const formattedMonthlyAmount = computed({
  get() {
    if (monthlyAmount.value === null || monthlyAmount.value === undefined)
      return '0';
    return monthlyAmount.value.toLocaleString('ko-KR');
  },
  set(newValue) {
    const numericValue = String(newValue).replace(/[^0-9]/g, '');
    monthlyAmount.value = numericValue ? Number(numericValue) : 0;
  },
});

// 🟢 숫자만 입력 가능하도록 제어하는 핸들러
const handleInput = (event) => {
  const cleanValue = event.target.value.replace(/[^0-9]/g, '');
  monthlyAmount.value = cleanValue ? Number(cleanValue) : 0;
  event.target.value = formattedMonthlyAmount.value;
};

const termOptions = [6, 12, 24, 36];

// 메뉴에서 조건 없이 step=3으로 바로 넘어온 경우(예: 이미 적금 가입한 사용자가 "적금 추천"
// 클릭), 이번 달 실제 남은 돈을 쓰면 금액이 너무 커서 조건에 맞는 상품이 안 나올 수 있어
// 그냥 고정된 최소 기준액(만원)으로 항상 조회해서 결과를 보여준다.
const MIN_DEFAULT_AMOUNT = 10000;

onMounted(async () => {
  await fetchSaveAmount();
  if (step.value === 3) {
    if (route.query.monthlyAmount === undefined) {
      monthlyAmount.value = MIN_DEFAULT_AMOUNT;
    }
    fetchRecommendations(false);
  }
});

const recommendedList = ref([]);
const displayList = ref([]);
const activeFilter = ref('ALL');
const loading = ref(false);

const expandedProductId = ref(null);

const toggleExpand = (productId) => {
  if (expandedProductId.value === productId) {
    expandedProductId.value = null;
  } else {
    expandedProductId.value = productId;
  }
};

const goBack = () => {
  if (step.value === 3) {
    router.back();
  } else {
    router.push('/home');
  }
};

// 🟢 [잔액 전액] 버튼 클릭 시 saveAmount(잔액)를 그대로 할당
const addAmount = (val) => {
  if (val === 'ALL') {
    monthlyAmount.value = saveAmount.value;
  } else {
    monthlyAmount.value += val;
  }
};

const fetchRecommendations = async (shouldPushQuery = true) => {
  if (!monthlyAmount.value || monthlyAmount.value <= 0) {
    alert('월 납입액은 1원 이상 입력해 주세요.');
    step.value = 2;
    return;
  }

  try {
    loading.value = true;
    if (shouldPushQuery) {
      updateQueryParams(3);
    }
    activeFilter.value = 'ALL';

    const res = await savingsApi.getRecommendedSavings({
      monthlyAmount: monthlyAmount.value,
      saveTerm: saveTerm.value,
    });

    recommendedList.value = res || [];
    displayList.value = res || [];

    if (displayList.value.length > 0) {
      expandedProductId.value = displayList.value[0].productId;
    }
  } catch (error) {
    console.error('적금 추천 목록 조회 실패:', error);
    recommendedList.value = [];
    displayList.value = [];
  } finally {
    loading.value = false;
  }
};

const selectFilter = async (filterType) => {
  activeFilter.value = filterType;

  if (filterType === 'ALL') {
    displayList.value = recommendedList.value;
    if (displayList.value.length > 0) {
      expandedProductId.value = displayList.value[0].productId;
    }
  } else {
    try {
      loading.value = true;
      const typeParam = filterType === 'FREEDOM' ? '자유적금' : '정액적금';
      const res = await savingsApi.getAllSavingsProducts(typeParam);
      displayList.value = res || [];

      if (displayList.value.length > 0) {
        expandedProductId.value = displayList.value[0].productId;
      }
    } catch (error) {
      console.error('전체 적금 목록 조회 실패:', error);
      alert('적금 목록을 불러오는 중 오류가 발생했습니다.');
    } finally {
      loading.value = false;
    }
  }
};

const goToDetail = (productId) => {
  router.push(`/savings/products/${productId}`);
};
</script>

<template>
  <div
    class="container py-3"
    style="max-width: 480px; background-color: #fff; min-height: 100vh"
  >
    <!-- 상단 헤더 -->
    <div class="d-flex align-items-center mb-4">
      <i
        class="fa-solid fa-chevron-left fs-5 me-3"
        style="cursor: pointer"
        @click="goBack"
      ></i>
      <h5 class="fw-bold mb-0">
        {{
          step === 1 ? '내 적금' : step === 2 ? '얼마씩 넣을까요?' : '적금 추천'
        }}
      </h5>
    </div>

    <!-- STEP 1 -->
    <div
      v-if="step === 1"
      class="d-flex flex-column align-items-center justify-content-center pt-5"
    >
      <div
        class="bg-warning bg-opacity-10 rounded-circle p-4 mb-4 d-flex align-items-center justify-content-center"
        style="width: 120px; height: 120px"
      >
        <i class="fa-solid fa-user text-warning display-4"></i>
      </div>

      <h4 class="fw-bold text-center mb-2">아직 개설한 적금이 없어요</h4>
      <p class="text-secondary text-center small mb-5">
        세이브 금액에 맞는 KB 적금을<br />추천해 드릴게요
      </p>

      <button
        class="btn btn-warning w-100 py-3 fw-bold rounded-4 mb-4 text-dark"
        @click="updateQueryParams(2)"
      >
        적금 추천
      </button>

      <div
        class="bg-warning bg-opacity-10 p-3 rounded-4 w-100 d-flex align-items-center gap-2"
      >
        <span class="badge bg-warning rounded-circle p-1"> </span>
        <span v-if="loadingSaveAmount" class="small text-secondary">
          세이브 금액을 계산하는 중입니다...
        </span>
        <span v-else class="small text-dark fw-semibold">
          현재 남아있는 {{ saveAmount.toLocaleString() }}원으로 시작할 수 있는
          적금이 있어요
        </span>
      </div>
    </div>

    <!-- STEP 2 -->
    <div v-else-if="step === 2" class="d-flex flex-column gap-4">
      <div
        class="bg-warning bg-opacity-10 p-3 rounded-4 d-flex justify-content-between align-items-center"
      >
        <div>
          <div class="micro-text text-secondary mb-1">현재 납입 가능 금액</div>
          <div class="fw-bold text-warning h5 mb-0">
            {{ saveAmount.toLocaleString() }}원
          </div>
        </div>
      </div>

      <div>
        <label class="form-label text-secondary small">월 납입액</label>
        <div class="position-relative mb-3">
          <!-- type="text", inputmode="numeric", @input 및 방어 로직 추가 -->
          <input
            :value="formattedMonthlyAmount"
            type="text"
            inputmode="numeric"
            class="form-control form-control-lg border-0 border-bottom rounded-0 px-0 fw-bold fs-2 text-end pe-4"
            placeholder="0"
            @input="handleInput"
          />
          <span class="position-absolute end-0 bottom-0 fs-4 fw-bold pb-2"
            >원</span
          >
        </div>

        <div class="row g-2">
          <!-- 하단 금액 버튼 영역 동일 -->
        </div>

        <div class="row g-2">
          <div class="col-3">
            <button
              class="btn btn-light bg-warning bg-opacity-10 w-100 fw-bold text-nowrap d-flex align-items-center justify-content-center rounded-3"
              style="
                height: 44px;
                font-size: 13px;
                letter-spacing: -0.5px;
                padding: 0 2px;
              "
              @click="addAmount('ALL')"
            >
              잔액 전액
            </button>
          </div>
          <div class="col-3">
            <button
              class="btn btn-light w-100 fw-bold text-nowrap d-flex align-items-center justify-content-center rounded-3"
              style="height: 44px; font-size: 14px"
              @click="addAmount(10000)"
            >
              +1만
            </button>
          </div>
          <div class="col-3">
            <button
              class="btn btn-light w-100 fw-bold text-nowrap d-flex align-items-center justify-content-center rounded-3"
              style="height: 44px; font-size: 14px"
              @click="addAmount(50000)"
            >
              +5만
            </button>
          </div>
          <div class="col-3">
            <button
              class="btn btn-light w-100 fw-bold text-nowrap d-flex align-items-center justify-content-center rounded-3"
              style="height: 44px; font-size: 14px"
              @click="addAmount(100000)"
            >
              +10만
            </button>
          </div>
        </div>
      </div>

      <div>
        <label class="form-label text-secondary small mb-2">목표 기간</label>
        <div class="row g-2">
          <div v-for="term in termOptions" :key="term" class="col-3">
            <button
              class="btn w-100 py-3 rounded-3 fw-bold text-nowrap d-flex align-items-center justify-content-center"
              style="height: 52px; font-size: 14px"
              :class="
                saveTerm === term
                  ? 'btn-warning text-dark'
                  : 'btn-light text-secondary'
              "
              @click="saveTerm = term"
            >
              {{ term }}개월
            </button>
          </div>
        </div>
        <div class="text-secondary micro-text mt-2">
          {{ saveTerm }}개월 동안 총
          {{ (monthlyAmount * saveTerm).toLocaleString() }}원을 모으게 돼요
        </div>
      </div>

      <button
        class="btn btn-warning w-100 py-3 fw-bold rounded-4 mt-3 text-dark"
        @click="fetchRecommendations(true)"
      >
        이 조건에 맞는 적금 보기 →
      </button>
    </div>

    <!-- STEP 3 -->
    <div v-else-if="step === 3">
      <div
        class="bg-warning bg-opacity-10 p-3 rounded-4 d-flex justify-content-between align-items-center mb-3"
      >
        <div>
          <div class="small fw-semibold text-dark">
            월 {{ monthlyAmount.toLocaleString() }}원 · {{ saveTerm }}개월
          </div>
          <div class="micro-text text-secondary">
            {{
              activeFilter === 'ALL' ? '조건에 맞는 KB 적금' : '전체 KB 적금'
            }}
            {{ displayList.length }}개
          </div>
        </div>
        <button
          class="btn btn-sm btn-outline-dark rounded-pill micro-text px-3"
          @click="updateQueryParams(2)"
        >
          조건 수정
        </button>
      </div>

      <div class="d-flex gap-2 mb-3">
        <button
          class="btn btn-sm rounded-pill px-3"
          :class="
            activeFilter === 'ALL' ? 'btn-dark' : 'btn-light text-secondary'
          "
          @click="selectFilter('ALL')"
        >
          추천상품
        </button>
        <button
          class="btn btn-sm rounded-pill px-3"
          :class="
            activeFilter === 'FREEDOM' ? 'btn-dark' : 'btn-light text-secondary'
          "
          @click="selectFilter('FREEDOM')"
        >
          자유적립
        </button>
        <button
          class="btn btn-sm rounded-pill px-3"
          :class="
            activeFilter === 'FIXED' ? 'btn-dark' : 'btn-light text-secondary'
          "
          @click="selectFilter('FIXED')"
        >
          정액적립
        </button>
      </div>

      <div v-if="loading" class="text-center py-5 text-secondary">
        적금 상품을 불러오는 중입니다...
      </div>

      <!--  추천 상품이 0개일 때 (Empty State UI) -->
      <div
        v-else-if="displayList.length === 0"
        class="d-flex flex-column align-items-center justify-content-center py-5 my-3"
      >
        <div
          class="bg-light rounded-circle p-3 mb-3 d-flex align-items-center justify-content-center"
          style="width: 70px; height: 70px"
        >
          <i class="fa-solid fa-triangle-exclamation text-secondary fs-3"></i>
        </div>
        <h6 class="fw-bold text-dark mb-1">조건에 맞는 적금 상품이 없어요</h6>
        <p class="text-secondary micro-text text-center mb-4">
          월 납입액이나 목표 기간을 변경해서<br />다시 추천받아 보세요.
        </p>
        <button
          type="button"
          class="btn btn-warning fw-bold px-4 py-2 rounded-4 text-dark micro-text"
          @click="updateQueryParams(2)"
        >
          조건 다시 설정하기
        </button>
      </div>

      <!--  추천 상품이 1개 이상 있을 때 (아코디언 리스트) -->
      <div v-else class="d-flex flex-column gap-3">
        <div
          v-for="(product, idx) in displayList"
          :key="product.productId || idx"
          class="card border rounded-4 p-3 position-relative"
          :class="{
            'border-warning border-2': expandedProductId === product.productId,
          }"
          style="cursor: pointer; transition: all 0.2s ease"
          @click="toggleExpand(product.productId)"
        >
          <span
            v-if="activeFilter === 'ALL' && idx === 0"
            class="badge bg-warning text-dark position-absolute top-0 start-0 m-3 px-2 py-1"
          >
            추천 1위
          </span>

          <div
            class="d-flex justify-content-between align-items-center"
            :class="{ 'mt-4': activeFilter === 'ALL' && idx === 0 }"
          >
            <div>
              <h6 class="fw-bold mb-0">{{ product.productName }}</h6>
              <div class="micro-text text-secondary">
                {{ product.productType || product.saveType }} ·
                {{ product.companyName || 'KB국민은행' }}
              </div>
            </div>

            <div class="d-flex align-items-center gap-2">
              <div class="text-end">
                <span class="fs-5 fw-bold text-dark">
                  {{ product.maxRate }}%
                </span>
                <div class="micro-text text-secondary">최고 연이율</div>
              </div>
              <i
                class="fa-solid ms-1 text-secondary"
                :class="
                  expandedProductId === product.productId
                    ? 'fa-chevron-up'
                    : 'fa-chevron-down'
                "
              ></i>
            </div>
          </div>

          <div
            v-if="expandedProductId === product.productId"
            class="mt-3 pt-2 border-top"
            @click.stop
          >
            <div
              v-if="
                activeFilter === 'ALL' &&
                product.finalReceiveAmount !== null &&
                product.finalReceiveAmount !== undefined
              "
              class="bg-light p-3 rounded-3 my-2"
            >
              <div
                class="d-flex justify-content-between micro-text text-secondary mb-1"
              >
                <span>{{ saveTerm }}개월 후 실수령</span>
                <span
                  >원금
                  {{ (monthlyAmount * saveTerm).toLocaleString() }}원</span
                >
              </div>
              <div class="h5 fw-bold text-dark mb-0">
                {{ product.finalReceiveAmount.toLocaleString() }}원
              </div>
              <div class="micro-text text-danger">
                이자 {{ (product.expectedInterest || 0).toLocaleString() }}원
                (세후)
              </div>
            </div>

            <button
              class="btn btn-warning w-100 fw-bold rounded-3 py-2 text-dark mt-2"
              @click="goToDetail(product.productId)"
            >
              자세히 보고 가입하기 →
            </button>
          </div>
        </div>

        <div
          v-if="activeFilter === 'ALL'"
          class="text-center micro-text text-secondary my-2"
        >
          상품을 누르면 예상 수령액을 확인할 수 있어요
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.micro-text {
  font-size: 11px;
}
</style>
