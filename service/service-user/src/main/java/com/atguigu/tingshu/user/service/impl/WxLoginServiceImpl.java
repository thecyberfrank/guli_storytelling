package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.WxLoginService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class WxLoginServiceImpl implements WxLoginService {


    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> wxLogin(String code) {
        //  获取openId
        WxMaJscode2SessionResult sessionInfo = null;
        try {
            sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
        String openId = sessionInfo.getOpenid();
        UserInfo userInfo = userInfoService.getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openId));
        //  如果数据库中没有这个对象
        if (null == userInfo) {
            //  创建对象
            userInfo = new UserInfo();
            //  赋值用户昵称
            userInfo.setNickname("听友" + System.currentTimeMillis());
            //  赋值用户头像图片
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            //  赋值wxOpenId
            userInfo.setWxOpenId(openId);
            //  保存用户信息
            userInfoService.save(userInfo);
            /**
             * 这里要初始化用户信息，和账户信息。需要远程调用账户信息微服务，这两个操作应该放到一个事务里，或者放到消息队列里可重试
             * 1. 可以使用kafka发送创建account的消息，account微服务接收消息，并创建账户
             * 发送消息或接收消息失败的重试方法: redis-记录重试次数，如果重试次数>3，则insert xx into exception_msg; 后续人工处理:
             *
             * 2. 可以使用分布式事务，创建账户失败后回滚，让账户信息也失败，抛出异常。
             */

            kafkaService.sendMessage(KafkaConstant.QUEUE_USER_REGISTER, userInfo.getId().toString());
        }

        //  创建 token
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        //  将用户信息存到redis hash中
        redisTemplate.opsForValue().set(
                RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                userInfo,
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        //  将这个数据存储到map中并返回
        HashMap<String, Object> map = new HashMap<>();
        map.put("token", token);
        return map;
    }
}
