package com.atguigu.tingshu.order.service.impl;

import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.service.OrderDerateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderDerateServiceImpl extends ServiceImpl<OrderDerateMapper, OrderDerate> implements OrderDerateService {
}
