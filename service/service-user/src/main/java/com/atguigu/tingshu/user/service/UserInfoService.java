package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
