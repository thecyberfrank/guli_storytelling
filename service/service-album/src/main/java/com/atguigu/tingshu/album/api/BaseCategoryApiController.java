package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album/category")
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;


    /**
     * 查询所有分类数据
     */
    @GetMapping(value = "/findAllCategory1")
    public Result findAllCategory1() {
        List<BaseCategory1> category1List = baseCategoryService.findAllCategory1();
        return Result.ok(category1List);
    }


    /**
     * 根据一级分类Id查询置顶到频道页的三级分类列表
     *
     * @param category1Id
     * @return
     */
    @Operation(summary = "根据一级分类Id查询置顶到频道页的三级分类列表")
    @GetMapping("/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id) {
        //  调用服务层方法.
        List<BaseCategory3> baseCategory3List = this.baseCategoryService.findTopBaseCategory3(category1Id);
        //  返回数据
        return Result.ok(baseCategory3List);
    }


    /**
     * 查询所有分类数据
     */
    @GetMapping(value = "/getBaseCategoryList")
    public Result getBaseCategoryList() {
        List<JSONObject> categoryList = baseCategoryService.getBaseCategoryList();
        return Result.ok(categoryList);
    }

    /**
     * 根据三级分类Id 获取到分类数据
     *
     * @param category3Id
     * @return
     */
    @Operation(summary = "根据三级分类Id 获取到分类数据")
    @GetMapping("/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id) {
        //  调用服务层方法
        BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
        //  返回数据
        return Result.ok(baseCategoryView);
    }

    @Operation(summary = "根据一级分类Id获取属性数据")
    @GetMapping("/findAttribute/{category1Id}")
    public Result findAttribute(@PathVariable Long category1Id) {
        //	调用服务层方法. this:表示当前对象
        List<BaseAttribute> list = this.baseCategoryService.findAttribute(category1Id);
        //	返回数据
        return Result.ok(list);
    }
}

