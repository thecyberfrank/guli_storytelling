package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

/**
 * 加了该注解表示该方法必须要登录
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GuiGuLogin {
    // 默认加了该注解都需要跳转
    // boolean required() default true;
}
