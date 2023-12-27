package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.respoistory.AlbumInfoEsRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    ElasticsearchClient elasticsearchClient;


    @Override
    public AlbumSearchResponseVo searchAlbum(AlbumIndexQuery albumIndexQuery) {
        /**
         *
         * 1. 构建查询请求
         * 2. 执行查询请求
         * 3. 将ES返回对象解析成VO，并添加分页，返回给前端
         */
        SearchRequest searchRequest = this.queryBuildDsl(albumIndexQuery);
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parseResultData(albumIndexQuery, searchResponse);
    }


    /**
     * 生成查询对象
     *
     * @param albumIndexQuery
     * @return
     */
    private SearchRequest queryBuildDsl(AlbumIndexQuery albumIndexQuery) {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        //  创建bool
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (!StringUtils.isEmpty(albumIndexQuery.getKeyword())) {
            //  第一个入口：根据关键词检索
            boolQueryBuilder.should(f -> f.match(m -> m.field("albumTitle").query(albumIndexQuery.getKeyword())));
            boolQueryBuilder.should(f -> f.match(m -> m.field("albumIntro").query(albumIndexQuery.getKeyword())));
            //  高亮显示查询的关键词
            searchRequestBuilder.highlight(f -> f.fields("albumTitle", h -> h.preTags("<span style=color:red>").postTags("</span>")));
        } else if (albumIndexQuery.getAttributeList() != null && !albumIndexQuery.getAttributeList().isEmpty()) {
            //  入口3：根据tags进行检索
            List<String> attributeList = albumIndexQuery.getAttributeList();
            for (String attribute : attributeList) {
                // 属性Id split[0];
                // 属性值Id split[1];
                String[] split = attribute.split(":");
                if (split.length == 2) {
                    boolQueryBuilder.filter(f -> f.nested(n -> n.path("attributeValueIndexList").query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0]))).must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(split[1])))))));
                }
            }
        } else {
            //  第二个入口：根据1/2/3级分类查询专辑
            if (!StringUtils.isEmpty(albumIndexQuery.getCategory1Id())) {
                boolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
            }
            if (!StringUtils.isEmpty(albumIndexQuery.getCategory2Id())) {
                boolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
            }
            if (!StringUtils.isEmpty(albumIndexQuery.getCategory3Id())) {
                boolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
            }
        }

        //  排序：默认按匹配度排序，如果前端传入 n:asc或desc，会走下面的代码
        String order = albumIndexQuery.getOrder();
        if (order != null) {
            String[] TypeAndSequence = order.split(":");
            //  获取排序规则：n=1:按hotScore综合排序，n=2按播放量排序，n=3按发布时间排序
            String orderType = TypeAndSequence[0];
            String orderSequence = TypeAndSequence[1];
            //  判断
            switch (orderType) {
                case "1":
                    orderType = "hotScore";
                    break;
                case "2":
                    orderType = "playStatNum";
                    break;
                case "3":
                    orderType = "createTime";
                    break;
            }
            String finalField = orderType;
            searchRequestBuilder.sort(s -> s.field(f -> f.field(finalField).order("asc".equals(orderSequence) ? SortOrder.Asc : SortOrder.Desc)));
        }


        //  分页：
        int from = (albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize();
        searchRequestBuilder.from(from);
        searchRequestBuilder.size(albumIndexQuery.getPageSize());

        //  构建查询语句
        searchRequestBuilder.index("albuminfo");
        searchRequestBuilder.query(f -> f.bool(boolQueryBuilder.build()));

        //  创建一个 SearchRequest 对象
        SearchRequest searchRequest = searchRequestBuilder.build();
        //  打印完整的dsl语句：
        System.out.println("dsl:\t" + searchRequest.toString());
        //  返回查询请求
        return searchRequest;

    }

    private AlbumSearchResponseVo parseResultData(AlbumIndexQuery albumIndexQuery, SearchResponse<AlbumInfoIndex> searchResponse) {
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        //  添加查询列表
        List<AlbumInfoIndexVo> albumInfoIndexVoList = searchResponse.hits().hits().stream().map(albumInfoIndexHit -> {
            //  获取到专辑对象
            AlbumInfoIndex albumInfoIndex = albumInfoIndexHit.source();
            //  创建 AlbumInfoIndexVo 对象
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            //  属性拷贝：
            BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
            //  设置高亮：
            if (null != albumInfoIndexHit.highlight().get("albumTitle")) {
                //  有高亮设置
                String albumTitle = albumInfoIndexHit.highlight().get("albumTitle").get(0);
                albumInfoIndexVo.setAlbumTitle(albumTitle);
            }
            //  返回数据
            return albumInfoIndexVo;
        }).collect(Collectors.toList());
        albumSearchResponseVo.setList(albumInfoIndexVoList);

        // 给前端添加页面信息
        albumSearchResponseVo.setTotal(searchResponse.hits().total().value());
        // 设置当前页显示的条数
        albumSearchResponseVo.setPageSize(albumIndexQuery.getPageSize());
        // 设置当前是第几页
        albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
        // 设置总页数
        long totalPages = (albumSearchResponseVo.getTotal() + albumIndexQuery.getPageSize() - 1) / albumIndexQuery.getPageSize();
        albumSearchResponseVo.setTotalPages(totalPages);
        return albumSearchResponseVo;
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

        CompletableFuture.allOf(albumInfoCompletableFuture, tagInfoCompletableFuture, categoryInfoCompletableFuture, authorInfoCompletableFuture).join();
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
