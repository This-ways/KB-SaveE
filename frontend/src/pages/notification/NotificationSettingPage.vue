<!-- src/pages/notification/NotificationSettingPage.vue -->
<!-- 마이페이지 > 알림 설정 -->
<!-- 푸시 수신 여부(user.push_enabled)를 켜고 끈다 -->
<!-- 브라우저 권한이 denied 면 토글로 해결할 수 없어 안내 문구로 대체한다 -->

<template>
  <div class="push-setting">
    <header class="push-setting-header">
      <button class="push-setting-back" @click="router.push('/mypage')" aria-label="뒤로">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <h1 class="push-setting-heading">알림 설정</h1>
    </header>

    <!-- 현재 상태를 먼저 보여줘 설정 화면의 목적을 드러낸다 -->
    <div class="push-setting-hero">
      <i class="fa-solid fa-bell push-setting-hero-icon"></i>
      <strong class="push-setting-hero-title">
        {{ pushEnabled ? '알림을 받고 있어요' : '알림이 꺼져 있어요' }}
      </strong>
    </div>

    <div class="push-setting-card">
      <div class="push-setting-row">
        <div class="push-setting-text">
          <strong>푸시 알림 받기</strong>
          <p>예산 소진과 적금 자동이체 결과</p>
        </div>

        <button
          class="push-switch"
          :class="{ 'is-on': pushEnabled }"
          :disabled="isLoading || isBlocked"
          @click="onToggle"
          role="switch"
          :aria-checked="pushEnabled"
        >
          <span class="push-switch-knob"></span>
        </button>
      </div>

      <!-- 브라우저 차단 상태 - 앱에서 해제할 수 없어 경로를 안내한다 -->
      <p v-if="isBlocked" class="push-setting-blocked">
        브라우저에서 알림이 차단되어 있어요.<br />
        주소창 왼쪽 아이콘 &gt; 알림 을 켜주세요.
      </p>

      <p v-else-if="errorMessage" class="push-setting-error">{{ errorMessage }}</p>
    </div>

    <div class="push-setting-card">
      <button class="push-setting-link" @click="router.push('/notifications')">
        <i class="fa-solid fa-inbox push-setting-link-icon"></i>
        <div class="push-setting-text">
          <strong>알림함</strong>
          <p>받은 알림 모두 보기</p>
        </div>
        <i class="fa-solid fa-chevron-right arrow"></i>
      </button>
    </div>

    <p class="push-setting-note">
      알림을 꺼도 알림함에는 기록이 남아요.<br />
      언제든 다시 확인할 수 있습니다.
    </p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import notificationApi from '@/api/notificationApi';
import { usePushSetup } from '@/util/usePushSetup';

const router = useRouter();
const { setupPush } = usePushSetup();

const pushEnabled = ref(false);
const isLoading = ref(false);
const errorMessage = ref('');

// 브라우저가 차단한 상태는 토글로 되돌릴 수 없다
const isBlocked = computed(
  () => 'Notification' in window && Notification.permission === 'denied',
);

onMounted(async () => {
  try {
    pushEnabled.value = await notificationApi.getPushEnabled();
  } catch (e) {
    // API 미구현 상태에서도 화면은 뜨도록 한다
    console.warn('[알림] 설정 조회 실패', e);
  }
});

async function onToggle() {
  const next = !pushEnabled.value;

  isLoading.value = true;
  errorMessage.value = '';

  try {
    await notificationApi.updatePushEnabled(next);
    pushEnabled.value = next;

    // 켠 경우 기기토큰이 없을 수 있어 등록을 시도한다
    if (next) {
      await setupPush();
    }
  } catch (e) {
    errorMessage.value = '설정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.';
    console.warn('[알림] 설정 변경 실패', e);
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.push-setting {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 40px;
  padding-top: 76px;
}

.push-setting-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 14px;
  background: #fff;
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  z-index: 100;
}

.push-setting-back {
  background: none;
  border: 0;
  outline: 0;
  font-size: 18px;
  color: #111;
  cursor: pointer;
  padding: 0 4px;
}

.push-setting-heading {
  font-size: 17px;
  font-weight: 700;
  color: #111;
  margin: 0;
}

/* 상단이 비어 보이지 않도록 현재 상태를 크게 보여준다 */
.push-setting-hero {
  background: #fff;
  padding: 32px 20px 28px;
  text-align: center;
  margin-bottom: 12px;
}

.push-setting-hero-icon {
  font-size: 34px;
  color: #ffbc00;
  margin-bottom: 14px;
}

.push-setting-hero-title {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #111;
  margin-bottom: 6px;
}

.push-setting-hero-desc {
  font-size: 13px;
  line-height: 1.5;
  color: #9ca3af;
  margin: 0;
  word-break: keep-all;
}

.push-setting-card {
  background: #fff;
  border-radius: 14px;
  margin: 0 16px 10px;
  padding: 16px 18px;
}

.push-setting-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.push-setting-text {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.push-setting-text strong {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #111;
  margin-bottom: 3px;
}

.push-setting-text p {
  font-size: 12px;
  line-height: 1.5;
  color: #9ca3af;
  margin: 0;
  word-break: keep-all;
}

/* 토글 스위치 - bootstrap 충돌을 피해 직접 구현 */
.push-switch {
  position: relative;
  width: 48px;
  height: 28px;
  border: 0;
  outline: 0;
  border-radius: 999px;
  background: #e5e7eb;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s ease;
  padding: 0;
}

.push-switch.is-on {
  background: #ffbc00;
}

.push-switch:disabled {
  opacity: 0.5;
  cursor: default;
}

.push-switch-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.push-switch.is-on .push-switch-knob {
  transform: translateX(20px);
}

.push-setting-blocked {
  font-size: 12px;
  line-height: 1.6;
  color: #e8590c;
  background: #fff4e6;
  border-radius: 10px;
  padding: 11px 13px;
  margin: 13px 0 0;
  word-break: keep-all;
}

.push-setting-error {
  font-size: 12px;
  color: #ef4444;
  margin: 12px 0 0;
}

.push-setting-link {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  background: none;
  border: 0;
  outline: 0;
  padding: 0;
  cursor: pointer;
}

.push-setting-link-icon {
  font-size: 17px;
  color: #ffbc00;
  flex-shrink: 0;
  width: 22px;
  text-align: center;
}

.arrow {
  font-size: 12px;
  color: #d1d5db;
  flex-shrink: 0;
}

.push-setting-note {
  font-size: 11px;
  line-height: 1.6;
  color: #c8ccd1;
  text-align: center;
  margin: 20px 20px 0;
  word-break: keep-all;
}
</style>