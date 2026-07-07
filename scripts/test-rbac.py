# -*- coding: utf-8 -*-
"""RBAC 实测(需 prm.auth.enabled=true 重启 confirm/authorize):
登录各角色 → 用 JWT 调审批端点 → 申报人403 / 审核员200 / 无token401。"""
import json, urllib.request, urllib.parse, sys

C = 'http://localhost:9102/api'
A = 'http://localhost:9103/api'

def call(method, url, body=None, params=None, token=None, timeout=30):
    if params: url += '?' + urllib.parse.urlencode(params)
    data = json.dumps(body, ensure_ascii=False).encode('utf-8') if body is not None else (b'' if method == 'POST' else None)
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read().decode('utf-8') or '{}')
        except Exception: return e.code, {}

def login(username):
    st, res = call('POST', C + '/auth/login', {'username': username, 'password': 'Prm@1234'})
    if res.get('code') == 0:
        return res['data']['token'], res['data']['user']
    return None, None

PASS, FAIL = [], []
def ck(name, cond, extra=''):
    (PASS if cond else FAIL).append(name)
    print(('ok   ' if cond else 'FAIL ') + name + ((' | ' + extra) if (extra and not cond) else ''))

# 登录三角色
t_apply, u_apply = login('apply')
t_review, u_review = login('review')
t_super, u_super = login('super')
ck('登录-申报人', t_apply is not None and u_apply and u_apply.get('role') == 'apply', str(u_apply))
ck('登录-审核员', t_review is not None and u_review and u_review.get('role') == 'review', str(u_review))
ck('登录-超管', t_super is not None and u_super and u_super.get('role') == 'all', str(u_super))
ck('错误密码被拒', login('apply')[0] is not None and call('POST', C + '/auth/login', {'username': 'apply', 'password': 'wrong'})[1].get('code') != 0)

# /me 反映身份
st, me = call('GET', C + '/auth/me', token=t_review)
ck('/me 返回审核员角色', me.get('code') == 0 and me.get('data', {}).get('role') == 'review', str(me))
st, me2 = call('GET', C + '/auth/me')
ck('/me 无token 401', st == 401 or me2.get('code') == 401, 'st=%s' % st)

# 造一个待审批确权单(用超管token,可approve)
st, d = call('POST', C + '/dpr/confirm/apply/draft', {
    'assetId': 'AST-001', 'assetName': 'RBAC测试单', 'rightType': '持有权',
    'subjectOrg': '广东电网', 'deptName': 'D', 'registerType': '首次登记', 'regulated': '非管制',
    'applyMode': '常规确权', 'purpose': 'rbac'}, token=t_super)
aid = d.get('data') if isinstance(d.get('data'), str) else (d.get('data') or {}).get('applyId')
call('POST', C + '/dpr/confirm/apply/%s/submit' % aid, token=t_super)

# 审批端点 RBAC:申报人403 / 无token401 / 审核员200
st_apply, r_apply = call('POST', C + '/dpr/confirm/apply/%s/approve' % aid, token=t_apply)
ck('审批·申报人被拒(403)', r_apply.get('code') == 403, 'code=%s' % r_apply.get('code'))
st_anon, r_anon = call('POST', C + '/dpr/confirm/apply/%s/approve' % aid)
ck('审批·无token被拒(401)', r_anon.get('code') == 401, 'code=%s' % r_anon.get('code'))
st_rev, r_rev = call('POST', C + '/dpr/confirm/apply/%s/approve' % aid, token=t_review)
ck('审批·审核员放行(0)', r_rev.get('code') == 0, 'code=%s msg=%s' % (r_rev.get('code'), r_rev.get('msg')))

# 授权审批端点同样受控
st, ad = call('POST', A + '/dpr/auth/apply/draft', {
    'assetId': 'AST-001', 'assetName': 'RBAC授权', 'equityCardId': 'EC-PRA-0001',
    'granteeOrg': '南网数字', 'rightType': '使用权', 'scope': '全字段', 'applyMode': '一事一议'}, token=t_super)
auth = ad.get('data') if isinstance(ad.get('data'), str) else (ad.get('data') or {}).get('applyId')
call('POST', A + '/dpr/auth/apply/%s/submit' % auth, token=t_super)
sa, ra = call('POST', A + '/dpr/auth/apply/%s/approve' % auth, token=t_apply)
ck('授权审批·申报人被拒(403)', ra.get('code') == 403, 'code=%s' % ra.get('code'))
sr, rr = call('POST', A + '/dpr/auth/apply/%s/approve' % auth, token=t_review)
ck('授权审批·审核员放行(0)', rr.get('code') == 0, 'code=%s' % rr.get('code'))

print('---')
print('RBAC RESULT: pass=%d fail=%d' % (len(PASS), len(FAIL)))
if FAIL: print('FAILED: ' + ', '.join(FAIL))
sys.exit(1 if FAIL else 0)
