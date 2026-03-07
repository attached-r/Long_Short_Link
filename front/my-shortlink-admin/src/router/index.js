import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 暂时不定义任何路由，App.vue 就是首页
  ]
})

export default router