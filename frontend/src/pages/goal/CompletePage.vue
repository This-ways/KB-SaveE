<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import goalApi from '@/api/goalApi';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

// 카테고리를 다시 조정하고 싶을 수 있어서 뒤로가기 추가.
// mode=edit 없이 그냥 카테고리 선택 화면으로 보낸다 - 그래야 "수정 모드"가 아니라
// "최초 온보딩 흐름"으로 계속 인식되어서, 8일 이후여도 날짜 제한 없이 바로 재조정 가능.
const goBack = () => router.push('/goal/category');

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

const goMain = () => router.push('/home');

const formatMoney = (n) => Number(n).toLocaleString();
</script>

<template>
  <div class="complete-page">
    <button class="back-btn" @click="goBack">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

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
      </div>
      <div class="money-icon">💰</div>
    </div>

    <div class="recheck">
      <p class="recheck-main">카테고리와 예산, 원하시는 대로 설정하셨나요?</p>
      <p class="recheck-sub">왼쪽 위 뒤로가기로 언제든 다시 확인하고 조정할 수 있어요</p>
    </div>

    <div class="bottom">
      <button class="next-btn" @click="goMain">SaveE 시작하기 →</button>
    </div>
  </div>
</template>

<style scoped>
.complete-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff8e7 0%, #fff 60%);
  padding: 76px 20px 120px;
  display: flex;
  flex-direction: column;
}
.back-btn {
  background: transparent;
  border: none;
  font-size: 20px;
  padding: 20px 20px 12px;
  color: #111;
  /* 스크롤해도 화면에 그대로 남아있게 - 다른 온보딩 화면들과 동일한 방식 */
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  z-index: 100;
  text-align: left;
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
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}
.card-text {
  display: flex;
  flex-direction: column;
  align-items: center;
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
.recheck {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 6px;
  padding: 20px 24px;
}
.recheck-main {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}
.recheck-sub {
  font-size: 12px;
  color: #9ca3af;
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
