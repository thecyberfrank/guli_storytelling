package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.service.SearchService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SearchReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 专辑上架
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void albumUpper(ConsumerRecord<String,String> record){
        //  获取发送的数据.
        String albumId = record.value();
        //  判断
        if (!StringUtils.isEmpty(albumId)){
            //  调用上架方法
            searchService.upperAlbum(Long.parseLong(albumId));
        }
    }

    /**
     * 专辑下架
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String,String> record){
        //  获取发送的数据.
        String albumId = record.value();
        //  判断
        if (!StringUtils.isEmpty(albumId)){
            //  调用上架方法
            searchService.lowerAlbum(Long.parseLong(albumId));
        }
    }
}
