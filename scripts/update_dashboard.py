# -*- coding: utf-8 -*-
import io

p = 'docs/实施进度看板.html'
with io.open(p, 'r', encoding='utf-8') as f:
    html = f.read()

old = '<div class="tl-item"><div class="t">11:33</div><div class="c">demo 按产品设计增强</div></div>'
anchor_end = '</div>\n    <div class="warn-box">下一步（P0）：补齐后端 P0 接口（同意 / 风险事件 / 危机资源 / 反馈 / 审计 / 日记 CRUD）、新增 5 张数据表、SSE 结构化风险事件、Vue 工程化。</div>'

addition = ('\n      <div class="tl-item"><div class="t">12:47</div><div class="c">P0 后端接口 + 前端工程实现</div></div>'
            '\n      <div class="tl-item"><div class="t">13:01</div><div class="c">SSE 修复（SseEmitter）</div></div>'
            '\n      <div class="tl-item"><div class="t">13:32</div><div class="c">数据库初始化 + 冒烟 31/31</div></div>'
            '\n      <div class="tl-item"><div class="t">13:40</div><div class="c">浏览器端到端联调验证</div></div>')

new_warn = '<div class="warn-box">下一步（P1）：对话帮助度反馈聚合、文章搜索/标签/收藏/推荐、日记编辑/删除/导出增强、自助练习库、提示词 / 模型 / 规则版本化。</div>'

if old in html:
    html = html.replace(old, old + addition, 1)
    print('timeline items added')
else:
    print('timeline anchor NOT FOUND')

# 处理 warn-box（兼容 CRLF）
for a, b in [
    ('下一步（P0）：补齐后端 P0 接口（同意 / 风险事件 / 危机资源 / 反馈 / 审计 / 日记 CRUD）、新增 5 张数据表、SSE 结构化风险事件、Vue 工程化。',
     '下一步（P1）：对话帮助度反馈聚合、文章搜索/标签/收藏/推荐、日记编辑/删除/导出增强、自助练习库、提示词 / 模型 / 规则版本化。')]:
    if a in html:
        html = html.replace(a, b, 1)
        print('warn-box updated')
    else:
        print('warn-box text NOT FOUND')

with io.open(p, 'w', encoding='utf-8') as f:
    f.write(html)
print('done')
