package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    /**
     * 根据用户Id 获取用户信息
     *
     * @param userId
     * @return
     */
    UserInfoVo getUserInfoById(Long userId);

    /**
     * 修改用户信息
     *
     * @param userInfoVo
     * @param userId
     */
    void updateUser(UserInfoVo userInfoVo, Long userId);

    /**
     * 获取用户信息
     *
     * @param userId
     * @return
     */
    UserInfo getUserInfoVo(Long userId);


    /**
     * 判断用户是否购买声音列表
     * @param albumId
     * @param trackIdList
     * @param userId
     * @return
     */
    Map<Long, Integer> userIsPaidTrack(Long albumId, List<Long> trackIdList, Long userId);

    /**
     * 判断用户是否购买过专辑
     * @param albumId
     * @param userId
     * @return
     */
    Boolean isPaidAlbum(Long albumId, Long userId);

    /**
     * 获取用户已购买的声音Id 列表。
     * @param albumId
     * @param userId
     * @return
     */
    List<Long> findUserAlreadyPaidTrackList(Long albumId, Long userId);
}