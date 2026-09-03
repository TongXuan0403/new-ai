# -*- coding: utf-8 -*-
"""SSE 诊断脚本：完整登录+同意+建会话+发消息+流式读取"""
import json
import urllib.request
import http.client

BASE = "http://localhost:1235"

def call(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(r, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))

d = call("POST", "/user/login", body={"username": "demo", "password": "Demo@123"})["data"]
dt = d["token"]
call("POST", "/consents", dt, {
    "ageConfirmed": True, "privacyPolicyVersion": "privacy-v1.0",
    "sensitiveInfoVersion": "sensitive-v1.0", "productBoundaryVersion": "boundary-v1.0"})
s = call("POST", "/psychological-chat/session/start", dt, {"title": "sse诊断"})["data"]
sid = s["id"]
m = call("POST", "/psychological-chat/messages", dt, {"sessionId": sid, "content": "我最近有点紧张"})["data"]
ast = m["assistantMessageId"]
print("sid =", sid, "| assistantMessageId =", ast, "| riskLevel =", m["riskLevel"])

# 用 http.client 手动读取 SSE
conn = http.client.HTTPConnection("localhost", 1235, timeout=30)
conn.request("GET", f"/psychological-chat/stream?sessionId={sid}&assistantMessageId={ast}",
             headers={"Authorization": "Bearer " + dt})
resp = conn.getresponse()
print("HTTP", resp.status, resp.getheader("Content-Type"))
body = resp.read()
print("=== RAW SSE (first 1500 chars) ===")
print(body.decode("utf-8", "ignore")[:1500])
print("=== total bytes:", len(body))
