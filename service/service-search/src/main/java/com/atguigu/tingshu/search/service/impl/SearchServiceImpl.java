package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.respoistory.AlbumInfoEsRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {

    @Resource
    AlbumInfoFeignClient albumInfoFeignClient;
    @Resource
    CategoryFeignClient categoryFeignClient;

    @Resource
    UserInfoFeignClient userInfoFeignClient;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    AlbumInfoEsRepository albumInfoEsRepository;


    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        return null;
    }

    /**
     * 上架专辑
     *
     * @param albumId 专辑ID
     */
    @Override
    public void upperAlbum(Long albumId) {
        // 多线程异步获取： 1.专辑基本信息 2.专辑标签 3.专辑分类信息 4.作者信息
        /**
         * runAsync() 异步执行不需要返回值
         * supplyAsync() 异步执行需要返回值
         */
        AlbumInfoIndex albumInfoEsEntity = new AlbumInfoIndex();

        // 1.获取专辑基本信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoResult, "调用远程方法失败");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfoResult, "没查到对应专辑");
            // 将专辑信息导入ES专辑对象
            BeanUtils.copyProperties(albumInfo, albumInfoEsEntity);
            return albumInfo;
        });

        //2. 获取专辑标签
        CompletableFuture<Void> tagInfoCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<List<AlbumAttributeValue>> albumTags = albumInfoFeignClient.findAlbumAttributeValue(albumId);
            Assert.notNull(albumTags, "调用远程方法失败");
            List<AlbumAttributeValue> albumTagsList = albumTags.getData();
            Assert.notNull(albumTagsList, "没查到专辑对应标签");
            // 将专辑信息导入ES专辑对象
            List<AttributeValueIndex> albumEsTags = albumTagsList.stream().map(tag -> {
                AttributeValueIndex tagEsValue = new AttributeValueIndex();
                tagEsValue.setAttributeId(tag.getId());
                tagEsValue.setValueId(tag.getValueId());
                return tagEsValue;
            }).collect(Collectors.toList());
            albumInfoEsEntity.setAttributeValueIndexList(albumEsTags);
        });

        //3.专辑分类信息
        CompletableFuture<Void> categoryInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<BaseCategoryView> baseCategoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(baseCategoryViewResult, "调用远程方法失败");
            BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
            Assert.notNull(baseCategoryView, "没查到分类信息");
            albumInfoEsEntity.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoEsEntity.setCategory2Id(baseCategoryView.getCategory2Id());
            albumInfoEsEntity.setCategory3Id(baseCategoryView.getCategory3Id());
        });

        //4.作者信息
        CompletableFuture<Void> authorInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(userInfoResult, "调用远程方法失败");
            UserInfoVo userInfoVo = userInfoResult.getData();
            Assert.notNull(userInfoResult, "没查到用户信息");
            albumInfoEsEntity.setAnnouncerName(userInfoVo.getNickname());
        });

        // 随机赋播放量等数据
        int playStatNum = new Random().nextInt(10000000);
        int subscribeStatNum = new Random().nextInt(100000);
        int buyStatNum = new Random().nextInt(100000);
        int commentStatNum = new Random().nextInt(100000);
        albumInfoEsEntity.setPlayStatNum(playStatNum);
        albumInfoEsEntity.setSubscribeStatNum(subscribeStatNum);
        albumInfoEsEntity.setBuyStatNum(buyStatNum);
        albumInfoEsEntity.setCommentStatNum(commentStatNum);

        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                tagInfoCompletableFuture,
                categoryInfoCompletableFuture,
                authorInfoCompletableFuture).join();
        albumInfoEsRepository.save(albumInfoEsEntity);
    }

    /**
     * 专辑下架
     *
     * @param albumId 专辑ID
     */
    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoEsRepository.deleteById(albumId);

    }





}
