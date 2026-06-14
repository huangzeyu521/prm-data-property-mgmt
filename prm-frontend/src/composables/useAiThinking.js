import { reactive, getCurrentInstance, onUnmounted } from 'vue'

/**
 * 大模型等待体验组合式:点击即显(消灭静默间隙) + 已用时计时 + 阶段旁白自动推进。
 * 诚实设计:不展示假百分比;单轮调用按估计节奏推进阶段、停在末阶段直到返回。
 *
 * 用法:
 *   const ai = useAiThinking()
 *   const r = await ai.run(() => aiCall(...), { phases: AI_PHASES.intent, title: '大模型识别意图中' })
 *   // 模板: <AiThinking v-bind="ai.state" />
 */
export function useAiThinking() {
  const state = reactive({
    active: false,
    elapsed: 0,
    phaseIndex: 0,
    phases: [],
    title: '',
  })
  let elapsedTimer = null
  let phaseTimer = null

  function clearTimers() {
    if (elapsedTimer) clearInterval(elapsedTimer)
    if (phaseTimer) clearInterval(phaseTimer)
    elapsedTimer = phaseTimer = null
  }

  function start({ phases = [], title = '大模型处理中', stepMs = 6000, bound = false } = {}) {
    clearTimers()
    state.phases = phases
    state.title = title
    state.phaseIndex = 0
    state.elapsed = 0
    state.active = true
    const startTs = Date.now()
    elapsedTimer = setInterval(() => {
      state.elapsed = Math.floor((Date.now() - startTs) / 1000)
    }, 1000)
    // 单轮调用:按估计节奏自动推进,停在末阶段(不虚标完成),由 stop() 收尾。
    // bound=true(多步管线):阶段由调用方据真实进度 setPhase() 驱动,不自动推进。
    if (!bound) {
      phaseTimer = setInterval(() => {
        if (state.phaseIndex < state.phases.length - 1) state.phaseIndex++
      }, stepMs)
    }
  }

  /** 绑定模式:据真实进度(0–100)映射到阶段下标 */
  function setProgress(pct) {
    const n = state.phases.length
    if (n === 0) return
    const idx = Math.min(n - 1, Math.floor((pct / 100) * n))
    state.phaseIndex = Math.max(state.phaseIndex, idx)
  }

  function stop() {
    state.active = false
    clearTimers()
  }

  /** 包裹一次异步 AI 调用:开始即显面板,resolve/reject 后收起 */
  async function run(fn, opts) {
    start(opts)
    try {
      return await fn()
    } finally {
      stop()
    }
  }

  // 组件卸载时清理定时器(防止 AI 调用进行中切走页面导致定时器泄漏/卸载后更新)
  if (getCurrentInstance()) {
    onUnmounted(clearTimers)
  }

  return { state, run, start, stop, setProgress }
}
