<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import savingsApi from '@/api/savingsApi';

const route = useRoute();
const router = useRouter();

const productId = route.params.id || route.params.productId;
const product = ref(null);
const loading = ref(true);

// 적금 현황에서 진입했는지 여부 확인
const isFromStatus = computed(() => route.query.from === 'status');

const fetchDetail = async () => {
  if (!productId || productId === 'undefined') {
    alert('상품 정보를 찾을 수 없습니다.');
    router.back();
    return;
  }

  try {
    loading.value = true;
    const res = await savingsApi.getSavingsDetail(productId);
    product.value = res;
  } catch (error) {
    console.error('적금 상세 정보 조회 실패:', error);
    alert('상품 정보를 불러오는 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchDetail();
});

const goBack = () => {
  router.back();
};

const handleSubscribe = () => {
  alert(`${product.value?.productName} 가입 신청 페이지로 이동합니다.`);
};
</script>

<template>
  <!-- 적금 현황에서 왔을 때는 하단 패딩을 줄이고 일반 상태일 땐 160px 적용 -->
  <div
    class="container py-3 position-relative"
    :style="{
      maxWidth: '480px',
      backgroundColor: '#fff',
      paddingBottom: isFromStatus ? '40px' : '160px',
    }"
  >
    <!-- 상단 헤더 -->
    <div class="d-flex align-items-center mb-3">
      <i
        class="fa-solid fa-chevron-left fs-6 me-3"
        style="cursor: pointer"
        @click="goBack"
      ></i>
      <h6 class="fw-bold mb-0 text-truncate">
        {{ product?.productName || '적금 상세' }}
      </h6>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="text-center py-5 text-secondary small">
      상품 정보를 불러오는 중입니다...
    </div>

    <div v-else-if="product">
      <!-- 히어로 카드 -->
      <div
        class="card border-0 rounded-4 p-3 mb-3 text-dark position-relative overflow-hidden"
        style="background-color: #ffd239"
      >
        <div class="position-relative" style="z-index: 1">
          <h5 class="fw-bold mb-1">{{ product.productName }}</h5>
          <div class="micro-text opacity-75 mb-2">
            {{ product.productType || '자유적립식' }} ·
            {{ product.bankName || product.companyName || 'KB국민은행' }}
          </div>
          <div class="h3 fw-bold mb-0">
            {{ product.maxRate }}% <span class="fs-6 fw-bold">최고 연이율</span>
          </div>
          <div class="micro-text opacity-75 mt-1">
            기본금리 + 우대금리 최대 적용 시
          </div>
        </div>
        <i
          class="fa-solid fa-sack-dollar position-absolute end-0 bottom-0 opacity-25 me-2 mb-1"
          style="font-size: 80px"
        ></i>
      </div>

      <!-- 금리 안내 -->
      <div class="mb-3">
        <div class="d-flex justify-content-between align-items-end mb-2">
          <span class="fw-bold small">금리 안내</span>
          <span class="micro-text text-secondary">
            ({{ product.rateStartDate || '조회일' }} 기준, 세금공제 전)
          </span>
        </div>

        <div class="table-responsive border rounded-3">
          <table
            class="table table-bordered text-center align-middle mb-0 micro-text"
          >
            <thead class="table-light text-secondary">
              <tr>
                <th style="width: 35%; padding: 6px">계약기간</th>
                <th style="padding: 6px">최종이율</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rate in product.rateList" :key="rate.saveTerm">
                <td style="padding: 6px">
                  {{ rate.termLabel || rate.saveTerm + '개월' }}
                </td>
                <td class="fw-semibold" style="padding: 6px">
                  최저 연 {{ rate.minRate }}% ~ 최고 연 {{ rate.maxRate }}%
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="micro-text text-secondary mt-1">
          ※ 최고이율은 신규가입일 당시 영업점 및 홈페이지에 게시한 우대이율을
          모두 적용 시
        </div>
      </div>

      <hr class="my-3 text-black-50" />

      <!-- 상세 요약 -->
      <div class="d-flex flex-column gap-2 mb-3 micro-text">
        <div v-if="product.feature" class="d-flex">
          <span
            class="text-secondary fw-semibold flex-shrink-0"
            style="width: 80px"
            >특징</span
          >
          <span class="text-dark">{{ product.feature }}</span>
        </div>
        <div v-if="product.target" class="d-flex">
          <span
            class="text-secondary fw-semibold flex-shrink-0"
            style="width: 80px"
            >가입대상</span
          >
          <span class="text-dark">{{ product.target }}</span>
        </div>
        <div v-if="product.productType" class="d-flex">
          <span
            class="text-secondary fw-semibold flex-shrink-0"
            style="width: 80px"
            >상품유형</span
          >
          <span class="text-dark">{{ product.productType }}</span>
        </div>
        <div v-if="product.depositAmountText" class="d-flex">
          <span
            class="text-secondary fw-semibold flex-shrink-0"
            style="width: 80px"
            >저축금액</span
          >
          <span class="text-dark">{{ product.depositAmountText }}</span>
        </div>
        <div v-if="product.contractTermsText" class="d-flex">
          <span
            class="text-secondary fw-semibold flex-shrink-0"
            style="width: 80px"
            >계약기간</span
          >
          <span class="text-dark">{{ product.contractTermsText }}</span>
        </div>
        <div v-if="product.interestPayType" class="d-flex">
          <span
            class="text-secondary fw-semibold flex-shrink-0"
            style="width: 80px"
            >이자지급시기</span
          >
          <span class="text-dark">{{ product.interestPayType }}</span>
        </div>
      </div>

      <!-- 유의사항 안내 박스 -->
      <div class="bg-warning bg-opacity-10 p-3 rounded-4 d-flex gap-2 mb-2">
        <i
          class="fa-solid fa-circle-info text-warning fs-6 flex-shrink-0 mt-1"
        ></i>
        <div class="micro-text text-dark">
          <div class="fw-bold mb-1">가입 전 꼭 확인하세요.</div>
          <ul class="ps-3 mb-0 text-secondary" style="line-height: 1.4">
            <li>중도해지 시 약정금리 대신 중도해지금리(연 0.1%)가 적용돼요.</li>
            <li>
              우대금리는 조건 충족 여부에 따라 실제 적용 금리가 달라질 수
              있어요.
            </li>
            <li>이 상품은 예금자보호법에 따라 보호돼요.</li>
          </ul>
        </div>
      </div>

      <!-- 적금 추천에서 왔을 때만 여백 블록 생성 -->
      <div v-if="!isFromStatus" style="height: 120px" aria-hidden="true"></div>
    </div>

    <!-- 적금 추천에서 들어왔을 때만 하단 고정 가입하기 버튼 노출 -->
    <div
      v-if="product && !isFromStatus"
      class="position-fixed bottom-0 start-50 translate-middle-x w-100 p-3 bg-white border-top"
      style="max-width: 480px; z-index: 100"
    >
      <button
        class="btn btn-warning w-100 py-2 fw-bold rounded-4 text-dark"
        @click="handleSubscribe"
      >
        가입하기
      </button>
    </div>
  </div>
</template>

<style scoped>
.micro-text {
  font-size: 12px;
}
</style>
