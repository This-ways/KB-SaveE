<script setup>
import Footer from './Footer.vue';
</script>

<template>
  <div class="container">
    <div class="content my-5 px-3">
      <slot></slot>
    </div>
    <Footer />
  </div>
</template>