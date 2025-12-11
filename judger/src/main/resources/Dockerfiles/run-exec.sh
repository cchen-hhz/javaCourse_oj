#!/usr/bin/env bash
set -e

# 参数 1：可执行文件路径（例如 /app/build/main）
# 参数 2：输入文件路径（例如 /app/input/1.in）

EXE_FILE="$1"
INPUT_FILE="$2"

if [ -z "$EXE_FILE" ] || [ -z "$INPUT_FILE" ]; then
  echo "Usage: run-exec.sh <exe-file-path> <input-file-path>" >&2
  exit 1
fi

cd /app

# 记录开始和结束时间（纳秒）
start_ns=$(date +%s%N)
"${EXE_FILE}" < "${INPUT_FILE}"
end_ns=$(date +%s%N)

# 计算毫秒
elapsed_ms=$(( (end_ns - start_ns) / 1000000 ))

# 用一个特殊前缀输出到 stderr，方便 Java 解析，且不影响 stdout
echo "__OJ_TIME_MS__=${elapsed_ms}" >&2