package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

    BigDecimal getTrackBreakSecond(Long trackId, Long userId);


    /**
     * 获取最近一次播放的声音
     * @param userId
     * @return
     */
    Map<String, Object> getLatestTrack(Long userId);
}
