// 申请草稿·自动保存(防丢)：本地即时 + 可选服务端 debounce 同步。
// 第一性：用户诉求「中断了填写、系统帮忙保存」= 系统自动持久化，而非用户记得点保存。
// 设计：本地(localStorage)即时抗关页/崩溃/离线；达业务底线后 debounce 调服务端(复用既有 saveDraft，applyId 去重)。
// 宽进·静默：绝不校验完成度、绝不推进步骤、绝不弹打扰式提示。

const PREFIX = 'prm-draft'

/** 本地草稿键：scope(域) + userId(归属隔离，私有) + id(applyId 或 'new')。 */
export function localDraftKey(scope, id, userId) {
  return `${PREFIX}:${scope}:${userId || 'anon'}:${id || 'new'}`
}

export function readLocalDraft(key) {
  try {
    const s = localStorage.getItem(key)
    return s ? JSON.parse(s) : null
  } catch { return null }
}

export function removeLocalDraft(key) {
  try { localStorage.removeItem(key) } catch { /* ignore */ }
}

/** 列出某域下本人所有本地草稿(草稿箱用于合并「本地未同步」行)。 */
export function listLocalDrafts(scope, userId) {
  const out = []
  const head = `${PREFIX}:${scope}:${userId || 'anon'}:`
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(head)) {
        const v = readLocalDraft(k)
        if (v) out.push({ key: k, id: k.slice(head.length), ...v })
      }
    }
  } catch { /* ignore */ }
  return out
}

function hhmm(ts) {
  const d = ts ? new Date(ts) : new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

import { ref, onBeforeUnmount } from 'vue'

/**
 * 自动保存钩子。
 * @param {object}   o
 * @param {() => string}  o.getKey       当前本地键(applyId 变化时随之迁移)
 * @param {() => object|null} o.getSnapshot 可序列化快照(返回 null / {__skip:true} 表示无内容不存)
 * @param {() => Promise<void>} [o.serverSync] 服务端同步回调(达底线才由调用方内部放行)
 * @param {() => boolean} [o.canServer] 是否允许本次服务端同步(如底线达成且非提交中)
 * @param {number}  [o.debounceMs=4000]
 */
export function useDraftAutosave({ getKey, getSnapshot, serverSync, canServer, debounceMs = 4000 }) {
  const lastSavedAt = ref('')   // 'HH:mm' 最近本地保存
  const syncing = ref(false)    // 服务端同步进行中
  let timer = null
  let serverBusy = false

  function writeLocal() {
    const key = getKey && getKey()
    if (!key) return false
    const snap = getSnapshot && getSnapshot()
    if (!snap || snap.__skip) return false
    try {
      localStorage.setItem(key, JSON.stringify({ ...snap, __ts: Date.now() }))
      lastSavedAt.value = hhmm()
      return true
    } catch { return false }
  }

  async function runServer() {
    if (!serverSync || serverBusy) return
    if (canServer && !canServer()) return
    serverBusy = true; syncing.value = true
    try { await serverSync() } catch { /* 静默:请求拦截器已处理 */ } finally { serverBusy = false; syncing.value = false }
  }

  /** 表单变化时调用:本地即时写入 + debounce 触发服务端同步。 */
  function schedule({ server = true } = {}) {
    const wrote = writeLocal()
    if (timer) clearTimeout(timer)
    if (server && wrote) timer = setTimeout(runServer, debounceMs)
  }

  /** 关页/卸载 best-effort 同步落本地。 */
  function flush() { writeLocal() }

  /** 清除当前本地草稿(提交成功 / 服务端已接管后调用)。 */
  function clear() {
    const key = getKey && getKey()
    if (key) removeLocalDraft(key)
    if (timer) { clearTimeout(timer); timer = null }
  }

  const onBeforeUnload = () => flush()
  if (typeof window !== 'undefined') window.addEventListener('beforeunload', onBeforeUnload)
  onBeforeUnmount(() => {
    if (typeof window !== 'undefined') window.removeEventListener('beforeunload', onBeforeUnload)
    flush()
  })

  return { lastSavedAt, syncing, schedule, flush, clear }
}
