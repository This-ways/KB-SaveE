<!-- src/components/notification/PushPermissionModal.vue -->
<!-- 로그인 직후 한 번 띄우는 알림 권한 안내 모달 -->
<!-- 브라우저 권한 팝업을 곧바로 띄우면 거부율이 높아지므로 -->
<!-- 이유를 먼저 설명하고 버튼을 눌렀을 때 실제 권한을 요청한다 -->
<!-- 클래스명에 push- 접두사를 붙인 이유 : bootstrap 의 .modal 이 display:none 이라 충돌함 -->

<template>
  <!-- app-frame 의 position 영향을 받지 않도록 body 직하로 이동시킨다 -->
  <Teleport to="body">
    <div v-if="visible" class="push-backdrop" @click.self="close">
      <div class="push-box">
        <div class="push-icon">🔔</div>

        <h2 class="push-title">알림을 받아보시겠어요?</h2>
        <p class="push-desc">
          설정하신 카테고리 목표 금액의 50 · 70 · 90%를 넘으면<br />
          바로 알려드려요. 적금 자동이체 결과도 함께 보내드립니다.
        </p>

        <p v-if="errorMessage" class="push-error">{{ errorMessage }}</p>

        <div class="push-actions">
          <button class="push-btn push-btn-ghost" @click="close">나중에</button>
          <button
            class="push-btn push-btn-primary"
            :disabled="isLoading"
            @click="onAllow"
          >
            {{ isLoading ? '설정 중...' : '알림 받기' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { usePushSetup } from '@/util/usePushSetup';

const emit = defineEmits(['done']);

const visible = ref(false);
const { isLoading, errorMessage, setupPush, checkBudgetUsage } = usePushSetup();
const { isLogin } = storeToRefs(useAuthStore());
const route = useRoute();

// 한 번 물어본 뒤에는 다시 띄우지 않는다
const ASKED_KEY = 'pushAsked';

// 예산 설정을 마친 뒤 도달하는 화면들에서만 노출한다
// 온보딩 중(카테고리/예산 설정 화면)에 끼어들면 흐름을 끊음
// 신규 가입자는 완료 화면에서, 재접속 사용자는 홈에서 잡힌다
const ALLOWED_PATHS = ['/home'];

// 로그인 여부와 현재 경로를 함께 본다
// 로그인만 보면 온보딩 화면에서 떠버리고, 경로만 보면 JWT 없이 API 를 호출해 401 이 남
// immediate 로 새로고침 후 이미 로그인된 경우도 처리
watch(
  [isLogin, () => route.path],
  async ([loggedIn, path]) => {
    if (!loggedIn) {
      visible.value = false;
      return;
    }

    // 허용된 화면이 아니면 아무것도 하지 않는다 (온보딩 진행 중)
    if (!ALLOWED_PATHS.includes(path)) {
      return;
    }

    // 웹 푸시 미지원 브라우저면 모달을 띄울 이유가 없음
    if (!('Notification' in window)) {
      await checkBudgetUsage();
      return;
    }

    // [진입점 1] 재접속 사용자
    // 이미 허용한 상태이므로 토큰만 조용히 갱신한 뒤 체크한다.
    // 토큰이 이미 있어 푸시가 정상 발송된다.
    if (Notification.permission === 'granted') {
      await setupPush();
      await checkBudgetUsage();
      return;
    }

    const alreadyAsked = localStorage.getItem(ASKED_KEY);

    if (!alreadyAsked && Notification.permission === 'default') {
      // 모달을 띄울 거면 여기서 체크하지 않는다
      // 토큰 없는 상태로 먼저 돌리면 DB 만 채워지고,
      // 허용 후 재호출 시 전부 중복으로 걸려 푸시가 영영 나가지 않음
      visible.value = true;
      return;
    }

    // 이미 물어봤고 거부한 사용자 - 기록만 남기면 되므로 체크
    await checkBudgetUsage();
  },
  { immediate: true },
);

async function onAllow() {
  // 권한 팝업 응답과 토큰 등록까지만 기다린다
  const ok = await setupPush();

  // 실패해도 모달은 닫는다 (권한 거부는 정상 시나리오)
  localStorage.setItem(ASKED_KEY, 'true');
  visible.value = false;
  emit('done', ok);

  // [진입점 2] 신규 가입자
  // 모달을 띄우는 경로에서는 일부러 checkBudgetUsage 를 부르지 않았다.
  // 토큰 없이 먼저 돌리면 DB 만 채워지고, 이후 재호출은 전부 중복으로 걸려
  // 푸시가 영영 나가지 않기 때문.
  // 권한을 허용한 지금이 "예산도 있고 기기토큰도 등록된" 첫 시점이라 여기서 처음 부른다.
  // 모달을 닫은 뒤 호출하므로 화면이 멈추지 않는다 - 결과는 토스트로 도착한다
  if (ok) {
    checkBudgetUsage();
  }
}

function close() {
  localStorage.setItem(ASKED_KEY, 'true');
  visible.value = false;
  emit('done', false);

  // 푸시는 안 받더라도 알림함에는 기록을 남긴다
  // 기기토큰이 없으므로 서버에서 발송은 건너뛰고 INSERT 만 수행됨
  checkBudgetUsage();
}
</script>
<style scoped>
.push-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

.push-box {
  background: #fff;
  border-radius: 20px;
  padding: 32px 24px 24px;
  width: 100%;
  max-width: 340px;
  text-align: center;
}

.push-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.push-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 10px;
  color: #1a1a1a;
}

.push-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin: 0 0 20px;
}

.push-error {
  font-size: 13px;
  color: #e03131;
  margin: 0 0 16px;
}

.push-actions {
  display: flex;
  gap: 8px;
}

.push-btn {
  flex: 1;
  padding: 13px 0;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.push-btn-ghost {
  background: #f1f3f5;
  color: #868e96;
}

.push-btn-primary {
  background: #ffbc00;
  color: #1a1a1a;
}

.push-btn-primary:disabled {
  opacity: 0.6;
  cursor: default;
}
</style>