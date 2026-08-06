<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import goalApi from '@/api/goalApi';
import { getCategoryStyle } from '@/constants/categories';

const route = useRoute();
const router = useRouter();

const selectedIds = (route.query.ids || '')
  .split(',')
  .filter(Boolean)
  .map(Number);

const items = ref([]); // [{ categoryId, name, avgAmount, targetAmount }]
const loading = ref(true);
const saving = ref(false);

onMounted(async () => {
  if (selectedIds.length === 0) {
    router.replace('/goal/category');
    return;
  }

  try {
    const [cats, averages] = await Promise.all([
      goalApi.getCategories(),
      goalApi.getCategoryAverages(3),
    ]);

    const avgMap = averages.reduce((acc, cur) => {
      acc[cur.categoryId] = cur.avgAmount;
      return acc;
    }, {});

    items.value = selectedIds.map((id) => {
      const cat = cats.find((c) => c.categoryId === id);
      return {
        categoryId: id,
        name: cat ? cat.name : '알 수 없음',
        avgAmount: avgMap[id] ?? null,
        targetAmount: avgMap[id] ?? 0, // 평균값을 기본 제안값으로
      };
    });
  } catch (e) {
    alert('정보를 불러오지 못했어요.');
  } finally {
    loading.value = false;
  }
});

// 평균 지출을 초과하면 절약이 아니므로 경고 대상
const isOver = (item) =>
  item.avgAmount != null && Number(item.targetAmount) > item.avgAmount;

const hasOver = computed(() => items.value.some(isOver));

const totalBudget = computed(() =>
  items.value.reduce((sum, it) => sum + (Number(it.targetAmount) || 0), 0)
);

const canSave = computed(
  () =>
    items.value.length > 0 &&
    items.value.every((it) => Number(it.targetAmount) > 0) &&
    !hasOver.value
);

const save = async () => {
  if (!canSave.value || saving.value) return;

  saving.value = true;
  try {
    await goalApi.saveAll(
      items.value.map((it) => ({
        categoryId: it.categoryId,
        targetAmount: Number(it.targetAmount),
      }))
    );
    router.push('/goal/complete');
  } catch (e) {
    const msg = e?.response?.data || '저장에 실패했어요.';
    alert(msg);
  } finally {
    saving.value = false;
  }
};

const formatMoney = (n) => (n ? Number(n).toLocaleString() : '0');
</script>

<template>
  <div class="budget-page">
    <button class="back-btn" @click="router.back()">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <h1 class="title">
      선택한 카테고리의<br />
      <span class="highlight">월 예산 금액</span>을<br />
      설정해주세요
    </h1>
    <p class="subtitle">평균 지출보다 낮게 잡아야 절약할 수 있어요</p>

    <div v-if="loading" class="loading">불러오는 중...</div>

    <div v-else class="list">
      <div v-for="item in items" :key="item.categoryId" class="item">
        <div class="row">
          <div
            class="cat-icon"
            :style="{ backgroundColor: getCategoryStyle(item.categoryId).color }"
          >
            <i :class="getCategoryStyle(item.categoryId).icon"></i>
          </div>

          <div class="info">
            <strong>{{ item.name }}</strong>
            <span class="avg">
              평균 지출 {{ formatMoney(item.avgAmount) }}원
            </span>
          </div>

          <div class="input-wrap" :class="{ over: isOver(item) }">
            <input
              v-model="item.targetAmount"
              type="number"
              min="0"
              step="10000"
              inputmode="numeric"
            />
            <span class="won">원</span>
          </div>
        </div>

        <p v-if="isOver(item)" class="warn">
          <i class="fa-solid fa-circle-exclamation"></i>
          평균 지출을 초과하였습니다
        </p>
      </div>
    </div>

    <div v-if="!loading" class="total">
      <span>예산</span>
      <strong>{{ formatMoney(totalBudget) }}원</strong>
    </div>

    <div class="bottom">
      <p v-if="hasOver" class="bottom-warn">
        평균 지출을 초과한 항목이 있어요. 금액을 다시 확인해 주세요
      </p>
      <button class="next-btn" :disabled="!canSave || saving" @click="save">
        {{ saving ? '저장 중...' : '다음' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.budget-page {
  padding: 20px 20px 140px;
  min-height: 100vh;
  background: #fff;
}
.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  padding: 8px 0;
  color: #111;
}
.title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
  margin-top: 20px;
}
.highlight {
  color: #ffbc00;
}
.subtitle {
  color: #6b7280;
  font-size: 14px;
  margin-top: 8px;
  margin-bottom: 28px;
}
.loading {
  text-align: center;
  color: #9ca3af;
  padding: 40px 0;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cat-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  flex-shrink: 0;
}
.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.info strong {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.avg {
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
}
/* 입력란이 아래로 밀리지 않도록 고정 폭 + 줄바꿈 방지 */
.input-wrap {
  display: flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
}
.input-wrap input {
  width: 96px;
  padding: 8px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  /* 숫자 입력 화살표 제거로 폭 확보 */
  -moz-appearance: textfield;
}
.input-wrap input::-webkit-outer-spin-button,
.input-wrap input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.input-wrap input:focus {
  outline: none;
  border-color: #ffbc00;
}
.input-wrap.over input {
  border-color: #ef4444;
  color: #ef4444;
}
.won {
  font-size: 13px;
  color: #6b7280;
}
.warn {
  margin: 0 0 0 44px;
  font-size: 11px;
  color: #ef4444;
  display: flex;
  align-items: center;
  gap: 4px;
}
.total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
  font-size: 15px;
}
.total strong {
  font-size: 18px;
  font-weight: 700;
}
.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-width: 420px;
  margin: 0 auto;
  padding: 12px 20px 24px;
  background: #fff;
}
.bottom-warn {
  margin: 0 0 10px;
  font-size: 12px;
  color: #ef4444;
  text-align: center;
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
.next-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}
</style>
