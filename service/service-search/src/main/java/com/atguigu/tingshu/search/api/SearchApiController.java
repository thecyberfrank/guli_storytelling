package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
     * 获取排行榜列表
     *
     * @param category1Id
     * @param dimension
     * @return
     */
    @Operation(summary = "获取排行榜列表")
    @Parameters({
            @Parameter(name = "category1Id", description = "一级分类", in = ParameterIn.PATH, required = true),
            @Parameter(name = "dimension", description = "热度:hotScore、播放量:playStatNum、订阅量:subscribeStatNum、购买量:buyStatNum、评论数:albumCommentStatNum", required = true, in = ParameterIn.PATH),
    })
    @GetMapping("findRankingList/{category1Id}/{dimension}")
    public Result<List<AlbumInfoIndexVo>> findRankingList(@PathVariable Long category1Id, @PathVariable String dimension) {
        searchService.updateLatelyAlbumRanking();
        //  调用服务层方法
        List<AlbumInfoIndexVo> infoIndexVoList = searchService.findRankingList(category1Id, dimension);
        //  返回结果集
        return Result.ok(infoIndexVoList);
    }

    /**
     * 更新排行榜
     *
     * @return
     */
    @SneakyThrows
    @Operation(summary = "更新排行榜")
    @GetMapping("updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking() {
        //  调用服务层方法
        searchService.updateLatelyAlbumRanking();
        //  默认返回
        return Result.ok();
    }

    /**
     * 根据一级分类Id获取频道页数据
     *
     * @param category1Id
     * @return
     */
    @Operation(summary = "获取频道页数据")
    @GetMapping("/channel/{category1Id}")
    public Result channel(@PathVariable Long category1Id) {
        //  调用服务层方法.
        List<Map<String, Object>> mapList = searchService.channel(category1Id);
        //  返回数据
        return Result.ok(mapList);
    }

    /**
     * 检索自动补全
     *
     * @param keyword
     * @return
     */
    @Operation(summary = "检索自动补全")
    @GetMapping("completeSuggest/{keyword}")
    public Result completeSuggest(@PathVariable String keyword) {
        //  调用服务层方法.
        List<String> list = searchService.completeSuggest(keyword);
        //  返回数据
        return Result.ok(list);
    }


    /**
     * 检索
     *
     * @param albumIndexQuery
     * @return
     */
    @Operation(summary = "搜索")
    @PostMapping
    public Result search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        //  调用服务层方法.
        AlbumSearchResponseVo albumSearchResponseVo = searchService.searchAlbum(albumIndexQuery);
        //  返回数据
        return Result.ok(albumSearchResponseVo);
    }

    /**
     * 专辑上架ES
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "专辑上架ES")
    @GetMapping("/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId) {
        //  调用服务层方法.
        searchService.upperAlbum(albumId);
        //  返回数据
        return Result.ok();
    }

    /**
     * 专辑下架ES
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "专辑下架ES")
    @GetMapping("/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId) {
        //  调用服务层方法.
        searchService.lowerAlbum(albumId);
        //  返回数据
        return Result.ok();
    }

    /**
     * 批量上架
     *
     * @return
     */
    @Operation(summary = "批量上架ES")
    @GetMapping("batchUpperAlbum")
    public Result batchUpperAlbum() {
        //  循环
        for (long i = 1; i <= 1500; i++) {
            searchService.upperAlbum(i);
        }
        //  返回数据
        return Result.ok();
    }
}

