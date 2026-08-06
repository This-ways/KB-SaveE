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
const today = moment().date();

const summary = ref(null);
const goals = ref([]);
const loading = ref(true);
const refreshing = ref(false);

// 예산 카테고리 수정은 매달 1~7일에만 가능 (백엔드 정책과 동일)
const canEditCategory = computed(() => today <= 7);

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
    refreshing.value = false;
  }
};

// categoryId -> 목표금액
const goalMap = computed(() =>
  goals.value.reduce((acc, g) => {
    acc[g.categoryId] = g.targetAmount;
    return acc;
  }, {})
);

// 지출 + 목표 대비 소진율
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

// 소진율에 따른 막대 색상
const barColor = (rate) => {
  if (rate == null) return '#e9ecef';
  if (rate >= 90) return '#ef4444';
  if (rate >= 70) return '#f97316';
  return '#ffd239';
};

const goEditCategory = () => {
  if (!canEditCategory.value) return;
  // 수정 모드로 진입 - 저장 후 완료 축하 화면 대신 이 화면으로 돌아옴
  router.push({ path: '/goal/category', query: { mode: 'edit' } });
};

const formatAmount = (n) => Number(n ?? 0).toLocaleString('ko-KR') + '원';
</script>

<template>
  <div class="container mt-3" style="max-width: 480px">
    <!-- 상단 -->
    <div class="d-flex align-items-center mb-3">
      <button type="button" class="btn p-0" @click="router.push('/home')">
        <i class="fa-solid fa-chevron-left" style="font-size: 18px"></i>
      </button>
    </div>

    <!-- 카테고리별 지출 -->
    <div class="card mb-3">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <span class="fw-semibold">카테고리별 지출</span>
          <button
            type="button"
            class="btn p-0 refresh-btn"
            :class="{ spinning: refreshing }"
            @click="refresh"
          >
            <i class="fa-solid fa-rotate-right"></i>
          </button>
        </div>

        <p v-if="loading" class="text-secondary text-center small my-4">
          불러오는 중이에요...
        </p>

        <p
          v-else-if="rows.length === 0"
          class="text-secondary text-center small my-4"
        >
          이번 달 지출 내역이 없어요
        </p>

        <template v-else>
          <div v-for="row in rows" :key="row.categoryId" class="mb-3">
            <div class="d-flex align-items-center gap-2 mb-1">
              <span
                class="cat-icon"
                :style="{
                  backgroundColor: getCategoryStyle(row.categoryId).color,
                }"
              >
                <i :class="getCategoryStyle(row.categoryId).icon"></i>
              </span>
              <span class="small flex-grow-1">{{ row.name }}</span>
              <span class="fw-semibold small">{{ formatAmount(row.amount) }}</span>
            </div>

            <div class="progress" style="height: 6px">
              <div
                class="progress-bar"
                :style="{
                  width: (row.rate ?? 0) + '%',
                  backgroundColor: barColor(row.rate),
                }"
              ></div>
            </div>

            <div class="text-secondary mt-1" style="font-size: 11px">
              <template v-if="row.target">
                목표 {{ formatAmount(row.target) }} · {{ row.rate }}% 사용
              </template>
              <template v-else>목표 미설정</template>
            </div>
          </div>

          <!-- 6위 이하 합계 -->
          <div v-if="summary?.remainder" class="d-flex align-items-center gap-2">
            <span class="cat-icon" style="background-color: #ced4da">
              <i class="fa-solid fa-ellipsis"></i>
            </span>
            <span class="small flex-grow-1">{{ summary.remainder.label }}</span>
            <span class="fw-semibold small">
              {{ formatAmount(summary.remainder.amount) }}
            </span>
          </div>
        </template>
      </div>
    </div>

    <!-- 수정 버튼 -->
    <p
      v-if="!canEditCategory"
      class="text-secondary text-center mb-2"
      style="font-size: 12px"
    >
      예산 카테고리는 매달 1일 ~ 7일에만 수정할 수 있어요
    </p>
    <button
      type="button"
      class="btn w-100 fw-bold py-3 edit-btn"
      :disabled="!canEditCategory"
      @click="goEditCategory"
    >
      예산 카테고리 수정하기
    </button>
  </div>
</template>

<style scoped>
.cat-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  flex: 0 0 28px;
}
.refresh-btn {
  color: #adb5bd;
  font-size: 15px;
  transition: color 0.15s;
}
.refresh-btn:hover {
  color: #ffbc00;
}
.refresh-btn.spinning i {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.edit-btn {
  background-color: #ffd239;
  color: #111;
  border-radius: 12px;
}
.edit-btn:disabled {
  background-color: #e9ecef;
  color: #adb5bd;
}
</style>
