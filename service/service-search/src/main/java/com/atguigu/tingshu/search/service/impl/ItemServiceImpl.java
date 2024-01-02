package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ItemServiceImpl implements ItemService {

    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    private CategoryFeignClient categoryFeignClient;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public Map<String, Object> getAlbumInfoItem(Long albumId) {
        //  声明一个集合
        Map<String, Object> result = new HashMap<>();
        //  创建异步编排对象
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            //  判断
            Assert.notNull(albumInfoResult, "调用远程服务失败");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "没查到对应专辑");
            //  保存记录
            result.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);

        //  统计信息
        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
            //  远程调用
            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStatVo(albumId);
            Assert.notNull(albumStatVoResult, "调用远程服务失败");
            AlbumStatVo albumStatVo = albumStatVoResult.getData();
            Assert.notNull(albumStatVo, "没查到对应专辑统计信息");
            result.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);

        //  获取分类信息：
        CompletableFuture<Void> cateCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<BaseCategoryView> baseCategoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(baseCategoryViewResult, "调用远程服务失败");
            BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
            Assert.notNull(baseCategoryView, "没查到对应专辑分类");
            result.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);

        //  远程调用获取作者信息
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(userInfoVoResult, "调用远程服务失败");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "没查到对应专辑作者");
            result.put("announcer", userInfoVo);
        }, threadPoolExecutor);

        //  多任务组合:
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                cateCompletableFuture,
                userCompletableFuture,
                statCompletableFuture
        ).join();
        //  返回数据
        return result;
    }
}
