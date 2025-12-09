#!/bin/sh
set -e
#set -x
# 用法：
#   /app/run-submission.sh <EXEC> <CASES_YAML> <RESULTS_YAML>

EXEC="$1"
CASES_YAML="$2"
RESULTS_YAML="$3"

if [ -z "$EXEC" ] || [ -z "$CASES_YAML" ] || [ -z "$RESULTS_YAML" ]; then
  echo "Usage: $0 <EXEC> <CASES_YAML> <RESULTS_YAML>" >&2
  exit 1
fi

if [ ! -x "$EXEC" ]; then
  echo "Executable not found or not executable: $EXEC" >&2
  exit 1
fi

if [ ! -f "$CASES_YAML" ]; then
  echo "cases.yaml not found: $CASES_YAML" >&2
  exit 1
fi

SUBMISSION_DIR="$(dirname "$CASES_YAML")"
mkdir -p "$SUBMISSION_DIR"

########################
# 1. 解析 cases.yaml   #
########################

CASES_JSON="$(
  python3 - "$CASES_YAML" << 'PYCODE'
import sys, json, yaml, os

if len(sys.argv) < 2:
    print("{}", end="")
    sys.exit(0)

cases_yaml_path = sys.argv[1]
if not cases_yaml_path or not os.path.isfile(cases_yaml_path):
    print("{}", end="")
    sys.exit(0)

with open(cases_yaml_path, "r", encoding="utf-8") as f:
    data = yaml.safe_load(f) or {}

out = {
    "timeLimitMs": data.get("timeLimitMs"),
    "testCases": []
}
for tc in data.get("testCases", []):
    out["testCases"].append({
        "index": tc.get("index"),
        "caseId": tc.get("caseId"),
        "inputFile": tc.get("inputFile"),
        "outputFile": tc.get("outputFile"),
    })

print(json.dumps(out, ensure_ascii=False), end="")
PYCODE
)"

if [ -z "$CASES_JSON" ] || [ "$CASES_JSON" = "{}" ]; then
  echo "Failed to parse cases.yaml or no testCases found." >&2
  exit 1
fi

###########################################
# 2. 从 JSON 中抽出 timeLimit 和用例列表  #
###########################################

PARSED_CASES="$(
  CASES_JSON_ENV="$CASES_JSON" python3 - << 'PYCODE'
import sys, json, os

cases_json = os.environ.get("CASES_JSON_ENV")
if not cases_json:
    sys.exit(0)

data = json.loads(cases_json)
time_limit_ms = data.get("timeLimitMs")
if time_limit_ms is None:
    time_limit_ms = 1000

cases = data.get("testCases", [])

print(time_limit_ms)
for tc in cases:
    idx = tc.get("index")
    cid = tc.get("caseId")
    inf = tc.get("inputFile")
    outf = tc.get("outputFile")
    if idx is None or cid is None or not inf or not outf:
        continue
    print(f"{idx}|{cid}|{inf}|{outf}")
PYCODE
)"

if [ -z "$PARSED_CASES" ]; then
  echo "No parsed cases from JSON." >&2
  exit 1
fi

TIME_LIMIT_MS="$(printf '%s\n' "$PARSED_CASES" | head -n1)"
CASE_LINES="$(printf '%s\n' "$PARSED_CASES" | tail -n +2)"

########################
# 3. 初始化 results.yaml
########################

TMP_RESULTS="$SUBMISSION_DIR/results.tmp.yaml"

cat > "$TMP_RESULTS" <<EOF
cases:
EOF

TMP_OUT_DIR="$SUBMISSION_DIR/tmp_outputs"
rm -rf "$TMP_OUT_DIR"
mkdir -p "$TMP_OUT_DIR"

###################################
# 4. 遍历所有用例，运行程序并判题  #
###################################

echo "$CASE_LINES" | while IFS='|' read -r INDEX CASE_ID IN_FILE OUT_FILE; do
  [ -z "$INDEX" ] && continue

  INPUT_PATH="/app/input/$IN_FILE"
  EXPECTED_PATH="/app/input/$OUT_FILE"
  ACTUAL_PATH="$TMP_OUT_DIR/${CASE_ID}.out"
  STDERR_PATH="$TMP_OUT_DIR/${CASE_ID}.err"

  STATUS=""
  MESSAGE=""
  EXEC_TIME_MS=""

  if [ ! -f "$INPUT_PATH" ]; then
    STATUS="RE"
    MESSAGE="Input file not found: $INPUT_PATH"
    ACTUAL_CONTENT=""
    EXEC_TIME_MS="null"

  else
    ##########################
    # 4.1 计时 + 限时运行程序 #
    ##########################

    START_MS="$(date +%s%3N 2>/dev/null || date +%s000)"

    TL_MS="${TIME_LIMIT_MS:-1000}"
    TL_SEC=$(( (TL_MS + 999) / 1000 ))
    [ "$TL_SEC" -le 0 ] && TL_SEC=1

    MEM_FILE="$TMP_OUT_DIR/${CASE_ID}.mem"
    rm -f "$MEM_FILE"

    set +e
    if command -v timeout >/dev/null 2>&1; then
      if command -v /usr/bin/time >/dev/null 2>&1; then
        /usr/bin/time -f "%M" -o "$MEM_FILE" timeout "${TL_SEC}s" "$EXEC" \
          < "$INPUT_PATH" > "$ACTUAL_PATH" 2> "$STDERR_PATH"
        EXIT_CODE=$?
      else
        timeout "${TL_SEC}s" "$EXEC" \
          < "$INPUT_PATH" > "$ACTUAL_PATH" 2> "$STDERR_PATH"
        EXIT_CODE=$?
      fi
    else
      if command -v /usr/bin/time >/dev/null 2>&1; then
        /usr/bin/time -f "%M" -o "$MEM_FILE" "$EXEC" \
          < "$INPUT_PATH" > "$ACTUAL_PATH" 2> "$STDERR_PATH"
        EXIT_CODE=$?
      else
        "$EXEC" < "$INPUT_PATH" > "$ACTUAL_PATH" 2> "$STDERR_PATH"
        EXIT_CODE=$?
      fi
    fi
    set -e

    if [ -f "$MEM_FILE" ]; then
      MEM_KB="$(cat "$MEM_FILE" 2>/dev/null || echo "")"
    else
      MEM_KB=""
    fi

    END_MS="$(date +%s%3N 2>/dev/null || date +%s000)"
    EXEC_TIME_MS=$((END_MS - START_MS))
    if [ "$EXEC_TIME_MS" -lt 0 ]; then
      EXEC_TIME_MS=0
    fi


    END_MS="$(date +%s%3N 2>/dev/null || date +%s000)"
    EXEC_TIME_MS=$((END_MS - START_MS))
    # 防止因为宿主机校时导致负数
    if [ "$EXEC_TIME_MS" -lt 0 ]; then
      EXEC_TIME_MS=0
    fi

    ##########################
    # 4.2 根据退出码初判 TLE/MLE #
    ##########################

    if [ "$EXIT_CODE" -eq 124 ]; then
      STATUS="TLE"
      MESSAGE="Time Limit Exceeded (timeout ${TL_SEC}s)"
    elif [ "$EXIT_CODE" -eq 137 ]; then
      # 137 = 128 + 9，一般是被内核用 SIGKILL 杀掉，最常见原因就是 OOM/cgroup memory
      STATUS="MLE"
      MESSAGE="Memory Limit Exceeded (process killed by OOM, exit 137)"
    fi

    ################################
    # 4.3 如果还没定性，再按 RE/AC/WA #
    ################################
    if [ -z "$STATUS" ]; then
      if [ "$EXIT_CODE" -ne 0 ]; then
        STATUS="RE"
        MESSAGE="Runtime Error (exit $EXIT_CODE)"
      else
        if [ ! -f "$EXPECTED_PATH" ]; then
          STATUS="RE"
          MESSAGE="Expected output file not found: $EXPECTED_PATH"
        else
          if diff -ZB "$EXPECTED_PATH" "$ACTUAL_PATH" >/dev/null 2>&1; then
            STATUS="AC"
            MESSAGE="Accepted (Execution time: ${EXEC_TIME_MS} ms)"
          else
            STATUS="WA"
            MESSAGE="Wrong Answer (Execution time: ${EXEC_TIME_MS} ms)"
          fi
        fi
      fi
    fi

    ##########################
    # 4.4 收集实际输出内容     #
    ##########################
    if [ -f "$ACTUAL_PATH" ]; then
      echo "DEBUG: ACTUAL_PATH for case $CASE_ID:" >&2
      cat "$ACTUAL_PATH" >&2 || echo "(read error)" >&2
      ACTUAL_CONTENT="$(cat "$ACTUAL_PATH")"
    else
      echo "DEBUG: ACTUAL_PATH not found: $ACTUAL_PATH" >&2
      ACTUAL_CONTENT=""
    fi
  fi

  ##########################
  # 4.5 写入 YAML 一条用例  #
  ##########################
  PY_ESCAPED="$(
    python3 - "$CASE_ID" "$INDEX" "$STATUS" "$MESSAGE" "$EXEC_TIME_MS" "$MEM_KB" << 'PYCODE'
import sys

case_id      = int(sys.argv[1])
index        = int(sys.argv[2])
status       = sys.argv[3]
message      = sys.argv[4]
exec_ms_raw  = sys.argv[5]
memory_kb_raw = sys.argv[6] if len(sys.argv) > 6 else ""
actual_out   = ""

try:
    exec_ms = int(exec_ms_raw)
except Exception:
    exec_ms = None

try:
    memory_kb = int(memory_kb_raw)
except Exception:
    memory_kb = None

actual_out   = sys.stdin.read()

def yaml_escape_block(s, indent="      "):
    if not s:
        return indent + '""'
    lines = s.splitlines()
    out = "|-"
    for line in lines:
        out += "\n" + indent + line
    return out

block = yaml_escape_block(actual_out)

print(f"  - caseId: {case_id}")
print(f"    index: {index}")
print(f"    status: {status}")
print(f"    message: \"{message}\"")
print(f"    actualOutput: {block}")
if exec_ms is None:
    print("    execTimeMs: null")
else:
    print(f"    execTimeMs: {exec_ms}")
if memory_kb is None:
    print("    memoryKb: null")
else:
    print(f"    memoryKb: {memory_kb}")
PYCODE
<< EOF_IN
$ACTUAL_CONTENT
EOF_IN
)"


  printf '%s\n' "$PY_ESCAPED" >> "$TMP_RESULTS"

done

echo "[run-submission] before mv: TMP_RESULTS=$TMP_RESULTS, RESULTS_YAML=$RESULTS_YAML" >&2
ls -l "$SUBMISSION_DIR" >&2 || echo "[run-submission] ls failed" >&2

mv "$TMP_RESULTS" "$RESULTS_YAML"
echo "[run-submission] after mv" >&2
echo "[run-submission] done, results written to $RESULTS_YAML" >&2
exit 0
