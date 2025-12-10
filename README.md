# 简易的 OJ 项目

后端已经大致容器化。

## Usage

```sh
cd backend
docker compose -f docker-compose.yml up --build
```

如果 `backend` 镜像构建好了，就不需要 build 了。

移除容器：记得删除卷

```sh
docker compose -p backend down -v
```


## Update

- 2025-12-10 评测机基本完整，准备消息队列对接

- 2025-11-25 注册和登录完成了。

- 2025-11-30 ~ 2025-12-02 写完 judger 的评测核心。——by szm

## TODO

当前的 mysql, kafka 认证是公开的，纯文本传输的，后期会加入认证。