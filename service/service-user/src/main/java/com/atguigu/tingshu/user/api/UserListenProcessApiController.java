package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessApiController {

    @Autowired
    private UserListenProcessService userListenProcessService;

    /**
     * 获取声音播放的时间
     *
     * @param trackId
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取声音的上次跳出时间")
    @GetMapping("/getTrackBreakSecond/{trackId}")
    public Result getTrackBreakSecond(@PathVariable Long trackId) {
        //	获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法
        BigDecimal trackSecond = userListenProcessService.getTrackBreakSecond(userId, trackId);
        //	返回数据
        return Result.ok(trackSecond);
    }

    /**
     * 更新播放进度和播放量
     *
     * @param userListenProcessVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "更新播放进度和播放量")
    @PostMapping("/updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
        //	获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法。
        userListenProcessService.updateListenProcess(userId, userListenProcessVo);
        //	返回数据
        return Result.ok();
    }

    /**
     * 获取最近一次播放声音
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取最近一次播放声音")
    @GetMapping("/getLatelyTrack")
    public Result<Map<String, Object>> getLatelyTrack() {
        // 获取用户Id
        Long userId = AuthContextHolder.getUserId();
        // 获取播放记录
        Map<String, Object> map = userListenProcessService.getLatestTrack(userId);
        // 返回数据
        return Result.ok(map);
    }
}

