<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import goalApi from '@/api/goalApi';
import { getCategoryStyle } from '@/constants/categories';

const route = useRoute();
const router = useRouter();

// 메인(지출 현황)에서 수정하러 들어온 경우 - 저장 후 완료 축하 화면을 건너뜀
const isEditMode = route.query.mode === 'edit';

const categories = ref([]);   // [{ categoryId, name }]
const selectedIds = ref([]);  // 선택한 categoryId 목록
const loading = ref(true);

onMounted(async () => {
  try {
    categories.value = await goalApi.getCategories();

    // 수정 모드면 이미 설정해둔 카테고리를 미리 체크해둠
    if (isEditMode) {
      const myGoals = await goalApi.getMyGoals();
      selectedIds.value = myGoals.map((g) => g.categoryId);
    }
  } catch (e) {
    alert('카테고리를 불러오지 못했어요.');
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

// 수정 모드면 지출 현황으로, 최초 설정이면 자산 연결 화면으로
const goBack = () =>
  router.push(isEditMode ? '/goal/spending' : '/mydata/connect');

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
</script>

<template>
  <div class="category-page">
    <button class="back-btn" @click="goBack">
      <i class="fa-solid fa-chevron-left"></i>
    </button>

    <h1 class="title">
      선택한 카테고리로<br />
      <span class="highlight">예산을 관리해요</span>
    </h1>
    <p class="subtitle">
      {{ isEditMode ? '수정할 카테고리를 다시 선택해 주세요' : '관리할 카테고리를 선택해 주세요' }}
    </p>

    <div class="section-head">
      <span class="section-title">예산 카테고리 선택</span>
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
</template>

<style scoped>
.category-page {
  padding: 20px 20px 100px;
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
