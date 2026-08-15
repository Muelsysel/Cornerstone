#!/usr/bin/env bash
# 文档完整性检查（CI 强制）：文档约束的可机器校验部分。
# 失败时输出缺失项并以非零退出。用法：bash scripts/check-docs.sh
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0

check() {
  local desc="$1" path="$2"
  if [ -e "$path" ]; then
    echo "OK   $desc ($path)"
  else
    echo "FAIL $desc ($path 缺失)"
    fail=1
  fi
}

echo "== Cornerstone 文档完整性检查 =="

# 1. 入口与地图
check "AI 入口" "AGENTS.md"
check "模块地图" "CONTEXT-MAP.md"

# 2. 工程技能配置
check "Issue tracker 配置" "docs/agents/issue-tracker.md"
check "Triage 标签配置" "docs/agents/triage-labels.md"
check "Domain docs 配置" "docs/agents/domain.md"

# 3. 每个模块必须有 CONTEXT.md（从父 POM modules 读取）
echo "== 模块文档 =="
modules=$(grep -oP '(?<=<module>)[^<]+' pom.xml || true)
if [ -z "$modules" ]; then
  echo "FAIL 无法从 pom.xml 解析模块列表"
  fail=1
else
  for m in $modules; do
    check "模块文档 ($m)" "$m/CONTEXT.md"
  done
fi

# 4. ADR 编号连续（0001, 0002, ...）
echo "== ADR 编号 =="
if [ -d docs/adr ]; then
  expected=1
  for f in $(ls docs/adr/ | grep -E '^[0-9]{4}-' | sort); do
    num=$(echo "$f" | cut -c1-4 | sed 's/^0*//')
    if [ "$num" -ne "$expected" ]; then
      echo "FAIL ADR 编号不连续：期望 $expected，实际 $num（$f）"
      fail=1
    fi
    expected=$((expected + 1))
  done
  if [ "$expected" -eq 1 ]; then
    echo "FAIL docs/adr/ 为空"
    fail=1
  fi
else
  echo "FAIL docs/adr/ 缺失"
  fail=1
fi

# 4b. 每个模块 CHANGELOG 必须含「测试方法」章节（文档维护义务的可机器校验部分）
echo "== CHANGELOG 测试方法 =="
for m in $modules; do
  if [ -f "$m/CHANGELOG.md" ] && ! grep -q '测试方法' "$m/CHANGELOG.md"; then
    echo "FAIL $m/CHANGELOG.md 缺「测试方法」章节"
    fail=1
  fi
done
if [ -f cornerstone-web/CHANGELOG.md ] && ! grep -q '测试方法' cornerstone-web/CHANGELOG.md; then
  echo "FAIL cornerstone-web/CHANGELOG.md 缺「测试方法」章节"
  fail=1
fi

# 5. 项目工作流技能
check "项目技能" "skills/cornerstone-dev/SKILL.md"

# 6. 前端模块文档（非 Maven 模块，单独检查）
echo "== 前端与仓库级文档 =="
check "前端 README" "cornerstone-web/README.md"
check "前端 CHANGELOG" "cornerstone-web/CHANGELOG.md"
check "前端 Dockerfile" "cornerstone-web/Dockerfile"
check "前端 nginx 配置" "cornerstone-web/nginx.conf"

# 7. 仓库级文档与工具
check "根 CHANGELOG" "CHANGELOG.md"
check "贡献指南" "CONTRIBUTING.md"
check "行为准则" "CODE_OF_CONDUCT.md"
check "许可证" "LICENSE"
check "一键启动脚本" "scripts/start-all.ps1"
check "端到端指南" "docs/guides/run-demo.md"
check "验证脚本" "scripts/verify-chain.ps1"

# 8. README / CONTRIBUTING 相对链接断链检查（文档约束：链接必须可点）
echo "== 文档链接完整性 =="
check_links() {
  local file="$1"
  # 提取 markdown 相对链接（排除 http/mailto/锚点），检查目标存在
  grep -oP '(?<=\]\()[^)]+' "$file" | while read -r link; do
    case "$link" in
      http* | mailto:* | '#'*) continue ;;
      *) [ -e "$link" ] || { echo "FAIL 断链: $file -> $link"; fail=1; } ;;
    esac
  done
}
check_links README.md
check_links CONTRIBUTING.md

echo ""
if [ "$fail" -eq 0 ]; then
  echo "PASS 文档完整性检查通过"
else
  echo "FAIL 文档完整性检查未通过"
  exit 1
fi
