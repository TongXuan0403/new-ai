# -*- coding: utf-8 -*-
"""端到端验证：通过前端代理 5175 模拟浏览器完整用户流程"""
import json
import http.client

FRONT = ("localhost", 5175)  # 前端 dev server（代理后端）


def req(method, path, data=None, token=None, host=FRONT):
    conn = http.client.HTTPConnection(*host, timeout=40)
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    body = json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
    conn.request(method, path, body=body, headers=headers)
    resp = conn.getresponse()
    raw = resp.read()
    conn.close()
    ct = resp.getheader("Content-Type", "")
    if "text/event-stream" in ct:
        return resp.status, raw.decode("utf-8", errors="replace")
    try:
        return resp.status, json.loads(raw.decode("utf-8"))
    except Exception:
        return resp.status, raw.decode("utf-8", errors="replace")


def show(name, status, obj):
    if isinstance(obj, dict):
        code = obj.get("code", "n/a")
        data = obj.get("data")
        print(f"[{status}/{code}] {name}")
        if isinstance(data, dict):
            keys = list(data.keys())[:8]
            print(f"    data字段: {keys}")
        elif isinstance(data, list):
            print(f"    data是数组, 长度 {len(data)}, 首元素: {json.dumps(data[0], ensure_ascii=False)[:200] if data else '空'}")
        else:
            print(f"    data: {str(data)[:200]}")
    else:
        print(f"[{status}] {name} -> {str(obj)[:200]}")


print("========== 1. 注册新用户 ==========")
st, r = req("POST", "/api/user/add", {
    "username": "zhangsan", "email": "zhangsan@test.com", "nickname": "张三",
    "phone": "13800138000", "password": "123456", "confirmPassword": "123456",
    "gender": 1, "userType": 1,
})
show("注册 zhangsan", st, r)

print("========== 2. 登录两个账号 ==========")
st, r = req("POST", "/api/user/login", {"username": "zhangsan", "password": "123456"})
show("登录 zhangsan", st, r)
user_token = r["data"]["token"] if isinstance(r, dict) and r.get("data") else None
st, r = req("POST", "/api/user/login", {"username": "admin", "password": "123456"})
show("登录 admin", st, r)
admin_token = r["data"]["token"] if isinstance(r, dict) and r.get("data") else None

print("========== 3. 前台知识库 ==========")
st, r = req("GET", "/api/knowledge/article/page?currentPage=1&size=10&sortField=publishedAt&sortDirection=desc")
show("知识列表", st, r)
st, r = req("GET", "/api/knowledge/article/1")
show("知识详情(admin)", st, r)

print("========== 4. 咨询会话 + SSE（zhangsan） ==========")
st, r = req("POST", "/api/psychological-chat/session/start",
            {"sessionTitle": "第一次咨询", "initialMessage": "你好，我最近有点焦虑"}, token=user_token)
show("创建会话", st, r)
sid = r["data"]["sessionId"] if isinstance(r, dict) and r.get("data") else None
db_id = sid.replace("session_", "") if sid else None

st, r = req("GET", f"/api/psychological-chat/sessions/{db_id}/messages", token=user_token)
show("会话消息", st, r)
st, r = req("GET", f"/api/psychological-chat/session/{db_id}/emotion", token=user_token)
show("会话情绪分析", st, r)

print("--- SSE 流式对话（经代理）---")
conn = http.client.HTTPConnection(*FRONT, timeout=60)
payload = json.dumps({"sessionId": sid, "userMessage": "我最近压力很大"}).encode("utf-8")
conn.request("POST", "/api/psychological-chat/stream", body=payload,
             headers={"Content-Type": "application/json", "token": user_token})
resp = conn.getresponse()
print("SSE HTTP:", resp.status, "Content-Type:", resp.getheader("Content-Type"))
lines = []
while True:
    line = resp.readline()
    if not line:
        break
    lines.append(line.decode("utf-8", errors="replace").rstrip())
conn.close()
events = [l for l in lines if l.startswith("event:")]
data_lines = [l for l in lines if l.startswith("data:")]
print(f"SSE 事件数: {len(events)}, data 行数: {len(data_lines)}")
print(f"done 事件: {any('done' in e for e in events)}")
# 拼接回复
frags = []
for l in data_lines:
    if l == "data:{}":
        continue
    try:
        d = json.loads(l[5:])
        if d.get("data") and isinstance(d["data"], dict) and d["data"].get("type") == "normal":
            frags.append(d["data"]["content"])
    except Exception:
        pass
print("回复片段数:", len(frags), "完整内容:", "".join(frags)[:200])

print("========== 5. 会话列表（管理员看全部） ==========")
st, r = req("GET", "/api/psychological-chat/sessions?currentPage=1&size=10", token=admin_token)
show("管理员会话列表", st, r)

print("========== 6. 情绪日记 ==========")
st, r = req("POST", "/api/emotion-diary", {
    "diaryDate": "2026-09-03", "moodScore": 6, "dominantEmotion": "平静",
    "emotionTriggers": "散步", "diaryContent": "今天散步心情不错", "sleepQuality": 8, "stressLevel": 3,
}, token=user_token)
show("新增情绪日记", st, r)
st, r = req("GET", "/api/emotion-diary/admin/page?currentPage=1&size=10", token=admin_token)
show("管理端情绪日记", st, r)

print("========== 7. 文章管理（管理员） ==========")
st, r = req("GET", "/api/knowledge/category/tree", token=admin_token)
show("分类树", st, r)
st, r = req("POST", "/api/knowledge/article", {
    "title": "端到端测试文章", "categoryId": 1, "summary": "e2e",
    "content": "<p>内容</p>", "tags": "测试", "status": 1, "coverImage": "/uploads/2026/09/11e740d9ca06411ea0f020a8efdc8d2e.png",
}, token=admin_token)
show("创建并发布文章", st, r)
aid = r["data"]["id"] if isinstance(r, dict) and r.get("data") else None
if aid:
    st, r = req("DELETE", f"/api/knowledge/article/{aid}", token=admin_token)
    show("删除测试文章", st, r)

print("========== 8. 数据看板 ==========")
st, r = req("GET", "/api/data-analytics/overview", token=admin_token)
show("看板总览", st, r)

print("\n===== 端到端验证完成 =====")
