<template>
  <div class="app-root">
    <div class="app-header">
      <div class="brand">
        <span class="title">选号系统</span>
      </div>
      <el-menu
        :default-active="activeTab"
        mode="horizontal"
        background-color="transparent"
        text-color="#fff"
        active-text-color="#ffd666"
        router
        class="nav-menu"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/ssq-spiral">双色球螺旋选号</el-menu-item>
        <el-menu-item index="/ssq-history">双色球开奖历史</el-menu-item>
        <el-menu-item index="/ssq-match">中奖查询/分析</el-menu-item>
        <el-menu-item index="/ssq-statistics">双色球统计分析</el-menu-item>
        <el-menu-item index="/kl8">快乐8选号</el-menu-item>
        <el-menu-item index="/kl8-history">快乐8开奖历史</el-menu-item>
        <el-menu-item index="/kl8-statistics">快乐8统计分析</el-menu-item>
        <el-menu-item index="/my-bets">我的投注</el-menu-item>
      </el-menu>
    </div>

    <div class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <div class="app-footer">
      <span>© 2026 Lottery Pick System · Spring Boot 3 + MyBatis + MySQL + Vue 3</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const activeTab = computed(() => route.path)
</script>

<style lang="scss" scoped>
.app-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  font-family: 'Microsoft YaHei', Arial, sans-serif;
}
.app-header {
  padding: 18px 40px 0;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  .brand {
    display: flex; align-items: center; gap: 12px; margin-bottom: 12px;
    .logo { font-size: 32px; }
    .title { font-size: 24px; font-weight: bold;
             background: linear-gradient(45deg, #ff6b35, #ffd666);
             -webkit-background-clip: text; -webkit-text-fill-color: transparent;
             text-shadow: 0 0 20px rgba(255, 107, 53, 0.3); }
  }
  .nav-menu {
    border: none !important;
    :deep(.el-menu-item) {
      border-bottom: 2px solid transparent !important;
      &.is-active { border-bottom-color: #ff6b35 !important; }
    }
  }
}
.app-main {
  flex: 1;
  padding: 30px 40px;
}
.app-footer {
  padding: 20px; text-align: center; color: rgba(255,255,255,0.4);
  font-size: 13px; border-top: 1px solid rgba(255,255,255,0.06);
}
.fade-enter-active, .fade-leave-active { transition: opacity .25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
