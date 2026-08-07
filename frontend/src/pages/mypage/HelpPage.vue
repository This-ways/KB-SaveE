<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const openKey = ref(null);
const toggle = (key) => (openKey.value = openKey.value === key ? null : key);

const faqs = [
  {
    key: 'start',
    q: 'SaveE는 어떤 서비스인가요?',
    a: '한 달 소비를 카테고리별로 관리하고, 목표보다 아낀 금액을 적금으로 이어주는 서비스예요. 계좌를 연결하면 소비 내역을 자동으로 분석해 드려요.',
  },
  {
    key: 'connect',
    q: '계좌 연결은 꼭 해야 하나요?',
    a: '계좌를 연결해야 소비 내역을 불러와 분석할 수 있어요. 연결하지 않으면 카테고리별 지출과 절약 금액이 표시되지 않습니다. 마이페이지 > 계좌 연결에서 언제든 연결할 수 있어요.',
  },
  {
    key: 'budget-period',
    q: '목표 금액은 왜 1일부터 7일까지만 수정할 수 있나요?',
    a: '달 중간에 목표를 바꾸면 절약 여부를 정확히 판단할 수 없어요. 그래서 매달 초 일주일 동안만 설정·수정할 수 있게 했습니다. 기간이 지나면 다음 달 1일에 다시 수정할 수 있어요.',
  },
  {
    key: 'carry-over',
    q: '매달 목표 금액을 다시 설정해야 하나요?',
    a: '아니에요. 따로 수정하지 않으면 지난달에 설정한 목표 금액이 이번 달로 자동으로 이어져요. 앱을 한동안 열지 않아도 관리가 끊기지 않습니다.',
  },
  {
    key: 'over-average',
    q: '목표 금액을 평균 지출보다 높게 설정할 수 없나요?',
    a: '평균보다 높은 금액은 절약 목표로 보기 어려워서 저장을 막고 있어요. 최근 3개월 평균 지출을 참고해서 조금씩 낮춰가는 걸 추천드려요.',
  },
  {
    key: 'saving-amount',
    q: '"적금에 넣을 수 있는 금액"은 어떻게 계산되나요?',
    a: '카테고리별로 (평균 지출 − 이번 달 목표 금액)을 더한 값이에요. 평소보다 아낄 수 있는 금액을 미리 계산해서 보여드립니다.',
  },
  {
    key: 'alarm',
    q: '알림은 언제 오나요?',
    a: '카테고리별 목표 금액의 50%, 70%, 90%를 넘길 때 알려드려요. 적금 자동이체 예정일과 이체 결과도 알림으로 보내드립니다.',
  },
  {
    key: 'auto-transfer',
    q: '적금 자동이체가 실패하면 어떻게 되나요?',
    a: '출금 시점에 잔액이 부족하면 해당 회차는 실패로 처리되고 알림이 발송돼요. 계좌에 잔액을 채운 뒤 다음 납입일에 다시 시도됩니다.',
  },
  {
    key: 'category',
    q: '카테고리를 바꾸고 싶어요.',
    a: '매달 1일부터 7일 사이에 메인 화면 상단에 카테고리 수정 안내가 떠요. 그때 눌러서 수정할 수 있어요.',
  },
];
</script>

<template>
  <div class="help-page">
    <div class="header">
      <button class="back-btn" @click="router.push('/mypage')">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <h1 class="header-title">도움말</h1>
    </div>

    <p class="lead">자주 묻는 질문을 모았어요</p>

    <div class="group">
      <template v-for="faq in faqs" :key="faq.key">
        <button class="row" @click="toggle(faq.key)">
          <span class="q-mark">Q</span>
          <span class="row-label">{{ faq.q }}</span>
          <i
            class="fa-solid arrow"
            :class="openKey === faq.key ? 'fa-chevron-up' : 'fa-chevron-down'"
          ></i>
        </button>
        <div v-if="openKey === faq.key" class="answer">
          {{ faq.a }}
        </div>
      </template>
    </div>

    <div class="contact">
      <p class="contact-title">더 궁금한 점이 있으신가요?</p>
      <p class="contact-desc">고객센터로 문의해 주세요</p>
    </div>
  </div>
</template>

<style scoped>
.help-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 16px 16px 40px;
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
}
.header-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
}
.lead {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 12px 4px;
}

.group {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
}
.row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 16px;
  background: none;
  border: none;
  border-bottom: 1px solid #f4f5f7;
  text-align: left;
  cursor: pointer;
}
.q-mark {
  width: 18px;
  font-size: 14px;
  font-weight: 800;
  color: #ffbc00;
  flex: 0 0 18px;
  line-height: 1.5;
}
.row-label {
  flex: 1 1 0;
  font-size: 14px;
  color: #111;
  line-height: 1.5;
}
.arrow {
  font-size: 12px;
  color: #d1d5db;
  margin-top: 4px;
}
.answer {
  padding: 0 16px 18px 44px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.75;
  border-bottom: 1px solid #f4f5f7;
}

.contact {
  text-align: center;
  padding: 28px 0 0;
}
.contact-title {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin: 0 0 4px;
}
.contact-desc {
  font-size: 12px;
  color: #9ca3af;
  margin: 0;
}
</style>
