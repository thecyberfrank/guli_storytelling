package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    //  监听用户注册消息
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void userRegister(ConsumerRecord<String, String> record) {
        //  获取数据
        String userId = record.value();
        //  判断
        if (!StringUtils.isEmpty(userId)) {
            //  调用服务层方法 Long.parseLong(userId)
            userAccountService.initUserAccount(Long.parseLong(userId));
        }
    }
}