# 简易的 OJ 项目

## 数据库

目前你需要自行创建数据库来完成本地调试。

在 mysql 下你可以运行 `sqlschema` 来创建表单，在此之前你需要有一名为 `oj_admin`, 密码为 `oj_token` 的账账户，并赋予数据库 `oj_db` 的权限，或者修改 `main` 下的 `appilication.yml`。

由于开启了 spring-dev，请务必先编译后运行，目前只有 main。
```shell
mvn clean install
mvn spring-boot:run -pl main
```

## Update

- 2025-11-25 注册和登录完成了。

- 2025-11-30 ~ 2025-12-02 写完 judger 的评测核心。——by szm