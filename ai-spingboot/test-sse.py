# -*- coding: utf-8 -*-
"""SSE 流式对话验证（http.client 流式读取）"""
import json
import http.client

BASE_HOST = "127.0.0.1"
BASE_PORT = 1236


def post_json(path, data, token=None):
    conn = http.client.HTTPConnection(BASE_HOST, BASE_PORT, timeout=30)
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    conn.request("POST", path, body=json.dumps(data, ensure_ascii=False).encode("utf-8"), headers=headers)
    resp = conn.getresponse()
    body = resp.read().decode("utf-8")
    conn.close()
    return json.loads(body)


# 登录 + 创建会话
login = post_json("/api/user/login", {"username": "demo", "password": "123456"})
token = login["data"]["token"]
print("login code:", login["code"])
sess = post_json("/api/psychological-chat/session/start",
                 {"sessionTitle": "SSE验证", "initialMessage": "你好"}, token=token)
session_id = sess["data"]["sessionId"]
print("session:", session_id)

# SSE 请求
conn = http.client.HTTPConnection(BASE_HOST, BASE_PORT, timeout=60)
payload = json.dumps({"sessionId": session_id, "userMessage": "你好，我最近压力很大很焦虑"}).encode("utf-8")
headers = {"Content-Type": "application/json", "token": token}
conn.request("POST", "/api/psychological-chat/stream", body=payload, headers=headers)
resp = conn.getresponse()
print("HTTP status:", resp.status)
print("Content-Type:", resp.getheader("Content-Type"))
print("===== SSE 内容 =====")
count = 0
while True:
    line = resp.readline()
    if not line:
        break
    line = line.decode("utf-8", errors="replace").rstrip("\n")
    if line.strip():
        count += 1
        if count <= 15:
            print(line[:300])
conn.close()
print("===== 总行数:", count, "=====")
