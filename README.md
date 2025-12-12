# 简易的 OJ 项目

前后端已经完成并容器化。

当前评测暂时无法工作，等待评测系统完成。

## Usage

```sh
cd backend
docker compose -f docker-compose.yml up --build
# 初次使用加 --build
```

关停

```sh
docker compose -p backend down
```

使用了 nginx，当前在 `81` 端口上运行。


## Update

- 2025-12-12 前后端全部完成，反代完成。

- 2025-12-10 评测机基本完整，准备消息队列对接

- 2025-11-25 注册和登录完成了。

- 2025-11-30 ~ 2025-12-02 写完 judger 的评测核心。——by szm

## Debug

目前我们给 seaweedfs 开放了访问，在启动后你可以访问 `8888` 端口查看文件，`9333` 端口监控状态。

对于 seaweedfs 交互端口 `8333`，启用了 jwt 认证，相关内容可以在 docker-compose.yml 中找到。

kafka 端口 `9292` 目前暴露公开

## TODO

当前的 mysql, kafka 认证是公开的，纯文本传输的，后期会加入认证。