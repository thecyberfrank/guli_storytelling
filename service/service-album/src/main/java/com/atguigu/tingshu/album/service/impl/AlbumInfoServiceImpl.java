package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private KafkaService kafkaService;


    @Override
    public IPage<AlbumListVo> selectUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery) {
        //	调用mapper 层方法
        return albumInfoMapper.selectUserAlbumPage(albumListVoPage, albumInfoQuery);
    }

    @Override
    public List<AlbumAttributeValue> findAlbumAttributeValue(Long albumId) {
        //	获取专辑属性值列表
        //	select * from album_attribute_value where album_id = ?
        return albumAttributeValueMapper.selectList(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
    }

    @Override
    public AlbumStatVo getAlbumStatVo(Long albumId) {
        return albumInfoMapper.selectAlbumStatVo(albumId);
    }


    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        //	select * from album_info where user_id = ? and is_deleted = 0 order by id desc;
        //	mybatis-plus 内部的方法，is_deleted = 0这个条件会自动追加 自己编写xmlsql的时候，需要自己添加。
        //	在追加一个限制条数 last("limit 20")
        //	return albumInfoMapper.selectList(new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getUserId,userId).orderByDesc(AlbumInfo::getId).last(" limit 20"));
        //	第一个参数Page对象：封装第几页，每页显示的条数
        Page<AlbumInfo> albumInfoPage = new Page<>(1, 20);
        //	第二个参数:构建查询条件
        //	select album_title from album_info where user_id = ? ...;
        Page<AlbumInfo> page = albumInfoMapper.selectPage(albumInfoPage, new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getUserId, userId).select(AlbumInfo::getAlbumTitle, AlbumInfo::getId).orderByDesc(AlbumInfo::getId));
        //	返回分页的集合
        return page.getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(AlbumInfoVo albumInfoVo, Long albumId) {
        //	更新专辑基础数据，album_info
        AlbumInfo albumInfo = new AlbumInfo();
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        albumInfo.setId(albumId);
        albumInfoMapper.updateById(albumInfo);

        // 更新专辑标签数据（一个专辑有多个标签，批量更新），album_attribute_value
        this.albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        //	新增：
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {
            //	批量保存
            List<AlbumAttributeValue> attributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                //	属性拷贝
                BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                albumAttributeValue.setAlbumId(albumId);
                return albumAttributeValue;
            }).collect(Collectors.toList());
            //	使用album_attribute_value 对应的服务层
            albumAttributeValueService.saveBatch(attributeValueList);
        }

        //	如果专辑设置为非隐藏，就发送kafka，后续接收并保存到es：
        if ("1".equals(albumInfoVo.getIsOpen())) {
            //	调用上架 发送的内容：是由消费者决定!
            kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumId.toString());
        } else {
            //	调用下架
            kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, albumId.toString());
        }

    }

    @Override
    public AlbumInfo getAlbumInfoById(Long albumId) {
        //	select * from album_info where  id = ?;
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (null != albumInfo) {
            //	select * from album_attribute_value where album_id = ?;
            List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueMapper.selectList(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
        }
        //	返回数据
        return albumInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long albumId) {
		/*
		# 物理删除：
		delete from album_info where id = ?;
		# 逻辑删除：
		update album_info set is_deleted = 1 where id = ?;
		 */
        //	album_info 本质执行更新的更新。
        albumInfoMapper.deleteById(albumId);
        //	album_stat
        //	ById 是根据主键删除： albumId
        albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        //	album_attribute_value
        albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        //	调用下架
        kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, albumId.toString());

    }


    @GuiGuLogin
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
        //	album_info
        AlbumInfo albumInfo = new AlbumInfo();
        //	赋值：属性拷贝：
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        //	赋值用户Id
        albumInfo.setUserId(userId);
        //	状态：
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        //	如果是付费专辑，则需要给免费的试听集数. 5
        if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfoVo.getPayType())) {
            //	vip 免费 付费 设置试听集数
            albumInfo.setTracksForFree(5);
        }
        //	保存专辑
        albumInfoMapper.insert(albumInfo);
        //	album_attribute_value attribute_id value_id
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        //	判断：
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {

            //	循环遍历获取对象，并将这个对象添加到集合.
            //			ArrayList<AlbumAttributeValue> albumAttributeValueArrayList = new ArrayList<>();
            //			//	循环遍历
            //			for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
            //				//	创建对象
            //				AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
            //				//	赋值：
            //				BeanUtils.copyProperties(albumAttributeValueVo,albumAttributeValue);
            //				//	上一个步骤执行了插入数据，所以就可以通过专辑对象来获取到专辑Id @TableId(type = IdType.AUTO)
            //				albumAttributeValue.setAlbumId(albumInfo.getId());
            //				albumAttributeValueArrayList.add(albumAttributeValue);
            //				//	保存属性数据 执行多次insert 语句.
            //				//	albumAttributeValueMapper.insert(albumAttributeValue);
            //			}
            List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
                //	创建对象
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                //	赋值：
                BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                //	上一个步骤执行了插入数据，所以就可以通过专辑对象来获取到专辑Id @TableId(type = IdType.AUTO)
                albumAttributeValue.setAlbumId(albumInfo.getId());
                return albumAttributeValue;
            }).collect(Collectors.toList());
            //	保存一个集合 insert into album_attribute_value (?,?,?) (?,?,?) (?,?,?)
            this.albumAttributeValueService.saveBatch(albumAttributeValueList);
        }

        //	album_stat  播放量，订阅量，购买量，评论数;
        //	调用保存专辑统计方法.
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_PLAY);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_SUBSCRIBE);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_BROWSE);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_COMMENT);

        // 上架ES
        if ("1".equals(albumInfoVo.getIsOpen())) {
            kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumInfo.getId().toString());
        }
    }

    /**
     * 初始化专辑统计状态。
     *
     * @param albumId
     * @param statPlay
     */
    private void saveAlbumStat(Long albumId, String statPlay) {
        //	创建对象
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statPlay);
        albumStat.setStatNum(0);
        albumStatMapper.insert(albumStat);
    }
}
