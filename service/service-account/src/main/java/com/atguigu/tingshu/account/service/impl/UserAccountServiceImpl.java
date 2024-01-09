package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private UserAccountDetailMapper userAccountDetailMapper;

    @Override
    public void initUserAccount(long userId) {
        //	创建对象
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(new BigDecimal("1000"));
        userAccount.setAvailableAmount(new BigDecimal("1000"));
        userAccountMapper.insert(userAccount);
    }

    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        UserAccount userAccount = this.getOne(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId));
        return userAccount.getAvailableAmount();
    }

    @Override
    @Transactional
    public AccountLockResultVo checkAndLock(AccountLockVo accountLockVo) {
        //	检查key
        String key = "checkAndLock:" + accountLockVo.getOrderNo();
        //	防止重复请求，如果set结果=true就是第一次set
        boolean firstPay = redisTemplate.opsForValue().setIfAbsent(key, accountLockVo.getOrderNo(), 1, TimeUnit.HOURS);
        if (!firstPay) {
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_REPEAT);
        } else {
            UserAccount userAccount = userAccountMapper.check(accountLockVo.getUserId(), accountLockVo.getAmount());
            if (null == userAccount) {
                //	远程调用失败，删除检查缓存
                this.redisTemplate.delete(key);
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
            }
            //	锁定金额
            //	update user_account set lock_amount = lock_amount + 100 , available_amount = available_amount - 100 where user_id = 41;
            int lock = userAccountMapper.lock(accountLockVo.getUserId(), accountLockVo.getAmount());
            if (0 == lock) {
                //	远程调用失败，删除检查缓存
                this.redisTemplate.delete(key);
                throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
            }

            //  记录账户明细-user_account_detail，将accountLockResultVo 保存到缓存。
            AccountLockResultVo accountLockResultVo = new AccountLockResultVo();
            accountLockResultVo.setUserId(accountLockVo.getUserId());
            accountLockResultVo.setAmount(accountLockVo.getAmount());
            accountLockResultVo.setContent(accountLockVo.getContent());

            //	数据key
            String dataKey = "account:lock:" + accountLockVo.getOrderNo();
            //	将用户锁定内容存储到缓存，扣减金额是异步的，所以从缓存中获取，这里也可以将锁定对象入库
            this.redisTemplate.opsForValue().set(dataKey, accountLockResultVo, 10, TimeUnit.MINUTES);

            // 将账户操作明细存入mysql
            this.addUserAccountDetail(
                    accountLockVo.getUserId(),
                    "锁定金额：" + accountLockVo.getContent(),
                    SystemConstant.ACCOUNT_TRADE_TYPE_LOCK,
                    accountLockVo.getAmount(),
                    accountLockVo.getOrderNo()
            );
            return accountLockResultVo;
        }
    }

    @Override
    public void minus(String orderNo) {

    }

    @Override
    public void unlock(String orderNo) {

    }


    /**
     * 在锁定金额 / 扣件金额 / 出现异常解锁金额时，更新账户详情
     */
    private void addUserAccountDetail(Long userId, String title, String typeLock, BigDecimal amount, String orderNo) {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(typeLock);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }
}
