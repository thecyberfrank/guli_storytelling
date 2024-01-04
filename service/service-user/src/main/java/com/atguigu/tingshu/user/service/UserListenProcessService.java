package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;

public interface UserListenProcessService {

    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

    BigDecimal getTrackBreakSecond(Long trackId, Long userId);
}
