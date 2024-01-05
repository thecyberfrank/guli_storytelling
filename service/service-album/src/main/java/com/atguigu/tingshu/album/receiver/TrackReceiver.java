package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AlbumStatService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.TrackStatService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TrackReceiver {

    @Resource
    RedisTemplate redisTemplate;

    @Resource
    TrackStatService trackStatService;

    @Resource
    AlbumStatService albumStatService;

    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void trackPlayNumUpdate(ConsumerRecord<String, String> record) {
        String jsonData = record.value();
        if (!jsonData.isEmpty()) {
            TrackStatMqVo trackStatMqVo = JSON.parseObject(jsonData, TrackStatMqVo.class);
            // 使用redis判断是否重复消费，如果是Absent则返回true并赋值，如果是有了，则是重复消费，不处理
            Boolean isNotConsumed = redisTemplate.opsForValue().setIfAbsent(trackStatMqVo.getBusinessNo(), "ok", 1, TimeUnit.DAYS);
            if (isNotConsumed) {

                //更新声音统计信息
                LambdaUpdateWrapper<TrackStat> trackStatUpdateWrapper = new LambdaUpdateWrapper<>();
                trackStatUpdateWrapper.eq(TrackStat::getTrackId, trackStatMqVo.getTrackId());
                trackStatUpdateWrapper.eq(TrackStat::getStatType, trackStatMqVo.getStatType());
                trackStatUpdateWrapper.setSql("stat_num = stat_num + " + trackStatMqVo.getCount());
                trackStatService.update(trackStatUpdateWrapper);

                //更新专辑统计信息
                LambdaUpdateWrapper<AlbumStat> albumStatUpdateWrapper = new LambdaUpdateWrapper<>();
                albumStatUpdateWrapper.eq(AlbumStat::getAlbumId, trackStatMqVo.getAlbumId());
                albumStatUpdateWrapper.eq(AlbumStat::getStatType, trackStatMqVo.getStatType());
                albumStatUpdateWrapper.setSql("stat_num = stat_num + " + trackStatMqVo.getCount());
                albumStatService.update(albumStatUpdateWrapper);
            }
        }
    }
}
