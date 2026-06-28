/*
 * 设计令牌守卫(南网《数研院-电脑端用户界面设计规范 V1.0》合规锁)。
 * 扫描全部 .vue 的「模板内联样式 + <style> 块」(跳过 <script>,放行 ECharts/canvas 等 JS 取色),
 * 禁止使用 Element Plus 默认色/被客户否决的橙色等"绕过令牌"的裸 hex,强制改用 src/styles/tokens.css 的语义令牌。
 *
 * 为何不用 stylelint:stylelint 只能解析 <style> 块,看不到模板里的 style="color:#xxx" 内联样式,
 * 而本仓库的偏离大多发生在内联样式。故用本脚本覆盖两处。
 *
 * 用法:node scripts/lint-tokens.mjs  (CI/pre-commit 可直接调用;有违规则退出码 1)
 */
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const SRC = new URL('../src', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')

// 被禁裸 hex → 应改用的令牌(大小写不敏感)
const BANNED = {
  '#909399': '--prm-color-text-weak（次说明文字 #8C8C8C）',
  '#606266': '--prm-color-text-secondary（二级文字 #595959）',
  '#303133': '--prm-color-text（一级文字 #262626）',
  '#c0c4cc': '--prm-color-text-disabled（禁用 #B4B4B4）',
  '#a8abb2': '--prm-color-text-disabled（禁用 #B4B4B4）',
  '#f56c6c': '--prm-color-danger（错误 #E21F0C）',
  '#409eff': '--prm-color-link / --prm-color-primary',
  '#67c23a': '--prm-color-success（成功 #36B21D）',
  '#e6a23c': '--prm-color-warning（警告=金黄 #FFC417）或 --prm-color-link；严禁橙色(客户已否决)',
  '#ff7800': '--prm-color-warning（警告=金黄 #FFC417);严禁橙色',
  '#ff7a00': '--prm-color-warning（警告=金黄 #FFC417);严禁橙色',
}

const SCRIPT_RE = /<script\b[\s\S]*?<\/script>/gi
const HEX_RE = new RegExp(Object.keys(BANNED).join('|'), 'gi')

function walk(dir, acc = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    const st = statSync(p)
    if (st.isDirectory()) walk(p, acc)
    else if (name.endsWith('.vue')) acc.push(p)
  }
  return acc
}

const violations = []
for (const file of walk(SRC)) {
  const src = readFileSync(file, 'utf8')
  // 计算 <script> 区间,命中其中的裸 hex 予以放行(JS/ECharts 取色)
  const spans = []
  let m
  while ((m = SCRIPT_RE.exec(src)) !== null) spans.push([m.index, m.index + m[0].length])
  const inScript = (i) => spans.some(([a, b]) => i >= a && i < b)

  let h
  HEX_RE.lastIndex = 0
  while ((h = HEX_RE.exec(src)) !== null) {
    if (inScript(h.index)) continue
    const line = src.slice(0, h.index).split('\n').length
    const hex = h[0].toLowerCase()
    violations.push({ file: relative(SRC, file), line, hex, hint: BANNED[hex] })
  }
}

if (violations.length) {
  console.error(`\n✗ 设计令牌守卫:发现 ${violations.length} 处绕过令牌的裸色,请改用 tokens.css 语义令牌:\n`)
  for (const v of violations) {
    console.error(`  src/${v.file}:${v.line}  ${v.hex}  →  ${v.hint}`)
  }
  console.error('\n(ECharts/canvas 等 JS 取色不受限——放在 <script> 内即可。)\n')
  process.exit(1)
}
console.log('✓ 设计令牌守卫:全部 .vue 模板/样式未发现绕过令牌的裸色。')
