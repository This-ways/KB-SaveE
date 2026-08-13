<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import goalApi from '@/api/goalApi';
import { getCategoryStyle } from '@/constants/categories';
import { useAlert } from '@/util/useAlert';
import CustomAlertModal from '@/components/common/CustomAlertModal.vue';

const route = useRoute();
const router = useRouter();

// 메인(지출 현황)에서 수정하러 들어온 경우 - 저장 후 완료 축하 화면을 건너뜀
const isEditMode = route.query.mode === 'edit';

const categories = ref([]);   // [{ categoryId, name }] - 분석 화면과 동일하게 지출 많은 순으로 정렬해서 보여줌
const selectedIds = ref([]);  // 선택한 categoryId 목록
const loading = ref(true);

onMounted(async () => {
  try {
    const [cats, averages] = await Promise.all([
      goalApi.getCategories(),
      goalApi.getCategoryAverages(3),
    ]);

    // 정렬 규칙:
    // 1) 지출 데이터가 있는 카테고리 - 지출 많은 순 (분석 화면에 top7로 안 보였어도, 지출만 있으면 여기 포함)
    // 2) 지출 데이터가 없는 카테고리 - 순서를 가릴 기준이 없으니 한글(가나다) 순
    // 3) "기타"는 위 두 그룹 어디에 속하든 상관없이 무조건 맨 마지막
    const avgMap = {};
    averages.forEach((a) => {
      avgMap[a.categoryId] = a.avgAmount;
    });

    // 정렬 규칙: 지출 있는 카테고리는 지출 많은 순, 없는 카테고리는 한글(가나다) 순.
    // "기타"도 특별 취급 없이 이 규칙에 똑같이 참여함 (예전엔 무조건 맨 마지막으로 뺐었는데 롤백).
    const withSpending = cats.filter((c) => (avgMap[c.categoryId] ?? 0) > 0);
    const noSpending = cats.filter((c) => !((avgMap[c.categoryId] ?? 0) > 0));

    withSpending.sort((a, b) => avgMap[b.categoryId] - avgMap[a.categoryId]);
    noSpending.sort((a, b) => a.name.localeCompare(b.name, 'ko'));

    categories.value = [...withSpending, ...noSpending];

    // 수정 모드면 이미 설정해둔 카테고리를 미리 체크해둠
    if (isEditMode) {
      const myGoals = await goalApi.getMyGoals();
      selectedIds.value = myGoals.map((g) => g.categoryId);
    }
  } catch (e) {
    showAlert('카테고리를 불러오지 못했어요.');
  } finally {
    loading.value = false;
  }
});

const isSelected = (id) => selectedIds.value.includes(id);

const toggle = (id) => {
  if (isSelected(id)) {
    selectedIds.value = selectedIds.value.filter((v) => v !== id);
  } else {
    selectedIds.value = [...selectedIds.value, id];
  }
};

const clearAll = () => (selectedIds.value = []);

const canProceed = computed(() => selectedIds.value.length > 0);

// 수정 모드면 홈으로, 최초 설정이면 자산 연결 화면으로
// 수정 모드면 홈으로, 최초 설정(온보딩 중)이면 자산 연결 화면으로
// 단, isEditMode가 아니면(=진짜 최초 온보딩 흐름) 뒤로가기를 눌렀을 때
// 어중간하게 앱 안에 남기지 않고 로그아웃 후 로그인 화면으로 완전히 빠져나가게 한다.
// 수정 모드면 홈으로, 최초 설정(온보딩 중)이면 바로 이전 단계(분석 결과 화면)로.
// 최초 설정 흐름에서 "여기서 로그아웃"이라는 개념은 이제 그 앞단계인 AnalysisResultPage로 옮겼다
// (계좌연결 -> 분석결과 -> 카테고리선택 순서로 화면이 하나 늘어났기 때문).
const goBack = () => {
  if (!isEditMode) {
    router.push('/goal/analysis');
    return;
  }
  router.push('/home');
};

const goNext = () => {
  if (!canProceed.value) return;
  router.push({
    path: '/goal/budget',
    query: {
      ids: selectedIds.value.join(','),
      ...(isEditMode ? { mode: 'edit' } : {}),
    },
  });
};
const { alertState, showAlert, hideAlert } = useAlert();

</script>

<template>
  <div class="category-page">
    <button class="back-btn" @click="goBack">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <h1 class="title">
      선택한 카테고리로<br />
      <span class="highlight">지출을 관리해요</span>
    </h1>
    <p class="subtitle">
      {{ isEditMode ? '수정할 카테고리를 다시 선택해 주세요' : '관리할 카테고리를 선택해 주세요' }}
    </p>

    <div class="section-head">
      <span class="section-title">지출 카테고리 선택</span>
      <button class="clear-btn" @click="clearAll">선택해제</button>
    </div>

    <div v-if="loading" class="loading">불러오는 중...</div>

    <div v-else class="grid">
      <button
        v-for="cat in categories"
        :key="cat.categoryId"
        class="cat-card"
        :class="{ active: isSelected(cat.categoryId) }"
        @click="toggle(cat.categoryId)"
      >
        <div
          class="cat-icon"
          :style="{ backgroundColor: getCategoryStyle(cat.categoryId).color }"
        >
          <i :class="getCategoryStyle(cat.categoryId).icon"></i>
        </div>
        <span class="cat-name">{{ cat.name }}</span>
      </button>
    </div>

    <div class="bottom">
      <button class="next-btn" :disabled="!canProceed" @click="goNext">
        다음
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
.category-page {
  padding: 76px 20px 100px;
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
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 28px 0 12px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}
.clear-btn {
  font-size: 12px;
  color: #9ca3af;
  background: none;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  padding: 4px 12px;
  cursor: pointer;
}
.loading {
  text-align: center;
  color: #9ca3af;
  padding: 40px 0;
}
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.cat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 6px;
  border: 2px solid #f3f4f6;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s;
}
.cat-card.active {
  border-color: #ffbc00;
  background: #fffdf5;
}
.cat-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
}
.cat-name {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
}
.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-width: 420px;
  margin: 0 auto;
  padding: 16px 20px 24px;
  background: #fff;
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
