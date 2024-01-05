package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SearchService {


    AlbumSearchResponseVo searchAlbum(AlbumIndexQuery albumIndexQuery) ;

    /**
     * 专辑上架
     * @param albumId
     */
    void upperAlbum(Long albumId);


    /**
     * 专辑下架
     * @param albumId
     */

    void lowerAlbum(Long albumId);


    /**
     * 根据一级分类Id获取数据
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> channel(Long category1Id);

    /**
     * 自动补全
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * 更新排行榜
     */
    void updateLatelyAlbumRanking();

    /**
     * 获取指定category1Id，指定维度的排行榜数据
     * @param category1Id
     * @param dimension
     * @return
     */
    List<AlbumInfoIndexVo> findRankingList(Long category1Id, String dimension);
}
