# -*- coding: utf-8 -*-
"""验证语义检索（BGE-M3）命中效果"""
import urllib.request
import urllib.parse
import json
import time


def get(url):
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode("utf-8"))


queries = ["脑子停不下来", "考试压力大喘不过气", "心情很糟糕", "和爸妈吵架了", "睡眠"]
print("=== 语义检索验证（BGE-M3）===")
for q in queries:
    url = "http://localhost:1236/api/knowledge?keyword=" + urllib.parse.quote(q) + "&page=1&pageSize=5"
    d = get(url)
    top = d["data"]["list"][:4]
    print()
    print("【%s】" % q)
    for a in top:
        print("   %3s %s  [%s]" % (a["id"], a["title"], a["category"]))
    time.sleep(0.3)
