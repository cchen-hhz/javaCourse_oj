#!/bin/sh
set -e

SRC_FILE="$1"   # 例如 Main.java 或 code.java
OUT_PATH="$2"   # 例如 /app/build/main

if [ -z "$SRC_FILE" ] || [ -z "$OUT_PATH" ]; then
  echo "Usage: $0 <SRC_FILE> <OUT_PATH>" >&2
  exit 1
fi

cd /app/src

mkdir -p /app/build

# 编译：把 class 文件输出到 /app/build
# 假设主类名和文件名一致：Main.java -> Main
javac -d /app/build "$SRC_FILE"

CLASS_NAME="${SRC_FILE%.java}"

# 生成统一的可执行包装脚本 /app/build/main（OUT_PATH）
cat > "$OUT_PATH" <<EOF
#!/bin/sh
exec java -cp /app/build $CLASS_NAME "\$@"
EOF

chmod +x "$OUT_PATH"
