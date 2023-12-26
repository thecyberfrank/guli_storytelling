package com.atguigu.tingshu.respoistory;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AlbumInfoEsRepository extends ElasticsearchRepository<AlbumInfoIndex, Long> {

}

