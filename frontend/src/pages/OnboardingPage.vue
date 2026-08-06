<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 0 = 스플래시, 1~3 = 온보딩 슬라이드
const step = ref(0);

const slides = [
  {
    emoji: '💳',
    title: '내 소비가\n어디로 새고 있는지',
    desc: '한눈에 확인하세요',
  },
  {
    emoji: '🎯',
    title: '목표 금액을\n설정해보세요',
    desc: '카테고리별 예산을 설정하면\n지출 관리가 쉬워집니다',
  },
  {
    emoji: '🐷',
    title: '절약한 금액을\n바로 적금으로',
    desc: '연결해보세요',
  },
];

const finish = () => {
  localStorage.setItem('onboardingSeen', 'true');
  router.replace('/auth/login');
};

const next = () => {
  if (step.value < slides.length) {
    step.value += 1;
  } else {
    finish();
  }
};

onMounted(() => {
  // 이미 본 적 있으면 온보딩 건너뛰고 바로 로그인으로
  if (localStorage.getItem('onboardingSeen')) {
    router.replace('/auth/login');
    return;
  }
  // 스플래시 1.5초 후 첫 슬라이드로
  setTimeout(() => {
    if (step.value === 0) step.value = 1;
  }, 1500);
});
</script>

<template>
  <!-- 스플래시 -->
  <div v-if="step === 0" class="splash">
    <div class="logo-bee">🐝</div>
    <h1 class="logo-text">SaveE</h1>
    <p class="logo-sub">Save Easy</p>
    <p class="brand">KB국민은행</p>
  </div>

  <!-- 온보딩 슬라이드 -->
  <div v-else class="onboarding">
    <button class="skip" @click="finish">건너뛰기</button>

    <div class="content">
      <div class="emoji">{{ slides[step - 1].emoji }}</div>
      <h2 class="title">{{ slides[step - 1].title }}</h2>
      <p class="desc">{{ slides[step - 1].desc }}</p>
    </div>

    <div class="dots">
      <span
        v-for="(s, i) in slides"
        :key="i"
        class="dot"
        :class="{ active: i === step - 1 }"
      ></span>
    </div>

    <button class="next-btn" @click="next">
      {{ step === slides.length ? '시작하기' : '다음' }}
    </button>
  </div>
</template>

<style scoped>
.splash {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #fff;
}
.logo-bee {
  font-size: 72px;
  margin-bottom: 8px;
}
.logo-text {
  font-size: 40px;
  font-weight: 800;
  color: #ffbc00;
  margin: 0;
}
.logo-sub {
  font-size: 14px;
  color: #d1a800;
  margin: 0;
}
.brand {
  margin-top: 32px;
  font-size: 12px;
  color: #9ca3af;
}

.onboarding {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 20px;
}
.skip {
  align-self: flex-end;
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 14px;
  padding: 8px;
  cursor: pointer;
}
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 16px;
}
.emoji {
  font-size: 96px;
  margin-bottom: 16px;
}
.title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
  white-space: pre-line;
  margin: 0;
}
.desc {
  font-size: 14px;
  color: #9ca3af;
  line-height: 1.6;
  white-space: pre-line;
  margin: 0;
}
.dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e5e7eb;
}
.dot.active {
  background: #ffbc00;
  width: 20px;
  border-radius: 999px;
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
  margin-bottom: 8px;
}
</style>
