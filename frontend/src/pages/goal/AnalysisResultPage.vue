<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import goalApi from '@/api/goalApi';
import categoryApi from '@/api/categoryApi';
import { getCategoryStyle } from '@/constants/categories';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

// 최초 온보딩 흐름 중간 화면이라, 여기서 뒤로가기를 누르면(계좌연결 화면으로 돌아가는 게 아니라)
// 로그아웃하고 로그인 화면으로 완전히 빠져나가게 한다.
// (예전엔 이 로직이 카테고리선택 화면에 있었는데, 이 분석결과 화면이 그 앞단계로 새로 생기면서
//  "온보딩 중 이탈 시 로그아웃" 지점도 여기로 옮겨왔다)
const goBack = () => {
  auth.logout();
  router.push('/auth/login');
};

const loading = ref(true);
const items = ref([]); // [{ categoryId, name, avgAmount }] - 지출 많은 순, 상위 5개

onMounted(async () => {
  try {
    const [averages, categories] = await Promise.all([
      goalApi.getCategoryAverages(3),
      categoryApi.getList(),
    ]);

    const nameMap = {};
    categories.forEach((c) => {
      nameMap[c.categoryId] = c.name;
    });

    items.value = averages
      .filter((a) => a.avgAmount > 0)
      .map((a) => ({
        categoryId: a.categoryId,
        name: nameMap[a.categoryId] || '',
        avgAmount: a.avgAmount,
      }))
      .sort((a, b) => b.avgAmount - a.avgAmount)
      .slice(0, 5);
  } catch (e) {
    console.error('소비 분석 조회 실패', e);
  } finally {
    loading.value = false;
  }
});

const maxAmount = computed(() =>
  items.value.length > 0 ? Math.max(...items.value.map((i) => i.avgAmount)) : 1
);

const goNext = () => router.push('/goal/category');
</script>

<template>
  <div class="analysis-page">
    <button class="back-btn" @click="goBack">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <div class="hero">
      <div class="bubble">소비 분석 완료</div>
      <h1 class="title">
        {{ auth.userName }}님의<br />
        <span class="highlight">최근 3개월 소비</span>를 분석했어요
      </h1>
      <p class="subtitle">가장 많이 쓴 카테고리부터 보여드릴게요</p>
    </div>

    <div v-if="loading" class="state-msg">불러오는 중이에요...</div>

    <div v-else-if="items.length === 0" class="state-msg">
      아직 분석할 만한 거래내역이 없어요.<br />
      바로 절약할 카테고리를 선택해볼까요?
    </div>

    <div v-else class="list">
      <div v-for="(item, idx) in items" :key="item.categoryId" class="row">
        <div class="rank">{{ idx + 1 }}</div>
        <div
          class="icon-circle"
          :style="{ backgroundColor: getCategoryStyle(item.categoryId).color + '22' }"
        >
          <i
            class="fa-solid"
            :class="getCategoryStyle(item.categoryId).icon"
            :style="{ color: getCategoryStyle(item.categoryId).color }"
          ></i>
        </div>
        <div class="row-body">
          <div class="row-top">
            <span class="row-name">{{ item.name }}</span>
            <span class="row-amount">월 {{ item.avgAmount.toLocaleString() }}원</span>
          </div>
          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{
                width: (item.avgAmount / maxAmount) * 100 + '%',
                backgroundColor: getCategoryStyle(item.categoryId).color,
              }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom">
      <button class="next-btn" @click="goNext">절약할 카테고리 선택하러 가기 →</button>
    </div>
  </div>
</template>

<style scoped>
.analysis-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff8e7 0%, #fff 220px);
  padding: 76px 20px 120px;
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
  padding: 40px 0 28px;
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
  margin-bottom: 14px;
}
.title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  color: #111;
}
.highlight {
  color: #ffbc00;
}
.subtitle {
  margin-top: 10px;
  font-size: 13px;
  color: #6b7280;
}
.state-msg {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  line-height: 1.6;
  padding: 60px 0;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.05);
}
.rank {
  flex-shrink: 0;
  width: 20px;
  font-size: 14px;
  font-weight: 700;
  color: #d1d5db;
  text-align: center;
}
.icon-circle {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.row-body {
  flex: 1;
  min-width: 0;
}
.row-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}
.row-name {
  font-size: 14px;
  font-weight: 600;
  color: #111;
}
.row-amount {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
}
.bar-track {
  height: 6px;
  border-radius: 999px;
  background: #f1f2f4;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
}
.bottom {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 100%;
  max-width: 420px;
  padding: 16px 20px 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, #fff 30%);
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
