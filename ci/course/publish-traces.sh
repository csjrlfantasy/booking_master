#!/usr/bin/env bash
# 把本次构建失败用例的 trace.zip 发布到 nginx 静态目录（/www/wwwroot/ci-traces/<构建号>/），
# 配合 https://trace.playwright.dev/?trace=<zip 的 URL> 实现"点开即看"，无需下载再拖文件。
#
# 用法：publish-traces.sh <工作区宿主机路径> <构建号>
# 说明：Jenkins 在容器里，docker.sock 直连宿主 daemon，所以挂载要用宿主机路径。
set -uo pipefail

HOST_WS="$1"
BUILD="$2"
SRC="$HOST_WS/course-ui-tests/test-results"

# 注意：这个脚本跑在 Jenkins 容器里，但路径是【宿主机】路径（docker.sock 直连宿主 daemon），
# 所以不能在本机 [ -d ] 判断目录是否存在——把检查放到 docker 容器内部做。
docker run --rm \
  -v "$SRC:/traces:ro" \
  -v /www/wwwroot/ci-traces:/out \
  alpine sh -c "
    mkdir -p /out/$BUILD
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
