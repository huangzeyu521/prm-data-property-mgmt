# -*- coding: utf-8 -*-
"""E2E 正向全链:确权(草稿→提交→三级审批→制卡) → 授权(草稿→提交→五级→生效发证) → aitool(上传→解析→比对→主张→冲突→决策)
输出 ASCII 化(避免控制台乱码),细节写 /tmp/e2e_detail.txt"""
import json, time, urllib.request, urllib.parse, sys, io

DETAIL = io.StringIO()
def call(method, url, body=None, params=None, timeout=120):
    if params: url += '?' + urllib.parse.urlencode(params)
    data = json.dumps(body, ensure_ascii=False).encode('utf-8') if body is not None else (b'' if method == 'POST' else None)
    req = urllib.request.Request(url, data=data, method=method,
        headers={'Content-Type': 'application/json', 'X-User-Id': 'e2e-bot', 'X-User-Name': 'e2e-bot'})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            res = json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        res = {'code': e.code, 'msg': 'HTTP %s' % e.code, 'data': None}
    DETAIL.write('%s %s -> %s\n' % (method, url, json.dumps(res, ensure_ascii=False)[:500]))
    return res

PASS, FAIL = [], []
def check(name, cond, extra=''):
    (PASS if cond else FAIL).append(name)
    print(('ok   ' if cond else 'FAIL ') + name + ((' | ' + extra) if (extra and not cond) else ''))

C = 'http://localhost:9102/api'
A = 'http://localhost:9103/api'

# ============ 1. 确权链 ============
ts = time.strftime('%H%M%S')
draft = call('POST', C + '/dpr/confirm/apply/draft', {
    'assetId': 'AST-002', 'assetName': 'E2E资产-' + ts, 'rightType': '数据资源持有权',
    'subjectOrg': '广东电网有限责任公司', 'deptName': '数字化部', 'sysOwner': 'E2E机器人',
    'contact': '13800000000', 'registerType': '首次登记', 'regulated': '非管制',
    'applyMode': '常规确权', 'purpose': 'E2E全链测试'})
check('confirm.draft', draft.get('code') == 0, str(draft)[:160])
d=draft.get('data'); apply_id = d if isinstance(d,str) else (d or {}).get('applyId')
sub = call('POST', C + '/dpr/confirm/apply/%s/submit' % apply_id)
check('confirm.submit', sub.get('code') == 0, str(sub)[:160])
status = None
for i in range(4):
    ap = call('POST', C + '/dpr/confirm/apply/%s/approve' % apply_id)
    pg = call('POST', C + '/dpr/confirm/apply/page', {'pageNum': 1, 'pageSize': 5, 'applyId': apply_id})
    rows = (pg.get('data') or {}).get('records') or []
    row = next((r for r in rows if r.get('applyId') == apply_id), None)
    status = row and (row.get('status') or row.get('applyStatus'))
    if status == u'已完成': break
check('confirm.approve-to-done', status == u'已完成', 'status=%s' % status)
cards = call('POST', C + '/dpr/confirm/card/page', {'pageNum': 1, 'pageSize': 10, 'applyId': apply_id})
recs = (cards.get('data') or {}).get('records') or []
card = next((r for r in recs if r.get('applyId') == apply_id), None)
check('confirm.card-generated', card is not None and card.get('cardStatus') in (u'正常', u'生效'),
      'cards=%d' % len(recs))
card_id = card and card.get('cardId'); card_no = card and card.get('cardNo')

# ============ 2. 授权链(先确后授:用刚制的卡) ============
adraft = call('POST', A + '/dpr/auth/apply/draft', {
    'assetId': 'AST-002', 'assetName': 'E2E资产-' + ts, 'equityCardId': card_no or card_id,
    'granteeOrg': '南网数字集团', 'rightType': '数据加工使用权', 'scope': '全字段',
    'sceneName': '电力金融征信', 'purpose': 'E2E链路验证', 'applyMode': '一事一议',
    'bizDomain': '市场营销', 'supervisor': 'E2E主管', 'contact': '13800000001'})
check('auth.draft', adraft.get('code') == 0, str(adraft)[:200])
ad=adraft.get('data'); auth_id = ad if isinstance(ad,str) else (ad or {}).get('applyId')
asub = call('POST', A + '/dpr/auth/apply/%s/submit' % auth_id)
check('auth.submit', asub.get('code') == 0, str(asub)[:200])
astatus = None
for i in range(6):
    call('POST', A + '/dpr/auth/apply/%s/approve' % auth_id, params={'opinion': 'E2E-pass-%d' % i})
    apg = call('POST', A + '/dpr/auth/apply/page', {'pageNum': 1, 'pageSize': 5, 'applyId': auth_id})
    arows = (apg.get('data') or {}).get('records') or []
    arow = next((r for r in arows if r.get('applyId') == auth_id), None)
    astatus = arow and (arow.get('status') or arow.get('applyStatus'))
    if astatus == u'已生效': break
check('auth.approve-to-effective', astatus == u'已生效', 'status=%s' % astatus)
acerts = call('POST', A + '/dpr/auth/cert/page', {'pageNum': 1, 'pageSize': 10})
acrecs = (acerts.get('data') or {}).get('records') or []
acert = next((r for r in acrecs if r.get('applyId') == auth_id), None)
check('auth.cert-issued', acert is not None, 'certs=%d' % len(acrecs))

# ============ 3. aitool 链(独立工具,弱关联 applyId) ============
up = call('POST', C + '/dpr/confirm/aitool/material/upload', {
    'fileName': 'E2E-确权证明-%s.pdf' % ts, 'fileType': 'PDF', 'applyId': apply_id,
    'content': u'兹证明E2E资产-%s由广东电网有限责任公司自行生产,权利类型为数据资源持有权,有效期3年,范围全字段,已盖章。' % ts})
check('aitool.upload', up.get('code') == 0, str(up)[:200])
ud=up.get('data'); mat_id = ud if isinstance(ud,str) else (ud or {}).get('materialId')
call('POST', C + '/dpr/confirm/aitool/material/%s/parse' % mat_id)
prog = 0
for i in range(60):
    p = call('GET', C + '/dpr/confirm/aitool/material/%s/progress' % mat_id)
    prog = (p.get('data') or {}).get('progress') or 0
    if prog >= 100: break
    time.sleep(1)
check('aitool.parse-complete', prog >= 100, 'progress=%s' % prog)
pr = call('GET', C + '/dpr/confirm/aitool/material/%s/parse' % mat_id)
prd = pr.get('data') or {}
check('aitool.parse-fields', bool(prd.get('rightSubject')) and bool(prd.get('rightType')),
      json.dumps(prd, ensure_ascii=False)[:200])
cmp_res = call('GET', C + '/dpr/confirm/aitool/material/%s/compares' % mat_id)
check('aitool.compares', cmp_res.get('code') == 0, str(cmp_res)[:160])
clm = call('POST', C + '/dpr/confirm/aitool/conflict/claim-from-material', params={'materialId': mat_id})
check('aitool.claim-from-material', clm.get('code') == 0, str(clm)[:200])
det = call('POST', C + '/dpr/confirm/aitool/conflict/detect', {'assetId': 'AST-002', 'subject': u'南网数字集团', 'rightType': u'数据加工使用权', 'authScope': u'全字段', 'exclusive': 0})
check('aitool.detect', det.get('code') == 0, str(det)[:200])
ana = call('POST', C + '/dpr/confirm/aitool/decision/analyze', params={'applyId': apply_id})
anad = ana.get('data') or {}
check('aitool.decision-analyze', ana.get('code') == 0 and bool(anad.get('prediction') or anad.get('suggestion')),
      json.dumps(anad, ensure_ascii=False)[:200] if ana.get('code') == 0 else str(ana)[:200])

print('---')
print('E2E RESULT: pass=%d fail=%d' % (len(PASS), len(FAIL)))
if FAIL:
    print('FAILED: ' + ', '.join(FAIL))
with open('/tmp/e2e_detail.txt', 'w', encoding='utf-8') as f:
    f.write(DETAIL.getvalue())
sys.exit(1 if FAIL else 0)
