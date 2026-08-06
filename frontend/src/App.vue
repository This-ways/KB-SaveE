<script setup>
import { RouterView } from 'vue-router';
</script>

<template>
  <!-- 모바일 앱 화면 프레임 (데스크탑에서도 폰 비율로 가운데 정렬) -->
  <div class="app-frame">
    <RouterView />
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
