# 心理健康助手后端接口全面验证脚本
$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:1236"

function Show($name, $result) {
    $code = if ($result.PSObject.Properties.Name -contains 'code') { $result.code } else { "n/a" }
    $summary = ""
    if ($result.PSObject.Properties.Name -contains 'data') {
        try { $summary = ($result.data | ConvertTo-Json -Depth 4 -Compress) } catch { $summary = "..." }
    }
    Write-Output "[$code] $name -> $summary"
}

# 1. 登录（demo 普通用户）
$loginBody = @{username="demo"; password="123456"} | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$base/api/user/login" -Method Post -ContentType "application/json" -Body $loginBody
Show "登录(demo)" $login
$token = $login.data.token
$headers = @{ token = $token }
Write-Output "token len: $($token.Length)"

# 2. 当前用户
$r = Invoke-RestMethod -Uri "$base/api/user/current" -Headers $headers
Show "GET /user/current" $r

# 3. 登出
$r = Invoke-RestMethod -Uri "$base/api/user/logout" -Method Post -Headers $headers
Show "POST /user/logout" $r

# 4. 分类树（公开）
$r = Invoke-RestMethod -Uri "$base/api/knowledge/category/tree"
Show "GET /knowledge/category/tree" $r

# 5. 文章分页-前台（公开，只看已发布）
$r = Invoke-RestMethod -Uri "$base/api/knowledge/article/page?currentPage=1&size=10&sortField=publishedAt&sortDirection=desc"
Show "GET /knowledge/article/page(前台)" $r

# 6. 文章详情（公开，阅读+1）
$r = Invoke-RestMethod -Uri "$base/api/knowledge/article/1"
Show "GET /knowledge/article/1" $r

# 7. 创建文章（管理员，带 token）
$articleBody = @{ title="测试文章-临时"; categoryId=1; summary="临时测试"; content="<p>测试内容</p>"; tags="测试"; status=0 } | ConvertTo-Json
$r = Invoke-RestMethod -Uri "$base/api/knowledge/article" -Method Post -Headers $headers -ContentType "application/json" -Body $articleBody
Show "POST /knowledge/article(建草稿)" $r
$newId = $r.data.id
Write-Output "new article id: $newId"

# 8. 更新文章
$r = Invoke-RestMethod -Uri "$base/api/knowledge/article/$newId" -Method Put -Headers $headers -ContentType "application/json" -Body $articleBody
Show "PUT /knowledge/article/$newId" $r

# 9. 发布文章
$r = Invoke-RestMethod -Uri "$base/api/knowledge/article/$newId/status" -Method Put -Headers $headers -ContentType "application/json" -Body (@{status=1} | ConvertTo-Json)
Show "PUT /knowledge/article/$newId/status" $r

# 10. 管理员分页（看全部状态）
$r = Invoke-RestMethod -Uri "$base/api/knowledge/article/page?currentPage=1&size=10&status=0" -Headers $headers
Show "GET /knowledge/article/page?status=0(管理员)" $r

# 11. 删除文章
$r = Invoke-RestMethod -Uri "$base/api/knowledge/article/$newId" -Method Delete -Headers $headers
Show "DELETE /knowledge/article/$newId" $r

# 12. 创建咨询会话
$sessionBody = @{ sessionTitle="我的第一次咨询"; initialMessage="我最近压力很大，总是失眠" } | ConvertTo-Json
$r = Invoke-RestMethod -Uri "$base/api/psychological-chat/session/start" -Method Post -Headers $headers -ContentType "application/json" -Body $sessionBody
Show "POST /psychological-chat/session/start" $r
$sessionId = $r.data.sessionId
Write-Output "session id: $sessionId"

# 13. 会话列表
$r = Invoke-RestMethod -Uri "$base/api/psychological-chat/sessions?currentPage=1&size=10" -Headers $headers
Show "GET /psychological-chat/sessions" $r

# 14. 会话消息（初始消息）
$id = $sessionId -replace "session_", ""
$r = Invoke-RestMethod -Uri "$base/api/psychological-chat/sessions/$id/messages" -Headers $headers
Show "GET /psychological-chat/sessions/$id/messages" $r

# 15. 会话情绪分析
$r = Invoke-RestMethod -Uri "$base/api/psychological-chat/session/$id/emotion" -Headers $headers
Show "GET /psychological-chat/session/$id/emotion" $r

# 16. 新增情绪日记
$diaryBody = @{ diaryDate="2026-09-03"; moodScore=4; dominantEmotion="焦虑"; emotionTriggers="考试临近"; diaryContent="今天复习很焦虑"; sleepQuality=6; stressLevel=4 } | ConvertTo-Json
$r = Invoke-RestMethod -Uri "$base/api/emotion-diary" -Method Post -Headers $headers -ContentType "application/json" -Body $diaryBody
Show "POST /emotion-diary" $r

# 17. 情绪日记管理分页
$r = Invoke-RestMethod -Uri "$base/api/emotion-diary/admin/page?currentPage=1&size=10" -Headers $headers
Show "GET /emotion-diary/admin/page" $r
$diaryId = $r.data.records[0].id

# 18. 删除情绪日记
$r = Invoke-RestMethod -Uri "$base/api/emotion-diary/admin/$diaryId" -Method Delete -Headers $headers
Show "DELETE /emotion-diary/admin/$diaryId" $r

# 19. 数据看板
$r = Invoke-RestMethod -Uri "$base/api/data-analytics/overview" -Headers $headers
Show "GET /data-analytics/overview" $r

# 20. 文件上传
$tmpFile = "D:\code\AI vue\心理健康助手（后端源码）\backend\sql\test-upload.png"
# 生成一个最小的 PNG 文件用于测试
[byte[]]$png = 0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,0x08,0x06,0x00,0x00,0x00,0x1F,0x15,0xC4,0x89,0x00,0x00,0x00,0x0D,0x49,0x44,0x41,0x54,0x78,0x9C,0x63,0x00,0x01,0x00,0x00,0x05,0x00,0x01,0x0D,0x0A,0x2D,0xB4,0x00,0x00,0x00,0x00,0x49,0x45,0x4E,0x44,0xAE,0x42,0x60,0x82
[System.IO.File]::WriteAllBytes($tmpFile, $png)
$form = @{ file = Get-Item $tmpFile }
$r = Invoke-RestMethod -Uri "$base/api/file/upload" -Method Post -Headers $headers -Form $form
Show "POST /file/upload" $r
$filePath = $r.data.filePath
# 验证静态资源可访问
$fileUrl = "$base$filePath"
try {
    $resp = Invoke-WebRequest -Uri $fileUrl -Method Get
    Write-Output "静态资源访问 $fileUrl -> HTTP $($resp.StatusCode), 字节数 $($resp.RawContentLength)"
} catch {
    Write-Output "静态资源访问失败: $($_.Exception.Message)"
}

Write-Output "=== 全部接口测试完成 ==="
