package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

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

	@Override
	public void updateTrackInfo(TrackInfoVo trackInfoVo, Long trackId) {
		//  获取到声音对象
		TrackInfo trackInfo = this.getById(trackId);
		//  获取到原有的流媒体Id
		String mediaFileId = trackInfo.getMediaFileId();
		//  track_info；考虑声音重新上传问题！
		//  属性拷贝：
		BeanUtils.copyProperties(trackInfoVo,trackInfo);
		//  考虑声音重新上传问题！
		if (!mediaFileId.equals(trackInfoVo.getMediaFileId())){
			//  说明这个声音被重新上传了. 对应流媒体声音属性要修改。 调用vodService方法
			TrackMediaInfoVo trackMediaInfo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
			if (null == trackMediaInfo){
				//	抛出异常
				throw new GuiguException(ResultCodeEnum.VOD_FILE_ID_ERROR);
			}
			//  赋值最新数据.
			trackInfo.setMediaUrl(trackMediaInfo.getMediaUrl());
			trackInfo.setMediaType(trackMediaInfo.getType());
			trackInfo.setMediaDuration(trackMediaInfo.getDuration());
			trackInfo.setMediaSize(trackMediaInfo.getSize());
			//  删除旧流媒体数据.
			//  vodService.removeTrack(mediaFileId);
			//  track_stat where track_id = ?
			//  trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId,trackId));
			//  新增了一条：
			//  this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY);
			//  this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT);
			//  this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE);
			//  this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT);
			//  专辑.include_track_count = 不需要、
		}
		//  修改：
		trackInfoMapper.updateById(trackInfo);
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
			albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount()-1);
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
