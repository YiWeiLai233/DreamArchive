<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'
useTheme()
</script>

<template>
  <router-view />
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  min-height: 100vh;
  font-family: 'Noto Sans SC', sans-serif;
}
</style>
