<!-- src/components/notification/PushToast.vue -->
<!-- 앱을 보고 있는 동안(포그라운드) 도착한 푸시를 상단에 띄우는 토스트 -->
<!-- 백그라운드에서는 OS 알림이 자동으로 뜨지만 포그라운드에서는 FCM 이 화면에 아무것도 그리지 않기 때문에 필요 -->
<!-- 메인 화면의 소진 배너와 같은 톤으로 맞춰 일관성을 유지한다 -->
<!-- 클래스명에 push- 접두사를 붙인 이유 : bootstrap 클래스와 충돌 방지 -->

<template>
  <!-- app-frame 의 position 영향을 받지 않도록 body 직하로 이동시킨다 -->
  <Teleport to="body">
    <Transition name="push-toast">
<div v-if="visible" class="push-toast" @click="onClick">
        <span class="push-toast-icon">🔔</span>

        <div class="push-toast-text">
          <div class="push-toast-head">
            <span class="push-toast-source">SaveE · 방금</span>
            <!-- 알림이 여러 개 쌓였을 때만 노출 - 하나씩 닫는 수고를 덜어준다 -->
            <button
              v-if="remaining > 0"
              class="push-toast-clear"
              @click.stop="clearAll"
            >
              +{{ remaining }} 모두 닫기
            </button>
          </div>
          <strong class="push-toast-title">{{ title }}</strong>
          <p class="push-toast-body">{{ body }}</p>
        </div>

        <button class="push-toast-close" @click.stop="hide" aria-label="닫기">×</button>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { onForegroundMessage } from '@/util/firebase';

const router = useRouter();

const visible = ref(false);
const title = ref('');
const body = ref('');

// 자동으로 닫지 않는다 - 사용자가 X 를 눌러야 다음 알림으로 넘어간다
// 여러 알림이 연달아 도착할 때 덮어쓰이지 않고 하나씩 확인할 수 있음
// 남은 개수를 화면에 보여줘야 하므로 ref 로 둔다
const queue = ref([]);

// 현재 표시 중인 것 외에 대기 중인 알림 수
const remaining = computed(() => queue.value.length);

function show(payload) {
  // 서버가 notification 페이로드로 보내므로 제목/본문이 여기 들어있다
  queue.value.push({
    title: payload?.notification?.title ?? '알림',
    body: payload?.notification?.body ?? '',
  });

  // 이미 표시 중이면 큐에만 넣고 끝 - 현재 것을 닫을 때 이어서 뜬다
  if (!visible.value) {
    next();
  }
}

// 큐에서 하나를 꺼내 노출한다
function next() {
  const item = queue.value.shift();
  if (!item) {
    visible.value = false;
    return;
  }

  title.value = item.title;
  body.value = item.body;
  visible.value = true;
}

// X 를 누르면 현재 것을 닫고 다음 알림으로 넘어간다
function hide() {
  visible.value = false;
  // 사라지는 애니메이션(0.25s)이 끝난 뒤 다음 것을 띄운다
  setTimeout(next, 300);
}

// 대기 중인 알림까지 한 번에 닫는다 - 내용은 알림함에서 다시 볼 수 있음
function clearAll() {
  queue.value = [];
  visible.value = false;
}

// 토스트를 누르면 알림함으로 이동 - OS 알림 클릭 시 동작과 맞춘다
// 남은 알림은 알림함에서 확인하면 되므로 큐를 비운다
function onClick() {
  queue.value = [];
  visible.value = false;
  router.push('/notifications');
}

let unsubscribe = null;

onMounted(() => {
  unsubscribe = onForegroundMessage(show);
});

onUnmounted(() => {
  // 컴포넌트가 사라진 뒤에도 리스너가 남아 중복 구독되지 않도록 해제
  if (unsubscribe) unsubscribe();
});
</script>

<style scoped>
.push-toast {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1100; /* 권한 모달(1000)보다 위 */

  /* app-frame 과 같은 폭으로 맞춰 데스크탑에서도 폰 화면 안에 있는 것처럼 보이게 함 */
  width: calc(100% - 32px);
  max-width: 390px;

  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;

  /* 화면 안 경고 배너(연한 빨강)와 헷갈리지 않도록 OS 알림 톤으로 처리 */
  /* 같은 색을 쓰면 "방금 도착한 푸시"와 "상시 표시되는 상태"가 구분되지 않음 */
  background: rgba(38, 38, 40, 0.96);
  backdrop-filter: blur(8px);
  border: none;
  border-radius: 14px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.28);
  cursor: pointer;
}

.push-toast-icon {
  font-size: 18px;
  line-height: 1.3;
  flex-shrink: 0;
}

.push-toast-text {
  flex: 1;
  min-width: 0;
}

/* 출처와 모두 닫기 버튼을 한 줄에 좌우로 배치 */
.push-toast-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 3px;
}

/* OS 알림처럼 출처를 먼저 보여줘 푸시임을 드러낸다 */
.push-toast-source {
  font-size: 11px;
  font-weight: 600;
  color: #9a9aa0;
  letter-spacing: 0.02em;
}

/* 대기 중인 알림이 있을 때만 노출 - 하나씩 닫는 수고를 덜어준다 */
.push-toast-clear {
  background: rgba(255, 255, 255, 0.12);
  border: none;
  border-radius: 6px;
  padding: 2px 7px;
  font-size: 11px;
  font-weight: 600;
  color: #d4d4d9;
  cursor: pointer;
  flex-shrink: 0;
}

.push-toast-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 2px;
}

.push-toast-body {
  font-size: 13px;
  line-height: 1.5;
  color: #c4c4c9;
  margin: 0;
  word-break: keep-all;
}

.push-toast-close {
  background: none;
  border: none;
  font-size: 20px;
  line-height: 1;
  color: #8a8a90;
  cursor: pointer;
  padding: 0 2px;
  flex-shrink: 0;
}

/* 위에서 내려왔다가 위로 사라지는 연출 */
.push-toast-enter-active,
.push-toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.push-toast-enter-from,
.push-toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-12px);
}
</style>