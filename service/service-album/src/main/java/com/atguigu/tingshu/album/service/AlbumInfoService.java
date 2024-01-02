package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /**
     * 保存专辑
     *
     * @param albumInfoVo
     * @param userId
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId);

    /**
     * 根据条件查询专辑列表
     *
     * @param albumListVoPage
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> selectUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery);

    /**
     * 根据专辑Id删除专辑数据
     *
     * @param albumId
     */
    void removeAlbumInfo(Long albumId);

    /**
     * 根据专辑Id回显专辑数据
     *
     * @param albumId
     * @return
     */
    AlbumInfo getAlbumInfoById(Long albumId);

    /**
     * 修改专辑
     *
     * @param albumInfoVo
     * @param albumId
     */
    void updateAlbumInfo(AlbumInfoVo albumInfoVo, Long albumId);

    /**
     * 查询用户Id对应的专辑列表
     *
     * @param userId
     * @return
     */
    List<AlbumInfo> findUserAllAlbumList(Long userId);


    /**
     * 获取专辑属性值列表
     *
     * @param albumId
     * @return
     */
    List<AlbumAttributeValue> findAlbumAttributeValue(Long albumId);

    /**
     * 根据专辑ID获取专辑的统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(Long albumId);
}

