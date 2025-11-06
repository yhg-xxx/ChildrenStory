import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'
import locale from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 定义全局基础URL常量
export const BASE_URL = 'http://localhost:8080';

// 全局配置 axios
axios.defaults.baseURL = BASE_URL;
// 全局配置 axios 携带 Cookie
axios.defaults.withCredentials = true;

const app = createApp(App)

// 使用插件
app.use(ElementPlus, { locale })
app.use(router)

// 注册全局属性
app.config.globalProperties.$axios = axios;
app.config.globalProperties.$baseUrl = BASE_URL;

// 注册图标组件
for(const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 挂载应用
app.mount('#app')