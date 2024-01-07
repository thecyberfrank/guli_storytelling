package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.TrackInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = TrackInfoDegradeFeignClient.class)
public interface TrackInfoFeignClient {

    /**
     * 获取要支付的声音列表
     *
     * @param itemId
     * @param trackCount 表示该声音的后N个声音
     * @return
     */
    @GetMapping("api/album/trackInfo/findTrackInfoNeedToPayList/{itemId}/{trackCount}")
    Result<List<TrackInfo>> findTrackInfoNeedToPayList(@PathVariable Long itemId, @PathVariable Integer trackCount);
}