<!-- src/components/common/CustomAlertModal.vue -->
<script setup>
defineProps({
  show: Boolean,
  message: String,
});

defineEmits(['close']);
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="show"
        class="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-50 d-flex justify-content-center align-items-center"
        style="z-index: 9999"
        @click="$emit('close')"
      >
        <div
          class="bg-white rounded-4 p-4 shadow text-center mx-3"
          style="width: 100%; max-width: 320px"
          @click.stop
        >
          <h6 class="fw-bold mb-2 text-dark">알림</h6>
          <p
            class="text-secondary small mb-4 lh-base"
            style="white-space: pre-line"
          >
            {{ message }}
          </p>
          <button
            type="button"
            class="btn btn-warning w-100 fw-bold py-2 rounded-3 text-dark shadow-sm"
            style="background-color: #ffcc00; border: none"
            @click="$emit('close')"
          >
            확인
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
