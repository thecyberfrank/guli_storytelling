package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController {

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 确认订单
     *
     * @param tradeVo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "确认订单")
    @PostMapping("trade")
    public Result<OrderInfoVo> trade(@RequestBody @Validated TradeVo tradeVo) {
        Long userId = AuthContextHolder.getUserId();
        OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo, userId);
        return Result.ok(orderInfoVo);
    }
}