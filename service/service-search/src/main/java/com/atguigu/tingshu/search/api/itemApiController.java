package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "专辑详情管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class itemApiController {

	@Autowired
	private ItemService itemService;

	/**
	 * 根据专辑Id获取到专辑详情数据
	 * @param albumId
	 * @return
	 */
	@Operation(summary = "根据专辑Id获取到专辑详情数据")
	@GetMapping("/{albumId}")
	public Result getAlbumInfoItem(@PathVariable Long albumId){
		/*
		result.put("albumInfo", albumInfo);			获取专辑信息
		result.put("albumStatVo", albumStatVo);		获取专辑统计信息
		result.put("baseCategoryView", baseCategoryView);	获取分类信息
		result.put("announcer", userInfoVo);	获取主播信息
		 */
		Map<String,Object> result = itemService.getAlbumInfoItem(albumId);
		return Result.ok(result);
	}


}

