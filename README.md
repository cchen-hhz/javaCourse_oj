# 简易的 OJ 项目

demo 已经基本完成！

## Usage

微服务的启动需要一定时间，在完全运转大致 10s 左右后再进入操作（这个后续会修的）。

```sh
docker compose -f docker-compose.yml up #--build 
```

使用了 nginx，当前在 `81` 端口上运行。

## Update

- 2025-12-18 合并了评测模块，demo 基本运转。

- 2025-12-12 前后端全部完成，反代完成。

- 2025-12-10 评测机基本完整，准备消息队列对接

- 2025-11-25 注册和登录完成了。

- 2025-11-30 ~ 2025-12-02 写完 judger 的评测核心。——by szm

## Debug

当前端口已经被屏蔽，如需打开访问请修改 `docker-compose.yml` 。

目前 seaweedfs 在启动后可以访问 `8888` 端口查看文件，`9333` 端口监控状态。

对于 seaweedfs 交互端口 `8333`，启用了 jwt 认证，相关内容可以在 docker-compose.yml 中找到。

kafka 端口使用 `9292` 

## TODO

当前的 mysql, kafka 认证是公开的，纯文本传输的，后期会加入认证。