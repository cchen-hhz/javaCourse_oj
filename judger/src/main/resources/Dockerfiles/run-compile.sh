#!/usr/bin/env bash
set -e

# 参数 1：源码文件名（位于 /app/src 下）
# 参数 2：输出可执行文件路径（例如 /app/build/main）

SRC_FILE="$1"
OUT_FILE="$2"

if [ -z "$SRC_FILE" ] || [ -z "$OUT_FILE" ]; then
  echo "Usage: run-compile.sh <src-file-name> <out-exe-path>" >&2
  exit 1
fi

cd /app

g++ -O2 -std=c++17 "/app/src/${SRC_FILE}" -o "${OUT_FILE}"