package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;


    /**
     * 根据userId 获取到用户信息
     *
     * @param userId
     * @return
     */
    @GetMapping("/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId) {
        return Result.ok(userInfoService.getUserInfoById(userId));
    }

    /**
     * 判断用户是否购买声音列表
     * @param albumId
     * @param trackIdList
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "判断用户是否购买声音列表")
    @PostMapping("/userIsPaidTrack/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable("albumId") Long albumId, @RequestBody List<Long> trackIdList){
        //	需要用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法.
        Map<Long, Integer> map = userInfoService.userIsPaidTrack(albumId,trackIdList,userId);
        //	返回数据
        return Result.ok(map);
    }
}

