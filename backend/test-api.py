# -*- coding: utf-8 -*-
"""心理健康助手后端接口全面验证脚本（Python/urllib）"""
import json
import urllib.request
import urllib.parse
import uuid
import io

BASE = "http://127.0.0.1:1236"


def req(method, path, data=None, token=None, headers=None, raw=False):
    url = BASE + path
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    if token:
        h["token"] = token
    body = None
    if data is not None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
    r = urllib.request.Request(url, data=body, method=method, headers=h)
    with urllib.request.urlopen(r, timeout=30) as resp:
        content = resp.read()
        if raw:
            return resp.status, content
        return resp.status, json.loads(content.decode("utf-8"))


def show(name, res):
    if isinstance(res, tuple):
        status, obj = res
        code = obj.get("code", "n/a")
        data = obj.get("data")
        summary = json.dumps(data, ensure_ascii=False)[:400] if data is not None else "(null)"
        print(f"[{code}] {name} -> {summary}")
    else:
        print(f"[?] {name} -> {res}")


print("========== 1. 认证/用户 ==========")
_, login = req("POST", "/api/user/login", {"username": "demo", "password": "123456"})
show("登录(demo)", (_, login))
token = login["data"]["token"]
print("token len:", len(token))

show("当前用户", req("GET", "/api/user/current", token=token))
show("登出", req("POST", "/api/user/logout", token=token))

print("========== 2. 知识库（公开） ==========")
show("分类树", req("GET", "/api/knowledge/category/tree"))
show("前台文章分页", req("GET", "/api/knowledge/article/page?currentPage=1&size=10&sortField=publishedAt&sortDirection=desc"))
show("文章详情(阅读+1)", req("GET", "/api/knowledge/article/1"))

print("========== 3. 文章管理（管理员） ==========")
_, created = req("POST", "/api/knowledge/article", {
    "title": "测试文章-临时", "categoryId": 1, "summary": "临时测试",
    "content": "<p>测试内容</p>", "tags": "测试", "status": 0,
}, token=token)
show("创建文章(草稿)", (_, created))
new_id = created["data"]["id"]
show("更新文章", req("PUT", f"/api/knowledge/article/{new_id}", {
    "title": "测试文章-更新", "categoryId": 1, "summary": "临时测试",
    "content": "<p>更新内容</p>", "tags": "测试", "status": 0,
}, token=token))
show("发布文章", req("PUT", f"/api/knowledge/article/{new_id}/status", {"status": 1}, token=token))
show("管理员分页(status=0)", req("GET", "/api/knowledge/article/page?currentPage=1&size=10&status=0", token=token))
show("删除文章", req("DELETE", f"/api/knowledge/article/{new_id}", token=token))

print("========== 4. 咨询会话 ==========")
_, sess = req("POST", "/api/psychological-chat/session/start", {
    "sessionTitle": "我的第一次咨询", "initialMessage": "我最近压力很大，总是失眠"
}, token=token)
show("创建会话", (_, sess))
session_id = sess["data"]["sessionId"]
db_id = session_id.replace("session_", "")
show("会话列表", req("GET", "/api/psychological-chat/sessions?currentPage=1&size=10", token=token))
show("会话消息", req("GET", f"/api/psychological-chat/sessions/{db_id}/messages", token=token))
show("会话情绪分析", req("GET", f"/api/psychological-chat/session/{db_id}/emotion", token=token))

print("========== 5. SSE 流式对话（本地兜底） ==========")
try:
    status, raw_body = req("POST", "/api/psychological-chat/stream", {
        "sessionId": session_id, "userMessage": "最近真的很焦虑，怎么办"
    }, token=token, raw=True)
    text = raw_body.decode("utf-8", errors="replace")
    print(f"[SSE] HTTP {status}, 事件数: {text.count('event:')}, 片段: {text[:300]}")
except Exception as e:
    print("SSE 调用异常:", e)

print("========== 6. 情绪日记 ==========")
_, diary = req("POST", "/api/emotion-diary", {
    "diaryDate": "2026-09-03", "moodScore": 4, "dominantEmotion": "焦虑",
    "emotionTriggers": "考试临近", "diaryContent": "今天复习很焦虑",
    "sleepQuality": 6, "stressLevel": 4,
}, token=token)
show("新增情绪日记", (_, diary))
_, page = req("GET", "/api/emotion-diary/admin/page?currentPage=1&size=10", token=token)
show("情绪日记分页", (_, page))
diary_id = page["data"]["records"][0]["id"]
show("删除情绪日记", req("DELETE", f"/api/emotion-diary/admin/{diary_id}", token=token))

print("========== 7. 数据看板 ==========")
show("看板总览", req("GET", "/api/data-analytics/overview", token=token))

print("========== 8. 文件上传（multipart） ==========")
# 构造最小 PNG
png = bytes([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
             0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
             0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
             0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
             0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
             0x54, 0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
             0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00,
             0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82])
boundary = "----testboundary" + uuid.uuid4().hex
parts = []
parts.append(f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n".encode())
parts.append(png)
parts.append(f"\r\n--{boundary}--\r\n".encode())
body = b"".join(parts)
h = {"Content-Type": f"multipart/form-data; boundary={boundary}", "token": token}
r = urllib.request.Request(BASE + "/api/file/upload", data=body, method="POST", headers=h)
with urllib.request.urlopen(r, timeout=30) as resp:
    up = json.loads(resp.read().decode("utf-8"))
show("文件上传", (200, up))
file_path = up["data"]["filePath"]
print("filePath:", file_path)
# 验证静态资源
try:
    with urllib.request.urlopen(BASE + file_path, timeout=15) as resp:
        print(f"静态资源访问 {BASE}{file_path} -> HTTP {resp.status}, 字节 {len(resp.read())}")
except Exception as e:
    print("静态资源访问失败:", e)

print("\n========== 全部接口测试完成 ==========")
