package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {

    /**
     * 获取分类数据
     *
     * @return
     */
    List<JSONObject> getBaseCategoryList();


    /**
     * 根据一级分类Id 获取属性数据
     *
     * @param category1Id
     * @return
     */
    List<BaseAttribute> findAttribute(Long category1Id);

    /**
     * 根据三级分类Id 获取到分类数据
     *
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);


    /**
     * 根据一级分类Id获取数据
     * @param category1Id
     * @return
     */
    JSONObject getBaseCategoryList(Long category1Id);

    /**
     * 根据一级分类Id查询置顶到频道页的三级分类列表
     *
     * @param category1Id
     * @return
     */
    List<BaseCategory3> findTopBaseCategory3(Long category1Id);

    /**
     * 获取所有一级分类ID
     * @return
     */
    List<BaseCategory1> findAllCategory1();

}