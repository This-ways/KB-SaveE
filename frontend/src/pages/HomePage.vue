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

const router = useRouter();
const authStore = useAuthStore();

const userId = computed(() => authStore.userId);
const userName = computed(() => authStore.userName);

const yearMonth = ref(moment().format('YYYY-MM'));
const summary = ref(null); // { totalAmount, categories: [...], remainder }
const goalBudgets = ref([]); // [{ categoryId, categoryName, targetAmount, actualAmount }]
const mySubscription = ref(null); // null이면 미가입 (또는 아직 로딩 전)

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
const top2Categories = () =>
  summary.value ? summary.value.categories.slice(0, 2) : [];

// ===== 목표 예산 (설정한 카테고리별 목표금액 vs 실제지출) =====
const totalBudget = computed(() =>
  goalBudgets.value.reduce((sum, g) => sum + (g.targetAmount - g.actualAmount), 0)
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
    const { subscriptionId } = await savingsApi.getMySubscriptionId()   // ← 객체에서 꺼내기
    if (!subscriptionId) {
      mySubscription.value = null
      return
    }
    mySubscription.value = await savingsApi.getSavingsStatus(subscriptionId)   // ← 메서드명 변경
  } catch (e) {
    console.error('내 적금 조회 실패', e)
    mySubscription.value = null
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
      alert('가입된 적금이 없습니다.');
      // router.push('/savings/recommend');
    }
  } catch (error) {
    console.error('적금 가입 정보 조회 실패:', error);
    alert('적금 정보를 불러오지 못했습니다.');
  }
};

const loadAll = () => {
  loadSummary();
  loadGoalBudget();
  loadMySubscription();
};
loadAll();

// ===== 네비게이션 =====
const goToTransactionList = () => router.push({ name: 'transaction/list' });
const goToReport = () => router.push({ name: 'report' });
const refreshHome = () => loadAll();
const goToCategoryDetail = () => router.push({ name: 'categorySpending' });
// TODO: 적금 가입 화면 아직 없음 - 생기면 라우팅 연결
const goToSavingsSubscribe = () => {
  console.log('TODO: 적금 가입 화면 라우팅 (C팀 담당)');
};
</script>

<template>
  <div style="padding: 20px 20px 100px">
    <!-- 상단 헤더 -->
    <div class="d-flex justify-content-between align-items-center mb-0">
      <img :src="logoImg" alt="SaveE" style="height: 80px" />
      <div class="d-flex gap-3">
        <!-- 알림함 -->
        <button
          type="button"
          class="btn p-0 border-0 bg-transparent"
          @click="router.push('/notifications')"
          aria-label="알림"
        >
          <i class="fa-solid fa-bell" style="color: #495057; font-size: 18px"></i>
        </button>
        <!-- TODO: 메뉴/마이페이지 (다른 팀원 담당), 지금은 자리만 -->
        <i class="fa-solid fa-bars" style="color: #ced4da; font-size: 18px"></i>
      </div>
    </div>

    <template v-if="summary">
      <!-- 이번 달 총 지출 -->
      <div
        class="total-block mb-3"
        style="cursor: pointer"
        @click="goToTransactionList"
      >
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <div class="text-secondary small mb-1">
              {{ moment(yearMonth, 'YYYY-MM').format('MM') }}월 나의 총 지출
            </div>
            <div class="h4 fw-bold mb-0">
              {{ formatAmount(summary.totalAmount) }}
            </div>
          </div>
          <i class="fa-solid fa-chevron-right text-secondary"></i>
        </div>
      </div>

      <!-- 카테고리별 지출 (설정한 목표 카테고리별 목표금액 vs 실제지출) -->
      <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
        <div class="card-body">
          <div
            class="d-flex justify-content-between align-items-center mb-3"
            style="cursor: pointer"
            @click="goToCategoryDetail"
          >
            <span class="fw-semibold">카테고리별 지출</span>
            <i class="fa-solid fa-chevron-right text-secondary"></i>
          </div>

          <div
            v-for="g in goalBudgets"
            :key="g.categoryId"
            class="d-flex align-items-center gap-2 mb-3"
          >
            <div
              class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
              :style="{
                width: '32px',
                height: '32px',
                backgroundColor: getCategoryStyle(g.categoryId).color + '22',
              }"
            >
              <i
                class="fa-solid"
                :class="getCategoryStyle(g.categoryId).icon"
                :style="{
                  color: getCategoryStyle(g.categoryId).color,
                  fontSize: '13px',
                }"
              ></i>
            </div>
            <div class="flex-grow-1">
              <div class="d-flex justify-content-between small mb-1">
                <span>{{ g.categoryName }}</span>
                <span
                  class="fw-semibold"
                  :style="{
                    color: g.actualAmount > g.targetAmount ? '#e8512b' : '',
                  }"
                >
                  {{ formatAmount(g.actualAmount) }} /
                  {{ formatAmount(g.targetAmount) }}
                </span>
              </div>
              <div class="progress" style="height: 6px">
                <div
                  class="progress-bar"
                  :style="{
                    width:
                      Math.min((g.actualAmount / g.targetAmount) * 100, 100) +
                      '%',
                    backgroundColor:
                      g.actualAmount > g.targetAmount
                        ? '#e8512b'
                        : getCategoryStyle(g.categoryId).color,
                  }"
                ></div>
              </div>
            </div>
          </div>

          <p v-if="goalBudgets.length === 0" class="text-secondary small mb-0">
            설정한 목표 예산이 없어요.
          </p>
        </div>
      </div>
    </template>

    <!-- 잔여 예산 -->
    <div class="card mb-3 border-0 shadow-sm rounded-4 bg-white">
      <div class="card-body">
        <div class="text-secondary small mb-1">잔여 예산</div>
        <div
          class="h5 fw-bold mb-0"
          :style="{ color: goalBudgets.length > 0 && totalBudget < 0 ? '#e8512b' : '' }"
        >
          {{ goalBudgets.length > 0 ? formatAmount(totalBudget) : '-' }}
        </div>
      </div>
    </div>

    <!-- 내 적금 -->
    <div class="card mb-3">
      <div class="card-body">
        <div class="text-secondary small mb-1">내 적금</div>
        <template v-if="mySubscription">
          <div class="fw-bold">{{ mySubscription.productName }}</div>
          <div class="small text-secondary mt-1">
            누적 납입액 {{ formatAmount(mySubscription.totalPrincipal) }}
          </div>
        </template>
        <button
          v-else
          type="button"
          class="btn w-100 mt-1"
          style="background-color: #ffd239"
          @click="goToSavingsSubscribe"
        >
          적금 가입하러 가기
        </button>
      </div>
    </div>

    <p v-if="!summary" class="text-secondary text-center mt-5">
      불러오는 중이에요...
    </p>

    <!-- 하단 네비게이션 -->
    <div class="d-flex align-items-center border-top mt-4 pt-3">
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
        <!-- TODO: 마이페이지 탭 (다른 팀원 담당) -->
        <button
          type="button"
          class="btn d-flex flex-column align-items-center gap-1 p-0"
          disabled
        >
          <i
            class="fa-solid fa-user"
            style="color: #ced4da; font-size: 18px"
          ></i>
          <span class="small text-secondary">마이페이지</span>
        </button>
      </nav>
    </div>
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

/* 총 지출은 카드 없이 배경에 바로 표시 */
.total-block {
  padding: 4px 4px 0;
}
</style>