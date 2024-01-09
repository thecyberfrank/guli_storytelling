package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 初始化账户信息
     *
     * @param userId
     */
    void initUserAccount(long userId);

    /**
     * 获取账户可用余额
     *
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);


    /**
     * 检查与锁定账户金额
     *
     * @param accountLockVo
     * @return
     */
    AccountLockResultVo checkAndLock(AccountLockVo accountLockVo);

    /**
     * 扣减余额
     *
     * @param orderNo
     */
    void minus(String orderNo);


    /**
     * 解锁余额
     *
     * @param orderNo
     */
    void unlock(String orderNo);
}
