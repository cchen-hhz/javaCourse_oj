#!/usr/bin/env bash
set -e

CODE_FILE="$1"    # 比如 code.java
INPUT_FILE="$2"   # /app/input/1.in

if [ -z "$CODE_FILE" ] || [ -z "$INPUT_FILE" ]; then
  echo "Usage: run.sh <code-file-name> <input-file-path>" >&2
  exit 1
fi

cd /app

# 统一用 Main.java
cp "/app/code/${CODE_FILE}" /app/Main.java

javac Main.java

java Main < "${INPUT_FILE}"