<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import goalApi from '@/api/goalApi';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const expectedSaving = ref(0);
const loading = ref(true);

onMounted(async () => {
  try {
    const data = await goalApi.getExpectedSaving();
    expectedSaving.value = data.expectedSaving ?? 0;
  } catch (e) {
    expectedSaving.value = 0;
  } finally {
    loading.value = false;
  }
});

const goMain = () => router.push('/');

const formatMoney = (n) => Number(n).toLocaleString();
</script>

<template>
  <div class="complete-page">
    <div class="hero">
      <div class="bubble">설정완료</div>
      <div class="bee">🐝</div>
      <h1 class="greeting">
        {{ auth.userName }}님,<br />
        환영합니다!
      </h1>
    </div>

    <div class="card">
      <div class="card-text">
        <span class="label">이번 달</span>
        <span class="sub">예상 절약 가능 금액</span>
        <strong class="amount">
          <template v-if="loading">계산 중...</template>
          <template v-else>{{ formatMoney(expectedSaving) }}원</template>
        </strong>
        <p class="desc">
          소비를 분석해서,<br />
          절약할 금액을 미리 계산해 봤어요
        </p>
      </div>
      <div class="money-icon">💰</div>
    </div>

    <div class="bottom">
      <button class="next-btn" @click="goMain">소비 분석 시작하기</button>
    </div>
  </div>
</template>

<style scoped>
.complete-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff8e7 0%, #fff 60%);
  padding: 20px 20px 120px;
  display: flex;
  flex-direction: column;
}
.hero {
  text-align: center;
  padding: 40px 0 32px;
  position: relative;
}
.bubble {
  display: inline-block;
  background: #fff;
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 12px;
}
.bee {
  font-size: 72px;
  line-height: 1;
  margin-bottom: 20px;
}
.greeting {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
}
.card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}
.card-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.label {
  font-size: 13px;
  color: #6b7280;
}
.sub {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
.amount {
  font-size: 28px;
  font-weight: 700;
  color: #16a34a;
  margin-bottom: 10px;
}
.desc {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
}
.money-icon {
  font-size: 56px;
}
.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16px 20px 24px;
}
.next-btn {
  width: 100%;
  padding: 16px;
  border: none;
  border-radius: 12px;
  background: #ffbc00;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
</style>
