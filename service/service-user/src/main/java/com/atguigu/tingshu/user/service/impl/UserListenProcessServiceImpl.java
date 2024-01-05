package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private KafkaService kafkaService;

    @Override
    public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
        //	创建一个存储声音对象
        UserListenProcess userListenProcess = null;
        //	判断MongoDB中是否已经存储声音对象
        Query query = new Query();
        query.addCriteria(Criteria.where("trackId").is(userListenProcessVo.getTrackId()));
        userListenProcess = this.mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        //	判断
        if (null == userListenProcess) {
            //	创建对象
            userListenProcess = new UserListenProcess();
            //	设置专辑ID，声音ID和跳出时间
            BeanUtils.copyProperties(userListenProcessVo, userListenProcess);
            userListenProcess.setId(ObjectId.get().toString());
            userListenProcess.setUserId(userId);
            userListenProcess.setIsShow(1);
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        } else {
            //	更新跳出时间
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        }

        // 更新专辑与声音播放量，同一个用户同一个声音10分钟只记录一次播放量
        // 专辑的播放量 = 声音播放量的总和
        String key = "user:track:" + userId;
        //	获取数据
        Boolean inTenMinus = this.redisTemplate.opsForValue().getBit(key, userListenProcessVo.getTrackId());
        // 如果是该用户10分钟内第一次进入，则修改redis里的播放量，并使用kafka发生消息更新mysql
        if (!inTenMinus) {
            this.redisTemplate.opsForValue().setBit(key, userListenProcessVo.getTrackId(), true);
            this.redisTemplate.expire(key, 10, TimeUnit.MINUTES);
            // 修改播放量
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
            trackStatMqVo.setCount(1);
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
            kafkaService.sendMessage(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));
        }
    }

    @Override
    public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
        //	根据用户Id,声音Id获取播放进度对象
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        //	判断
        if (null != userListenProcess) {
            //	获取到播放的跳出时间
            return userListenProcess.getBreakSecond();
        }
        return new BigDecimal("0");
    }

    @Override
    public Map<String, Object> getLatestTrack(Long userId) {
        // 构建query对象，根据用户ID查找mangodb中存储的声音，并按照更新时间降序
        Query query = Query.query(Criteria.where("userId").is(userId));
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        query.with(sort);
        // 根据条件查询数据并返回结果
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        HashMap<String, Object> map = new HashMap<>();
        if (null == userListenProcess) {
            return null;
        }
        map.put("albumId", userListenProcess.getAlbumId());
        map.put("trackId", userListenProcess.getTrackId());
        return map;
    }
}
