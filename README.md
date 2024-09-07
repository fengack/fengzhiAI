# 峰智BI项目后端

## 主流框架 & 特性

- Spring Boot 2.7.x
- Spring MVC
- MyBatis + MyBatis Plus 数据访问（开启分页）
- Spring Boot 调试工具和项目处理器
- Redis请求限流
- RabbitMQ实现异步操作

## 数据存储

- MySQL 数据库
- Redis 内存数据库

## 工具类

- Easy Excel 表格处理
- Hutool 工具库
- Gson 解析库
- Lombok 注解

## 业务特性

- Redis 分布式登录
- 全局请求响应拦截器
- 全局异常处理器
- 自定义错误码
- 封装通用响应类
- Swagger + Knife4j 接口文档
- 自定义权限注解 + 全局校验
- 全局跨域处理
- 长整数丢失精度解决
- 多环境配置

## MySQL 数据库

1）修改 `application.yml` 的数据库配置为你自己的：

```yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://#######
    username: #######
    password: #######
```

2）执行 `sql/create_table.sql` 中的数据库语句，自动创建库表

3）启动项目，访问 `http://localhost:8080/api/doc.html` 即可打开接口文档，不需要写前端就能在线调试接口了~

## Redis分布式缓存

1）修改 `application.yml` 的 Redis 配置为你自己的：

```yml
spring:
  redis:
    database: #######
    host: #######
    port: #######
    timeout: #######
    password: #######
```

## 科大讯飞星火大模型

1）修改 `application.yml`，通过注册科大讯飞星火大模型的账号可免费领取Spark-3.5（Spark Max）的使用token，将自己的api-key，api-secret，appid填入。

```
xunfei:
  client:
    api-key: #######
    api-secret: ########
    appid: ########
# 智普 AI
ai:
  apiKey: ########
```

## RabbitMQ配置

1）修改` application.yml`

```
  rabbitmq:
    host: #######
    port: #######
    username: #######
    password: #######
    virtual-host: #######
```