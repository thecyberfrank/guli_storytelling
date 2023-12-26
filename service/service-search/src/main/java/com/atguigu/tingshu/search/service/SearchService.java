package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

public interface SearchService {


    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    void upperAlbum(Long albumId);

    void lowerAlbum(Long albumId);
}
