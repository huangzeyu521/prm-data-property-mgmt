# 前端 E2E 回归门(Playwright · 真浏览器跑活后端)

真浏览器加载/操作前端,捕获 `npm run build` 抓不到的运行时错误(`<script setup>` 未定义引用、事件处理器异常、整页崩溃)。此门此前揪出 `lib/chartBase.js` 的 `initChart` 返回 `undefined`(ECharts `setOption()` 返回 void)致「数据产权全景」「产权台账统计」两页整页崩。

## 前置
1. **后端三服务在跑**(E2E 经 vite 代理直连):`docker start prm-ledger prm-confirm prm-authorize`(9101/9102/9103)。
2. **浏览器**:
   - CI / 干净机器:`npm run e2e:install`(下载 chromium)。
   - 本机已装 chromium(`~/AppData/Local/ms-playwright/chromium-XXXX/...chrome.exe`)想免下载:运行前 `export PW_CHROME="C:/Users/<you>/AppData/Local/ms-playwright/chromium-1217/chrome-win64/chrome.exe"`(config 用 `executablePath` 覆盖捆绑浏览器)。

## 运行
```bash
# vite dev 由 playwright webServer 自动起(5173 被占则复用),登录由 global-setup 自动完成
npm run e2e
# 或本机复用已装浏览器:
PW_CHROME="C:/Users/zeyuh/AppData/Local/ms-playwright/chromium-1217/chrome-win64/chrome.exe" npm run e2e
```

## 用例
- `route-load.spec.js` —— 逐 40+ 路由加载,断言 0 console error / 0 pageerror / 0 API≥400。
- `batch-authorize.spec.js` —— 批量授权一站式:清单头(含联系人/联系方式必填)→ 资源池勾选 → 加入明细 → 校验明细非空 + 跨系统域判定。

## 约定
- 登录:`admin/Prm@1234`(可用 `PW_USER/PW_PASS` 覆盖);会话存 `e2e/.auth/state.json`(已 gitignore)。
- 串行(workers=1):活后端共享 dev H2,避免用例相互污染。
- 后端状态机(提交→审批→制卡/驳回)由后端 311 单测覆盖(`ConfirmFlowTest` 等),并经线上 API 实链验证;E2E 侧重前端加载与关键交互。
