## 项目背景
随着智能手机和高速互联网的普及，人们开始寻求更便捷的方式来获取信息和娱乐。有声书的出现使得人们可以在旅途中、跑步时、做家务时等各种场景下，以更加灵活的方式享受阅读。在过去，有声书主要是由专业的演员朗读，制作成录音带或CD。但随着数字化媒体的发展，听书软件应运而生，为用户提供了更多选择，包括自助出版的有声书和多样化的内容。

## 项目技术栈
- **SpringBoot**：简化Spring应用的初始搭建以及开发过程
- **SpringCloud**：基于Spring Boot实现的云原生应用开发工具，SpringCloud使用的技术：（Spring Cloud Gateway、Spring Cloud Task和Spring Cloud Feign等）
- **SpringBoot+SpringCloudAlibaba(Nacos，Sentinel)+Cloud OpenFeign**
- MyBatis-Plus：持久层框架，也依赖mybatis
- Redis：内存做缓存
- Redisson：基于redis的Java驻内存数据网格 - 框架；操作redis的框架
- MongoDB: 分布式文件存储的数据库
- Kafka：消息中间件；大型分布式项目是标配；分布式事务最终一致性
- ElasticSearch+Kibana+Logstash/Filebeat 全文检索服务器+可视化数据监控：检索
- ThreadPoolExecutor+CompletableFuture：线程池来实现异步操作，提高效率
- xxl-Job: 分布式定时任务调用中心
- Knife4J/YAPI：Api接口文档工具
- MinIO（私有化对象存储集群）：分布式文件存储 类似于OSS（公有）
- MySQL：关系型数据库 {shardingSphere-jdbc 进行读写分离; 分库，分表}
- Lombok: 实体类的中get/set 生成的jar包
- natapp：内网穿透
- Docker：容器化技术;  生产环境Redis（运维人员）；快速搭建环境Docker run
- 微信支付

![jiagoutu](https://github.com/thecyberfrank/guli_storytelling/assets/148995949/579803be-ae8d-4ef6-8a4f-9e7160a968fa)
