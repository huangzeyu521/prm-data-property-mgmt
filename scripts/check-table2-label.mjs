// 定向复测:确权向导勾选 J 暴露"表2"区,断言 来源权益限制摘要/信息识别关联主体 单行,并截图取证
const listResp = await fetch('http://localhost:9222/json/list')
const page = (await listResp.json()).find((t) => t.type === 'page')
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
await send('Page.navigate', { url: 'http://localhost:5173/dpr/confirm/wizard' })
await new Promise((r) => setTimeout(r, 2500))

// 勾选 J 其他第三方协议 → needTable2 为真,表2 区出现
await evalJs(`(() => {
  const cb = [...document.querySelectorAll('.el-checkbox')].find((c) => c.textContent.includes('其他第三方协议'))
  if (cb) cb.querySelector('input')?.click()
  return !!cb
})()`)
await new Promise((r) => setTimeout(r, 800))

const raw = await evalJs(`(() => {
  const items = [...document.querySelectorAll('.el-form-item__label')]
  const targets = items.filter((el) => /来源权益限制摘要|信息识别关联主体|来源主体名称|权益风险说明/.test(el.textContent))
  return JSON.stringify(targets.map((el) => {
    const r = el.getBoundingClientRect()
    return { label: el.textContent.trim(), w: Math.round(r.width), h: Math.round(r.height), overflow: el.scrollWidth - el.clientWidth }
  }))
})()`)
const rows = JSON.parse(raw || '[]')
let fail = rows.length < 4
for (const it of rows) {
  const singleLine = it.h <= 36 && it.overflow <= 1
  if (!singleLine) fail = true
  console.log(`${singleLine ? 'ok  ' : 'FAIL'} ${it.label}  w=${it.w} h=${it.h} overflow=${it.overflow}`)
}
if (rows.length < 4) console.log(`FAIL: only ${rows.length}/4 表2 labels visible`)

// 截图取证
await evalJs(`(() => { const d = [...document.querySelectorAll('.el-divider')].find((x) => x.textContent.includes('表2')); d?.scrollIntoView({ block: 'start' }); return true })()`)
await new Promise((r) => setTimeout(r, 500))
const shot = await send('Page.captureScreenshot', { format: 'png' })
const { writeFileSync } = await import('node:fs')
writeFileSync('D:/MyProject/PRM/.ui-review/table2-after.png', Buffer.from(shot.result.data, 'base64'))
console.log(fail ? 'RESULT: FAIL' : 'RESULT: PASS (screenshot .ui-review/table2-after.png)')
ws.close()
process.exit(fail ? 1 : 0)
