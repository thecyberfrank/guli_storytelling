package com.atguigu.tingshu.user.api;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.WxLoginService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private WxLoginService wxLoginService;

    @Autowired
    private UserInfoService userInfoService;

    @Operation(summary = "微信登录")
    @GetMapping("/wxLogin/{code}")
    public Result wxLogin(@PathVariable String code) {
        Map<String, Object> map = wxLoginService.wxLogin(code);
        return Result.ok(map);
    }

    /**
     * 根据用户Id获取到用户数据
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取登录信息")
    @GetMapping("getUserInfo")
    public Result getUserInfo() {
        //  获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //  调用服务层方法. select * from user_info id = ?;
        UserInfoVo userInfoVo = this.userInfoService.getUserInfoById(userId);
        return Result.ok(userInfoVo);
    }

    /**
     * 修改用户信息
     *
     * @param userInfoVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "修改用户信息")
    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        this.userInfoService.updateUser(userInfoVo, userId);
        return Result.ok();
    }

}