package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;

	@Override
	public void updateUser(UserInfoVo userInfoVo, Long userId) {
		//	创建对象
		UserInfo userInfo = new UserInfo();
		//	属性拷贝：
		BeanUtils.copyProperties(userInfoVo,userInfo);
		userInfo.setId(userId);
		//	修改方法
		userInfoMapper.updateById(userInfo);
	}

	@Override
	public UserInfoVo getUserInfoById(Long userId) {
		//	获取用户信息
		UserInfo userInfo = this.getById(userId);
		//	创建UserInfoVo 对象
		UserInfoVo userInfoVo = new UserInfoVo();
		//	属性拷贝：
		BeanUtils.copyProperties(userInfo,userInfoVo);
		return userInfoVo;
	}
}
