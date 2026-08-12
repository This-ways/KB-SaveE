import { ref } from 'vue';

const alertState = ref({
  show: false,
  message: '',
});

export function useAlert() {
  const showAlert = (message) => {
    alertState.value.message = message;
    alertState.value.show = true;
  };

  const hideAlert = () => {
    alertState.value.show = false;
  };

  return {
    alertState,
    showAlert,
    hideAlert,
  };
}
