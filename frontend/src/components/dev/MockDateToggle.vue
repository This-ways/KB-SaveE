<!-- src/components/dev/MockDateToggle.vue -->
<!-- 시연/발표용 - "오늘" 날짜를 즉시 켜고 끄는 플로팅 버튼. 프론트+백엔드 둘 다 같이 맞춰서 호출한다. -->
<!-- 실제 제출/배포 전에는 App.vue에서 이 컴포넌트를 빼는 걸 잊지 말 것. -->
<script setup>
import { ref, computed } from 'vue';
import { mockToday, getToday } from '@/utils/today';
import devApi from '@/api/devApi';
import { useAlert } from '@/util/useAlert';
import CustomAlertModal from '@/components/common/CustomAlertModal.vue';

const open = ref(false);
const dateInput = ref(mockToday.value || getToday().format('YYYY-MM-DD'));
const saving = ref(false);
const { alertState, showAlert, hideAlert } = useAlert();

const isOn = computed(() => !!mockToday.value);

const toggleOpen = () => {
  open.value = !open.value;
};

// 켜기: 입력한 날짜로 프론트+백엔드 둘 다 고정
const turnOn = async () => {
  if (!dateInput.value) return;
  saving.value = true;
  try {
    await devApi.setMockToday(dateInput.value);
    mockToday.value = dateInput.value;
  } catch (e) {
    console.error('날짜 mock 설정 실패', e);
    showAlert('날짜 설정에 실패했어요. 백엔드 서버가 켜져 있는지 확인해 주세요.');
  } finally {
    saving.value = false;
  }
};

// 끄기: 프론트+백엔드 둘 다 진짜 오늘로 복귀
const turnOff = async () => {
  saving.value = true;
  try {
    await devApi.setMockToday('');
    mockToday.value = '';
  } catch (e) {
    console.error('날짜 mock 해제 실패', e);
    showAlert('날짜 해제에 실패했어요. 백엔드 서버가 켜져 있는지 확인해 주세요.');
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div class="mock-date-widget">
    <!-- 접혀있을 때: 작은 원형 버튼 하나만 -->
    <button
      type="button"
      class="fab"
      :class="{ on: isOn }"
      @click="toggleOpen"
    >
      <i class="fa-solid fa-calendar-days"></i>
    </button>

    <!-- 펼쳤을 때: 날짜 입력 + 켜기/끄기 -->
    <div v-if="open" class="panel">
      <div class="panel-title">
        시연용 날짜 조작
        <span class="badge" :class="{ on: isOn }">{{ isOn ? '켜짐' : '꺼짐' }}</span>
      </div>
      <p class="panel-sub">
        {{ isOn ? `지금 "오늘"은 ${mockToday}로 고정돼 있어요` : '지금은 진짜 오늘 날짜를 쓰고 있어요' }}
      </p>

      <input v-model="dateInput" type="date" class="date-input" />

      <div class="panel-actions">
        <button type="button" class="btn-on" :disabled="saving" @click="turnOn">
          이 날짜로 켜기
        </button>
        <button
          v-if="isOn"
          type="button"
          class="btn-off"
          :disabled="saving"
          @click="turnOff"
        >
          끄고 진짜 오늘로
        </button>
      </div>
    </div>
  </div>

  <CustomAlertModal
    :show="alertState.show"
    :message="alertState.message"
    @close="hideAlert"
  />
</template>

<style scoped>
.mock-date-widget {
  position: fixed;
  right: 16px;
  bottom: 90px;
  z-index: 999;
}
.fab {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: #374151;
  color: #fff;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
  cursor: pointer;
}
.fab.on {
  background: #ffbc00;
  color: #111;
}
.panel {
  position: absolute;
  right: 0;
  bottom: 54px;
  width: 240px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.18);
  padding: 14px;
}
.panel-title {
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.badge {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 999px;
}
.badge.on {
  color: #b8860b;
  background: #fff7de;
}
.panel-sub {
  font-size: 11px;
  color: #6b7280;
  margin: 0 0 10px;
}
.date-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 10px;
}
.panel-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.btn-on,
.btn-off {
  padding: 9px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.btn-on {
  background: #ffbc00;
  color: #111;
}
.btn-off {
  background: #f3f4f6;
  color: #6b7280;
}
.btn-on:disabled,
.btn-off:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
