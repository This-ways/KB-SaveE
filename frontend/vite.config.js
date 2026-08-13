import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools';

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
      },
      '/savings': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 새로고침 시 브라우저의 HTML 요청은 백엔드로 보내지 않고 Vue 라우터가 처리하도록 설정
        bypass(req) {
          if (req.headers.accept && req.headers.accept.includes('html')) {
            return '/index.html';
          }
        },
      },
    },
  },
});
