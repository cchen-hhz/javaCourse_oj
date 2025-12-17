# judge-worker

这是一个独立评测机 Worker：从 Kafka 接收 `SubmissionMessage`，从 S3 下载题目 zip 与提交代码，编译并逐测试点运行；每测完一个测试点立即向 Kafka 发送一次 `ResultMessage`（最后一条 `isOver=true`）。

## 1) Maven 构建

```bash
mvn -DskipTests package
```

产物：`target/judge-worker-1.0.0.jar`

## 2) 运行（本地）

```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export KAFKA_SUBMISSION_TOPIC=submission
export KAFKA_RESULT_TOPIC=result

export S3_ENDPOINT=http://localhost:8333
export S3_REGION=ap-northeast-1
export S3_ACCESS_KEY=admin
export S3_SECRET_KEY=admin
export S3_BUCKET=oj-data

java -jar target/judge-worker-1.0.0.jar
```

## 3) S3 路径约定（与 FileSystemManager 一致）

- 题目包：`s3://{bucket}/problem/{problemId}.zip`
- 提交代码：`s3://{bucket}/submission/{submissionId}/code.cpp`
- 题目 zip 解压结构：`config.yml`、`statement.md`、`testcases/*.in/*.out`

> 如果你强制要用 `problems/{problemId}.zip` 前缀，请把 `FileSystemManager` 里 `problem/` 改成 `problems/`，或者做一个“先试 problem/，失败再试 problems/”的兼容逻辑。

## 4) Docker

```bash
docker build -f Dockerfile.judge -t oj-judge-worker:1.0.0 .
```

`docker-compose.yml` 中挂载/启动该镜像即可。

### 沙盒依赖

运行阶段依赖：`g++`、`nsjail`、`/usr/bin/time`。

如果你的宿主机/内核对 nsjail 限制较严格，容器可能需要额外能力（例如 `privileged: true` 或 `cap_add`）。

## 5) Kafka 约定

- 入站 topic：`KAFKA_SUBMISSION_TOPIC`（默认 `submission`）
- 出站 topic：`KAFKA_RESULT_TOPIC`（默认 `result`）
- 编译信息：用 `testCaseId=0` 回传
- 逐测试点：`testCaseId=1..N`，每个测试点结束立刻回传一次；最后一条 `isOver=true`
