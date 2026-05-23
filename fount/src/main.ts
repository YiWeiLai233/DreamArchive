import { createApp } from 'vue'
import { createPinia } from 'pinia'
import axios from 'axios'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import { attachAuthHeader } from '@/utils/authHeader'
import { installSessionTimeout } from '@/utils/sessionTimeout'

axios.interceptors.request.use(attachAuthHeader)

const app = createApp(App)

app.use(createPinia())
app.use(router)

installSessionTimeout(router)

app.mount('#app')
