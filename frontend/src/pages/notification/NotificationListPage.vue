<!-- src/pages/notification/NotificationListPage.vue -->
<!-- 알림함 - 종 아이콘 / 토스트 클릭 / OS 알림 클릭이 모두 이 경로로 온다 -->
<!-- 푸시를 거부한 사용자도 여기서는 알림 이력을 볼 수 있다 (기록은 항상 남기는 정책) -->

<template>
  <div class="push-list">
    <header class="push-list-header">
      <!-- 히스토리가 있으면 직전 화면으로, 푸시 클릭 등 첫 진입이면 홈으로 -->
      <button class="push-list-back" @click="goBack" aria-label="뒤로">‹</button>
      <h1 class="push-list-heading">알림</h1>
    </header>

    <p v-if="isLoading" class="push-list-empty">불러오는 중...</p>

    <p v-else-if="items.length === 0" class="push-list-empty">
      아직 도착한 알림이 없어요
    </p>

    <template v-else>
      <section v-for="group in grouped" :key="group.label" class="push-list-group">
        <h2 class="push-list-label">{{ group.label }}</h2>
        <article v-for="item in group.items" :key="item.notificationId" class="push-list-item">
        <i class="push-list-icon" :class="iconOf(item)" :style="{ color: colorOf(item) }"></i>
          <div class="push-list-text">
            <strong class="push-list-title">{{ item.title }}</strong>
            <p class="push-list-body">{{ item.body }}</p>
            <time class="push-list-time">{{ formatTime(item.sentAt) }}</time>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import notificationApi from '@/api/notificationApi';
import { getCategoryStyle, BRAND } from '@/constants/categories';

const router = useRouter();

// 알림 설정에서 들어오면 알림 설정으로 돌아가야 한다.
// 다만 푸시/OS 알림 클릭으로 바로 진입하면 직전 기록이 없어 back() 이 먹히지 않으므로 홈으로 보낸다.
const goBack = () => {
  if (window.history.state?.back) {
    router.back();
  } else {
    router.push('/home');
  }
};

const items = ref([]);
const isLoading = ref(true);

onMounted(async () => {
  try {
    items.value = await notificationApi.getNotifications();
  } catch (e) {
    console.warn('[알림] 목록 조회 실패', e);
  } finally {
    isLoading.value = false;
  }
});

// 오늘 / 이전으로만 나눈다 - 알림은 한 달치만 유지되므로 더 잘게 쪼갤 이유가 없음
const grouped = computed(() => {
  const today = [];
  const earlier = [];
  const now = new Date();

  for (const item of items.value) {
    const sent = new Date(item.sentAt);
    const isToday =
      sent.getFullYear() === now.getFullYear() &&
      sent.getMonth() === now.getMonth() &&
      sent.getDate() === now.getDate();

    (isToday ? today : earlier).push(item);
  }

  const result = [];
  if (today.length) result.push({ label: '오늘', items: today });
  if (earlier.length) result.push({ label: '이전', items: earlier });
  return result;
});

// 카테고리 아이콘은 팀 확정본을 그대로 사용(추후 다른 아이콘으로 교체할 수 있음)
function iconOf(item) {
  if (item.typeCode !== 'BUDGET') return 'fa-solid fa-piggy-bank';
  return getCategoryStyle(item.categoryId).icon;
}

// 카테고리 컬러를 그대로 사용
function colorOf(item) {
  if (item.typeCode !== 'BUDGET') return BRAND.primary;
  return getCategoryStyle(item.categoryId).color;
}

// 24시간 안이면 상대 시간, 넘으면 날짜와 시각
function formatTime(sentAt) {
  const sent = new Date(sentAt);
  const diffMin = Math.floor((Date.now() - sent) / 60000);

  if (diffMin < 1) return '방금';
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffMin < 1440) return `${Math.floor(diffMin / 60)}시간 전`;

  const ampm = sent.getHours() < 12 ? '오전' : '오후';
  const hour = sent.getHours() % 12 || 12;
  const min = String(sent.getMinutes()).padStart(2, '0');
  return `${sent.getMonth() + 1}월 ${sent.getDate()}일 ${ampm} ${hour}:${min}`;
}
</script>

<style scoped>
.push-list {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 40px;
}

.push-list-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 12px;
  background: #fff;
}

.push-list-back {
  background: none;
  border: none;
  font-size: 26px;
  line-height: 1;
  color: #1a1a1a;
  cursor: pointer;
  padding: 0 6px;
}

.push-list-heading {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.push-list-label {
  font-size: 13px;
  font-weight: 500;
  color: #868e96;
  margin: 18px 0 8px;
  padding: 0 20px;
}

.push-list-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: #fff;
  padding: 18px 20px;
  border-bottom: 1px solid #f1f3f5;
}

.push-list-icon {
  font-size: 18px;
  line-height: 1.3;
  flex-shrink: 0;
}

.push-list-icon.is-danger { color: #e03131; }
.push-list-icon.is-warn   { color: #f08c00; }
.push-list-icon.is-mild   { color: #fab005; }
.push-list-icon.is-info   { color: #1c7ed6; }

.push-list-text {
  flex: 1;
  min-width: 0;
}

.push-list-title {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.push-list-body {
  font-size: 14px;
  line-height: 1.5;
  color: #868e96;
  margin: 0 0 6px;
  word-break: keep-all;
}

.push-list-time {
  font-size: 13px;
  color: #adb5bd;
}

.push-list-empty {
  font-size: 14px;
  color: #adb5bd;
  text-align: center;
  padding: 80px 20px;
  margin: 0;
}
</style>