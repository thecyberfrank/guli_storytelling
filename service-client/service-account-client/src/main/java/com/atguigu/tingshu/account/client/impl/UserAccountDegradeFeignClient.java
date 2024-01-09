package com.atguigu.tingshu.account.client.impl;


import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import org.springframework.stereotype.Component;

@Component
public class UserAccountDegradeFeignClient implements UserAccountFeignClient {

    @Override
    public Result<AccountLockResultVo> checkAndLock(AccountLockVo accountLockVo) {
        return null;
    }
}
