<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import moment from 'moment';
import transactionApi from '@/api/transactionApi';
import logoImg from '@/assets/SaveE_logo.png';
import savingsApi from '@/api/savingsApi';

const router = useRouter();

const authStore = useAuthStore();
const userId = computed(() => authStore.userId);
const userName = computed(() => authStore.userName);

const yearMonth = ref(moment().format('YYYY-MM'));
const summary = ref(null); // { totalAmount, categories: [...], remainder }

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
loadSummary();

const formatAmount = (amount) => amount.toLocaleString('ko-KR') + '원';

// 카테고리별 지출 Top2 (summary.categories는 이미 금액 큰 순으로 옴)
const top2Categories = () =>
  summary.value ? summary.value.categories.slice(0, 2) : [];

// 이번 달 총 지출 -> 지출 상세 내역 페이지로 이동
const goToTransactionList = () => router.push({ name: 'transaction/list' });

// 소비분석(네비바) -> 소비 리포트 페이지로 이동
const goToReport = () => router.push({ name: 'report' });

// 홈(네비바) -> 지금 화면 데이터 다시 불러오기("새로고침" 개념)
const refreshHome = () => loadSummary();

// 카테고리별 지출 오른쪽 화살표 -> 카테고리별 지출 현황 상세 화면
const goToCategoryDetail = () => router.push({ name: 'categorySpending' });

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
</script>

<template>
  <div class="container mt-3" style="max-width: 480px">
    <!-- 상단 헤더 -->
    <div class="d-flex justify-content-between align-items-center mb-0">
      <img :src="logoImg" alt="SaveE" style="height: 100px" />
      <div class="d-flex gap-3">
        <!-- TODO: 알림 기능 (B팀 담당), 지금은 자리만 -->
        <i class="fa-solid fa-bell" style="color: #ced4da; font-size: 18px"></i>
        <!-- TODO: 메뉴/마이페이지 (다른 팀원 담당), 지금은 자리만 -->
        <i class="fa-solid fa-bars" style="color: #ced4da; font-size: 18px"></i>
      </div>
    </div>

    <template v-if="summary">
      <!-- 이번 달 총 지출 -->
      <div
        class="card mb-3"
        style="cursor: pointer"
        @click="goToTransactionList"
      >
        <div
          class="card-body d-flex justify-content-between align-items-center"
        >
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

      <!-- 카테고리별 지출 Top2 -->
      <div class="card mb-3">
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
            v-for="cat in top2Categories()"
            :key="cat.categoryId"
            class="mb-2"
          >
            <div class="d-flex justify-content-between small mb-1">
              <span>{{ cat.categoryName }}</span>
              <span class="fw-semibold">{{ formatAmount(cat.amount) }}</span>
            </div>
            <div class="progress" style="height: 6px">
              <div
                class="progress-bar"
                style="background-color: #ffd239"
                :style="{
                  width: (cat.amount / summary.totalAmount) * 100 + '%',
                }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 지금 적금에 넣을 수 있는 금액 (자리만, 비워둠 - C팀 담당) -->
    <div class="card mb-3">
      <div class="card-body">
        <div class="text-secondary small mb-1">
          지금 적금에 넣을 수 있는 금액
        </div>
        <div class="h5 mb-0">-</div>
      </div>
    </div>

    <!-- 내 적금 (자리만, 비워둠 - C팀 담당) -->
    <div class="card mb-3">
      <div class="card-body">
        <div class="text-secondary small mb-1">내 적금</div>
        <div class="h5 mb-0">-</div>
      </div>
    </div>

    <p v-if="!summary" class="text-secondary text-center mt-5">
      불러오는 중이에요...
    </p>
    <!-- 하단 네비게이션 -->
    <nav
      class="d-flex justify-content-around align-items-center border-top mt-4 pt-3"
    >
      <button
        type="button"
        class="btn d-flex flex-column align-items-center gap-1 p-0"
        @click="refreshHome"
      >
        <i
          class="fa-solid fa-house"
          style="color: #127f5f; font-size: 18px"
        ></i>
        <span class="small" style="color: #127f5f">홈</span>
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
      <!-- TODO: 적금 탭 (C팀 담당) -->
      <button
        type="button"
        class="btn d-flex flex-column align-items-center gap-1 p-0"
        @click="goToSavings"
      >
        <i class="fa-solid fa-coins text-secondary" style="font-size: 18px"></i>
        <span class="small text-secondary">적금</span>
      </button>
      <!-- TODO: 마이페이지 탭 (다른 팀원 담당) -->
      <button
        type="button"
        class="btn d-flex flex-column align-items-center gap-1 p-0"
        disabled
      >
        <i class="fa-solid fa-user" style="color: #ced4da; font-size: 18px"></i>
        <span class="small text-secondary">마이페이지</span>
      </button>
    </nav>
  </div>
</template>
