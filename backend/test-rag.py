# -*- coding: utf-8 -*-
"""验证 AI 对话能优先参考 MySQL 知识库（含 JSON 迁移后的历史知识）"""
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
print("login code:", login["code"])
token = login["data"]["token"]
sess = post_json("/api/psychological-chat/session/start",
                 {"sessionTitle": "RAG验证", "initialMessage": "我最近失眠"}, token=token)
session_id = sess["data"]["sessionId"]
print("session:", session_id)

# SSE 请求：提问命中"睡眠"知识库
conn = http.client.HTTPConnection(BASE_HOST, BASE_PORT, timeout=90)
payload = json.dumps({"sessionId": session_id, "userMessage": "我最近总是失眠，睡前脑子里一直想事情，有什么办法吗？"}).encode("utf-8")
headers = {"Content-Type": "application/json", "token": token}
conn.request("POST", "/api/psychological-chat/stream", body=payload, headers=headers)
resp = conn.getresponse()
print("HTTP status:", resp.status)
print("===== AI 回复 =====")
buf = []
while True:
    line = resp.readline()
    if not line:
        break
    line = line.decode("utf-8", errors="replace").rstrip("\n")
    if line.startswith("data:"):
        buf.append(line[5:].strip())
conn.close()
full = "".join(buf)
print(full)
print("===== END =====")
