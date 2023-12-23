package com.atguigu.tingshu.user.service;

import java.util.Map;

public interface WxLoginService {
    Map<String, Object> wxLogin(String code);
}
