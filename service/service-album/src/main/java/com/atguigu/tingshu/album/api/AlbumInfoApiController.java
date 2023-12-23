package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    //	Request URL: http://127.0.0.1/api/album/albumInfo/saveAlbumInfo

    /**
     * 保存专辑
     *
     * @param albumInfoVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "保存专辑")
    @PostMapping("/saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo) {
        //	用户如果登录成功，则将用户Id 封装到一个方法中。
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法.
        albumInfoService.saveAlbumInfo(albumInfoVo, userId);
        //	返回数据
        return Result.ok();
    }

    //  http://127.0.0.1/api/album/albumInfo/findUserAlbumPage/1/10

    /**
     * 根据条件查询专辑列表
     *
     * @param page
     * @param limit
     * @param albumInfoQuery
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取当前用户专辑分页列表")
    @PostMapping("findUserAlbumPage/{page}/{limit}")
    public Result findUserAlbumPage(@PathVariable Long page,
                                    @PathVariable Long limit,
                                    @RequestBody AlbumInfoQuery albumInfoQuery) {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        albumInfoQuery.setUserId(userId);
        //  调用服务层方法. 第一个参数表示 当前页，第二个参数页的大小
        Page<AlbumListVo> albumListVoPage = new Page<>(page, limit);
        //  查询分页方法
        IPage<AlbumListVo> iPage = albumInfoService.selectUserAlbumPage(albumListVoPage, albumInfoQuery);
        //  返回数据.iPage.getRecords();
        return Result.ok(iPage);
    }


    /**
     * 根据专辑Id删除专辑数据
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "根据专辑Id删除专辑数据")
    @DeleteMapping("/removeAlbumInfo/{albumId}")
    public Result removeAlbumInfo(@PathVariable Long albumId) {
        //  调用服务层方法.
        albumInfoService.removeAlbumInfo(albumId);
        //  返回
        return Result.ok();
    }

    /**
     * 根据专辑Id回显专辑数据
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "根据专辑Id回显专辑数据")
    @GetMapping("/getAlbumInfo/{albumId}")
    public Result getAlbumInfo(@PathVariable Long albumId) {
        //  调用服务层方法.
        AlbumInfo albumInfo = albumInfoService.getAlbumInfoById(albumId);
        //  返回数据
        return Result.ok(albumInfo);
    }

    /**
     * 专辑修改
     *
     * @param albumInfoVo
     * @param albumId
     * @return
     */
    @Operation(summary = "修改专辑")
    @PutMapping("updateAlbumInfo/{albumId}")
    public Result updateAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo, @PathVariable Long albumId) {
        //  调用服务层方法
        albumInfoService.updateAlbumInfo(albumInfoVo, albumId);
        //  返回数据
        return Result.ok();
    }

    /**
     * 获取当前用户专辑列表
     *
     * @return
     */
    @Operation(summary = "获取当前用户全部专辑列表")
    @GetMapping("/findUserAllAlbumList")
    public Result findUserAllAlbumList() {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  调用服务层方法.
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList(userId);
        //  返回数据
        return Result.ok(albumInfoList);
    }
}

