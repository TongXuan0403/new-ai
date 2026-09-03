# -*- coding: utf-8 -*-
"""
Codex-AI P1 接口冒烟验证脚本
覆盖《技术方案》§12.2 P1 剩余三项：
  P1-1 文章搜索/标签/收藏/推荐
  P1-2 自助练习库与完成记录
  P1-3 提示词/模型/风险规则版本化
外加：P1 相关越权与审计安全回归。

运行前提：服务端已在 localhost:1235 运行（mvn spring-boot:run）。
用法：python scripts/p1_test.py
"""
import json
import sys
import time
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
        with urllib.request.urlopen(r, timeout=30) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "ignore")[:400]
        return e.code, {"_http_error": raw}


def check(name, cond, detail=""):
    global FAIL
    mark = "PASS" if cond else "FAIL"
    if not cond:
        FAIL += 1
    print(f"[{mark}] {name} {detail}")
    return cond


def login(username, password):
    st, r = req("POST", "/user/login", body={"username": username, "password": password})
    if st == 200 and r.get("code") == "200":
        return r["data"]["token"]
    return None


def register_user(username, password):
    st, r = req("POST", "/user/add", body={"username": username, "password": password, "nickname": username})
    if st == 200 and r.get("code") == "200":
        return r["data"]["token"]
    return login(username, password)


def submit_consent(token):
    return req("POST", "/consents", token, {
        "ageConfirmed": True,
        "privacyPolicyVersion": "privacy-v1.0",
        "sensitiveInfoVersion": "sensitive-v1.0",
        "productBoundaryVersion": "boundary-v1.0"
    })


def main():
    print("=== 准备：登录与账号 ===")
    at = login("admin", "Admin@123")
    check("admin 登录", at is not None)
    dt = login("demo", "Demo@123")
    check("demo 登录", dt is not None)
    req("POST", "/consents/revoke", dt)
    st, c1 = submit_consent(dt)
    check("demo 提交同意", st == 200 and c1["data"]["complete"] is True)

    su = "p1" + str(int(time.time()))[-8:]
    st_token = register_user(su, "Secure@123")
    check(f"注册越权测试账号 {su}", st_token is not None)

    print()
    print("=== P1-1 文章搜索 / 标签 / 收藏 / 推荐 ===")
    st, tags = req("GET", "/knowledge/tags")
    check("标签列表可查", st == 200 and tags.get("code") == "200", f"tags={tags.get('data')}")
    # 已有文章带 tags？若无 tag 数据则演示用文章兜底
    if not tags["data"]:
        req("PUT", "/admin/knowledge/article/1", at, {
            "categoryId": 2, "title": "考试周睡不着时，先降低入睡压力",
            "summary": "把目标从必须睡着调成让身体休息十分钟，减少入睡压力。",
            "content": "<p>测试标签</p>", "tags": "睡眠,压力"
        })
        st, tags = req("GET", "/knowledge/tags")
        check("标签列表（补数据后）", st == 200 and bool(tags["data"]), f"tags={tags['data']}")
    first_tag = tags["data"][0] if tags["data"] else None

    st, kb = req("GET", "/knowledge?page=1&pageSize=12")
    art_id = kb["data"]["records"][0]["id"] if kb["data"]["records"] else None
    check("文章列表可查", st == 200 and art_id is not None, f"total={kb['data']['total']}")

    if first_tag:
        st, kb_tag = req("GET", f"/knowledge?tag={urllib.parse.quote(first_tag)}&page=1&pageSize=12")
        check("按标签筛选文章", st == 200 and kb_tag.get("code") == "200",
              f"tag={first_tag} total={kb_tag['data']['total']}")

    st, rec = req("GET", "/knowledge/recommend?limit=5")
    check("推荐列表可查（热门兜底）", st == 200 and rec.get("code") == "200",
          f"count={len(rec['data'])}")
    st, rec2 = req("GET", "/knowledge/recommend?limit=5", dt)
    check("登录用户推荐可查", st == 200 and rec2.get("code") == "200")

    if art_id:
        req("DELETE", f"/article-favorites/{art_id}", dt)  # 保证幂等
        st, fav = req("POST", f"/article-favorites/{art_id}", dt)
        check("收藏文章", st == 200 and fav["data"] is True, fav.get("message", ""))
        st, fav2 = req("POST", f"/article-favorites/{art_id}", dt)
        check("重复收藏不新增", st == 200 and fav2["data"] is False, "")
        st, favs = req("GET", "/article-favorites", dt)
        check("我的收藏列表", st == 200 and favs["data"]["total"] >= 1, f"total={favs['data']['total']}")
        st, favIds = req("GET", "/article-favorites/ids", dt)
        check("我的收藏 ID 列表", st == 200 and art_id in favIds["data"], "")
        # 越权：他人看不到我的收藏（用户间隔离）
        st, h = req("GET", "/article-favorites", st_token)
        check("他人收藏列表为空（隔离）", st == 200 and h["data"]["total"] == 0,
              f"total={h['data']['total']}")
        req("POST", "/consents/revoke", st_token)
        submit_consent(st_token)
        st, h2 = req("DELETE", f"/article-favorites/{art_id}", st_token)
        check("他人取消我的收藏不生效（仅删本人记录）", st == 200 and h2["data"] is True, "")
        st, favs2 = req("GET", "/article-favorites", dt)
        check("我的收藏未被他人删除", st == 200 and favs2["data"]["total"] >= 1,
              f"total={favs2['data']['total']}")
        # 浏览量自增
        v1 = None
        st, d1 = req("GET", f"/knowledge/article/{art_id}")
        if st == 200:
            v1 = d1["data"]["viewCount"]
        st, d2 = req("GET", f"/knowledge/article/{art_id}")
        check("详情浏览量自增", st == 200 and d2["data"]["viewCount"] == v1 + 1,
              f"v1={v1} v2={d2['data']['viewCount']}")

    print()
    print("=== P1-2 自助练习库与完成记录 ===")
    st, ex = req("GET", "/exercises?page=1&pageSize=12", dt)
    check("练习列表可查", st == 200 and ex["data"]["total"] >= 3, f"total={ex['data']['total']}")
    ex_id = ex["data"]["records"][0]["id"] if ex["data"]["records"] else None
    check("练习列表含已发布内容", ex_id is not None, "")
    st, exd = req("GET", f"/exercises/{ex_id}", dt)
    check("练习详情可查", st == 200 and exd["data"]["title"], "")

    st, comp = req("POST", f"/exercises/{ex_id}/complete", dt, {"moodAfter": "做完放松了一些"})
    check("标记练习完成", st == 200 and comp.get("code") == "200", comp.get("message", ""))
    st, comp2 = req("POST", f"/exercises/{ex_id}/complete", dt, {"moodAfter": "第二次更新感受"})
    check("重复完成更新感受", st == 200 and comp2["data"]["id"] == comp["data"]["id"], "")
    st, mine = req("GET", "/exercises/my/completions", dt)
    check("我的完成记录", st == 200 and any(c["exerciseId"] == ex_id for c in mine["data"]),
          f"count={len(mine['data'])}")
    st, exd2 = req("GET", f"/exercises/{ex_id}", dt)
    check("详情标注已完成", st == 200 and exd2["data"]["completed"] is True, "")

    # 管理端练习 CRUD + 审计
    st, nex = req("POST", "/admin/exercises", at, {
        "categoryId": 4, "title": "P1 测试练习", "summary": "冒烟",
        "content": "步骤一：测试", "minutes": 3, "tags": "测试", "sortOrder": 99, "status": "DRAFT"
    })
    check("管理员新建练习", st == 200 and nex.get("code") == "200", nex.get("message", ""))
    nex_id = nex["data"]["id"]
    st, uex = req("PUT", f"/admin/exercises/{nex_id}", at, {
        "categoryId": 4, "title": "P1 测试练习-改", "summary": "冒烟改",
        "content": "步骤一：测试改", "minutes": 4, "tags": "测试", "sortOrder": 99, "status": "DRAFT"
    })
    check("管理员更新练习", st == 200 and uex["data"]["title"].endswith("改"), "")
    st, pex = req("PUT", f"/admin/exercises/{nex_id}/status?status=PUBLISHED", at)
    check("发布练习", st == 200 and pex["data"]["status"] == "PUBLISHED", "")
    st, dex = req("DELETE", f"/admin/exercises/{nex_id}", at)
    check("删除练习", st == 200 and dex.get("code") == "200", "")
    # 越权：普通用户不能操作管理端练习
    st, h = req("POST", "/admin/exercises", st_token, {"title": "越权", "categoryId": 4})
    check("普通用户管理练习被拒", st != 200 or h.get("code") != "200", f"http={st}")

    print()
    print("=== P1-3 提示词 / 模型 / 规则版本化 ===")
    st, versions = req("GET", "/admin/system-config/versions?page=1&pageSize=50", at)
    check("版本列表可查", st == 200 and versions["data"]["total"] >= 3,
          f"total={versions['data']['total']}")
    types = {v["configType"] for v in versions["data"]["records"]}
    check("三类配置均存在", {"PROMPT", "MODEL", "RISK_RULE"}.issubset(types), f"types={types}")

    stamp = str(int(time.time()))[-6:]
    st, nver = req("POST", "/admin/system-config/versions", at, {
        "configType": "PROMPT", "name": "测试提示词", "version": "prompt-test-" + stamp,
        "content": "测试提示词内容：只提供克制的支持。", "remark": "p1冒烟"
    })
    check("新建配置版本（草稿）", st == 200 and nver["data"]["status"] == "DRAFT", nver.get("message", ""))
    nver_id = nver["data"]["id"]
    st, dup = req("POST", "/admin/system-config/versions", at, {
        "configType": "PROMPT", "name": "重复版本", "version": "prompt-test-" + stamp, "content": "x"
    })
    check("重复版本号被拒", st == 200 and dup.get("code") != "200", dup.get("message", ""))
    st, upver = req("PUT", f"/admin/system-config/versions/{nver_id}", at, {
        "configType": "PROMPT", "name": "测试提示词改", "version": "prompt-test-" + stamp,
        "content": "测试提示词内容（更新）", "remark": "p1冒烟改"
    })
    check("编辑草稿版本", st == 200 and upver.get("code") == "200", "")
    st, act = req("POST", f"/admin/system-config/versions/{nver_id}/activate", at)
    check("激活版本", st == 200 and act["data"]["status"] == "ACTIVE", "")
    st, versions2 = req("GET", "/admin/system-config/versions?configType=PROMPT&page=1&pageSize=50", at)
    active = [v for v in versions2["data"]["records"] if v["status"] == "ACTIVE"]
    check("每类仅一条生效", len(active) == 1, f"active_count={len(active)}")
    # 激活后对话应使用新提示词版本标识
    st, session = req("POST", "/psychological-chat/session/start", dt, {"title": "版本化冒烟"})
    sid = session["data"]["id"]
    st, msg = req("POST", "/psychological-chat/messages", dt,
                  {"sessionId": sid, "content": "我最近有点累，想聊聊"})
    check("对话返回生效规则版本", st == 200 and msg["data"]["ruleVersion"] == "rule-v1.0",
          f"rule={msg['data'].get('ruleVersion')}")
    st, hist = req("GET", f"/psychological-chat/sessions/{sid}", dt)
    ast = [m for m in hist["data"]["messages"] if m["senderType"] == 2][-1] if hist["data"].get("messages") else None
    check("消息记录可追溯提示词版本", ast is not None and ("prompt-test-" + stamp) in str(ast.get("aiModel")),
          f"model={ast.get('aiModel') if ast else None}")
    # 恢复默认提示词版本生效
    st, restore = req("POST", f"/admin/system-config/versions/1/activate", at)
    check("恢复 prompt-v1.0 生效", st == 200 and restore["data"]["status"] == "ACTIVE", "")
    st, disable = req("POST", f"/admin/system-config/versions/{nver_id}/disable", at)
    check("停用测试版本", st == 200 and disable["data"]["status"] == "DISABLED", "")
    st, dver = req("DELETE", f"/admin/system-config/versions/{nver_id}", at)
    check("删除已停用版本", st == 200 and dver.get("code") == "200", dver.get("message", ""))
    # 越权：普通用户不能访问配置版本
    st, h = req("GET", "/admin/system-config/versions?page=1&pageSize=10", st_token)
    check("普通用户访问配置版本被拒", st != 200 or h.get("code") != "200", f"http={st}")

    # 风险规则版本化：新增规则关键词并激活，验证危机检测使用新规则
    rver = "rule-p1test-" + stamp
    st, nrule = req("POST", "/admin/system-config/versions", at, {
        "configType": "RISK_RULE", "name": "风险规则测试", "version": rver,
        "content": '{"crisis":["彻底崩溃了","想永远消失"],"harmOthers":[],"warning":[],"concern":[]}',
        "remark": "p1规则冒烟"
    })
    check("新建风险规则版本", st == 200 and nrule.get("code") == "200", nrule.get("message", ""))
    nrule_id = nrule["data"]["id"]
    st, ract = req("POST", f"/admin/system-config/versions/{nrule_id}/activate", at)
    check("激活风险规则版本", st == 200 and ract["data"]["status"] == "ACTIVE", "")
    time.sleep(1)  # 等待缓存失效
    st, rmsg = req("POST", "/psychological-chat/messages", dt,
                   {"sessionId": sid, "content": "我感觉彻底崩溃了"})
    check("新规则命中危机", st == 200 and rmsg["data"]["riskLevel"] == 3,
          f"level={rmsg['data'].get('riskLevel')}")
    check("风险事件记录新规则版本", rmsg["data"]["ruleVersion"] == rver,
          f"rule={rmsg['data'].get('ruleVersion')}")
    # 恢复内置规则
    st, rrestore = req("POST", "/admin/system-config/versions/3/activate", at)
    check("恢复 rule-v1.0 生效", st == 200 and rrestore["data"]["status"] == "ACTIVE", "")
    st, drule = req("DELETE", f"/admin/system-config/versions/{nrule_id}", at)
    check("删除风险规则测试版本", st == 200 and drule.get("code") == "200", drule.get("message", ""))

    print()
    print("=== P1 审计日志 ===")
    st, audit = req("GET", "/admin/audit-logs/page?page=1&pageSize=100", at)
    actions = [x["action"] for x in audit["data"]["records"]]
    check("审计记录 CREATE_EXERCISE", "CREATE_EXERCISE" in actions, "")
    check("审计记录 ACTIVATE_CONFIG_VERSION", "ACTIVATE_CONFIG_VERSION" in actions, "")
    check("审计记录 CREATE_CONFIG_VERSION", "CREATE_CONFIG_VERSION" in actions, "")

    print()
    if FAIL:
        print(f"RESULT: {FAIL} 项失败")
        sys.exit(1)
    print("RESULT: P1 全部通过")


if __name__ == "__main__":
    import urllib.parse
    main()
