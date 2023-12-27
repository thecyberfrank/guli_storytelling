package com.atguigu.tingshu.common.service;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class KafkaService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);


    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 发送消息
     *
     * @param topic
     * @param value
     */
    public boolean sendMessage(String topic, String value) {
        //  CompletableFuture 异步编排对象
        CompletableFuture completableFuture = this.kafkaTemplate.send(topic, value);
        completableFuture.thenAccept(result -> {
            logger.debug("发送消息成功: topic={}，value={}", topic, JSON.toJSONString(value));
        }).exceptionally(result -> {
            //  发生失败.
            logger.debug("发送消息失败: topic={}，value={}", topic, JSON.toJSONString(value));
            //  调用发送消息的重试方法: 1.redis-记录重试次数； 2. this.kafkaTemplate.send(topic, value); count>3  3.本地消息记录 insert into expection_msg values(); 人工处理:
            return false;
        });
        return true;
    }
}
