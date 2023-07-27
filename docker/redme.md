# docker
> 服务镜像启动，描述

### 使用镜像 zlmediakit_ubuntu:v1.0
> zlmediakit 一个基于C++11的高性能运营级流媒体服务框架
> github地址： https://github.com/ZLMediaKit/ZLMediaKit
> zlmediakit_ubuntu:v1.0 镜像基于 ubuntu20.04 进行编译
```shell
# zlm 服务器 启动命令
docker run -idt -p 1935:1935 -p 80:80 -p 8554:554 -p 10000:10000 -p 10000:10000/udp -p 8000:8000/udp -p 30000-30500:30000-30500 -p 30000-30500:30000-30500/udp zlmediakit_ubuntu:v1.0 /bin/bash
# 启动服务命令
/opt/media/MediaServer -d
```

### 使用镜像 mysql:8.0.30
> 官方 mysql 镜像 版本： 8.0.30

```shell
# 拉取 mysql:8.0.30 版本镜像
docker pull mysql:8.0.30
# 启动mysql命令
docker run -itd --name mysql-test -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0.30
```
