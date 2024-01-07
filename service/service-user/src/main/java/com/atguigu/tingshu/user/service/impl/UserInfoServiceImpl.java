package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Override
    public UserInfo getUserInfoVo(Long userId) {
        return userInfoMapper.selectById(userId);
        //	return this.getById(userId);
    }

    /**
     * @param albumId     专辑Id
     * @param trackIdList 专辑Id对应的声音列表。
     * @param userId      用户Id
     * @return
     */
    @Override
    public Map<Long, Integer> userIsPaidTrack(Long albumId, List<Long> trackIdList, Long userId) {
        // 获取用户是否购买过专辑 user_paid_album 或声音Id user_paid_track
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getAlbumId, albumId).eq(UserPaidAlbum::getUserId, userId));
        //	判断这个专辑对象是否为空.
        if (null != userPaidAlbum) {
            //	购买了专辑.所有的声音Id 都免费. key=trackId,value=1
            Map<Long, Integer> map = trackIdList.stream().collect(Collectors.toMap(trackId -> trackId, trackId -> 1));
            //	返回数据
            return map;
        }
        //	判断用户是否购买过声音Id.
        //	select * from user_paid_track where user_id = 26 and track_id in (48246,48247,48888);
        //	获取用户购买的声音集合对象 48246,48247
        //	判断：所以需要付费的声音Id 是否包含用户购买的声音Id
        HashMap<Long, Integer> map = new HashMap<>();
        //	获取数据
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getUserId, userId).in(UserPaidTrack::getTrackId, trackIdList));
        if (!CollectionUtils.isEmpty(userPaidTrackList)) {
            //	获取到用户购买过声音Id 集合
            List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            //	循环遍历
            trackIdList.stream().forEach(trackId -> {
                Integer value = userPaidTrackIdList.contains(trackId) ? 1 : 0;
                //	存储数据
                map.put(trackId, value);
            });
        } else {
            //	没有购买过任何记录信息。
            trackIdList.stream().forEach(trackId -> {
                //	存储数据
                map.put(trackId, 0);
            });
        }
        //	返回map集合
        return map;
    }

    @Override
    public Boolean isPaidAlbum(Long albumId, Long userId) {
        //	select * from user_paid_album where user_id = ? and album_id = ?;
        LambdaQueryWrapper<UserPaidAlbum> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        queryWrapper.eq(UserPaidAlbum::getUserId, userId);
        UserPaidAlbum paidAlbum = userPaidAlbumMapper.selectOne(queryWrapper);
        return null == paidAlbum ? false : true;
    }

    @Override
    public List<Long> findUserAlreadyPaidTrackList(Long albumId, Long userId) {
        List<Long> trackIdList = new ArrayList<>();
        //	user_paid_track -- 记录着用户购买专辑对应的声音Id.
        //	select * from user_paid_track where album_id = ? and user_id = ?;
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getAlbumId,albumId).eq(UserPaidTrack::getUserId,userId);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(userPaidTrackList)){
            //  返回数据。
            trackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        }
        return trackIdList;
        //  return userPaidTrackMapper.selectList(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getAlbumId, albumId).eq(UserPaidTrack::getUserId, userId)).stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
    }


    @Override
    public void updateUser(UserInfoVo userInfoVo, Long userId) {
        //	创建对象
        UserInfo userInfo = new UserInfo();
        //	属性拷贝：
        BeanUtils.copyProperties(userInfoVo, userInfo);
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
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return userInfoVo;
    }
}
