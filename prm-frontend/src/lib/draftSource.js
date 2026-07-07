// 草稿存储来源标记(自动 autosave / 手动 保存草稿·下一步):本地 best-effort,守"零后端"。
// 后端 saveDraft 不区分来源,故用本地 map 按草稿 id(applyId)留痕;跨设备未知时草稿箱回退「—」。
const KEY = 'prm-draft-src'

function readMap() {
  try { return JSON.parse(localStorage.getItem(KEY) || '{}') } catch { return {} }
}
function writeMap(m) {
  try { localStorage.setItem(KEY, JSON.stringify(m)) } catch { /* ignore */ }
}

/** 标记某草稿的最近保存来源。src: 'auto' | 'manual' */
export function markDraftSource(id, src) {
  if (!id) return
  const m = readMap()
  m[id] = { src, ts: Date.now() }
  writeMap(m)
}

export function getDraftSource(id) {
  const e = readMap()[id]
  return e ? e.src : ''
}

/** 草稿提交/删除后清除其来源记录,避免本地 map 无限增长。 */
export function clearDraftSource(id) {
  if (!id) return
  const m = readMap()
  if (m[id]) { delete m[id]; writeMap(m) }
}
