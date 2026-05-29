<script setup lang="ts">
import ThemeToggle from '@/components/ThemeToggle.vue'
</script>

<template>
  <router-view />
  <ThemeToggle />
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  min-height: 100vh;
  font-family: 'Noto Sans SC', sans-serif;
}
</style>
