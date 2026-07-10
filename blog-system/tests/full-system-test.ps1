param(
  [string]$BaseUrl = 'http://127.0.0.1:8080/api',
  [string]$AdminUsername = 'admin',
  [string]$AdminPassword = '123456'
)

$ErrorActionPreference = 'Stop'
$script:passed = 0
$script:userIds = @()
$script:articleIds = @()
$script:stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$script:prefix = "e2e_$($script:stamp)"

function Read-LocalEnvironment {
  $values = @{}
  if (Test-Path '.env.local') {
    Get-Content '.env.local' | ForEach-Object {
      $line = $_.Trim()
      if ($line -and -not $line.StartsWith('#') -and $line -match '=') {
        $name, $value = $line -split '=', 2
        $values[$name.Trim()] = $value.Trim().Trim('"').Trim("'")
      }
    }
  }
  return $values
}

$localEnvironment = Read-LocalEnvironment
$dbUser = if ($localEnvironment['MYSQL_USER']) { $localEnvironment['MYSQL_USER'] } elseif ($env:MYSQL_USER) { $env:MYSQL_USER } else { 'root' }
$dbPassword = if ($localEnvironment['MYSQL_PASSWORD']) { $localEnvironment['MYSQL_PASSWORD'] } elseif ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { 'root' }
$env:MYSQL_PWD = $dbPassword

function Invoke-Db([string]$Sql) {
  $output = & mysql -u $dbUser -N -s mydataset -e $Sql 2>$null
  if ($LASTEXITCODE -ne 0) { throw "MySQL command failed: $Sql" }
  return $output
}

function Invoke-Api([string]$Method, [string]$Path, [string]$Token = '', $Body = $null) {
  $headers = @{}
  if ($Token) { $headers['X-Token'] = $Token }
  $params = @{
    Method = $Method
    Uri = $BaseUrl + $Path
    Headers = $headers
    TimeoutSec = 60
  }
  if ($null -ne $Body) {
    $params['ContentType'] = 'application/json; charset=utf-8'
    $params['Body'] = $Body | ConvertTo-Json -Depth 12
  }
  $response = Invoke-RestMethod @params
  if (-not $response.success) { throw "API failed: $Method $Path - $($response.message)" }
  return $response.data
}

function Expect-BadRequest([string]$Method, [string]$Path, [string]$Token = '', $Body = $null) {
  try {
    Invoke-Api $Method $Path $Token $Body | Out-Null
    return $false
  } catch {
    return $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 400
  }
}

function Assert-True([bool]$Condition, [string]$Name) {
  if (-not $Condition) { throw "FAIL: $Name" }
  $script:passed += 1
  Write-Host ("PASS {0:D2}  {1}" -f $script:passed, $Name)
}

function Login([string]$Username, [string]$Password) {
  return (Invoke-Api 'POST' '/auth/login' '' @{ username = $Username; password = $Password }).token
}

function Invoke-AiWithRetry([string]$Path, [string]$Token, $Body) {
  $lastResult = $null
  for ($attempt = 1; $attempt -le 3; $attempt++) {
    $lastResult = Invoke-Api 'POST' $Path $Token $Body
    if (-not $lastResult.error) { return $lastResult }
    if ($attempt -lt 3) { Start-Sleep -Seconds 1 }
  }
  return $lastResult
}

function Cleanup-TestData {
  try {
    $ids = @($script:userIds | Where-Object { $_ } | Select-Object -Unique)
    if (-not $ids.Count) {
      $resolved = Invoke-Db "SELECT id FROM mydataset.users WHERE username LIKE '$($script:prefix)%';"
      $ids = @($resolved | Where-Object { $_ })
    }
    if ($ids.Count) {
      $csv = $ids -join ','
      Invoke-Db @"
DELETE FROM mydataset.notifications WHERE user_id IN ($csv) OR actor_user_id IN ($csv) OR content LIKE '%$($script:stamp)%';
DELETE cl FROM mydataset.comment_likes cl INNER JOIN mydataset.comments c ON c.id=cl.comment_id WHERE c.user_id IN ($csv) OR c.article_id IN (SELECT id FROM mydataset.articles WHERE author_id IN ($csv));
DELETE FROM mydataset.comments WHERE user_id IN ($csv) OR article_id IN (SELECT id FROM mydataset.articles WHERE author_id IN ($csv));
DELETE FROM mydataset.article_likes WHERE user_id IN ($csv) OR article_id IN (SELECT id FROM mydataset.articles WHERE author_id IN ($csv));
DELETE FROM mydataset.article_attachments WHERE article_id IN (SELECT id FROM mydataset.articles WHERE author_id IN ($csv));
DELETE FROM mydataset.article_tags WHERE article_id IN (SELECT id FROM mydataset.articles WHERE author_id IN ($csv));
DELETE FROM mydataset.articles WHERE author_id IN ($csv);
DELETE FROM mydataset.private_messages WHERE sender_id IN ($csv) OR receiver_id IN ($csv);
DELETE FROM mydataset.user_follows WHERE follower_id IN ($csv) OR followed_id IN ($csv);
DELETE FROM mydataset.gallery_photos WHERE owner_id IN ($csv);
DELETE FROM mydataset.user_gallery_settings WHERE user_id IN ($csv);
DELETE FROM mydataset.ai_usage_logs WHERE user_id IN ($csv);
DELETE FROM mydataset.users WHERE id IN ($csv);
"@ | Out-Null
    }
    Invoke-Db "DELETE FROM mydataset.notifications WHERE content LIKE '%$($script:stamp)%';" | Out-Null
    if ($script:aiLogStartId -ne $null) {
      Invoke-Db "DELETE FROM mydataset.ai_usage_logs WHERE id > $($script:aiLogStartId);" | Out-Null
    }
    Invoke-Db "DELETE FROM mydataset.tags WHERE name LIKE '$($script:prefix)%'; DELETE FROM mydataset.categories WHERE name LIKE '$($script:prefix)%';" | Out-Null
  } catch {
    Write-Warning "Test cleanup failed: $($_.Exception.Message)"
  }
}

try {
  $script:aiLogStartId = [long](Invoke-Db 'SELECT COALESCE(MAX(id),0) FROM mydataset.ai_usage_logs;')
  $adminToken = Login $AdminUsername $AdminPassword
  Assert-True ([bool]$adminToken) '管理员登录'

  $userAName = "$($script:prefix)_a"
  $userBName = "$($script:prefix)_b"
  $userCName = "$($script:prefix)_c"
  $password = 'Test123456!'
  $userA = Invoke-Api 'POST' '/auth/register' '' @{ username=$userAName; password=$password; nickname='E2E A'; email="$userAName@test.local" }
  $userB = Invoke-Api 'POST' '/auth/register' '' @{ username=$userBName; password=$password; nickname='E2E B'; email="$userBName@test.local" }
  $userC = Invoke-Api 'POST' '/auth/register' '' @{ username=$userCName; password=$password; nickname='E2E C'; email="$userCName@test.local" }
  $script:userIds = @([long]$userA.id, [long]$userB.id, [long]$userC.id)
  Assert-True (($userA.username -eq $userAName) -and ($userB.username -eq $userBName)) '注册三个临时用户'
  Assert-True (Expect-BadRequest 'POST' '/auth/register' '' @{ username=$userAName; password=$password; email='dup@test.local' }) '用户名唯一性校验'
  Assert-True (Expect-BadRequest 'POST' '/auth/login' '' @{ username=$userAName; password='wrong' }) '错误密码被拒绝'

  $tokenA = Login $userAName $password
  $tokenB = Login $userBName $password
  $meA = Invoke-Api 'GET' '/auth/me' $tokenA
  Assert-True (($meA.id -eq $userA.id) -and ($null -eq $meA.password)) '登录态与密码字段隐藏'

  $category = Invoke-Api 'POST' '/categories' $adminToken @{ name="$($script:prefix)_category"; description='E2E category' }
  $tag = Invoke-Api 'POST' '/tags' $adminToken @{ name="$($script:prefix)_tag" }
  $category = Invoke-Api 'PUT' "/categories/$($category.id)" $adminToken @{ name="$($script:prefix)_category_updated"; description='updated' }
  $tag = Invoke-Api 'PUT' "/tags/$($tag.id)" $adminToken @{ name="$($script:prefix)_tag_updated" }
  Assert-True ((Invoke-Api 'GET' '/categories').id -contains $category.id) '分类新增与修改'
  Assert-True ((Invoke-Api 'GET' '/tags').id -contains $tag.id) '标签新增与修改'

  $roleAdmin = Invoke-Api 'PUT' "/admin/users/$($userC.id)/role?role=ADMIN" $adminToken
  $roleUser = Invoke-Api 'PUT' "/admin/users/$($userC.id)/role?role=USER" $adminToken
  $banned = Invoke-Api 'PUT' "/admin/users/$($userC.id)/ban?banned=true" $adminToken
  Assert-True (($roleAdmin.role -eq 'ADMIN') -and ($roleUser.role -eq 'USER') -and $banned.banned) '管理员修改角色并封禁用户'
  Assert-True (Expect-BadRequest 'POST' '/auth/login' '' @{ username=$userCName; password=$password }) '封禁用户不能登录'
  Invoke-Api 'PUT' "/admin/users/$($userC.id)/ban?banned=false" $adminToken | Out-Null
  Invoke-Api 'DELETE' "/admin/users/$($userC.id)" $adminToken | Out-Null
  Assert-True (-not ((Invoke-Api 'GET' '/admin/users' $adminToken).id -contains $userC.id)) '管理员解封并删除空用户'

  $followAB = Invoke-Api 'POST' "/users/$($userB.id)/follow" $tokenA
  $followBA = Invoke-Api 'POST' "/users/$($userA.id)/follow" $tokenB
  Assert-True ($followAB.followedByCurrentUser -and $followBA.mutualFollow) '关注与互关状态'
  Assert-True (((Invoke-Api 'GET' "/users/$($userA.id)/followers" $tokenA).id -contains $userB.id) -and ((Invoke-Api 'GET' "/users/$($userA.id)/following" $tokenA).id -contains $userB.id)) '关注和粉丝列表'
  $profileA = Invoke-Api 'GET' "/users/$($userA.id)/profile" $tokenB
  Assert-True (($profileA.maskedEmail -notmatch $userAName) -and ($profileA.photos.Count -eq 8)) '资料卡邮箱脱敏与默认相册'
  $searchUser = Invoke-Api 'GET' ("/search?keyword=" + [uri]::EscapeDataString($userAName)) $tokenB
  Assert-True ($searchUser.users.id -contains $userA.id) '按用户名搜索用户'

  $articleTitle = "$($script:prefix)_article"
  $draft = Invoke-Api 'POST' '/articles' $tokenA @{
    title=$articleTitle; summary='E2E draft'; content="# Overview`nDraft body"; categoryId=$category.id;
    tagIds=@($tag.id); status='DRAFT';
    attachments=@(@{ name='e2e.txt'; type='text/plain'; size=1; dataUrl='data:text/plain;base64,QQ==' })
  }
  $script:articleIds += [long]$draft.id
  Assert-True ($draft.status -eq 'DRAFT') '保存文章草稿'
  $published = Invoke-Api 'PUT' "/articles/$($draft.id)" $tokenA @{
    title=$articleTitle; summary='E2E published'; content="# Overview`nBody`n`n一、Design`nDetails`n`n1.1 Data`nMore details";
    categoryId=$category.id; tagIds=@($tag.id); status='PUBLISHED';
    attachments=@(@{ name='e2e.txt'; type='text/plain'; size=1; dataUrl='data:text/plain;base64,QQ==' })
  }
  Assert-True (($published.status -eq 'PUBLISHED') -and ($published.attachments.Count -eq 1)) 'AI 初审通过后发布并保存附件'
  Assert-True ((Invoke-Api 'GET' '/articles/mine' $tokenA).id -contains $draft.id) '我的文章列表'
  Assert-True ((Invoke-Api 'GET' '/articles/feed' $tokenB).id -contains $draft.id) '用户文章流'
  $detail = Invoke-Api 'GET' "/articles/$($draft.id)" $tokenB
  Assert-True (($detail.viewCount -ge 1) -and ($detail.authorId -eq $userA.id)) '文章详情与阅读量'
  $searchArticle = Invoke-Api 'GET' ("/search?keyword=" + [uri]::EscapeDataString($articleTitle)) $tokenB
  Assert-True ($searchArticle.articles.id -contains $draft.id) '按标题搜索文章'
  Assert-True (Expect-BadRequest 'DELETE' "/categories/$($category.id)" $adminToken) '使用中的分类不能删除'
  Assert-True (Expect-BadRequest 'DELETE' "/tags/$($tag.id)" $adminToken) '使用中的标签不能删除'

  Invoke-Api 'POST' "/articles/$($draft.id)/like" $tokenB @{} | Out-Null
  Invoke-Api 'DELETE' "/articles/$($draft.id)/like" $tokenB | Out-Null
  $likedArticle = Invoke-Api 'POST' "/articles/$($draft.id)/like" $tokenB @{}
  Assert-True ($likedArticle.likedByCurrentUser -and $likedArticle.likeCount -ge 1) '文章点赞、取消与再次点赞'

  $comment = Invoke-Api 'POST' "/articles/$($draft.id)/comments" $tokenB @{ content='A normal E2E comment.' }
  Assert-True ($comment.status -eq 'APPROVED') '普通评论经 AI 初审直接公开'
  $reply = Invoke-Api 'POST' "/articles/$($draft.id)/comments" $tokenA @{ content='A normal reply.'; parentId=$comment.id }
  Assert-True (($reply.status -eq 'APPROVED') -and ($reply.parentId -eq $comment.id)) '评论回复'
  Invoke-Api 'POST' "/comments/$($comment.id)/like" $tokenA @{} | Out-Null
  Invoke-Api 'DELETE' "/comments/$($comment.id)/like" $tokenA | Out-Null
  $likedComment = Invoke-Api 'POST' "/comments/$($comment.id)/like" $tokenA @{}
  Assert-True ($likedComment.likedByCurrentUser -and $likedComment.likeCount -eq 1) '评论点赞与取消'

  $pendingComment = Invoke-Api 'POST' "/articles/$($draft.id)/comments" $tokenB @{ content='spam add wechat e2e' }
  Assert-True ($pendingComment.status -eq 'PENDING') '风险评论转人工审核'
  $approvedComment = Invoke-Api 'PUT' "/admin/comments/$($pendingComment.id)/moderate?status=APPROVED" $adminToken
  Assert-True ($approvedComment.status -eq 'APPROVED') '管理员通过风险评论'
  $deleteComment = Invoke-Api 'POST' "/articles/$($draft.id)/comments" $tokenB @{ content='spam delete-me e2e' }
  Invoke-Api 'PUT' "/admin/comments/$($deleteComment.id)/moderate?status=REJECTED" $adminToken | Out-Null
  Invoke-Api 'DELETE' "/admin/comments/$($deleteComment.id)" $adminToken | Out-Null
  Assert-True (-not ((Invoke-Api 'GET' '/admin/comments' $adminToken).id -contains $deleteComment.id)) '管理员驳回并删除评论'

  $riskTitle = "$($script:prefix)_risk_article"
  $riskArticle = Invoke-Api 'POST' '/articles' $tokenA @{
    title=$riskTitle; summary='add wechat'; content='add wechat for spam'; categoryId=$category.id; tagIds=@($tag.id); status='PUBLISHED'; attachments=@()
  }
  $script:articleIds += [long]$riskArticle.id
  Assert-True ($riskArticle.status -eq 'PENDING') '风险文章转人工审核'
  $riskPublished = Invoke-Api 'PUT' "/admin/articles/$($riskArticle.id)/status?status=PUBLISHED" $adminToken
  Assert-True ($riskPublished.status -eq 'PUBLISHED') '管理员发布风险文章'

  $notificationsA = @(Invoke-Api 'GET' '/notifications?unread=false' $tokenA)
  $notificationsB = @(Invoke-Api 'GET' '/notifications?unread=false' $tokenB)
  Assert-True (($notificationsA.type -contains 'ARTICLE_LIKED') -and ($notificationsA.type -contains 'ARTICLE_COMMENTED')) '文章作者收到点赞和公开评论通知'
  Assert-True (($notificationsB.type -contains 'COMMENT_REPLIED') -and ($notificationsB.type -contains 'COMMENT_LIKED') -and ($notificationsB.type -contains 'USER_ARTICLE_PUBLISHED')) '互动用户收到回复、评论点赞和关注文章通知'
  Assert-True (-not ($notificationsA.type -contains 'ARTICLE_PUBLISHED')) '文章通过审核不发送通过通知'
  Assert-True (-not ($notificationsB.type -contains 'COMMENT_APPROVED')) '评论通过审核不发送通过通知'
  $unreadBefore = @(Invoke-Api 'GET' '/notifications?unread=true' $tokenB)
  if ($unreadBefore.Count) {
    Invoke-Api 'PUT' "/notifications/$($unreadBefore[0].id)/read" $tokenB | Out-Null
    $unreadAfterOne = @(Invoke-Api 'GET' '/notifications?unread=true' $tokenB)
    Assert-True ($unreadAfterOne.Count -eq ($unreadBefore.Count - 1)) '单条通知标记已读'
  }
  Invoke-Api 'PUT' '/notifications/read' $tokenB | Out-Null
  Assert-True (@(Invoke-Api 'GET' '/notifications?unread=true' $tokenB).Count -eq 0) '全部通知标记已读'

  Invoke-Api 'DELETE' "/users/$($userB.id)/follow" $tokenA | Out-Null
  Invoke-Api 'DELETE' "/users/$($userA.id)/follow" $tokenB | Out-Null
  1..3 | ForEach-Object { Invoke-Api 'POST' "/messages/$($userB.id)" $tokenA @{ content="message $_" } | Out-Null }
  Assert-True (Expect-BadRequest 'POST' "/messages/$($userB.id)" $tokenA @{ content='message 4 blocked' }) '非互关用户三条私信上限'
  Invoke-Api 'POST' "/users/$($userB.id)/follow" $tokenA | Out-Null
  Invoke-Api 'POST' "/users/$($userA.id)/follow" $tokenB | Out-Null
  $mutualConversation = Invoke-Api 'POST' "/messages/$($userB.id)" $tokenA @{ content='message 4 mutual' }
  $receivedConversation = Invoke-Api 'GET' "/messages/$($userA.id)" $tokenB
  Assert-True ($mutualConversation.mutualFollow -and $receivedConversation.messages.Count -eq 4) '互关后私信不限量并可读取会话'

  $photosA = @(Invoke-Api 'GET' '/gallery/photos' $tokenA)
  Assert-True ($photosA.Count -eq 8) '新账号默认八张相册图片'
  Assert-True (Expect-BadRequest 'POST' '/gallery/photos' $tokenA @{ title='ninth'; imageDataUrl='data:image/png;base64,iVBORw0KGgo=' }) '相册第九张图片被拒绝'
  Invoke-Api 'DELETE' "/gallery/photos/$($photosA[0].id)" $tokenA | Out-Null
  $newPhoto = Invoke-Api 'POST' '/gallery/photos' $tokenA @{ title='E2E_PHOTO'; description='created'; imageDataUrl='data:image/png;base64,iVBORw0KGgo=' }
  $updatedPhoto = Invoke-Api 'PUT' "/gallery/photos/$($newPhoto.id)" $tokenA @{ title='E2E_PHOTO_UPDATED'; description='updated'; imageDataUrl=$null }
  $profileAfterPhoto = Invoke-Api 'GET' "/users/$($userA.id)/profile" $tokenB
  Assert-True (($updatedPhoto.title -eq 'E2E_PHOTO_UPDATED') -and ($profileAfterPhoto.photos.Count -eq 8) -and ($profileAfterPhoto.photos.title -contains 'E2E_PHOTO_UPDATED')) '相册删除、上传、修改及资料卡展示'
  Assert-True (Expect-BadRequest 'PUT' "/gallery/photos/$($newPhoto.id)" $tokenB @{ title='cross account'; imageDataUrl=$null }) '相册账户隔离'

  Assert-True (Expect-BadRequest 'POST' '/ai/summary' '' @{ content='anonymous' }) 'AI 接口拒绝匿名调用'
  $aiOutline = Invoke-AiWithRetry '/ai/outline' $tokenA @{ title='Spring Boot Blog'; content='Java backend and Hexo frontend' }
  $aiSummary = Invoke-AiWithRetry '/ai/summary' $tokenA @{ title='Spring Boot Blog'; content='This project contains a Java backend, Hexo frontend, MySQL database and AI features.' }
  $aiTags = Invoke-AiWithRetry '/ai/tags' $tokenA @{ title='Spring Boot Blog'; content='Java Spring Hexo AI' }
  $aiCategory = Invoke-AiWithRetry '/ai/category' $tokenA @{ title='Spring Boot Blog'; content='Java backend tutorial' }
  $aiQa = Invoke-AiWithRetry '/ai/qa' $tokenA @{ question='What does this blog system contain?' }
  Assert-True (($aiOutline.outline.Count -ge 1) -and [bool]$aiSummary.summary -and ($aiTags.tags.Count -ge 1) -and [bool]$aiCategory.category -and [bool]$aiQa.answer) '五项 AI 功能'

  $stats = Invoke-Api 'GET' '/admin/stats' $adminToken
  $adminArticles = Invoke-Api 'GET' '/admin/articles' $adminToken
  $adminComments = Invoke-Api 'GET' '/admin/comments' $adminToken
  $aiLogs = Invoke-Api 'GET' '/admin/ai-logs' $adminToken
  Assert-True (($stats.userCount -ge 3) -and ($adminArticles.id -contains $draft.id) -and ($adminComments.id -contains $comment.id) -and ($aiLogs.Count -ge 5)) '管理员统计、文章、评论和 AI 日志'

  Invoke-Api 'DELETE' "/articles/$($draft.id)" $tokenA | Out-Null
  Assert-True (Expect-BadRequest 'GET' "/articles/$($draft.id)" $tokenB) '删除文章后详情不可访问'
  $cascadeCounts = Invoke-Db "SELECT (SELECT COUNT(*) FROM mydataset.articles WHERE id=$($draft.id)),(SELECT COUNT(*) FROM mydataset.comments WHERE article_id=$($draft.id)),(SELECT COUNT(*) FROM mydataset.article_likes WHERE article_id=$($draft.id)),(SELECT COUNT(*) FROM mydataset.notifications WHERE article_id=$($draft.id));"
  Assert-True ($cascadeCounts -eq "0`t0`t0`t0") '删除文章级联清理评论、点赞和关联通知'
  Invoke-Api 'DELETE' "/articles/$($riskArticle.id)" $tokenA | Out-Null
  Invoke-Api 'DELETE' "/tags/$($tag.id)" $adminToken | Out-Null
  Invoke-Api 'DELETE' "/categories/$($category.id)" $adminToken | Out-Null
  Assert-True (-not ((Invoke-Api 'GET' '/categories').id -contains $category.id) -and -not ((Invoke-Api 'GET' '/tags').id -contains $tag.id)) '删除未使用分类与标签'

  Invoke-Api 'DELETE' "/admin/users/$($userA.id)" $adminToken | Out-Null
  Invoke-Api 'DELETE' "/admin/users/$($userB.id)" $adminToken | Out-Null
  Assert-True (-not ((Invoke-Api 'GET' '/admin/users' $adminToken).username -match "^$($script:prefix)")) '管理员删除测试用户并级联清理账户数据'

  Write-Host ""
  Write-Host "FULL SYSTEM TEST PASSED: $script:passed checks"
} finally {
  Cleanup-TestData
}
