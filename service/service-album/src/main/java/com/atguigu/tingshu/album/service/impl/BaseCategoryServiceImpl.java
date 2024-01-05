package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;
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
    public BaseCategoryView getCategoryView(Long category3Id) {
        //	根据主键查询
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    public JSONObject getBaseCategoryList(Long category1Id) {
        //	分别获取到一级,二级，三级 分类Id，分类名称
        JSONObject category1 = new JSONObject();
        //	如何找到一级分类Id，一级分类名称.
        //	获取分类视图数据集合. 根据一级分类Id 获取到集合数据 一级分类Id 查询的集合数据对应的名称都一样
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(new LambdaQueryWrapper<BaseCategoryView>().eq(BaseCategoryView::getCategory1Id, category1Id));
        //	给一级分类对象赋值.
        category1.put("categoryId", category1Id);
        category1.put("categoryName", baseCategoryViewList.get(0).getCategory1Name());
        //	获取二级分类对应的数据集合. 根据二级分类Id 进行分组
        Map<Long, List<BaseCategoryView>> category2Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
        //	循环遍历。
        //	创建二级分类对象集合.
        ArrayList<JSONObject> category2ChildList = new ArrayList<>();
        Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = category2Map.entrySet().iterator();
        //	category2Map.entrySet().stream().map()
        while (iterator.hasNext()) {
            //	获取数据
            Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
            //	获取二级分类Id
            Long category2Id = entry.getKey();
            List<BaseCategoryView> categoryViewList = entry.getValue();
            //	创建二级分类对象
            JSONObject category2 = new JSONObject();
            category2.put("categoryId", category2Id);
            category2.put("categoryName", categoryViewList.get(0).getCategory2Name());
            //	获取三级分类数据.
            List<JSONObject> categoryChild3list = categoryViewList.stream().map(baseCategoryView -> {
                //	创建三级分类对象
                JSONObject category3 = new JSONObject();
                category3.put("categoryId", baseCategoryView.getCategory3Id());
                category3.put("categoryName", baseCategoryView.getCategory3Name());
                //	返回数据
                return category3;
            }).collect(Collectors.toList());
            category2.put("categoryChild", categoryChild3list);
            //	添加二级分类到集合
            category2ChildList.add(category2);
        }
        //	将二级分类数据添加到一级分类对象中.
        category1.put("categoryChild", category2ChildList);
        //	返回对象
        return category1;
    }

    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {
        return baseAttributeMapper.findAttribute(category1Id);
    }

    /**
     * 根据一级分类Id查询置顶到频道页的三级分类列表
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory3> findTopBaseCategory3(Long category1Id) {
        //	根据一级分类Id查询二级分类数据
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(new LambdaQueryWrapper<BaseCategory2>().eq(BaseCategory2::getCategory1Id, category1Id));
        //	获取二级分类Id
        //	List<Long> list = baseCategory2List.stream().map(baseCategory2 -> baseCategory2.getId()).collect(Collectors.toList());
        List<Long> category2IdList = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
        //	查询三级分类数据.
        return baseCategory3Mapper.selectList(new LambdaQueryWrapper<BaseCategory3>().eq(BaseCategory3::getIsTop, 1).in(BaseCategory3::getCategory2Id, category2IdList).last(" limit 7 "));
    }

    @Override
    public List<BaseCategory1> findAllCategory1() {
        LambdaQueryWrapper<BaseCategory1> queryWrapper = new LambdaQueryWrapper<>();
        return baseCategory1Mapper.selectList(queryWrapper);
    }


}