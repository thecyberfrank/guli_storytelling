package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;


    /**
     * 声音分类列表
     * http://127.0.0.1/api/album/trackInfo/findAlbumTrackPage/139/1/10
     */
    @GuiGuLogin
    @Operation(summary = "声音分类列表")
    @GetMapping("/findAlbumTrackPage/{albumId}/{page}/{limit}")
    public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(
            @Parameter(name = "albumId", description = "专辑id", required = true)
            @PathVariable Long albumId,
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,
            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit) {

        Long userId = AuthContextHolder.getUserId();
        //	创建一个Page 对象
        Page<AlbumTrackListVo> albumTrackListVoPage = new Page<>(page, limit);
        //	调用服务层方法.
        IPage<AlbumTrackListVo> trackPage = trackInfoService.findAlbumTrackPage(albumTrackListVoPage, albumId, userId);
        //	返回数据
        return Result.ok(trackPage);
    }

    /**
     * 上传声音：
     *
     * @param file
     * @return
     */
    @Operation(summary = "上传声音")
    @PostMapping("/uploadTrack")
    public Result uploadTrack(MultipartFile file) {
        //	调用服务层方法.
        Map<String, Object> map = trackInfoService.uploadTrack(file);
        //	返回数据 mediaFileId mediaUrl
        return Result.ok(map);
    }

    /**
     * 保存声音：
     *
     * @param trackInfoVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "保存声音")
    @PostMapping("/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo) {
        //	获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法
        trackInfoService.saveTrackInfo(trackInfoVo, userId);
        //	返回数据
        return Result.ok();
    }

    /**
     * 查看声音分页列表
     *
     * @param page
     * @param limit
     * @param trackInfoQuery: 封装了页面传递的status , 同时还将用户Id 与 声音标题赋值给当前对象。
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "查看声音分页列表")
    @PostMapping("/findUserTrackPage/{page}/{limit}")
    public Result findUserTrackPage(@PathVariable Long page,
                                    @PathVariable Long limit,
                                    @RequestBody TrackInfoQuery trackInfoQuery) {
        //	获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //	赋值用户Id
        trackInfoQuery.setUserId(userId);

        //	创建page 对象
        Page<TrackListVo> trackListVoPage = new Page<>(page, limit);
        //	调用服务层方法
        IPage<TrackListVo> iPage = trackInfoService.findUserTrackPage(trackListVoPage, trackInfoQuery);
        //	返回数据
        return Result.ok(iPage);

    }

    //	http://127.0.0.1/api/album/trackInfo/removeTrackInfo/51960

    /**
     * 根据声音Id删除数据
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "根据声音Id删除数据")
    @DeleteMapping("/removeTrackInfo/{trackId}")
    public Result removeTrackInfo(@PathVariable Long trackId) {
        //	调用服务层方法。
        trackInfoService.removeTrackInfo(trackId);
        //	返回数据
        return Result.ok();
    }


    /**
     * 根据Id 获取数据
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "获取声音信息")
    @GetMapping("/getTrackInfo/{trackId}")
    public Result getTrackInfo(@PathVariable Long trackId) {
        //	调用服务层方法. 接收前端传递的数据：TrackInfoDto
        TrackInfo trackInfo = trackInfoService.getTrackInfo(trackId);
        //	返回数据
        return Result.ok(trackInfo);
    }

    /**
     * 修改声音
     *
     * @param trackId
     * @param trackInfoVo
     * @return
     */
    @Operation(summary = "修改声音")
    @PutMapping("/updateTrackInfo/{trackId}")
    public Result updateTrackInfo(@PathVariable Long trackId, @RequestBody @Validated TrackInfoVo trackInfoVo) {
        //	调用服务层方法.
        trackInfoService.updateTrackInfo(trackInfoVo, trackId);
        //	返回数据
        return Result.ok();
    }

    /**
     * 获取声音统计接口
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "获取声音统计信息")
    @GetMapping("getTrackStatVo/{trackId}")
    public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId) {
        //	调用服务层方法
        TrackStatVo trackStatVo = trackInfoService.getTrackStatVoByTrackId(trackId);
        //	返回对象
        return Result.ok(trackStatVo);
    }
}

