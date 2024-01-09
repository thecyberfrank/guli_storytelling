package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Resource
    UserInfoFeignClient userInfoFeignClient;

    @Resource
    AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    TrackInfoFeignClient trackInfoFeignClient;

    @Resource
    VipServiceConfigFeignClient vipServiceConfigFeignClient;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    OrderDetailService orderDetailService;

    @Autowired
    OrderDerateMapper orderDerateMapper;

    @Resource
    UserAccountFeignClient userAccountFeignClient;

    @Autowired
    KafkaService kafkaService;

    @Override

    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        //  订单原始金额
        BigDecimal originalAmount = new BigDecimal("0.00");
        //  订单减免金额
        BigDecimal derateAmount = new BigDecimal("0.00");
        //  订单实际金额
        BigDecimal orderAmount = new BigDecimal("0.00");
        //  订单明细
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //  减免明细
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();

        if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_ALBUM)) {
            //  如果是购买专辑
            // 判断用户是否购买过专辑. user_paid_album
            Result<Boolean> booleanResult = userInfoFeignClient.isPaidAlbum(tradeVo.getItemId());
            Assert.notNull(booleanResult, "购买专辑返回结果集为空");
            Boolean already_paid_album = booleanResult.getData();
            Assert.notNull(already_paid_album, "购买专辑返回结果为空");
            if (already_paid_album) {
                //  抛出异常(已经购买过专辑)
                throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
            }

            //获取用户信息
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
            Assert.notNull(userInfoVoResult, "获取用户信息返回结果集为空");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户信息为空");

            //  获取到专辑信息
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(tradeVo.getItemId());
            Assert.notNull(albumInfoResult, "获取专辑信息返回结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");

            //  原始金额
            originalAmount = albumInfo.getPrice();

            // 专辑自身折扣
            if (albumInfo.getDiscount().intValue() != -1) {
                if (userInfoVo.getIsVip() == 0) {
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount())).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
            }
            // VIP额外折扣
            if (albumInfo.getVipDiscount().intValue() == -1) {
                if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getVipDiscount())).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
            }
            //  订单实际金额
            orderAmount = originalAmount.subtract(derateAmount);
            //  计算订单明细：
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVoList.add(orderDetailVo);
            //  计算减免明细：
            if (derateAmount.compareTo(BigDecimal.ZERO) != 0) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVoList.add(orderDerateVo);
            }
        } else if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_TRACK)) {
            // 购买的是声音
            // 如果要购买的集数小于0，抛出异常
            if (tradeVo.getTrackCount() < 0) {
                throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
            }
            //  获取要支付的声音列表(选中要购买的n个声音 除去 用户对于该专辑已经购买的声音)
            Result<List<TrackInfo>> trackInfoListResult = trackInfoFeignClient.findTrackInfoNeedToPayList(tradeVo.getItemId(), tradeVo.getTrackCount());
            Assert.notNull(trackInfoListResult, "需要支付的声音列表结果集为空");
            List<TrackInfo> trackInfoList = trackInfoListResult.getData();
            Assert.notNull(trackInfoList, "需要支付的声音列表为空");

            //  根据声音Id 获取到专辑信息
            Long albumId = trackInfoList.get(0).getAlbumId();
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoResult, "获取专辑信息返回结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");

            // VIP付费只能购买全部专辑，VIP免费只能根据声音一集一集的购买
            // 所以使用album.getPrice()，只能整张专辑购买的会获取整张专辑的价格，如果只能单集购买的会得到单集声音的价格
            originalAmount = tradeVo.getTrackCount().intValue() > 0 ? albumInfo.getPrice().multiply(new BigDecimal(tradeVo.getTrackCount())) : albumInfo.getPrice();
            //  计算订单总价(购买声音不参与会员和专辑折扣)
            orderAmount = originalAmount;

            //  循环遍历声音集合对象赋值订单明细
            orderDetailVoList = trackInfoList.stream().map(trackInfo -> {
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemPrice(albumInfo.getPrice());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                return orderDetailVo;
            }).collect(Collectors.toList());
        } else {
            // 购买的是VIP，getItemId() = vip服务配置id (1-一个月，2-三个月...)
            // 获取VIP配置信息（原价,优惠价，月份信息等等...）
            Result<VipServiceConfig> vipServiceConfigResult = vipServiceConfigFeignClient.getVipServiceConfig(tradeVo.getItemId());
            Assert.notNull(vipServiceConfigResult, "vip服务配置信息结果集为空");
            VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
            Assert.notNull(vipServiceConfig, "vip服务配置对象为空");

            //  订单原始金额
            originalAmount = vipServiceConfig.getPrice();
            //  实际金额
            orderAmount = vipServiceConfig.getDiscountPrice();
            //  减免金额
            derateAmount = originalAmount.subtract(orderAmount);

            //  订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            //  vip服务配置Id
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName("购买VIP" + vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(orderAmount);

            orderDetailVoList.add(orderDetailVo);
            //  减免明细
            if (derateAmount.compareTo(BigDecimal.ZERO) != 0) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVoList.add(orderDerateVo);
            }
        }

        //  生成一个交易号。
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        String tradeNoKey = "user:trade:" + userId;
        //  将数据存储到缓存，在用户提交订单时，会删除对应的缓存，如果删除缓存失败（已经提过订单，缓存已经被删除）则抛出异常提示重复下单
        this.redisTemplate.opsForValue().set(tradeNoKey, tradeNo, 10, TimeUnit.MINUTES);
        //  创建对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //  赋值：
        orderInfoVo.setTradeNo(tradeNo);
        //  购买类型
        orderInfoVo.setItemType(tradeVo.getItemType());
        //  赋值金额
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        //  时间戳，
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        //  orderInfoVo--->Map
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        String sign = SignHelper.getSign(map);
        // 将订单信息的md5保存为签名，如果订单被篡改，则md5值会变更
        orderInfoVo.setSign(sign);

        return orderInfoVo;
    }

    @Override
    public String submitOrder(OrderInfoVo orderInfoVo, Long userId) {
        //  校验签名，如果订单信息被篡改，就抛出异常
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        SignHelper.checkSign(map);

        // 删除redis里面的用户对应tradeNo，删除成功则是第一次提交该订单，删除失败为重复提交
        String tradeNo = orderInfoVo.getTradeNo();
        String tradeNoKey = "user:trade" + userId;
        // 使用lua脚本保证查询该key是否存在和删除该key的原子性
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        // 执行lua脚本 execute(script, key, value)
        Long count = (Long) this.redisTemplate.execute(redisScript, List.of(tradeNoKey), tradeNo);
        if (count == 0) {
            throw new GuiguException(ResultCodeEnum.ORDER_SUBMIT_REPEAT);
        }

        // 判断支付方式
        if (orderInfoVo.getPayWay().equals(SystemConstant.ORDER_PAY_WAY_WEIXIN)) {
            // 生成订单号
            this.saveOrder(orderInfoVo, userId, tradeNo);
            // 调用微信支付接口
            return tradeNo;
        } else {
            try {
                // 余额支付
                // 先锁定金额，再扣除锁定的金额，如果中间出现异常则解锁金额
                AccountLockVo accountLockVo = new AccountLockVo();
                accountLockVo.setOrderNo(tradeNo);
                accountLockVo.setUserId(userId);
                accountLockVo.setAmount(orderInfoVo.getOrderAmount());
                accountLockVo.setContent(orderInfoVo.getItemType());

                Result<AccountLockResultVo> result = userAccountFeignClient.checkAndLock(accountLockVo);
                if (200 != result.getCode()) {
                    throw new GuiguException(result.getCode(), result.getMessage());
                }
                // 保存订单数据到：order_info order_detail order_derate
                this.saveOrder(orderInfoVo, userId, tradeNo);
                // 扣减锁定金额
                kafkaService.sendMessage(KafkaConstant.QUEUE_ACCOUNT_MINUS, tradeNo);
                //  返回订单编号
                return tradeNo;
            } catch (GuiguException e) {
                //  解锁金额：
                kafkaService.sendMessage(KafkaConstant.QUEUE_ACCOUNT_UNLOCK, tradeNo);
                throw new GuiguException(ResultCodeEnum.ORDER_SUBMIT_FAIL);
            }
        }
    }


    /**
     * 保存订单数据到：order_info order_detail order_derate
     *
     * @param orderInfoVo
     * @param userId
     * @param orderNo
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        //  保存到order_info
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoVo, orderInfo);
        orderInfo.setUserId(userId);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        orderInfoMapper.insert(orderInfo);

        // 保存到order_detail
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (!CollectionUtils.isEmpty(orderDetailVoList)) {
            List<OrderDetail> orderDetailList = orderDetailVoList.stream().map(orderDetailVo -> {
                //  创建对象
                OrderDetail orderDetail = new OrderDetail();
                //  属性拷贝.
                BeanUtils.copyProperties(orderDetailVo, orderDetail);
                orderDetail.setOrderId(orderInfo.getId());
                return orderDetail;
            }).collect(Collectors.toList());
            //  mybatis-plus的service层有批量保存方法
            orderDetailService.saveBatch(orderDetailList);
        }

        // 保存到order_derate订单见面明细
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (!CollectionUtils.isEmpty(orderDerateVoList)) {
            //  循环遍历。
            orderDerateVoList.stream().forEach(orderDerateVo -> {
                //  创建对象
                OrderDerate orderDerate = new OrderDerate();
                //  属性拷贝.
                BeanUtils.copyProperties(orderDerateVo, orderDerate);
                orderDerate.setOrderId(orderInfo.getId());
                // 也可以直接使用mybatis的mapper层里面的insert插入
                orderDerateMapper.insert(orderDerate);
            });
        }


    }

}
