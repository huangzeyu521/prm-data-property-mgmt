// 标签折行巡检:逐路由打开页面,断言所有 .el-form-item__label 单行(高压线"标签不可折行")
// 用法: node scripts/check-label-wrap.mjs
const ROUTES = [
  '/dpr/confirm/wizard',
  '/dpr/auth/wizard',
  '/dpr/auth/batch-wizard',
  '/dpr/auth/apply',
  '/dpr/auth/scenario',
  '/dpr/auth/cert-template',
  '/dpr/auth/filing',
  '/aitool/material',
  '/aitool/conflict',
  '/dpr/monitor/alert',
  '/dpr/monitor/rule',
]

const listResp = await fetch('http://localhost:9222/json/list')
const targets = await listResp.json()
const page = targets.find((t) => t.type === 'page')
const ws = new WebSocket(page.webSocketDebuggerUrl)
let seq = 0
const pending = new Map()
ws.onmessage = (ev) => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) {
    pending.get(msg.id)(msg)
    pending.delete(msg.id)
  }
}
const send = (method, params = {}) =>
  new Promise((resolve) => {
    const id = ++seq
    pending.set(id, resolve)
    ws.send(JSON.stringify({ id, method, params }))
  })
const evalJs = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  return r.result?.result?.value
}
await new Promise((resolve) => (ws.onopen = resolve))
await send('Page.enable')

const CHECK = `(() => {
  const labels = [...document.querySelectorAll('.el-form-item__label')]
  const bad = []
  for (const el of labels) {
    const cs = getComputedStyle(el)
    const lineH = parseFloat(cs.lineHeight) || 22
    const rect = el.getBoundingClientRect()
    if (rect.height > lineH * 1.6) bad.push({ label: el.textContent.trim(), h: Math.round(rect.height) })
    if (el.scrollWidth > el.clientWidth + 1) bad.push({ label: el.textContent.trim(), overflow: el.scrollWidth - el.clientWidth })
  }
  return JSON.stringify({ total: labels.length, bad })
})()`

let fail = 0
for (const route of ROUTES) {
  await send('Page.navigate', { url: 'http://localhost:5173' + route })
  await new Promise((r) => setTimeout(r, 2500))
  // 向导/列表页的表单可能藏在"新增"对话框里:尝试点开首个新增类按钮
  await evalJs(`(() => {
    const btn = [...document.querySelectorAll('button')].find((b) => /新增|新建|创建|登记/.test(b.textContent))
    if (btn) btn.click()
    return true
  })()`)
  await new Promise((r) => setTimeout(r, 800))
  const raw = await evalJs(CHECK)
  const { total, bad } = JSON.parse(raw || '{"total":0,"bad":[]}')
  const status = bad.length ? 'FAIL' : 'ok'
  if (bad.length) fail++
  console.log(`${status}  ${route}  labels=${total}` + (bad.length ? '  bad=' + JSON.stringify(bad) : ''))
}
console.log(fail ? `RESULT: ${fail} route(s) with wrapped/overflowing labels` : 'RESULT: all labels single-line')
ws.close()
process.exit(fail ? 1 : 0)
