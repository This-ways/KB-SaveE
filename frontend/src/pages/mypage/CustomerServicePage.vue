<script setup>
import { ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 실제 채팅 서버 없이, 정적인 봇 대사 + 빠른 답변 버튼만으로 채팅방처럼 보이게 구성
const messages = ref([
  {
    from: 'bot',
    text: '안녕하세요! SaveE 고객센터예요 🐝\n아래에서 궁금하신 내용을 골라주세요.',
  },
]);

const scrollBox = ref(null);
const scrollToBottom = async () => {
  await nextTick();
  if (scrollBox.value) {
    scrollBox.value.scrollTop = scrollBox.value.scrollHeight;
  }
};

const addUserMessage = (text) => {
  messages.value.push({ from: 'user', text });
};

const addBotMessage = (msg) => {
  // 약간의 텀을 줘서 실제로 답변을 "타이핑"하는 것처럼 보이게
  setTimeout(() => {
    messages.value.push(msg);
    scrollToBottom();
  }, 400);
};

const options = [
  {
    key: 'faq',
    icon: 'fa-solid fa-circle-question',
    label: '자주 묻는 질문 보기',
    userText: '자주 묻는 질문이 궁금해요',
    handle: () => {
      addBotMessage({
        from: 'bot',
        text: '도움말 페이지에 자주 묻는 질문을 모아뒀어요. 바로 확인해보세요!',
        action: { label: '도움말 바로가기', to: '/mypage/help' },
      });
    },
  },
  {
    key: 'account',
    icon: 'fa-solid fa-building-columns',
    label: '계좌 연결 문의',
    userText: '계좌 연결이 잘 안 돼요',
    handle: () => {
      addBotMessage({
        from: 'bot',
        text: '계좌 연결에 문제가 있으신가요? 마이페이지의 계좌 연결 메뉴에서 다시 시도해보시고, 계속 안 되면 아래 이메일로 상황을 알려주세요.',
        action: { label: '계좌 연결 다시 하기', to: '/mydata/connect' },
      });
    },
  },
  {
    key: 'call',
    icon: 'fa-solid fa-phone',
    label: '전화로 문의하기',
    userText: '전화로 상담하고 싶어요',
    handle: () => {
      addBotMessage({
        from: 'bot',
        text: '평일 09:00 ~ 18:00 (주말·공휴일 휴무)\n1588-0000 로 연락 주시면 상담해 드릴게요.',
        action: { label: '1588-0000 전화 걸기', href: 'tel:1588-0000' },
      });
    },
  },
  {
    key: 'email',
    icon: 'fa-solid fa-envelope',
    label: '이메일로 문의하기',
    userText: '이메일로 문의하고 싶어요',
    handle: () => {
      addBotMessage({
        from: 'bot',
        text: '아래 메일로 문의 내용을 보내주시면 영업일 기준 1~2일 내에 답변드려요.',
        action: {
          label: 'support@savee.kr 메일 보내기',
          href: 'mailto:support@savee.kr?subject=SaveE 문의',
        },
      });
    },
  },
  {
    key: 'bug',
    icon: 'fa-solid fa-triangle-exclamation',
    label: '오류 신고하기',
    userText: '앱에서 오류를 발견했어요',
    handle: () => {
      addBotMessage({
        from: 'bot',
        text: '불편을 드려 죄송해요! 어떤 화면에서, 어떤 상황에 발생했는지 최대한 자세히 적어서 메일로 보내주시면 빠르게 확인할게요.',
        action: {
          label: '오류 내용 메일로 제보하기',
          href: 'mailto:support@savee.kr?subject=SaveE 오류 제보',
        },
      });
    },
  },
];

const selectOption = (opt) => {
  addUserMessage(opt.userText);
  scrollToBottom();
  opt.handle();
};

const goAction = (action) => {
  if (action.to) {
    router.push(action.to);
  } else if (action.href) {
    window.location.href = action.href;
  }
};
</script>

<template>
  <div class="cs-page">
    <div class="header">
      <button class="back-btn" @click="router.push('/mypage')">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <h1 class="header-title">고객센터</h1>
    </div>

    <!-- 채팅 영역 -->
    <div ref="scrollBox" class="chat-box">
      <div
        v-for="(m, i) in messages"
        :key="i"
        class="bubble-row"
        :class="m.from"
      >
        <span v-if="m.from === 'bot'" class="bot-avatar">🐝</span>
        <div class="bubble" :class="m.from">
          <p class="bubble-text">{{ m.text }}</p>
          <button
            v-if="m.action"
            class="bubble-action"
            @click="goAction(m.action)"
          >
            {{ m.action.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 빠른 답변 버튼 -->
    <div class="quick-replies">
      <button
        v-for="opt in options"
        :key="opt.key"
        class="quick-btn"
        @click="selectOption(opt)"
      >
        <i :class="opt.icon" class="quick-icon"></i>
        <span>{{ opt.label }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.cs-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 76px 16px 24px;
  display: flex;
  flex-direction: column;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 420px;
  z-index: 100;
  background: #f7f8fa;
}
.back-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #111;
  padding: 0;
}
.header-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0;
}

.chat-box {
  flex: 1 1 auto;
  min-height: 320px;
  max-height: 52vh;
  overflow-y: auto;
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.bubble-row.user {
  justify-content: flex-end;
}
.bot-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #fff7de;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex: 0 0 28px;
}

.bubble {
  max-width: 74%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.6;
}
.bubble.bot {
  background: #f4f5f7;
  color: #111;
  border-bottom-left-radius: 4px;
}
.bubble.user {
  background: #ffbc00;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.bubble-text {
  margin: 0;
  white-space: pre-line;
}
.bubble-action {
  margin-top: 8px;
  width: 100%;
  border: none;
  border-radius: 10px;
  background: #fff;
  color: #d99a00;
  font-size: 12px;
  font-weight: 700;
  padding: 8px 10px;
  cursor: pointer;
}

.quick-replies {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.quick-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 13px 16px;
  background: #fff;
  border: 1px solid #f0f1f3;
  border-radius: 12px;
  text-align: left;
  font-size: 13px;
  font-weight: 500;
  color: #111;
  cursor: pointer;
}
.quick-btn:hover {
  background: #fafafa;
}
.quick-icon {
  width: 18px;
  text-align: center;
  font-size: 14px;
  color: #ffbc00;
  flex: 0 0 18px;
}
</style>