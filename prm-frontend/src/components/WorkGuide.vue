<template>
  <el-button text class="wg-trigger" @click="open = true">
    <el-icon :size="17"><Reading /></el-icon>
    <span class="wg-trigger-label">工作指引</span>
  </el-button>

  <el-drawer v-model="open" :size="drawerSize" direction="rtl" class="wg-drawer" :with-header="false">
    <div class="wg-shell">
      <header class="wg-top">
        <div class="wg-titles">
          <h2 class="wg-title">指引中心</h2>
          <div class="wg-sub">{{ docMeta.sub }}</div>
        </div>
        <div class="wg-actions">
          <el-input v-model="q" placeholder="搜索关键词" clearable size="small" class="wg-search">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button size="small" @click="openPdf" :disabled="!docMeta.pdf"><el-icon><Document /></el-icon> 原文</el-button>
          <el-button size="small" text @click="open = false"><el-icon><Close /></el-icon></el-button>
        </div>
      </header>

      <div class="wg-docbar">
        <el-radio-group v-model="doc" size="small">
          <el-radio-button v-for="d in DOCS" :key="d.key" :value="d.key">{{ d.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <div class="wg-body">
        <nav class="wg-toc">
          <a
            v-for="e in shown" :key="e.id"
            class="wg-toc-item" :class="['lv' + (e.level || 1), { active: activeId === e.id }]"
            @click="scrollTo(e.id)"
          >{{ e.title }}</a>
          <div v-if="loading" class="wg-nomatch">加载中…</div>
          <div v-else-if="shown.length === 0" class="wg-nomatch">无匹配内容</div>
        </nav>

        <article ref="contentRef" class="wg-content" @scroll="onScroll">
          <section v-for="e in shown" :key="e.id" :id="'wg-' + e.id" class="wg-sec">
            <!-- 散文(工作指引) -->
            <template v-if="e.kind === 'prose'">
              <component :is="e.level === 1 ? 'h3' : 'h4'" class="wg-h" :class="'h' + (e.level || 1)">{{ e.title }}</component>
              <p v-for="(p, i) in e.body" :key="i" class="wg-p" :class="{ 'wg-li': /^[0-9（(]/.test(p) }" v-html="hi(p)"></p>
            </template>

            <!-- 表单(附件C) -->
            <template v-else-if="e.kind === 'form'">
              <h3 class="wg-h h1">{{ e.title }}</h3>
              <div class="wg-tablewrap">
                <table class="wg-table">
                  <tr v-for="(row, ri) in e.rows" :key="ri">
                    <component :is="ri === 0 ? 'th' : 'td'" v-for="(c, ci) in row" :key="ci" v-html="hi(c)"></component>
                  </tr>
                </table>
              </div>
            </template>

            <!-- 材料(确权/授权指引,实时) -->
            <template v-else>
              <h3 class="wg-h h1">{{ e.title }}</h3>
              <div class="wg-mtags">
                <el-tag v-if="e.type" size="small" effect="plain">{{ e.type }}</el-tag>
                <el-tag v-if="e.version" size="small" type="info" effect="plain">{{ e.version }}</el-tag>
                <el-tag v-if="e.status" size="small" type="success" effect="plain">{{ e.status }}</el-tag>
                <span class="wg-mpub">{{ e.publisher }}<template v-if="e.date"> · {{ e.date }}</template></span>
              </div>
              <p v-if="e.content" class="wg-p" v-html="hi(e.content)"></p>
              <p v-else class="wg-p wg-muted">（本指引为附件材料,正文从略）</p>
              <a v-if="e.fileUrl" class="wg-file" :href="e.fileUrl" target="_blank"><el-icon><Download /></el-icon> 下载附件</a>
            </template>
          </section>

          <div v-if="doc === 'guide'" class="wg-footnote">
            附录D《数据授权运营协议》示例、附录E 保密承诺函等，请<a @click="openPdf">查看原文 PDF →</a>
          </div>
          <div v-else-if="doc === 'confirm'" class="wg-footnote">
            本页为只读查阅。维护（新增 / 删除指引材料）请至 <b>数据确权管理 › 确权指引管理</b>。
          </div>
          <div v-else-if="doc === 'auth'" class="wg-footnote">
            本页为只读查阅。维护（新增 / 启停指引材料）请至 <b>数据授权管理 › 授权指引管理</b>。
          </div>
        </article>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { GUIDE_META, GUIDE_SECTIONS, GUIDE_FORMS } from '@/assets/workGuide.js'
import { pageGuidance } from '@/api/confirm'
import { pageCatalog } from '@/api/authorize'

const DOCS = [
  { key: 'guide', label: '工作指引' },
  { key: 'forms', label: '流程表单' },
  { key: 'confirm', label: '确权指引' },
  { key: 'auth', label: '授权指引' }
]

const open = ref(false)
const doc = ref('guide')
const q = ref('')
const activeId = ref('')
const contentRef = ref(null)
const loading = ref(false)
const confirmItems = ref(null)   // null=未加载
const authItems = ref(null)

const drawerSize = computed(() => (window.innerWidth < 1100 ? '94%' : '940px'))

const docMeta = computed(() => {
  if (doc.value === 'guide') return { sub: `${GUIDE_META.doc} · ${GUIDE_META.issuer}`, pdf: GUIDE_META.pdf }
  if (doc.value === 'forms') return { sub: '附件C 数据确权授权流程表单（表1–表6）', pdf: GUIDE_META.pdf }
  if (doc.value === 'confirm') return { sub: '数据确权操作指引材料（实时）', pdf: '' }
  return { sub: '数据授权操作指引材料（实时）', pdf: '' }
})

// 各文档归一化为统一条目
const entries = computed(() => {
  if (doc.value === 'guide') {
    return GUIDE_SECTIONS.map((s) => ({ ...s, kind: 'prose' }))
  }
  if (doc.value === 'forms') {
    return GUIDE_FORMS.map((f) => ({ id: f.id, level: 1, title: f.title, kind: 'form', rows: f.rows }))
  }
  const src = doc.value === 'confirm' ? confirmItems.value : authItems.value
  return (src || []).map((r, i) => ({
    id: 'm' + i, level: 1, kind: 'material',
    title: r.title || r.name,
    type: r.guidanceType || r.itemType,
    version: r.version,
    status: r.status,
    publisher: r.publisher,
    date: (r.publishDate || '').slice(0, 10),
    content: r.content,
    fileUrl: r.fileUrl
  }))
})

const shown = computed(() => {
  const kw = q.value.trim()
  if (!kw) return entries.value
  return entries.value.filter((e) =>
    (e.title && e.title.includes(kw)) ||
    (e.body && e.body.some((p) => p.includes(kw))) ||
    (e.content && e.content.includes(kw)) ||
    (e.rows && e.rows.some((r) => r.some((c) => c.includes(kw))))
  )
})

function hi(p) {
  const safe = String(p ?? '').replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]))
  const kw = q.value.trim()
  return kw ? safe.replaceAll(kw, `<mark>${kw}</mark>`) : safe
}

async function loadDoc(key) {
  if (key === 'confirm' && confirmItems.value === null) {
    loading.value = true
    try { confirmItems.value = (await pageGuidance({ current: 1, size: 100 }))?.records || [] }
    finally { loading.value = false }
  } else if (key === 'auth' && authItems.value === null) {
    loading.value = true
    try { authItems.value = (await pageCatalog({ category: 'GUIDANCE', current: 1, size: 100 }))?.records || [] }
    finally { loading.value = false }
  }
}

watch(doc, (k) => { q.value = ''; activeId.value = entries.value[0]?.id || ''; loadDoc(k); resetScroll() })
watch(open, (v) => { if (v) { activeId.value = entries.value[0]?.id || ''; loadDoc(doc.value) } })

function resetScroll() { nextTick(() => contentRef.value && contentRef.value.scrollTo({ top: 0 })) }

function scrollTo(id) {
  activeId.value = id
  nextTick(() => {
    const el = document.getElementById('wg-' + id)
    if (el && contentRef.value) contentRef.value.scrollTo({ top: el.offsetTop - 8, behavior: 'smooth' })
  })
}

function onScroll() {
  const root = contentRef.value
  if (!root) return
  const top = root.scrollTop + 16
  let cur = shown.value[0]?.id
  for (const e of shown.value) {
    const el = document.getElementById('wg-' + e.id)
    if (el && el.offsetTop <= top) cur = e.id
  }
  if (cur) activeId.value = cur
}

function openPdf() { if (docMeta.value.pdf) window.open(docMeta.value.pdf, '_blank') }
</script>

<style scoped>
.wg-trigger { color: #71717a; padding: 6px 8px; }
.wg-trigger:hover { color: var(--prm-color-primary, #1e87f0); }
.wg-trigger-label { margin-left: 4px; font-size: 13px; }
</style>

<style>
.wg-drawer .el-drawer__body { padding: 0; }
.wg-shell { height: 100%; display: flex; flex-direction: column; background: #fff; }
.wg-top {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;
  padding: 16px 22px 10px; background: linear-gradient(180deg, #f7f9ff, #fff);
}
.wg-title { margin: 0; font-size: 17px; font-weight: 700; color: #1f2329; }
.wg-sub { margin-top: 4px; font-size: 12px; color: #8a93a6; }
.wg-actions { display: flex; align-items: center; gap: 8px; flex: none; }
.wg-search { width: 170px; }
.wg-docbar { padding: 0 22px 12px; border-bottom: 1px solid #eef0f3; }
.wg-body { flex: 1; min-height: 0; display: grid; grid-template-columns: 240px 1fr; }
.wg-toc { border-right: 1px solid #eef0f3; padding: 12px 8px; overflow-y: auto; background: #fafbfc; }
.wg-toc-item {
  display: block; padding: 7px 12px; margin: 1px 0; border-radius: 6px;
  font-size: 13px; color: #4a5160; cursor: pointer; line-height: 1.35;
  transition: background 0.15s, color 0.15s; border-left: 3px solid transparent;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.wg-toc-item.lv2 { padding-left: 24px; font-size: 12.5px; color: #6b7280; }
.wg-toc-item:hover { background: #eef3ff; color: #1e87f0; }
.wg-toc-item.active { background: #eaf1ff; color: #1e87f0; font-weight: 600; border-left-color: #1e87f0; }
.wg-nomatch { padding: 16px 12px; color: #aab; font-size: 13px; }
.wg-content { overflow-y: auto; padding: 20px 30px 40px; }
.wg-sec { margin-bottom: 18px; }
.wg-h { color: #1f2329; }
.wg-h.h1 { font-size: 17px; font-weight: 700; margin: 18px 0 10px; padding-bottom: 6px; border-bottom: 2px solid #eaf1ff; }
.wg-h.h2 { font-size: 15px; font-weight: 600; margin: 14px 0 8px; color: #2f5fd0; }
.wg-p { margin: 8px 0; font-size: 13.5px; line-height: 1.8; color: #303540; text-align: justify; }
.wg-li { padding-left: 14px; }
.wg-muted { color: #99a; }
.wg-content mark { background: #fff2a8; padding: 0 2px; border-radius: 2px; }
.wg-tablewrap { overflow-x: auto; margin: 8px 0 4px; }
.wg-table { border-collapse: collapse; font-size: 12px; min-width: 100%; }
.wg-table th, .wg-table td { border: 1px solid #e3e8ef; padding: 6px 9px; text-align: left; vertical-align: top; white-space: nowrap; }
.wg-table th { background: #eef3ff; color: #2f5fd0; font-weight: 600; }
.wg-table tr:nth-child(even) td { background: #fafbfd; }
.wg-mtags { display: flex; align-items: center; gap: 8px; margin: 6px 0 10px; flex-wrap: wrap; }
.wg-mpub { font-size: 12px; color: #8a93a6; }
.wg-file { display: inline-flex; align-items: center; gap: 4px; margin-top: 8px; color: #1e87f0; font-size: 13px; }
.wg-footnote { margin-top: 24px; padding: 12px 16px; background: #f7f9ff; border-radius: 8px; font-size: 12.5px; color: #6b7280; line-height: 1.7; }
.wg-footnote a { color: #1e87f0; cursor: pointer; }
</style>
