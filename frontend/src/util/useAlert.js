import { ref, onMounted } from 'vue';

const alertState = ref({
  show: false,
  message: '',
});

// 확인 버튼 눌렀을 때 알림을 켠 쪽에 "닫혔다"는 걸 알려주기 위한 콜백.
// showAlert()가 Promise를 반환하게 해서, await showAlert(...) 다음 줄(예: 페이지 이동)이
// 사용자가 "확인"을 실제로 누르기 전까지는 실행되지 않게 한다.
let resolvePromise = null;

export function useAlert() {
  // 화면(컴포넌트)이 새로 열릴 때, 이전 화면에서 못 닫고 넘어온 알림 상태가 남아있으면
  // 여기서 처음부터 다시 뜨는 버그가 있었다 (alertState가 여러 페이지가 공유하는 값이라
  // 페이지를 이동해버리면 그 알림을 띄웠던 화면 자체가 사라져서 다시 닫을 방법이 없어짐).
  // 그래서 이 컴포넌트가 새로 마운트될 때는 무조건 꺼진 상태로 초기화한다.
  onMounted(() => {
    alertState.value.show = false;
  });

  const showAlert = (message) => {
    alertState.value.message = message;
    alertState.value.show = true;
    return new Promise((resolve) => {
      resolvePromise = resolve;
    });
  };

  const hideAlert = () => {
    alertState.value.show = false;
    if (resolvePromise) {
      resolvePromise();
      resolvePromise = null;
    }
  };

  return {
    alertState,
    showAlert,
    hideAlert,
  };
}
