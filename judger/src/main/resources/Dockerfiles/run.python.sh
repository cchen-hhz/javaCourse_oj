#!/usr/bin/env bash
set -e

CODE_FILE="$1"
INPUT_FILE="$2"

if [ -z "$CODE_FILE" ] || [ -z "$INPUT_FILE" ]; then
  echo "Usage: run.sh <code-file-name> <input-file-path>" >&2
  exit 1
fi

cd /app

python "/app/code/${CODE_FILE}" < "${INPUT_FILE}"