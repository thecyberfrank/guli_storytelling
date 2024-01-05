package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.CategoryDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = CategoryDegradeFeignClient.class)
public interface CategoryFeignClient {


    /**
     * 根据三级分类Id 获取到分类数据
     *
     * @param category3Id
     * @return
     */
    @GetMapping("api/album/category/getCategoryView/{category3Id}")
    Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id);


    /**
     * 根据一级分类Id查询置顶到频道页的三级分类列表
     *
     * @param category1Id
     * @return
     */
    @GetMapping("api/album/category/findTopBaseCategory3/{category1Id}")
    Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id);

    /**
     * 获取所有一级分类ID
     *
     * @return
     */
    @GetMapping("api/album/category/findAllCategory1")
    Result<List<BaseCategory1>> findAllCategory1();

}