<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAlert } from '@/util/useAlert';
import CustomAlertModal from '@/components/common/CustomAlertModal.vue';
import { useAuthStore } from '@/stores/auth';

const props = defineProps({
  open: { type: Boolean, default: false },
});
const emit = defineEmits(['close']);

const router = useRouter();
const auth = useAuthStore();

const userName = computed(() => auth.userName || '사용자');

// 메뉴 이동 시 항상 서랍을 닫고 이동
const go = (to) => {
  emit('close');
  router.push(to);
};

const logout = () => {
  emit('close');
  auth.logout();
  router.push('/auth/login');
};

// 적금: 구독 ID를 조회해서 현재 가입한 적금 상세 페이지로 이동
const goToMySavings = async () => {
  emit('close');
  try {
    const savingsApi = (await import('@/api/savingsApi')).default;
    const res = await savingsApi.getMySubscriptionId();
    const subId = res?.subscriptionId;
    if (subId) {
      router.push(`/savings/status/${subId}`);
    } else {
      showAlert('가입된 적금이 없습니다. 적금 추천에서 먼저 가입해보세요.');
    }
  } catch (error) {
    console.error('적금 가입 정보 조회 실패:', error);
  }
};

const menus = [
  { icon: 'fa-solid fa-receipt', label: '지출내역', to: { name: 'transaction/list' } },
  { icon: 'fa-solid fa-chart-pie', label: '소비분석', to: { name: 'report' } },
  { icon: 'fa-solid fa-magnifying-glass-dollar', label: '적금 추천', to: '/savings/recommend?step=3' },
  { icon: 'fa-solid fa-piggy-bank', label: '내 적금', action: 'mySavings' },
  { icon: 'fa-solid fa-user', label: '마이페이지', to: '/mypage' },
  { icon: 'fa-solid fa-circle-question', label: '도움말', to: '/mypage/help' },
];

const onMenuClick = (m) => {
  if (m.action === 'mySavings') {
    goToMySavings();
  } else {
    go(m.to);
  }
};
const { alertState, showAlert, hideAlert } = useAlert();

</script>

<template>
  <Teleport to="body">
    <!-- 뒷배경 - 클릭하면 닫힘 -->
    <div v-if="open" class="backdrop" @click="emit('close')"></div>

    <!-- 서랍 -->
    <aside class="drawer" :class="{ open }">
      <div class="drawer-head">
        <button class="close-btn" aria-label="닫기" @click="emit('close')">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>

      <!-- 프로필 -->
      <button class="profile" @click="go('/mypage')">
        <span class="avatar">
          <i class="fa-solid fa-user"></i>
        </span>
        <span class="profile-text">
          <strong>안녕하세요!</strong>
          <span class="name">{{ userName }}님</span>
        </span>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>

      <!-- 메뉴 -->
      <nav class="menu">
        <button
          v-for="m in menus"
          :key="m.label"
          class="menu-item"
          @click="onMenuClick(m)"
        >
          <i :class="m.icon" class="menu-icon"></i>
          <span class="menu-label">{{ m.label }}</span>
          <i class="fa-solid fa-chevron-right arrow"></i>
        </button>
      </nav>

      <!-- 로그아웃 -->
      <div class="drawer-foot">
        <button class="menu-item" @click="logout">
          <i class="fa-solid fa-right-from-bracket menu-icon"></i>
          <span class="menu-label">로그아웃</span>
          <i class="fa-solid fa-chevron-right arrow"></i>
        </button>
      </div>
    </aside>
  </Teleport>
    <CustomAlertModal
      :show="alertState.show"
      :message="alertState.message"
      @close="hideAlert"
    />
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 900;
}

.drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 78%;
  max-width: 320px;
  background: #fff;
  z-index: 901;
  display: flex;
  flex-direction: column;
  transform: translateX(100%);
  transition: transform 0.25s ease;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.08);
}
.drawer.open {
  transform: translateX(0);
}

.drawer-head {
  display: flex;
  justify-content: flex-end;
  padding: 16px 16px 0;
}
.close-btn {
  background: none;
  border: none;
  font-size: 22px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 8px;
}

.profile {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 12px 20px 22px;
  background: none;
  border: none;
  border-bottom: 1px solid #f1f2f4;
  text-align: left;
  cursor: pointer;
}
.avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #ffbc00;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex: 0 0 46px;
}
.profile-text {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.profile-text strong {
  font-size: 15px;
  font-weight: 700;
  color: #111;
}
.name {
  font-size: 13px;
  color: #9ca3af;
}

.menu {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 8px 0;
}
.menu-item {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 14px;
  width: 100%;
  padding: 15px 20px;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
}
.menu-item:hover {
  background: #fafafa;
}
.menu-icon {
  width: 22px;
  text-align: center;
  font-size: 17px;
  color: #ffbc00;
  flex: 0 0 22px;
}
.menu-label {
  flex: 1 1 0;
  font-size: 14px;
  font-weight: 500;
  color: #111;
}
.arrow {
  font-size: 12px;
  color: #d1d5db;
  flex: 0 0 auto;
}

.drawer-foot {
  border-top: 1px solid #f1f2f4;
  padding: 6px 0 20px;
}
</style>
