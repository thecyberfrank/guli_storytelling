package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountApiController {

    @Autowired
    private UserAccountService userAccountService;


    /**
     * 获取账户可用余额
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取账号可用金额")
    @GetMapping("getAvailableAmount")
    public Result<BigDecimal> getAvailableAmount() {
        //	调用服务层方法
        return Result.ok(userAccountService.getAvailableAmount(AuthContextHolder.getUserId()));
    }


    /**
     * 检查锁定账户金额
     *
     * @param accountLockVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "检查与锁定账户金额")
    @PostMapping("checkAndLock")
    public Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo) {
        //	调用服务层方法
        return Result.ok(this.userAccountService.checkAndLock(accountLockVo));
    }
}

