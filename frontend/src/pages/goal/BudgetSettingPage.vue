<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import moment from 'moment';
import goalApi from '@/api/goalApi';
import { getCategoryStyle } from '@/constants/categories';

const route = useRoute();
const router = useRouter();

const selectedIds = (route.query.ids || '')
  .split(',')
  .filter(Boolean)
  .map(Number);

// 메인(지출 현황)에서 수정하러 들어온 경우 - 저장 후 완료 축하 화면을 건너뜀
const isEditMode = route.query.mode === 'edit';

// 백엔드 정책과 동일 - 매달 1~7일에만 저장 가능
const canEdit = moment().date() <= 7;

const items = ref([]);
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
        targetAmount: avgMap[id] ?? 0,
      };
    });
  } catch (e) {
    alert('정보를 불러오지 못했어요.');
  } finally {
    loading.value = false;
  }
});

const isOver = (item) =>
  item.avgAmount != null && Number(item.targetAmount) > item.avgAmount;

const hasOver = computed(() => items.value.some(isOver));

const totalBudget = computed(() =>
  items.value.reduce((sum, it) => sum + (Number(it.targetAmount) || 0), 0)
);

const canSave = computed(
  () =>
    canEdit &&
    items.value.length > 0 &&
    items.value.every((it) => Number(it.targetAmount) > 0) &&
    !hasOver.value
);

// 카테고리 선택 화면으로 (수정 모드 유지)
const goBack = () =>
  router.push({
    path: '/goal/category',
    query: isEditMode ? { mode: 'edit' } : {},
  });

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
    // 최초 설정이면 완료 축하 화면, 수정이면 원래 보던 지출 현황으로
    router.push(isEditMode ? '/goal/spending' : '/goal/complete');
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
    <button class="back-btn" @click="goBack">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <h1 class="title">
      선택한 카테고리의<br />
      <span class="highlight">월 예산 금액</span>을<br />
      설정해주세요
    </h1>
    <p class="subtitle">현실적인 예산이 절약의 시작이에요!</p>

    <div v-if="loading" class="loading">불러오는 중...</div>

    <div v-else class="list">
      <div
        v-for="item in items"
        :key="item.categoryId"
        class="item"
        :class="{ over: isOver(item) }"
      >
        <div class="item-row">
          <span
            class="cat-icon"
            :style="{ backgroundColor: getCategoryStyle(item.categoryId).color }"
          >
            <i :class="getCategoryStyle(item.categoryId).icon"></i>
          </span>

          <div class="info">
            <strong>{{ item.name }}</strong>
            <span class="avg">
              나의 평균 지출 : {{ formatMoney(item.avgAmount) }}원
            </span>
          </div>

          <div class="input-box">
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

      <!-- 목록과 이어지도록 구분선 없이 배치 -->
      <div class="total">
        <span>예산</span>
        <strong>{{ formatMoney(totalBudget) }}원</strong>
      </div>
    </div>

    <div class="bottom">
      <p v-if="!canEdit" class="bottom-warn">
        예산은 매달 1일 ~ 7일에만 설정할 수 있어요
      </p>
      <p v-else-if="hasOver" class="bottom-warn">
        평균 지출을 초과한 항목이 있어요
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
  line-height: 1.5;
  margin-top: 20px;
}
.highlight {
  color: #ffbc00;
}
.subtitle {
  color: #9ca3af;
  font-size: 13px;
  margin-top: 14px;
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
  gap: 28px;
}
.item {
  display: flex;
  flex-direction: column;
}

/* 아이콘 | 이름·평균 | 금액 - 한 줄 고정 */
.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
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
}
.info {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.info strong {
  font-size: 14px;
  font-weight: 700;
  color: #111;
}
.avg {
  font-size: 11px;
  color: #9ca3af;
}
.input-box {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 1px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 8px 10px;
  background: #fff;
}
.input-box input {
  width: 78px;
  border: none;
  outline: none;
  background: transparent;
  text-align: right;
  font-size: 15px;
  font-weight: 700;
  color: #111;
  padding: 0;
  -moz-appearance: textfield;
  appearance: textfield;
}
.input-box input::-webkit-outer-spin-button,
.input-box input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.input-box .won {
  font-size: 15px;
  font-weight: 700;
  color: #111;
}
.item.over .input-box {
  border-color: #ef4444;
}
.item.over .input-box input,
.item.over .input-box .won {
  color: #ef4444;
}

.warn {
  margin: 8px 0 0 46px;
  font-size: 11px;
  color: #ef4444;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 구분선 없이 목록에 이어지도록 */
.total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  font-size: 15px;
  color: #111;
}
.total strong {
  font-size: 19px;
  font-weight: 700;
}

.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-width: 420px;
  margin: 0 auto;
  padding: 12px 20px 28px;
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
  padding: 17px;
  border: none;
  border-radius: 12px;
  background: #ffbc00;
  color: #111;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}
.next-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}
</style>
