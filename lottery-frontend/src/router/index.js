import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/',           component: () => import('@/views/Home.vue'),         meta: { title: '首页' } },
  { path: '/ssq-spiral', component: () => import('@/views/SsqSpiral.vue'),    meta: { title: '双色球螺旋选号' } },
  { path: '/ssq-history',component: () => import('@/views/SsqHistory.vue'),   meta: { title: '双色球开奖历史' } },
  { path: '/ssq-match',  component: () => import('@/views/SsqMatch.vue'),     meta: { title: '中奖查询/分析' } },
  { path: '/ssq-statistics', component: () => import('@/views/SsqStatistics.vue'), meta: { title: '双色球统计分析' } },
  { path: '/kl8',        component: () => import('@/views/Kl8Pick.vue'),      meta: { title: '快乐8选号' } },
  { path: '/kl8-statistics', component: () => import('@/views/Kl8Statistics.vue'), meta: { title: '快乐8统计分析' } },
  { path: '/my-bets',    component: () => import('@/views/MyBets.vue'),       meta: { title: '我的投注记录' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.afterEach((to) => {
  if (to.meta?.title) document.title = to.meta.title + ' - 选号系统'
})

export default router
