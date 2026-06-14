// 全路由错误巡检:JS exception / console.error / 失败请求(HTTP>=400 或网络错误) / 标签折行
const ROUTES = [
  '/dpr/workbench/todo',
  '/dpr/ledger/overview', '/dpr/ledger/archive', '/dpr/ledger/dataset', '/dpr/ledger/change', '/dpr/ledger/statistics',
  '/dpr/monitor/status', '/dpr/monitor/alert', '/dpr/monitor/compliance', '/dpr/monitor/rule', '/dpr/monitor/notification',
  '/dpr/confirm/wizard', '/dpr/confirm/guidance', '/dpr/confirm/catalog', '/dpr/confirm/history', '/dpr/confirm/review',
  '/dpr/confirm/card', '/dpr/confirm/cert',
  '/dpr/auth/wizard', '/dpr/auth/batch-wizard', '/dpr/auth/filing', '/dpr/auth/guidance', '/dpr/auth/form-template',
  '/dpr/auth/scenario', '/dpr/auth/batch-list', '/dpr/auth/compliance', '/dpr/auth/history', '/dpr/auth/review',
  '/dpr/auth/agreement-template', '/dpr/auth/agreement-seal', '/dpr/auth/agreement-review', '/dpr/auth/agreement-archive',
  '/dpr/auth/cert', '/dpr/auth/cert-template',
  '/dpr/dashboard/overview', '/dpr/dashboard/confirm', '/dpr/dashboard/auth',
  '/aitool/material', '/aitool/conflict', '/aitool/decision',
]

const page = (await (await fetch('http://localhost:9222/json/list')).json()).find((t) => t.type === 'page')
const ws = new WebSocket(page.webSocketDebuggerUrl)
let seq = 0
const pending = new Map()
const events = []
ws.onmessage = (ev) => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); return }
  if (msg.method === 'Runtime.exceptionThrown') events.push({ kind: 'exception', detail: msg.params.exceptionDetails?.exception?.description || msg.params.exceptionDetails?.text })
  if (msg.method === 'Runtime.consoleAPICalled' && msg.params.type === 'error') events.push({ kind: 'console.error', detail: (msg.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ').slice(0, 300) })
  if (msg.method === 'Network.responseReceived' && msg.params.response.status >= 400) events.push({ kind: 'http' + msg.params.response.status, detail: msg.params.response.url })
  if (msg.method === 'Network.loadingFailed' && !msg.params.canceled) events.push({ kind: 'net-fail', detail: msg.params.errorText })
}
const send = (method, params = {}) => new Promise((res) => { const id = ++seq; pending.set(id, res); ws.send(JSON.stringify({ id, method, params })) })
const evalJs = async (e) => (await send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true })).result?.result?.value
await new Promise((r) => (ws.onopen = r))
await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable')

// 登录守卫已启用:先以超管登录取 token,写入 localStorage(否则所有路由都被弹到 /login)
const loginResp = await fetch('http://localhost:5173/api/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'super', password: 'Prm@1234' }),
})
const loginJson = await loginResp.json().catch(() => ({}))
const token = loginJson?.data?.token || ''
await send('Page.navigate', { url: 'http://localhost:5173/login' })
await new Promise((r) => setTimeout(r, 1500))
await evalJs(`localStorage.setItem('prm-token','${token}'); localStorage.setItem('prm-role','all'); localStorage.setItem('X-User-Id','super'); localStorage.setItem('X-User-Roles','all')`)

let totalIssues = 0
for (const route of ROUTES) {
  events.length = 0
  await send('Page.navigate', { url: 'http://localhost:5173' + route })
  await new Promise((r) => setTimeout(r, 2200))
  const wrap = await evalJs(`(()=>{const ls=[...document.querySelectorAll('.el-form-item__label')];return ls.filter(el=>el.getBoundingClientRect().height>36||el.scrollWidth>el.clientWidth+1).length})()`)
  const issues = events.filter((e) => !/favicon/.test(e.detail || ''))
  if (wrap > 0) issues.push({ kind: 'label-wrap', detail: `${wrap} wrapped labels` })
  if (issues.length) {
    totalIssues += issues.length
    console.log(`FAIL ${route}`)
    for (const i of issues.slice(0, 5)) console.log(`   [${i.kind}] ${i.detail}`)
  } else console.log(`ok   ${route}`)
}
console.log(totalIssues ? `RESULT: ${totalIssues} issues` : 'RESULT: clean sweep')
ws.close()
process.exit(totalIssues ? 1 : 0)
