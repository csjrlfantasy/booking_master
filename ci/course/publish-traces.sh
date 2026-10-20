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

if [ ! -d "$SRC" ]; then
  echo "没有 test-results 目录，跳过 trace 发布"
  exit 0
fi

docker run --rm \
  -v "$SRC:/traces:ro" \
  -v /www/wwwroot/ci-traces:/out \
  alpine sh -c "
    mkdir -p /out/$BUILD
    for d in /traces/*/; do
      if [ -f \"\$d/trace.zip\" ]; then
        cp \"\$d/trace.zip\" \"/out/$BUILD/\$(basename \$d).zip\"
        echo \"published: \$(basename \$d).zip\"
      fi
    done
  "
