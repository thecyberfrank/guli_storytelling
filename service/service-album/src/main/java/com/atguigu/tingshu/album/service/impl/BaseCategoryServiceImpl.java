package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;


    @Override
    public List<JSONObject> getBaseCategoryList() {
        //	创建集合对象
        List<JSONObject> list = new ArrayList<>();
        //	查看所有分类数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //	按照一级分类Id 进行分组 key:一级分类Id， value:一级分类Id 对应的集合数据
        Map<Long, List<BaseCategoryView>> map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //	循环遍历数据
        for (Map.Entry<Long, List<BaseCategoryView>> entry : map.entrySet()) {
            //	获取到一级分类Id
            Long category1Id = entry.getKey();
            //	获取到一级分类Id 对应的集合数据
            List<BaseCategoryView> category1ViewList = entry.getValue();
            // 声明一级分类对象
            JSONObject category1 = new JSONObject();
            category1.put("categoryId", category1Id);
            category1.put("categoryName", category1ViewList.get(0).getCategory1Name());

            //	按照二级分类Id 进行分组
            Map<Long, List<BaseCategoryView>> map2 = category1ViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 声明二级分类对象集合
            List<JSONObject> category2Child = new ArrayList<>();
            //	循环遍历
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : map2.entrySet()) {
                //	获取到二级分类Id
                Long category2Id = entry2.getKey();
                //	获取到二级分类Id 对应的集合数据
                List<BaseCategoryView> category2ViewList = entry2.getValue();
                //	创建二级分类对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId", category2Id);
                category2.put("categoryName", category2ViewList.get(0).getCategory2Name());

                // 循环三级分类数据
                List<JSONObject> category3Child = category2ViewList.stream().map(baseCategoryView -> {
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", baseCategoryView.getCategory3Id());
                    category3.put("categoryName", baseCategoryView.getCategory3Name());
                    return category3;
                }).collect(Collectors.toList());
                // 将三级数据放入二级里面
                category2.put("categoryChild", category3Child);
                //	将二级分类对象添加到集合中
                category2Child.add(category2);
            }
            // 将三级数据放入二级里面
            category1.put("categoryChild", category2Child);
            //	将一级分类数据放入到集合中。
            list.add(category1);
        }
        return list;
    }

    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {
        List<BaseAttribute> attributes = baseAttributeMapper.findAttribute(category1Id);
        return attributes;
    }
}