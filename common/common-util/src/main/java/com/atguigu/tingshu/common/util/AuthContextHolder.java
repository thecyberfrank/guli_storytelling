package com.atguigu.tingshu.common.util;

/**
 * 获取当前用户信息帮助类
 * threadLoacl里的内容用AOP在请求执行之前注入，在执行之后移除，生命周期就在这次请求中。存储在当前请求的线程中，速度非常快。
 * 如果单机环境：threadLocal在第一次请求中注入，如果第二次请求切换线程，就获取不到内容了
 * 如果是微服务环境：在用户模块的节点中将用户信息注入threadLocal，再调用其他模块的不同节点后，也获取不到threadLocal内容
 */
public class AuthContextHolder {

    private static ThreadLocal<Long> userId = new ThreadLocal<Long>();
    private static ThreadLocal<String> username = new ThreadLocal<String>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    public static Long getUserId() {
        return userId.get();
    }

    public static void removeUserId() {
        userId.remove();
    }

    public static void setUsername(String _username) {
        username.set(_username);
    }

    public static String getUsername() {
        return username.get();
    }

    public static void removeUsername() {
        username.remove();
    }

}
