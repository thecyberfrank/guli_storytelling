package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 上传声音：
     *
     * @param file
     * @return
     */
    Map<String, Object> uploadTrack(MultipartFile file);

    /**
     * 保存声音
     *
     * @param trackInfoVo
     * @param userId
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);

    /**
     * 查询声音分页列表
     *
     * @param trackListVoPage
     * @param trackInfoQuery
     * @return
     */
    IPage<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery);

    /**
     * 根据声音Id删除数据
     *
     * @param trackId
     */
    void removeTrackInfo(Long trackId);

    /**
     * 根据Id 获取数据
     *
     * @param trackId
     * @return
     */
    TrackInfo getTrackInfo(Long trackId);

    /**
     * 修改声音
     *
     * @param trackInfoVo
     * @param trackId
     */
    void updateTrackInfo(TrackInfoVo trackInfoVo, Long trackId);

    /**
     * 声音分类列表
     *
     * @param albumTrackListVoPage
     * @param albumId
     * @param userId
     * @return
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId);

    /**
     * 根据声音Id 获取统计信息
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVoByTrackId(Long trackId);

    /**
     * 根据声音Id 获取购买列表
     * @param trackId
     * @return
     */
    List<Map<String, Object>> findUserNeedToPayTrackList(Long userId,Long trackId);

    /**
     * 根据声音ID和购买数量，获取需要付钱的声音信息
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> findTrackInfoNeedToPayList(Long trackId, Integer trackCount);
}
