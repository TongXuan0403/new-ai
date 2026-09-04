# -*- coding: utf-8 -*-
"""闭环验证：管理后台新增文章 -> AI 知识库立即可检索（缓存失效）"""
import json
import http.client

BASE_HOST = "127.0.0.1"
BASE_PORT = 1236


def req(method, path, data=None, token=None):
    conn = http.client.HTTPConnection(BASE_HOST, BASE_PORT, timeout=30)
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    body = json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
    conn.request(method, path, body=body, headers=headers)
    resp = conn.getresponse()
    raw = resp.read().decode("utf-8")
    conn.close()
    return json.loads(raw)


# 1) admin 登录
login = req("POST", "/api/user/login", {"username": "admin", "password": "123456"})
print("admin login code:", login["code"])
token = login["data"]["token"]

# 2) 新增一篇已发布文章（标题唯一，便于识别）
title = "RAG闭环验证：正念呼吸缓解考试焦虑"
create = req("POST", "/api/knowledge/article", {
    "title": title,
    "categoryId": 2,
    "summary": "考前焦虑时，用 4-4-6 呼吸法快速稳定情绪。",
    "content": "<p>考试前感到紧张焦虑是正常反应。采用 4-4-6 呼吸节奏（吸气4秒、屏息4秒、呼气6秒），配合简短自我提示，可以在短时间内把注意力拉回当下。</p>",
    "tags": "考试,焦虑,呼吸",
    "status": 1,
    "coverImage": ""
}, token=token)
print("create code:", create["code"], "| id:", create["data"]["id"] if create.get("data") else None)

# 3) 通过知识接口立即检索（不重启、不重新编译）
kb = req("GET", "/api/knowledge?keyword=%E8%80%83%E8%AF%95%E7%84%A6%E8%99%91&page=1&pageSize=10")
hit = [a for a in kb["data"]["list"] if a["title"] == title]
print("knowledge total:", kb["data"]["total"])
print("新增文章可被检索到:", len(hit) > 0)
if hit:
    print("  ->", hit[0]["id"], hit[0]["title"], "|", hit[0]["category"], "|", hit[0]["status"])

# 4) 清理：删除测试文章，避免污染数据
if create.get("data") and create["data"].get("id"):
    del_res = req("DELETE", f"/api/knowledge/article/{create['data']['id']}", token=token)
    print("cleanup delete code:", del_res["code"])
