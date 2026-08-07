<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import moment from 'moment';
import transactionApi from '@/api/transactionApi';
import goalApi from '@/api/goalApi';
import { useAuthStore } from '@/stores/auth';
import { getCategoryStyle } from '@/constants/categories';

const router = useRouter();
const authStore = useAuthStore();

const userId = computed(() => authStore.userId);
const yearMonth = moment().format('YYYY-MM');

const summary = ref(null);
const goals = ref([]);
const loading = ref(true);
const refreshing = ref(false);

const load = async () => {
  const [sum, goalList] = await Promise.all([
    transactionApi.getSummary({ userId: userId.value, yearMonth }),
    goalApi.getMyGoals(yearMonth),
  ]);
  summary.value = sum;
  goals.value = goalList;
};

onMounted(async () => {
  try {
    await load();
  } catch (e) {
    console.error('카테고리별 지출 조회 실패', e);
  } finally {
    loading.value = false;
  }
});

const refresh = async () => {
  if (refreshing.value) return;
  refreshing.value = true;
  try {
    await load();
  } catch (e) {
    console.error('새로고침 실패', e);
  } finally {
    // 눌렀다는 게 보이도록 최소 회전 시간 확보
    setTimeout(() => (refreshing.value = false), 500);
  }
};

const goalMap = computed(() =>
  goals.value.reduce((acc, g) => {
    acc[g.categoryId] = g.targetAmount;
    return acc;
  }, {})
);

const rows = computed(() => {
  if (!summary.value?.categories) return [];

  return summary.value.categories.map((c) => {
    const target = goalMap.value[c.categoryId] ?? null;
    const rate =
      target && target > 0
        ? Math.min(Math.round((c.amount / target) * 100), 100)
        : null;

    return {
      categoryId: c.categoryId,
      name: c.categoryName,
      amount: c.amount,
      target,
      rate,
    };
  });
});

const barColor = (rate) => {
  if (rate == null) return '#e5e7eb';
  if (rate >= 90) return '#ef4444';
  if (rate >= 70) return '#f97316';
  return '#ffbc00';
};

const formatMoney = (n) => Number(n ?? 0).toLocaleString('ko-KR');
</script>

<template>
  <div class="spending-page">
    <button class="back-btn" @click="router.push('/home')">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <div class="head">
      <h1 class="title">카테고리별 지출</h1>
      <button
        class="refresh-btn"
        :class="{ spinning: refreshing }"
        aria-label="새로고침"
        @click="refresh"
      >
        <i class="fa-solid fa-rotate-right"></i>
      </button>
    </div>
    <p class="subtitle">{{ yearMonth.replace('-', '년 ') }}월 지출 현황이에요</p>

    <p v-if="loading" class="state">불러오는 중...</p>
    <p v-else-if="rows.length === 0" class="state">
      이번 달 지출 내역이 없어요
    </p>

    <div v-else class="list">
      <div v-for="row in rows" :key="row.categoryId" class="item">
        <div class="item-row">
          <span
            class="cat-icon"
            :style="{ backgroundColor: getCategoryStyle(row.categoryId).color }"
          >
            <i :class="getCategoryStyle(row.categoryId).icon"></i>
          </span>

          <div class="info">
            <div class="line">
              <span class="name">{{ row.name }}</span>
              <span class="amount">{{ formatMoney(row.amount) }}원</span>
            </div>

            <div class="bar-bg">
              <div
                class="bar"
                :style="{
                  width: (row.rate ?? 0) + '%',
                  backgroundColor: barColor(row.rate),
                }"
              ></div>
            </div>

            <span v-if="row.target" class="target">
              목표 {{ formatMoney(row.target) }}원 · {{ row.rate }}% 사용
            </span>
            <span v-else class="target no-goal">목표 미설정</span>
          </div>
        </div>
      </div>

      <div v-if="summary?.remainder" class="item">
        <div class="item-row">
          <span class="cat-icon etc">
            <i class="fa-solid fa-ellipsis"></i>
          </span>
          <div class="info">
            <div class="line">
              <span class="name">{{ summary.remainder.label }}</span>
              <span class="amount">
                {{ formatMoney(summary.remainder.amount) }}원
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spending-page {
  min-height: 100vh;
  background: #fff;
  padding: 20px 20px 140px;
}
.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  padding: 8px 0;
  color: #111;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
}
.title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}
.refresh-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #9ca3af;
  cursor: pointer;
  padding: 6px;
  transition: color 0.15s;
}
.refresh-btn:hover {
  color: #ffbc00;
}
.refresh-btn.spinning {
  color: #ffbc00;
}
.refresh-btn.spinning i {
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.subtitle {
  color: #9ca3af;
  font-size: 13px;
  margin-top: 10px;
  margin-bottom: 32px;
}

.state {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 60px 0;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 26px;
}
.item-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.cat-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  flex: 0 0 36px;
  margin-top: 2px;
}
.cat-icon.etc {
  background: #d1d5db;
}
.info {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.name {
  font-size: 14px;
  font-weight: 600;
  color: #111;
}
.amount {
  font-size: 14px;
  font-weight: 700;
  color: #111;
  white-space: nowrap;
}
.bar-bg {
  height: 7px;
  border-radius: 999px;
  background: #f1f2f4;
  overflow: hidden;
}
.bar {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}
.target {
  font-size: 11px;
  color: #9ca3af;
}
.target.no-goal {
  color: #d1d5db;
}

</style>
