<!-- src/components/notification/NotificationBanner.vue -->
<!-- 메인 상단 알림 배너 - 지금 가장 급한 것 하나만 보여준다 -->
<!-- 1~7일 : 카테고리 수정 기간 안내 (기한이 있는 정보라 우선) -->
<!-- 8일 이후 : 소진율이 가장 높은 알림 1건 -->
<!-- 전체 목록은 알림함에서 확인 -->

<template>
  <div v-if="isEditablePeriod" class="push-banner is-info">
    <i class="fa-solid fa-calendar-check push-banner-icon"></i>
    <div class="push-banner-text">
      <strong class="push-banner-title">예산 카테고리 수정 기간</strong>
      <p class="push-banner-body">이번 달 7일까지 카테고리를 바꿀 수 있어요</p>
    </div>
    <button class="push-banner-action" @click="goToEdit">수정하기</button>
  </div>

  <div v-else-if="topAlert" class="push-banner is-warn" @click="goToList">
    <i class="fa-solid fa-triangle-exclamation push-banner-icon"></i>
    <div class="push-banner-text">
      <strong class="push-banner-title">{{ topAlert.title }}</strong>
      <p class="push-banner-body">{{ topAlert.body }}</p>
    </div>
    <i class="fa-solid fa-chevron-right push-banner-arrow"></i>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import notificationApi from '@/api/notificationApi';

const router = useRouter();
const list = ref([]);

// 매달 1~7일에만 카테고리 변경이 열린다
const isEditablePeriod = computed(() => new Date().getDate() <= 7);

// 임계값이 가장 높은 예산 알림 1건
// 같은 임계값이 여러 개면 최근 것을 쓴다 (서버가 sent_at DESC 로 내려줌)
const topAlert = computed(() => {
  let top = null;

  for (const n of list.value) {
    if (n.typeCode !== 'BUDGET') continue;
    if (!top || n.thresholdRate > top.thresholdRate) {
      top = n;
    }
  }

  return top;
});

// 수정 모드로 진입 - 온보딩 흐름과 구분하기 위해 쿼리를 붙인다
function goToEdit() {
  router.push('/goal/category?mode=edit');
}

function goToList() {
  router.push('/notifications');
}

onMounted(async () => {
  try {
    list.value = await notificationApi.getNotifications();
  } catch (e) {
    // 배너는 부가 정보이므로 실패해도 화면을 막지 않는다
    console.warn('[알림] 배너 조회 실패', e);
  }
});
</script>

<style scoped>
.push-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 68px;
  padding: 14px 16px;
  border-radius: 12px;
  margin-bottom: 12px;
}

/* 소진율 알림만 클릭 가능 - 수정 기간 안내는 버튼으로만 이동한다 */
.push-banner.is-warn {
  background: #fff5f5;
  border: 1px solid #ffe3e3;
  cursor: pointer;
}

.push-banner.is-info {
  background: #fff9e6;
  border: 1px solid #f5e6b8;
}

.push-banner-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  flex-shrink: 0;
}

.push-banner.is-warn .push-banner-icon {
  color: #e03131;
  background: #ffe3e3;
}

.push-banner.is-info .push-banner-icon {
  color: #f0a830;
  background: #ffefc2;
}

.push-banner-text {
  flex: 1;
  min-width: 0;
}

.push-banner-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 2px;
}

.push-banner-body {
  font-size: 13px;
  line-height: 1.5;
  color: #868e96;
  margin: 0;
  word-break: keep-all;
}

/* 브랜드 컬러로 눌러야 할 곳임을 드러낸다 */
.push-banner-action {
  background: #ffd239;
  border: 0;
  outline: 0;
  box-shadow: none;
  border-radius: 8px;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  cursor: pointer;
  flex-shrink: 0;
  white-space: nowrap;
}

/* 알림함으로 이동한다는 신호 */
.push-banner-arrow {
  font-size: 13px;
  color: #ced4da;
  flex-shrink: 0;
}
</style>