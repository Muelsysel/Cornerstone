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

# 5. 项目工作流技能
check "项目技能" "skills/cornerstone-dev/SKILL.md"

echo ""
if [ "$fail" -eq 0 ]; then
  echo "PASS 文档完整性检查通过"
else
  echo "FAIL 文档完整性检查未通过"
  exit 1
fi
