#!/usr/bin/env bash
# 把本次构建的 HTML 报告和失败用例的 trace.zip 发布到 nginx 静态目录
# （/www/wwwroot/ci-traces/<构建号>/）：
#   - report.html 直接浏览器打开，失败截图已内嵌；
#   - trace.zip 配合 https://trace.playwright.dev/?trace=<zip 的 URL> 在线回放。
#
# 用法：publish-traces.sh <工作区宿主机路径> <构建号>
# 说明：Jenkins 在容器里，docker.sock 直连宿主 daemon，所以挂载要用宿主机路径。
set -uo pipefail

HOST_WS="$1"
BUILD="$2"
SRC="$HOST_WS/course-ui-tests/test-results"
REPORT="$HOST_WS/course-ui-tests/report.html"

# 注意：这个脚本跑在 Jenkins 容器里，但路径是【宿主机】路径（docker.sock 直连宿主 daemon），
# 所以不能直接用 [ -f ] 判断 $REPORT——换成 Jenkins 容器视角的路径再判断。
# 另外 report.html 不存在时不能挂载（docker -v 会把缺失的源创建成同名目录，
# 下次构建的报告文件就写不进去了），所以挂载参数有条件地拼。
LOCAL_REPORT=$(echo "$REPORT" | sed 's#^/opt/jenkins_home#/var/jenkins_home#')
MOUNT_REPORT=""
[ -f "$LOCAL_REPORT" ] && MOUNT_REPORT="-v $REPORT:/report.html:ro"

docker run --rm \
  -v "$SRC:/traces:ro" \
  $MOUNT_REPORT \
  -v /www/wwwroot/ci-traces:/out \
  alpine sh -c "
    mkdir -p /out/$BUILD
    if [ -f /report.html ]; then
      cp /report.html /out/$BUILD/report.html
      echo \"published: report.html\"
    fi
    found=0
    for d in /traces/*/; do
      if [ -f \"\$d/trace.zip\" ]; then
        cp \"\$d/trace.zip\" \"/out/$BUILD/\$(basename \$d).zip\"
        echo \"published: \$(basename \$d).zip\"
        found=1
      fi
    done
    [ \"\$found\" = 0 ] && echo '没有 trace.zip 产物（全绿构建属正常）'
    exit 0
  "
