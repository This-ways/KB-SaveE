<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import savingsApi from '@/api/savingsApi';
import transactionApi from '@/api/transactionApi';

const router = useRouter();

// 화면 단계 (1: 미개설 안내, 2: 조건 입력, 3: 추천 결과)
const step = ref(1);

// 세이브 금액 및 입력 Form 데이터
const saveAmount = ref(0);
const monthlyAmount = ref(0);
const saveTerm = ref(12);

const loadingSaveAmount = ref(true);

const fetchSaveAmount = async () => {
  try {
    loadingSaveAmount.value = true;
    const summary = await transactionApi.getSummary();

    if (summary) {
      if (summary.remainder !== null && summary.remainder !== undefined) {
        saveAmount.value = Math.max(summary.remainder, 0);
      } else {
        const target =
          summary.targetAmount || summary.target || summary.budget || 0;
        const spent = summary.totalAmount || 0;
        const calculated = target - spent;

        saveAmount.value = calculated > 0 ? calculated : 0;
      }
    } else {
      saveAmount.value = 0;
    }
  } catch (error) {
    console.error('세이브 금액 조회 실패:', error);
    saveAmount.value = 0;
  } finally {
    monthlyAmount.value = saveAmount.value;
    loadingSaveAmount.value = false;
  }
};

onMounted(() => {
  fetchSaveAmount();
});

const termOptions = [6, 12, 24, 36];

const recommendedList = ref([]);
const displayList = ref([]);
const activeFilter = ref('ALL');
const loading = ref(false);

// 🟢 현재 펼쳐진 카드의 productId 저장 (기본값: null)
const expandedProductId = ref(null);

// 🟢 카드 클릭 시 토글 함수
const toggleExpand = (productId) => {
  if (expandedProductId.value === productId) {
    expandedProductId.value = null; // 이미 펼쳐진 상태에서 누르면 접음
  } else {
    expandedProductId.value = productId; // 다른 항목 누르면 해당 항목 펼침
  }
};

const goBack = () => {
  if (step.value > 1) {
    step.value -= 1;
  } else {
    router.push('/home');
  }
};

const addAmount = (val) => {
  if (val === 'ALL') {
    monthlyAmount.value = saveAmount.value;
  } else {
    monthlyAmount.value += val;
  }
};

const fetchRecommendations = async () => {
  try {
    loading.value = true;
    step.value = 3;
    activeFilter.value = 'ALL';

    const res = await savingsApi.getRecommendedSavings({
      monthlyAmount: monthlyAmount.value,
      saveTerm: saveTerm.value,
    });

    recommendedList.value = res || [];
    displayList.value = res || [];

    // 🟢 기본으로 첫 번째 상품 펼쳐두기
    if (displayList.value.length > 0) {
      expandedProductId.value = displayList.value[0].productId;
    }
  } catch (error) {
    console.error('적금 추천 목록 조회 실패:', error);
    alert('추천 적금을 불러오는 중 오류가 발생했습니다.');
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

      // 🟢 탭 이동 시에도 첫 번째 상품 자동 펼침
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
    <!-- 상단 헤더, STEP 1, STEP 2 동일 -->
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

    <!-- STEP 1 생략 -->
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
        @click="step = 2"
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
          지금까지 모은 세이브 {{ saveAmount.toLocaleString() }}원으로 시작할 수
          있는 적금이 있어요
        </span>
      </div>
    </div>

    <!-- STEP 2 생략 -->
    <div v-else-if="step === 2" class="d-flex flex-column gap-4">
      <div
        class="bg-warning bg-opacity-10 p-3 rounded-4 d-flex justify-content-between align-items-center"
      >
        <div>
          <div class="micro-text text-secondary mb-1">이번 달 모은 세이브</div>
          <div class="fw-bold text-warning h5 mb-0">
            {{ saveAmount.toLocaleString() }}원
          </div>
        </div>
      </div>

      <div>
        <label class="form-label text-secondary small">월 납입액</label>
        <div class="position-relative mb-3">
          <input
            v-model.number="monthlyAmount"
            type="number"
            class="form-control form-control-lg border-0 border-bottom rounded-0 px-0 fw-bold fs-2 text-end pe-4"
            placeholder="0"
          />
          <span class="position-absolute end-0 bottom-0 fs-4 fw-bold pb-2"
            >원</span
          >
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
              세이브 전액
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
        @click="fetchRecommendations"
      >
        이 조건에 맞는 적금 보기 →
      </button>
    </div>

    <!-- ================= STEP 3: 아코디언 적금 목록 ================= -->
    <div v-else-if="step === 3">
      <!-- 상단 선택 조건 요약 카드 -->
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
          @click="step = 2"
        >
          조건 수정
        </button>
      </div>

      <!-- 필터 탭 -->
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

      <!-- 로딩 상태 -->
      <div v-if="loading" class="text-center py-5 text-secondary">
        적금 상품을 불러오는 중입니다...
      </div>

      <!-- 적금 아코디언 목록 -->
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
          <!-- 추천 1위 태그 (추천 탭 첫 번째 상품만) -->
          <span
            v-if="activeFilter === 'ALL' && idx === 0"
            class="badge bg-warning text-dark position-absolute top-0 start-0 m-3 px-2 py-1"
          >
            추천 1위
          </span>

          <!-- 카드 헤더 -->
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

            <!-- 금리 & 화살표 아이콘 -->
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

          <!-- 펼쳐지는 상세 영역 -->
          <div
            v-if="expandedProductId === product.productId"
            class="mt-3 pt-2 border-top"
            @click.stop
          >
            <!-- 추천 탭('ALL')에서만 예상 수령액 정보 카드 노출 -->
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

            <!-- 가입하기 버튼 -->
            <button
              class="btn btn-warning w-100 fw-bold rounded-3 py-2 text-dark mt-2"
              @click="goToDetail(product.productId)"
            >
              자세히 보고 가입하기 →
            </button>
          </div>
        </div>

        <!-- 추천 탭('ALL')일 때만 안내 문구 노출 -->
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
