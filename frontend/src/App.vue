<script setup>
import { RouterView } from 'vue-router';
import PushPermissionModal from '@/components/notification/PushPermissionModal.vue';
import PushToast from '@/components/notification/PushToast.vue';
</script>

<template>
  <!-- 모바일 앱 화면 프레임 (데스크탑에서도 폰 비율로 가운데 정렬) -->
  <div class="app-frame">
    <RouterView />
      <!-- 알림 권한 요청 모달 - 로그인 상태를 감지해 스스로 노출 여부를 판단한다 -->
    <PushPermissionModal />
    <!-- 포그라운드 푸시 토스트 - 앱을 보고 있을 때 도착한 알림을 상단에 띄운다 -->
    <PushToast />
  </div>
</template>

<style>
/* 전역 리셋 - scoped 아님 */
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  background: #f3f4f6;
  font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, 'Segoe UI',
    sans-serif;
  color: #111;
}
button {
  font-family: inherit;
}
input {
  font-family: inherit;
}
</style>

<style scoped>
.app-frame {
  max-width: 420px;
  min-height: 100vh;
  margin: 0 auto;
  background: #fff;
  position: relative;
  box-shadow: 0 0 24px rgba(0, 0, 0, 0.06);
}
</style>
