package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private TrackStatMapper trackStatMapper;

    @Autowired
    private VodService vodService;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Override
    public void updateTrackInfo(TrackInfoVo trackInfoVo, Long trackId) {
        //  获取到声音对象
        TrackInfo trackInfo = this.getById(trackId);
        //  获取到原有的流媒体Id
        String mediaFileId = trackInfo.getMediaFileId();
        //  track_info；考虑声音重新上传问题！
        //  属性拷贝：
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        //  考虑声音重新上传问题！
        if (!mediaFileId.equals(trackInfoVo.getMediaFileId())) {
            //  说明这个声音被重新上传了. 对应流媒体声音属性要修改。 调用vodService方法
            TrackMediaInfoVo trackMediaInfo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
            if (null == trackMediaInfo) {
                //	抛出异常
                throw new GuiguException(ResultCodeEnum.VOD_FILE_ID_ERROR);
            }
            //  赋值最新数据.
            trackInfo.setMediaUrl(trackMediaInfo.getMediaUrl());
            trackInfo.setMediaType(trackMediaInfo.getType());
            trackInfo.setMediaDuration(trackMediaInfo.getDuration());
            trackInfo.setMediaSize(trackMediaInfo.getSize());
        }
        //  修改：
        trackInfoMapper.updateById(trackInfo);
    }

    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId) {
        //  1.  根据专辑Id 获取到对应的声音列表集合.
        IPage<AlbumTrackListVo> iPage = albumInfoMapper.selectAlbumTrackPage(albumTrackListVoPage, albumId);
        //  根据专辑Id 获取到专辑对象
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        //  2.  判断用户是否登录
        if (null == userId) {
            //  当前专辑是付费的情况下，所有的声音列表都需要付费！
            //  0101-免费 0102-vip付费 0103-付费
            if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType())) {
                //  设置付费标识. {免费的要出去 tracks_for_free=5 } boolean test(T t);
                //  如果试听声音有删除 select * from track_info track where track.album_id = 139 and track.is_deleted = 0 order by id limit 5;
                List<AlbumTrackListVo> albumTrackListVoList = iPage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree()).collect(Collectors.toList());
                for (AlbumTrackListVo albumTrackListVo : albumTrackListVoList) {
                    //  设置付费标识：
                    albumTrackListVo.setIsShowPaidMark(true);
                }
            }
        } else {
            boolean isNeedPaid = false;
            //  看专辑类型： 0102-vip付费
            if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(albumInfo.getPayType())) {
                //  看用户是否属于vip. is_vip 字段.
                Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
                Assert.notNull(userInfoVoResult, "用户返回结果集为空");
                UserInfoVo userInfoVo = userInfoVoResult.getData();
                Assert.notNull(userInfoVo, "用户信息为空");
                //  判断用户类型 非vip
                if (userInfoVo.getIsVip() == 0) {
                    isNeedPaid = true;
                }
                //  用户是vip 但是，已经过期了.
                if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().before(new Date())) {
                    isNeedPaid = true;
                }
            } else if (SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(albumInfo.getPayType())) {
                //  0103-付费
                isNeedPaid = true;
            }
            //  统一处理付费声音标识：
            if (isNeedPaid) {
                //  有哪些声音需要付费? 6,7,8,9,10  buy=9 67810￥user_paid_track  购买了整张专辑.user_paid_album
                List<AlbumTrackListVo> albumTrackListVoList = iPage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree()).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(albumTrackListVoList)) {
                    //  获取到专辑对应的声音id集合列表。
                    List<Long> trackIdList = albumTrackListVoList.stream().map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
                    //  获取用户是否购买过专辑或声音Id map.put(trackId,1); map.get(trackId);
                    //  key=trackId value=0/1 0:表示用户没有购买过声音Id 1:表示购买过.
                    Result<Map<Long, Integer>> mapResult = userInfoFeignClient.userIsPaidTrack(albumId, trackIdList);
                    Assert.notNull(mapResult, "处理声音结果集为空");
                    Map<Long, Integer> map = mapResult.getData();
                    Assert.notNull(map, "声音map集合为空");
                    //  循环遍历
                    for (AlbumTrackListVo albumTrackListVo : albumTrackListVoList) {
                        //  判断是否需要付费
                        boolean buy = map.get(albumTrackListVo.getTrackId()) == 1 ? false : true;
                        //  设置付费标识
                        albumTrackListVo.setIsShowPaidMark(buy);
                    }
                }
            }
        }
        return iPage;
    }

    @Override
    public TrackStatVo getTrackStatVoByTrackId(Long trackId) {
        //	调用mapper 层方法
        return trackInfoMapper.selectTrackStat(trackId);
    }

    @Override
    public TrackInfo getTrackInfo(Long trackId) {
        //  获取声音对象.
        return this.getById(trackId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long trackId) {
        //	获取到专辑对象：已知声音Id 求专辑对象? 已知声音Id--->track_info.album_id-->album_info.include_track_count
        //	this.getById();// ServiceImpl getById()
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        //	专辑对象
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        //	track_info track_stat album_info.include_track_count-1; 云点播
        //  this:当前类的对象。this.removeById(trackId); == trackInfoMapper.deleteById(trackId);
        this.removeById(trackId);

        //	构建删除条件:
        //	delete from track_stat where track_id = ?;
        trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));

        //	必须知道更新的字段与更新的数据.  include_track_count=include_track_count-1
        if (null != albumInfo) {
            albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        }
        //  更新专辑包含的声音总数
        albumInfoMapper.updateById(albumInfo);
        //  删除声音服务器上的声音
        vodService.removeTrack(trackInfo.getMediaFileId());
    }

    @Override
    public IPage<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery) {
        //	sql 语句：
        return trackInfoMapper.selectUserTrackPage(trackListVoPage, trackInfoQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
        //	track_info track_stat 新增
        //	创建对象
        TrackInfo trackInfo = new TrackInfo();
        //	赋值：
        //	属性拷贝：
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        trackInfo.setUserId(userId);
        //	order_num: 集数：
        //	select * from track_info where album_id = 1890 order by order_num desc limit 1;
        //	初始化一个变量
        int orderNum = 1;
        //	判断当前专辑的OrderNum 是否有数据.
        //	编写查询条件
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, trackInfoVo.getAlbumId()).select(TrackInfo::getOrderNum).orderByDesc(TrackInfo::getOrderNum).last("limit 1");
        TrackInfo preTrackInfo = trackInfoMapper.selectOne(wrapper);
        //	防止空指针异常
        if (null != preTrackInfo) {
            orderNum = preTrackInfo.getOrderNum() + 1;
        }
        trackInfo.setOrderNum(orderNum);
        //	给流媒体声音赋值：
        TrackMediaInfoVo trackMediaInfo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
        trackInfo.setMediaSize(trackMediaInfo.getSize());
        trackInfo.setMediaType(trackMediaInfo.getType());
        trackInfo.setMediaUrl(trackMediaInfo.getMediaUrl());
        trackInfo.setMediaDuration(trackMediaInfo.getDuration());
        //	trackInfo.setStatus();
        trackInfoMapper.insert(trackInfo);

        //	track_stat
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT);
        //	album_info 更新 当前专辑所包含的声音集数.
        //	根据专辑Id 获取到专辑对象.
        AlbumInfo albumInfo = this.albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        //	先获取到原有的声音集数再+1  第一条数据没有将包含声音改为1; 原来是0 +1
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        //	albumInfo.setIncludeTrackCount(preTrackInfo.getOrderNum()+1);
        //	集数应该是1 但是是0!
        //		LambdaQueryWrapper<AlbumInfo> wrapper1 = new LambdaQueryWrapper<>();
        //		wrapper1.eq(AlbumInfo::getId,trackInfoVo.getAlbumId());
        //		this.albumInfoMapper.update(albumInfo,wrapper1);
        this.albumInfoMapper.updateById(albumInfo);


    }

    private void saveTrackStat(Long trackId, String statPlay) {
        //	创建对象
        TrackStat trackStat = new TrackStat();
        //	赋值：
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statPlay);
        trackStat.setStatNum(0);
        trackStatMapper.insert(trackStat);
    }

    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {
        //	声明map 集合
        Map<String, Object> map = new HashMap<>();
        //	初始化客户端
        VodUploadClient client = new VodUploadClient(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        //	构造上传请求对象
        VodUploadRequest request = new VodUploadRequest();
        //	设置路径
        String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
        request.setMediaFilePath(tempPath);
        //	封面：request.setCoverFilePath("/data/videos/Wildlife.jpg");
        //	任务流：request.setProcedure("Your Procedure Name");
        //	APPID: request.setSubAppId(vodConstantProperties.getAppId());
        //	调用上传方法.
        try {
            VodUploadResponse response = client.upload(vodConstantProperties.getRegion(), request);
            log.info("Upload FileId = {}", response.getFileId());
            map.put("mediaFileId", response.getFileId());
            map.put("mediaUrl", response.getMediaUrl());
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error("Upload Err", e);
        }
        //	返回数据
        return map;
    }
}
