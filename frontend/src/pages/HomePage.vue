<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import moment from 'moment';
import transactionApi from '@/api/transactionApi';
import categoryApi from '@/api/categoryApi';
import goalApi from '@/api/goalApi';
import savingsApi from '@/api/savingsApi';
import { useAuthStore } from '@/stores/auth';
import { getCategoryStyle } from '@/constants/categories';
import logoImg from '@/assets/SaveE_logo.png';
import SideMenu from '@/components/SideMenu.vue';
import NotificationBanner from '@/components/notification/NotificationBanner.vue';

const router = useRouter();
const authStore = useAuthStore();

const userId = computed(() => authStore.userId);
const userName = computed(() => authStore.userName);

const yearMonth = ref(moment().format('YYYY-MM'));
const summary = ref(null); // { totalAmount, categories: [...], remainder }
const goalBudgets = ref([]); // [{ categoryId, categoryName, targetAmount, actualAmount }]
const mySubscription = ref(null); // null이면 미가입 (또는 아직 로딩 전)
const expectedSaving = ref(0); // 이번 달 예상 절약 가능 금액 (2~3달 평균지출 - 목표금액)

const formatAmount = (amount) => amount.toLocaleString('ko-KR') + '원';

// ===== 이번 달 총 지출 + 카테고리별 지출 =====
const loadSummary = async () => {
  try {
    summary.value = await transactionApi.getSummary({
      userId: userId.value,
      yearMonth: yearMonth.value,
    });
  } catch (e) {
    console.error('메인화면 요약 조회 실패', e);
  }
};
// ===== 지난달 대비 지출 비교 문구 (예: "지난달보다 126,000원 덜 썼어요") =====
const lastMonthTotal = ref(null);
const loadLastMonthSummary = async () => {
  try {
    const lastYearMonth = moment(yearMonth.value, 'YYYY-MM')
      .subtract(1, 'months')
      .format('YYYY-MM');
    const res = await transactionApi.getSummary({
      userId: userId.value,
      yearMonth: lastYearMonth,
    });
    lastMonthTotal.value = res?.totalAmount ?? null;
  } catch (e) {
    console.error('지난달 총 지출 조회 실패', e);
    lastMonthTotal.value = null;
  }
};
const spendCompareText = computed(() => {
  if (!summary.value || lastMonthTotal.value == null) return '';
  const diff = summary.value.totalAmount - lastMonthTotal.value;
  if (diff === 0) return '지난달과 똑같이 썼어요';
  if (diff < 0) return `지난달보다 ${formatAmount(Math.abs(diff))} 덜 쓰는 중이에요`;
  return `지난달보다 ${formatAmount(diff)} 더 썼어요`;
});

// ===== 목표 예산 (설정한 카테고리별 목표금액 vs 실제지출) =====
const totalBudget = computed(() =>
  goalBudgets.value.reduce(
    (sum, g) => sum + (g.targetAmount - g.actualAmount),
    0,
  ),
);

// 이번 달 목표를 초과한 카테고리들의 초과분 합계 (예상 절약금액을 실시간으로 깎는 데 씀)
const overageThisMonth = computed(() =>
  goalBudgets.value.reduce(
    (sum, g) => sum + Math.max(g.actualAmount - g.targetAmount, 0),
    0,
  ),
);

// 최종 표시값 = (최근평균-목표 기반 예상절약액) - (이번 달 실제 초과분), 0 밑으로는 안 내려감
const adjustedExpectedSaving = computed(() =>
  Math.max(expectedSaving.value - overageThisMonth.value, 0),
);

const loadGoalBudget = async () => {
  try {
    const [goals, txns, categories] = await Promise.all([
      goalApi.getMyGoals(yearMonth.value),
      transactionApi.getList({
        userId: userId.value,
        yearMonth: yearMonth.value,
      }),
      categoryApi.getList(),
    ]);

    const nameMap = {};
    categories.forEach((c) => {
      nameMap[c.categoryId] = c.name;
    });

    // 카테고리별 실제 지출 합계 (Top5 제한 없이 전체 거래내역 기준으로 직접 집계)
    const spendMap = {};
    txns.forEach((t) => {
      if (t.categoryId) {
        spendMap[t.categoryId] = (spendMap[t.categoryId] || 0) + t.amount;
      }
    });

    goalBudgets.value = goals.map((g) => ({
      categoryId: g.categoryId,
      categoryName: nameMap[g.categoryId] || '',
      targetAmount: g.targetAmount,
      actualAmount: spendMap[g.categoryId] || 0,
    }));
  } catch (e) {
    console.error('목표 예산 조회 실패', e);
  }
};

// ===== 내 적금 =====
const loadMySubscription = async () => {
  try {
    const { subscriptionId } = await savingsApi.getMySubscriptionId(); // ← 객체에서 꺼내기
    if (!subscriptionId) {
      mySubscription.value = null;
      return;
    }
    mySubscription.value = await savingsApi.getSavingsStatus(subscriptionId); // ← 메서드명 변경
  } catch (e) {
    console.error('내 적금 조회 실패', e);
    mySubscription.value = null;
  }
};

//적금
const goToSavings = async () => {
  try {
    const res = await savingsApi.getMySubscriptionId();
    console.log('적금 ID 조회 응답:', res); // { subscriptionId: 1 }

    const subId = res?.subscriptionId;

    if (subId) {
      // 적금 번호를 뒤에 붙여 적금 현황 페이지로 이동
      router.push(`/savings/status/${subId}`);
    } else {
      //alert('가입된 적금이 없습니다.');
      router.push('/savings/recommend');
    }
  } catch (error) {
    console.error('적금 가입 정보 조회 실패:', error);
    alert('적금 정보를 불러오지 못했습니다.');
  }
};

// ===== 이번 달 예상 절약 가능 금액 (2~3달 평균지출 기준, 목표설정 완료화면과 동일 API) =====
const loadExpectedSaving = async () => {
  try {
    const data = await goalApi.getExpectedSaving();
    expectedSaving.value = data.expectedSaving ?? 0;
  } catch (e) {
    console.error('예상 절약 가능 금액 조회 실패', e);
    expectedSaving.value = 0;
  }
};

const loadAll = () => {
  loadSummary();
  loadLastMonthSummary();
  loadGoalBudget();
  loadMySubscription();
  loadExpectedSaving();
};
loadAll();

// ===== 네비게이션 =====
const goToReport = () => router.push({ name: 'report' });
const refreshHome = () => loadAll();
const goToCategoryDetail = () => router.push({ name: 'transaction/list' });
const goToMyPage = () => router.push({ name: 'mypage' });

// 햄버거 메뉴(서랍) 열림 상태
const menuOpen = ref(false);
</script>

<template>
  <div style="padding: 70px 20px 100px; background: #f7f8fa; min-height: 100vh">
    <!-- 상단 헤더 (고정) -->
    <div
      class="d-flex justify-content-between align-items-center"
      style="
        position: fixed;
        top: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 100%;
        max-width: 420px;
        padding: 12px 20px;
        z-index: 100;
        background: #f7f8fa;
        border-bottom: 1px solid #eceef1;
      "
    >
      <img :src="logoImg" alt="SaveE" style="height: 40px" />
      <div class="d-flex gap-3">
        <!-- 알림함 -->
        <button
          type="button"
          class="btn p-0 border-0 bg-transparent"
          @click="router.push('/notifications')"
          aria-label="알림"
        >
          <i
            class="fa-solid fa-bell"
            style="color: #495057; font-size: 18px"
          ></i>
        </button>

        <button
          type="button"
          class="btn p-0 border-0 bg-transparent"
          @click="menuOpen = true"
          aria-label="메뉴"
        >
          <i class="fa-solid fa-bars" style="color: #495057; font-size: 18px"></i>
        </button>
      </div>
    </div>

    <!-- 알림 배너 - 예산 경고 및 카테고리 수정 기간 안내 -->
    <NotificationBanner />

    <!-- 카테고리별 지출 + 남은 돈 (하나의 카드로 통합: 이만큼 썼다 -> 그래서 이만큼 남았다) -->
    <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <span class="fw-semibold">카테고리별 지출</span>
        </div>
        <div
          v-for="cat in goalBudgets"
          :key="cat.categoryId"
          class="d-flex align-items-center gap-2 mb-3"
        >
          <div
            class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
            :style="{
              width: '32px',
              height: '32px',
              backgroundColor: getCategoryStyle(cat.categoryId).color + '22',
            }"
          >
            <i
              class="fa-solid"
              :class="getCategoryStyle(cat.categoryId).icon"
              :style="{
                color: getCategoryStyle(cat.categoryId).color,
                fontSize: '13px',
              }"
            ></i>
          </div>
          <div class="flex-grow-1">
            <div class="d-flex justify-content-between small mb-1">
              <span>{{ cat.categoryName }}</span>
              <span
                class="fw-semibold"
                :style="{
                  color: cat.actualAmount > cat.targetAmount ? '#e8512b' : '',
                }"
              >
                {{ formatAmount(cat.actualAmount) }} /
                {{ formatAmount(cat.targetAmount) }}
              </span>
            </div>
            <div class="progress" style="height: 6px">
              <div
                class="progress-bar"
                :style="{
                  width:
                    Math.min((cat.actualAmount / cat.targetAmount) * 100, 100) +
                    '%',
                  backgroundColor:
                    cat.actualAmount > cat.targetAmount
                      ? '#e8512b'
                      : getCategoryStyle(cat.categoryId).color,
                }"
              ></div>
            </div>
          </div>
        </div>

        <p v-if="goalBudgets.length === 0" class="text-secondary small mb-0">
          선택한 카테고리가 없어요.
        </p>

        <hr class="my-2" style="opacity: 0.08" />

        <!-- 남은 돈: 위 리스트에서 이어져서 "그래서 얼마 남았는지"처럼 보이게 -->
        <div class="d-flex justify-content-between align-items-center mt-2">
          <span class="text-secondary small">목표 대비 남은 금액</span>
          <span
            class="h5 fw-bold mb-0"
            :style="{
              color: goalBudgets.length > 0 && totalBudget < 0 ? '#e8512b' : '',
            }"
          >
            {{ goalBudgets.length > 0 ? formatAmount(totalBudget) : '-' }}
          </span>
        </div>
        <p
          v-if="goalBudgets.length === 0"
          class="text-secondary small mb-0 mt-1"
        >
          설정한 목표가 없어요.
        </p>
      </div>
    </div>

    <!-- 이번 달 총 지출 (장식용, 클릭 연결 없음) -->
    <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <span class="text-secondary small">
            {{ moment(yearMonth, 'YYYY-MM').format('MM') }}월 나의 총 지출
          </span>
          <button
            type="button"
            class="detail-link-btn"
            @click="goToCategoryDetail"
          >
            지출 상세
          </button>
        </div>
        <div class="h4 fw-bold mb-0">
          {{ summary ? formatAmount(summary.totalAmount) : '-' }}
        </div>
        <div v-if="spendCompareText" class="text-secondary small mt-1">
          {{ spendCompareText }}
        </div>
      </div>
    </div>

    <!-- 이번 달 예상 절약 가능 금액 -->
    <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
      <div class="card-body">
        <div class="text-secondary small mb-1">이번 달 예상 절약 가능 금액</div>
        <div class="h4 fw-bold mb-0">
  {{ goalBudgets.length === 0 ? '-' : formatAmount(adjustedExpectedSaving) }}
</div>
        <div class="text-secondary small mt-1">
          {{
            goalBudgets.length === 0
              ? '절약 목표를 세워보세요'
              : adjustedExpectedSaving === 0
                ? '조금만 아껴볼까요?'
                : '적금으로 모아보세요'
          }}
        </div>
        <button
          v-if="!mySubscription"
          type="button"
          class="savable-btn mt-3"
          @click="goToSavings"
        >
          적금 추천 받기 →
        </button>
      </div>
    </div>

    <!-- 내 적금 -->
    <div class="card mb-3 border-0 shadow-sm rounded-4" style="background-color: #ffd239">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-1">
  <span class="small fw-semibold" style="color: #6b5a00">내 적금</span>
  <button
    v-if="mySubscription"
    type="button"
    class="detail-link-btn"
    style="color: #6b5a00"
    @click="goToSavings"
  >
    적금 상세
  </button>
</div>
        <template v-if="mySubscription">
          <div class="fw-bold">{{ mySubscription.productName }}</div>
          <div class="small mt-1" style="color: #6b5a00">
            누적 납입액 {{ formatAmount(mySubscription.totalPrincipal) }}
          </div>
        </template>
        <p v-else class="small mb-0" style="color: #6b5a00">가입한 적금이 없습니다</p>
      </div>
    </div>

    <!-- 하단 네비게이션 -->
    <div
  class="d-flex align-items-center border-top"
  style="
    position: fixed;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 100%;
    max-width: 420px;
    padding: 12px 20px;
    z-index: 100;
    background: #f7f8fa;
  "
>
      <nav class="d-flex justify-content-around align-items-center flex-grow-1">
        <button
          type="button"
          class="btn d-flex flex-column align-items-center gap-1 p-0"
          @click="refreshHome"
        >
          <i
            class="fa-solid fa-house"
            style="color: #ffd239; font-size: 18px"
          ></i>
          <span class="small" style="color: #ffd239">홈</span>
        </button>
        <button
          type="button"
          class="btn d-flex flex-column align-items-center gap-1 p-0"
          @click="goToReport"
        >
          <i
            class="fa-solid fa-chart-pie text-secondary"
            style="font-size: 18px"
          ></i>
          <span class="small text-secondary">소비분석</span>
        </button>
        <!-- TODO: 적금 탭 수정 -->
        <button
          type="button"
          class="btn d-flex flex-column align-items-center gap-1 p-0"
          @click="goToSavings"
        >
          <i
            class="fa-solid fa-coins text-secondary"
            style="font-size: 18px"
          ></i>
          <span class="small text-secondary">적금</span>
        </button>
        <button
          type="button"
          class="btn d-flex flex-column align-items-center gap-1 p-0"
          @click="goToMyPage"
        >
          <i
            class="fa-solid fa-user text-secondary"
            style="font-size: 18px"
          ></i>
          <span class="small text-secondary">마이페이지</span>
        </button>
      </nav>
    </div>

    <!-- 햄버거 메뉴 (서랍) -->
    <SideMenu :open="menuOpen" @close="menuOpen = false" />
  </div>
</template>

<style scoped>
/* 카드 경계를 시안처럼 뚜렷하게 - Bootstrap 기본은 너무 흐릿함 */
.card {
  border: 1px solid #eceef1;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.card-body {
  padding: 18px 16px;
}
.detail-link-btn {
  border: 1px solid #dee2e6;
  background: #fff;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 999px;
}
.detail-link-btn:hover {
  background: #f8f9fa;
}

.savable-btn {
  width: 100%;
  border: none;
  border-radius: 12px;
  background: #ffd239;
  color: #212529;
  font-weight: 700;
  font-size: 14px;
  padding: 12px;
}
.savable-btn:hover {
  background: #e6bd33;
}
</style>