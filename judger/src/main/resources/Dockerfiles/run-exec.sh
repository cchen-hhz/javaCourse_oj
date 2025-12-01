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

"${EXE_FILE}" < "${INPUT_FILE}"