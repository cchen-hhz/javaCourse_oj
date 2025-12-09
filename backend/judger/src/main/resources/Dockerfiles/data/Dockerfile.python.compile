#!/bin/sh
set -e

SRC_FILE="$1"   # 例如 main.py 或 code.py
OUT_PATH="$2"   # 例如 /app/build/main

if [ -z "$SRC_FILE" ] || [ -z "$OUT_PATH" ]; then
  echo "Usage: $0 <SRC_FILE> <OUT_PATH>" >&2
  exit 1
fi

mkdir -p /app/build
cd /app

# 把源文件复制到 build 目录，这样运行容器只挂载 /app/build 就够了
cp "/app/src/$SRC_FILE" /app/build/code.py

# 语法检查：把 .pyc 放到可写的 /app/build/__pycache__
mkdir -p /app/build/__pycache__
export PYTHONPYCACHEPREFIX=/app/build/__pycache__
python3 -m py_compile /app/build/code.py

# 生成统一的包装脚本 /app/build/main
cat > "$OUT_PATH" <<EOF
#!/bin/sh
exec python3 /app/build/code.py "\$@"
EOF

chmod +x "$OUT_PATH"
