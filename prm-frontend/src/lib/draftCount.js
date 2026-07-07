// 确权草稿数(菜单徽标 + 提醒"有未完成的活"):服务端 status='草稿' 且本人 + 本地未同步 'new' 草稿。
// 轻量共享单例:App.vue 在路由切换/挂载时刷新;向导/草稿箱增删草稿后 dispatch('prm-draft-changed') 触发刷新。
import { ref } from 'vue'
import { pageConfirmApply } from '@/api/confirm'
import { pageAuthApply, pageBatchList } from '@/api/authorize'
import { currentUser } from '@/api/auth'
import { listLocalDrafts } from '@/composables/useDraftAutosave'

export const confirmDraftCount = ref(0)
export const authDraftCount = ref(0)
const me = () => (currentUser() && currentUser().userId) || ''

function localNewCount() {
  return [...listLocalDrafts('confirm-initial', me()), ...listLocalDrafts('confirm-change', me())]
    .filter((d) => d.id === 'new' && (d.form?.systemName || (d.tableItems || []).length || d.form?.rightHolder)).length
}

let inflight = false
export async function refreshConfirmDraftCount() {
  if (inflight) return
  inflight = true
  let server = 0
  try {
    // 注意:/page 端点不接受 status 入参(多传字段会 400),状态一律客户端过滤(同 MyApplications)
    const res = await pageConfirmApply({ pageNum: 1, pageSize: 100 })
    server = (res.records || []).filter((r) => (!me() || r.creatorId === me()) && r.status === '草稿').length
  } catch { /* 保留上次值,不清零 */ } finally { inflight = false }
  confirmDraftCount.value = server + localNewCount()
}

// ===== 授权草稿数:一事一议(按 formNo 分组的草稿)+ 批量(草案清单)+ 本地未同步 =====
function localAuthNewCount() {
  return [...listLocalDrafts('auth-special', me()), ...listLocalDrafts('auth-batch', me())]
    .filter((d) => d.id === 'new' && (d.listForm?.granteeOrg || d.pendingAsset || (d.items || []).length || d.listForm?.listYear)).length
}
let authInflight = false
export async function refreshAuthDraftCount() {
  if (authInflight) return
  authInflight = true
  let special = 0, batch = 0
  try {
    // /page 端点不接受 status 入参(多传字段会 400),状态一律客户端过滤
    const res = await pageAuthApply({ pageNum: 1, pageSize: 100 })
    const mine = (res.records || []).filter((r) => (!me() || r.creatorId === me()) && r.status === '草稿')
    special = new Set(mine.filter((r) => r.authMode === '一事一议' && r.formNo).map((r) => r.formNo)).size
  } catch { /* 保留上次值 */ }
  try {
    // pageBatchList 忽略 status 入参;按 listStatus 客户端过滤草案
    const res2 = await pageBatchList({ pageNum: 1, pageSize: 100 })
    batch = (res2.records || []).filter((r) => (!me() || r.creatorId === me()) && r.listStatus === '草案').length
  } catch { /* 保留上次值 */ } finally { authInflight = false }
  authDraftCount.value = special + batch + localAuthNewCount()
}

// 草稿增删事件(向导保存/提交/删除、草稿箱删除)→ 通知徽标刷新
export function notifyDraftChanged() {
  try { window.dispatchEvent(new Event('prm-draft-changed')) } catch { /* ignore */ }
}
