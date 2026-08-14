<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import moment from 'moment';
import goalApi from '@/api/goalApi';
import { getCategoryStyle } from '@/constants/categories';
import { useAuthStore } from '@/stores/auth';
import { useAlert } from '@/util/useAlert';
import CustomAlertModal from '@/components/common/CustomAlertModal.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const selectedIds = (route.query.ids || '')
  .split(',')
  .filter(Boolean)
  .map(Number);

// 메인(지출 현황)에서 수정하러 들어온 경우 - 저장 후 완료 축하 화면을 건너뜀
const isEditMode = route.query.mode === 'edit';

// 백엔드 정책과 동일 - 매달 1~7일에만 저장 가능
// 단, 신규 가입자의 최초 설정(회원가입 -> 계좌연결 -> 카테고리 선택 흐름, mode=edit 아님)은
// 날짜와 무관하게 항상 허용한다. 안 그러면 8일 이후 가입한 사람은 온보딩을 완주할 수 없다.
const canEdit = !isEditMode || moment().date() <= 7;

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

    // 수정 모드(기존 사용자, 1~7일)일 때만 지난달 목표를 미리 조회해서 보여준다.
    // - 지난달에 이미 있던 카테고리 -> 그 금액 그대로 채워서, 안 건드리면 이월되는 것처럼 보이게
    // - 이번에 새로 추가한 카테고리 -> 지난달엔 없었으니 0원부터
    // 최초 온보딩(!isEditMode)은 지난달이라는 개념 자체가 없으니 전부 0원부터 시작.
    // 주의: 반드시 "지난달"을 명시해서 조회해야 함 - 인자 없이 부르면 서버가 "이번 달"로 보고
    // 이번 달이 비어있을 때 자동 이월(carry-over)을 실제로 실행해버려서, 사용자가 저장하기도
    // 전에 DB에 값이 먼저 생겨버리는 부작용이 생긴다.
    let prevMap = {};
    if (isEditMode) {
      try {
        const prevYearMonth = moment().subtract(1, 'months').format('YYYY-MM');
        const prevGoals = await goalApi.getMyGoals(prevYearMonth);
        prevMap = prevGoals.reduce((acc, g) => {
          acc[g.categoryId] = g.targetAmount;
          return acc;
        }, {});
      } catch (e) {
        console.error('지난달 목표 조회 실패 - 0원부터 시작으로 대체', e);
      }
    }

    items.value = selectedIds.map((id) => {
      const cat = cats.find((c) => c.categoryId === id);
      return {
        categoryId: id,
        name: cat ? cat.name : '알 수 없음',
        avgAmount: avgMap[id] ?? null,
        targetAmount: isEditMode ? (prevMap[id] ?? 0) : 0,
      };
    });
  } catch (e) {
    showAlert('정보를 불러오지 못했어요.');
  } finally {
    loading.value = false;
  }
});

const isOver = (item) =>
  item.avgAmount != null && Number(item.targetAmount) > item.avgAmount;

// 최초 온보딩(신규가입)만 이걸로 저장을 막는다 - 아직 아무 맥락 없는 사용자한테는
// 평균 안에서 시작하게 가이드하는 게 맞다고 판단.
// 반면 나중에(수정 모드) 예산을 고칠 때는, 3개월 평균 안에 있던 큰 변동 하나가 지나가버리면
// 평균 자체가 왜곡돼서 오히려 사용자를 방해할 수 있어 제한을 안 건다 (빨간 테두리 표시만 유지).
const hasOver = computed(() => items.value.some(isOver));

const totalBudget = computed(() =>
  items.value.reduce((sum, it) => sum + (Number(it.targetAmount) || 0), 0)
);

const canSave = computed(
  () =>
    canEdit &&
    items.value.length > 0 &&
    items.value.every((it) => Number(it.targetAmount) > 0) &&
    (isEditMode || !hasOver.value) // 최초 온보딩일 때만 평균 초과 시 저장 막음
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
    auth.setHasGoals(); // 온보딩 완료(목표 설정 끝) - 다음 로그인부터 /home으로 바로 가게
    // 최초 설정이면 완료 축하 화면, 수정이면 홈으로
    router.push(isEditMode ? '/home' : '/goal/complete');
  } catch (e) {
    const msg = e?.response?.data || '저장에 실패했어요.';
    showAlert(msg);
  } finally {
    saving.value = false;
  }
};

const formatMoney = (n) => (n ? Number(n).toLocaleString() : '0');

// 입력창에 숫자만 남기고(콤마·문자 제거), 실제 값은 숫자로 저장 -> 화면엔 formatMoney로 다시 콤마 붙여서 보여줌
const onAmountInput = (item, event) => {
  const digitsOnly = event.target.value.replace(/[^\d]/g, '');
  item.targetAmount = digitsOnly ? Number(digitsOnly) : 0;
  event.target.value = formatMoney(item.targetAmount);
};
const { alertState, showAlert, hideAlert } = useAlert();

</script>

<template>
  <div class="budget-page">
    <button class="back-btn" @click="goBack">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <h1 class="title">
      선택한 카테고리의<br />
      <span class="highlight">이번 달 쓸 금액</span>을<br />
      설정해주세요
    </h1>
    <p class="subtitle">
      {{
        isEditMode
          ? '원하시는 예산을 자유롭게 입력해주세요!'
          : '평균 지출을 참고해서 예산을 설정해보세요!'
      }}
    </p>

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
              평소 지출 : {{ formatMoney(item.avgAmount) }}원
            </span>
          </div>

          <div class="input-box">
            <input
              :value="formatMoney(item.targetAmount)"
              type="text"
              inputmode="numeric"
              @input="onAmountInput(item, $event)"
            />
            <span class="won">원</span>
          </div>
        </div>
      </div>

      <!-- 목록과 이어지도록 구분선 없이 배치 -->
      <div class="total">
        <span>합계</span>
        <strong>{{ formatMoney(totalBudget) }}원</strong>
      </div>
    </div>

    <div class="bottom">
      <p v-if="!canEdit" class="bottom-warn">
        쓸 금액은 매달 1일 ~ 7일에만 설정할 수 있어요
      </p>
      <p v-else-if="!isEditMode && hasOver" class="bottom-warn">
        평균 지출을 초과한 항목이 있어요. 평균 이하로 입력해 주세요
      </p>
      <button class="next-btn" :disabled="!canSave || saving" @click="save">
        {{ saving ? '저장 중...' : '다음' }}
      </button>
    </div>
  </div>
    <CustomAlertModal
      :show="alertState.show"
      :message="alertState.message"
      @close="hideAlert"
    />
</template>

<style scoped>
.budget-page {
  padding: 76px 20px 140px;
  min-height: 100vh;
  background: #fff;
}
.back-btn {
  background: #fff;
  border: none;
  font-size: 20px;
  padding: 20px 20px 12px;
  color: #111;
  /* 스크롤해도 화면에 그대로 남아있게 - 다른 화면들과 동일한 방식 */
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  z-index: 100;
  text-align: left;
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
