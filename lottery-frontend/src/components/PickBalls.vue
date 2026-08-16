<template>
  <div class="pick-balls-wrapper">
    <div v-if="!hideRed" class="ball-area">
      <div v-if="showTitle" class="title">
        红球 ({{ reds.size }}
        <template v-if="redLimit">/<span :style="{color:reds.size===redLimit?'#ffd666':'#aaa'}">{{redLimit}}</span></template>
        <template v-else-if="redMin">，最少 {{ redMin }}</template>
        )
      </div>
      <div class="ball-grid red-grid">
        <span v-for="n in 33" :key="n"
              :class="['ball', redClass(n)]"
              @click="toggleRed(n)">
          {{ pad(n) }}
        </span>
      </div>
    </div>

    <div v-if="!hideBlue" class="ball-area" style="margin-top:14px">
      <div v-if="showTitle" class="title">
        蓝球 ({{ blues.size }}
        <template v-if="blueLimit">/<span :style="{color:blues.size===blueLimit?'#ffd666':'#aaa'}">{{blueLimit}}</span></template>
        <template v-else-if="blueMin">，最少 {{ blueMin }}</template>
        )
      </div>
      <div class="ball-grid blue-grid">
        <span v-for="n in 16" :key="n"
              :class="['ball', blueClass(n)]"
              @click="toggleBlue(n)">
          {{ pad(n) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  reds:     { type: Set, required: true },
  blues:    { type: Set, required: true },
  redLimit: { type: Number, default: 0 },  // =0 表示不限定上限
  redMin:   { type: Number, default: 0 },
  blueLimit:{ type: Number, default: 0 },
  blueMin:  { type: Number, default: 0 },
  hideRed:  { type: Boolean, default: false },
  hideBlue: { type: Boolean, default: false },
  excludeReds:{ type: Set, default: () => new Set() },
  showTitle:{ type: Boolean, default: true }
})
const emit = defineEmits(['update:reds','update:blues'])
import { ElMessage } from 'element-plus'
const pad = (n) => String(n).padStart(2, '0')

function toggleRed(n) {
  const c = pad(n)
  if (props.excludeReds.has(c)) return ElMessage.warning('该号码已作为胆码使用')
  if (props.reds.has(c)) {
    props.reds.delete(c)
  } else {
    if (props.redLimit && props.reds.size >= props.redLimit) {
      if (props.redLimit === 1) { props.reds.clear(); props.reds.add(c) }
      else { return ElMessage.warning(`红球最多选择 ${props.redLimit} 个`) }
    } else props.reds.add(c)
  }
  emit('update:reds', new Set(props.reds))
}
function toggleBlue(n) {
  const c = pad(n)
  if (props.blues.has(c)) props.blues.delete(c)
  else {
    if (props.blueLimit && props.blues.size >= props.blueLimit) {
      if (props.blueLimit === 1) { props.blues.clear(); props.blues.add(c) }
      else { return ElMessage.warning(`蓝球最多选择 ${props.blueLimit} 个`) }
    } else props.blues.add(c)
  }
  emit('update:blues', new Set(props.blues))
}
function redClass(n) {
  const c = pad(n)
  const sel = props.reds.has(c)
  const ex = props.excludeReds.has(c)
  return ex ? 'gray' : sel ? 'red selected' : 'gray'
}
function blueClass(n) {
  return props.blues.has(pad(n)) ? 'blue selected' : 'gray'
}
</script>

<style lang="scss" scoped>
.title { color: rgba(255,255,255,0.7); font-size: 13px; margin-bottom: 8px; }
.ball-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.ball { width: 36px; height: 36px; }
</style>
