# -*- coding: utf-8 -*-
"""Codex-AI P0 接口冒烟验证脚本"""
import json
import sys
import http.client
import urllib.request
import urllib.error

BASE = "http://localhost:1235"
FAIL = 0

def req(method, path, token=None, body=None):
    global FAIL
    url = BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r, timeout=20) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "ignore")[:300]
        return e.code, {"_http_error": raw}

def check(name, cond, detail=""):
    global FAIL
    mark = "PASS" if cond else "FAIL"
    if not cond:
        FAIL += 1
    print(f"[{mark}] {name} {detail}")

# ---------- 认证 ----------
st, admin = req("POST", "/user/login", body={"username": "admin", "password": "Admin@123"})
check("admin 登录", st == 200 and admin.get("code") == "200" and admin["data"]["token"], "")
at = admin["data"]["token"]

st, demo = req("POST", "/user/login", body={"username": "demo", "password": "Demo@123"})
check("demo 登录", st == 200 and demo.get("code") == "200" and demo["data"]["token"], "")
dt = demo["data"]["token"]

st, bad = req("POST", "/user/login", body={"username": "demo", "password": "wrong"})
check("错误密码被拒", st == 200 and bad.get("code") != "200", bad.get("message", ""))

# ---------- 同意管理 ----------
req("POST", "/consents/revoke", dt)  # 确保演示用户处于未同意干净状态
st, consent0 = req("GET", "/consents/current", dt)
check("同意状态(未同意)", st == 200 and consent0["data"]["complete"] is False, "")

st, consent1 = req("POST", "/consents", dt, {
    "ageConfirmed": True,
    "privacyPolicyVersion": "privacy-v1.0",
    "sensitiveInfoVersion": "sensitive-v1.0",
    "productBoundaryVersion": "boundary-v1.0"
})
check("提交同意", st == 200 and consent1["data"]["complete"] is True, "")

# ---------- 情绪日记 ----------
st, diary = req("POST", "/emotion-diary", dt, {
    "emotionStatus": "焦虑", "score": 5, "event": "冒烟测试：任务堆在一起",
    "sleepStatus": "较差", "energyStatus": "不足"
})
check("日记新增", st == 200 and diary.get("code") == "200", diary.get("message", ""))
st, diaryPage = req("GET", "/emotion-diary/page?page=1&pageSize=10", dt)
check("日记列表", st == 200 and diaryPage["data"]["total"] >= 1, f"total={diaryPage['data']['total']}")
st, trend = req("GET", "/emotion-diary/trend?days=7", dt)
check("日记趋势", st == 200 and trend["data"]["days"] == 7, "")
st, diaryUpd = req("PUT", f"/emotion-diary/{diary['data']['id']}", dt, {
    "emotionStatus": "平静", "score": 7, "event": "已更新"
})
check("日记更新", st == 200 and diaryUpd["data"]["score"] == 7, "")

# ---------- 会话 + 二阶段消息 ----------
st, session = req("POST", "/psychological-chat/session/start", dt, {"title": "冒烟测试会话"})
check("创建会话", st == 200 and session.get("code") == "200", session.get("message", ""))
sid = session["data"]["id"]

st, msg = req("POST", "/psychological-chat/messages", dt, {"sessionId": sid, "content": "我最近考试复习压力很大"})
check("普通消息(风险0)", st == 200 and msg["data"]["riskLevel"] == 0 and msg["data"]["assistantMessageId"], "")
check("普通消息回复非空", bool(msg["data"]["reply"]), "")

st, riskMsg = req("POST", "/psychological-chat/messages", dt, {"sessionId": sid, "content": "我不想活了"})
check("风险消息(危机3)", st == 200 and riskMsg["data"]["riskLevel"] == 3, f"level={riskMsg['data']['riskLevel']}")
check("风险消息含危机卡动作", riskMsg["data"]["actionType"] == "SHOW_CRISIS_CARD", "")
check("风险消息回复为安全引导", "120" in riskMsg["data"]["reply"] or "12356" in riskMsg["data"]["reply"], "")

# ---------- SSE 流式 ----------
import urllib.parse
ast = msg["data"]["assistantMessageId"]
stream_url = f"{BASE}/psychological-chat/stream?sessionId={sid}&assistantMessageId={ast}"
r = urllib.request.Request(stream_url)
r.add_header("Authorization", "Bearer " + dt)
sse_chunks = []
try:
    with urllib.request.urlopen(r, timeout=20) as resp:
        while True:
            chunk = resp.read(4096)
            if not chunk:
                break
            sse_chunks.append(chunk.decode("utf-8", "ignore"))
            if len(b"".join(c.encode() for c in sse_chunks)) > 8000:
                break
except http.client.IncompleteRead as e:
    # Tomcat chunked 结束方式会让 http.client 误报 IncompleteRead，partial 中已有数据
    sse_chunks.append(e.partial.decode("utf-8", "ignore"))
except Exception as e:
    print(f"    [sse read note] {type(e).__name__}: {e}")
sse = "".join(sse_chunks)
has_risk = "event:risk" in sse or '"riskLevel"' in sse
has_delta = "event:delta" in sse
has_done = "event:done" in sse
check("SSE 包含 risk 事件", has_risk, f"len={len(sse)}")
check("SSE 包含 delta 事件", has_delta, "")
check("SSE 包含 done 事件", has_done, "")

# ---------- 对话反馈 ----------
st, fb = req("POST", "/chat-feedback", dt, {"sessionId": sid, "helpfulness": 1, "comment": "有帮助"})
check("提交反馈", st == 200 and fb.get("code") == "200", fb.get("message", ""))

# ---------- 危机资源(公开) ----------
st, cr = req("GET", "/crisis-resources")
check("危机资源公开查询", st == 200 and len(cr["data"]) >= 3, f"count={len(cr['data'])}")

# ---------- 知识库(公开) ----------
st, kb = req("GET", "/knowledge?page=1&pageSize=12")
check("知识库公开列表", st == 200 and kb["data"]["total"] >= 3, f"total={kb['data']['total']}")

# ---------- 管理端(权限) ----------
st, ov = req("GET", "/admin/data-analytics/overview", at)
check("管理端概览(admin)", st == 200 and ov.get("code") == "200", ov.get("message", ""))
st, ov2 = req("GET", "/admin/data-analytics/overview", dt)
check("管理端概览(普通用户被拒)", st != 200 or ov2.get("code") != "200", f"http={st} code={ov2.get('code')}")

st, riskEvents = req("GET", "/admin/risk-events/page?page=1&pageSize=10", at)
check("风险事件中心(admin)", st == 200 and riskEvents.get("code") == "200", "")
check("风险事件已记录", riskEvents["data"]["total"] >= 1, f"total={riskEvents['data']['total']}")

st, audit = req("GET", "/admin/audit-logs/page?page=1&pageSize=10", at)
check("审计日志(admin)", st == 200 and audit.get("code") == "200", "")

st, fbPage = req("GET", "/admin/chat-feedback/page?page=1&pageSize=10", at)
check("反馈列表(admin)", st == 200 and fbPage.get("code") == "200", "")

# ---------- 隐私与数据管理 ----------
st, profile = req("GET", "/user/privacy/profile", dt)
check("隐私概览", st == 200 and profile["data"]["consentComplete"] is True, "")
st, export = req("POST", "/user/privacy/export", dt)
check("数据导出", st == 200 and "content" in export["data"], "")
st, deletion = req("POST", "/user/privacy/deletion-request", dt, {"reason": "冒烟测试"})
check("删除申请提交", st == 200 and deletion["data"]["status"] == "待处理", deletion["data"]["status"])
st, deletionCancel = req("POST", "/user/privacy/deletion-request/cancel", dt)
check("删除申请取消", st == 200 and deletionCancel["data"] is True, "")

# ---------- 撤回同意 ----------
st, revoke = req("POST", "/consents/revoke", dt)
check("撤回同意", st == 200 and revoke["data"]["revoked"] is True, "")

print()
if FAIL:
    print(f"RESULT: {FAIL} 项失败")
    sys.exit(1)
print("RESULT: 全部通过")
