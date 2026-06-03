import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import { installSessionTimeout } from '@/utils/sessionTimeout'

const app = createApp(App)

app.use(createPinia())
app.use(router)

installSessionTimeout(router)

app.mount('#app')
