# -*- coding: utf-8 -*-
"""负面用例:先确后授熔断 / 边界约束 / 非法输入 / HTTP 规范化。期望全部被正确拒绝。"""
import json, time, urllib.request, urllib.parse, sys

def call(method, url, body=None, params=None, timeout=60):
    if params: url += '?' + urllib.parse.urlencode(params)
    data = json.dumps(body, ensure_ascii=False).encode('utf-8') if body is not None else (b'' if method in ('POST','PUT') else None)
    req = urllib.request.Request(url, data=data, method=method,
        headers={'Content-Type': 'application/json', 'X-User-Id': 'neg-bot'})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read().decode('utf-8') or '{}')
        except Exception: return e.code, {}

PASS, FAIL = [], []
def check(name, cond, extra=''):
    (PASS if cond else FAIL).append(name)
    print(('ok   ' if cond else 'FAIL ') + name + ((' | ' + extra) if (extra and not cond) else ''))

C = 'http://localhost:9102/api'; A = 'http://localhost:9103/api'
ts = time.strftime('%H%M%S')

def auth_draft(card, scope=u'全字段', right=u'数据加工使用权'):
    st, res = call('POST', A + '/dpr/auth/apply/draft', {
        'assetId': 'AST-001', 'assetName': u'负面用例资产', 'equityCardId': card,
        'granteeOrg': u'南网数字集团', 'rightType': right, 'scope': scope,
        'purpose': u'负面用例', 'applyMode': u'一事一议'})
    return res.get('data') if res.get('code') == 0 else None

# N1 冻结卡 EC-PRA-0003 提交授权 → 熔断拒绝
aid = auth_draft('EC-PRA-0003')
st, res = call('POST', A + '/dpr/auth/apply/%s/submit' % aid)
check('N1.frozen-card-blocked', res.get('code') != 0, str(res)[:150])

# N2 失效卡 EC-PRA-0008 → 拒绝
aid = auth_draft('EC-PRA-0008')
st, res = call('POST', A + '/dpr/auth/apply/%s/submit' % aid)
check('N2.invalid-card-blocked', res.get('code') != 0, str(res)[:150])

# N3 不存在的卡号 → 拒绝
aid = auth_draft('EC-NO-SUCH-CARD')
st, res = call('POST', A + '/dpr/auth/apply/%s/submit' % aid)
check('N3.nonexistent-card-blocked', res.get('code') != 0, str(res)[:150])

# N4 正常卡 EC-PRA-0001 提交 → 应放行(对照组,防止全拒假象)
aid = auth_draft('EC-PRA-0001')
st, res = call('POST', A + '/dpr/auth/apply/%s/submit' % aid)
check('N4.normal-card-allowed', res.get('code') == 0, str(res)[:200])

# N5 不存在申请ID审批 → 报错而非 500 裸异常
st, res = call('POST', C + '/dpr/confirm/apply/NO-SUCH-ID/approve')
check('N5.approve-missing-apply', res.get('code') != 0 and res.get('code') != 999, 'code=%s' % res.get('code'))

# N6 不存在材料解析 → 报错
st, res = call('POST', C + '/dpr/confirm/aitool/material/NO-SUCH/parse')
check('N6.parse-missing-material', res.get('code') != 0, str(res)[:120])

# N7 错误方法 → 405 (不是 999 系统异常)
st, res = call('DELETE', C + '/dpr/confirm/dashboard')
check('N7.method-not-allowed-405', st == 405, 'http=%s code=%s' % (st, res.get('code')))

# N8 不存在路由 → 404
st, res = call('GET', C + '/dpr/confirm/no-such-route')
check('N8.unknown-route-404', st == 404 or res.get('code') == 404, 'http=%s' % st)

# N9 确权草稿缺必填(无资产ID) → 拒绝
st, res = call('POST', C + '/dpr/confirm/apply/draft', {'assetName': u'缺ID', 'purpose': 'x'})
check('N9.draft-missing-required', res.get('code') != 0, str(res)[:150])

# N10 非草稿状态重复提交 → 拒绝(用 N4 的已提交单)
st, res = call('POST', A + '/dpr/auth/apply/%s/submit' % aid)
check('N10.double-submit-blocked', res.get('code') != 0, str(res)[:150])

print('---')
print('NEG RESULT: pass=%d fail=%d' % (len(PASS), len(FAIL)))
if FAIL: print('FAILED: ' + ', '.join(FAIL))
sys.exit(1 if FAIL else 0)
