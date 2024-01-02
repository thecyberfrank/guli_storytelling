package com.atguigu.tingshu.album.client.impl;


import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlbumInfoDegradeFeignClient implements AlbumInfoFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        return null;
    }

    @Override
    public Result<List<AlbumAttributeValue>> findAlbumAttributeValue(Long albumId) {
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        return null;
    }
}
