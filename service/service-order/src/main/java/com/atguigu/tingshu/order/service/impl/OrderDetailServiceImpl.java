package com.atguigu.tingshu.order.service.impl;

import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}
