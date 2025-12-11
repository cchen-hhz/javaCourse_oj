现在要把评测机分离出来，直接单独起一个项目，`common` 中的模块完全不用，不用管复用的问题。

可以阅读 backend 中的组件编写获得更多信息。

通信完全基于 kafka 。

## kafka 要求

- consumer 和 producer 的序列化反序列化策略都用 org.springframework.kafka.support.serializer.JsonSerializer
- 两个 Message 的定义应该相同：

## message 规范

```java
public class SubmissionMessage {
    public Long submissionId;
    public Long problemId;
    public String language;
}

```

注意 ResultMessage 的注释内容。

```java
public class ResultMessage {
    private Long submissionId;
    private Long problemId;
    
    private Long testCaseId; // 编号，id=0 在 message 存储编译信息等，指示评测开始
    private Long numCases; // 总评测用例
    private Long score;
    private Long timeUsed;
    private Long memoryUsed;
    private Long status;

    private String input;
    private String expectedOutput;
    private String userOutput;
    private String message;

    private Boolean correct; // 当前评测是否完整，若为 false 应当呼出 system_error
    private Boolean isOver; // 若为真则评测结束，忽略后续的数据点
}
```

Status 的映射表如下，（有多的也可以加）：

```plain
-2: System Error (SE)
-1: Compile Error (CE)
0: Accepted (AC)
1: Wrong Answer (WA)
2: Time Limit Exceeded (TLE)
3: Memory Limit Exceeded (MLE)
4: Runtime Error (RE)
```

## 文件读取

我们采用了自建 `seaweedfs` 读取文件。

评测应该只需要读取文件，从 s3 中读取：

你需要配置 s3 ，相关代码在 common/config/S3config, S3Properties

参数应该写在 application.yml 中，main 中的参数如下，可以参考：

```yml
spring:
  application:
    name: oj-application
  web:
    resources:
      add-mappings: false
  datasource:
    url: jdbc:mysql://localhost:3306/oj_db
    username: oj_admin
    password: oj_token
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 50MB
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        "[spring.json.add.type.headers]": false
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        "[spring.json.trusted.packages]": "*"
        "[spring.json.value.default.type]": "com.edu.oj.message.ResultMessage"
    topic:
      submission: submission-queue
      result: judge-result

mybatis:
  configuration:
    map-underscore-to-camel-case: true
  type-aliases-package: com.edu.oj.entity

logging:
  level:
    web: DEBUG

server:
  port: 8080
  servlet:
    session:
      timeout: 60m

s3:
  endpoint: http://localhost:8333
  region: us-east-1
  access-key: admin
  secret-key: admin
  bucket: oj-data
```

保持 localhost，后续装载 docker 时会覆盖部分路由。

其中 kafka 中 topic 的定义和序列化默认对象的定义需要修改一下。

配置请完全放在 application.yml 中，不要在主逻辑中加配置。

题目文件键为 "problem/" + problemId + ".zip" ，内部目录包含一个 config.yml 和 statement.md 和 testcases 文件夹。你应该需要自行读取解压。

提交代码的键为 "submission/" + submissionId + "code." + extension ，extension 是后缀名，在 SubmissionMessage 中有写可以拼接。

## 关键重构

评测机不使用 docker ，完全由 shell 做内存隔离（因为这玩意要挂到 docker 上，直接 DinD 完蛋了）。

每评测一个测试点都发送消息，输出做截断等等。









