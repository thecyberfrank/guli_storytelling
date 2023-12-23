package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class GuiGuLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
     * 意思为该注解将被应用于 com.atguigu.tingshu 包及其子包中的 api 子包下所有类的所有方法，但前提是这些方法必须被 guiGuLogin 注解标记
     */
    @SneakyThrows
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object loginAspect(ProceedingJoinPoint point, GuiGuLogin guiGuLogin) {
        // 获取请求对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();

        // 获取请求头中的token，判断token是否存在
        String token = request.getHeader("token");
        if (StringUtils.isEmpty(token)) {
            // 如果token没在请求头中，就是没登录，返回错误码，让前端跳转登录页面
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        } else {
            // 如果token在请求头中，就是已经登录过了，从redis获取用户信息
            String redisLoginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            //  获取缓存中的数据
            UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(redisLoginKey);
            if (null == userInfo) {
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
            //  存储用户Id，调用threadLocal.set(user_id)，使用threadLocal存储后，这次请求就可以使用threadLocal里面的内容
            AuthContextHolder.setUserId(userInfo.getId());
            //  存储用户昵称，调用threadLocal.set(user_name)
            AuthContextHolder.setUsername(userInfo.getNickname());
        }

        // 执行被切方法
        Object result = point.proceed();

        // 移除threadLocal里的对象，避免增加垃圾回收的负担
        AuthContextHolder.removeUserId();
        AuthContextHolder.removeUsername();
        // 可以在执行后，获取响应对象
        // HttpServletResponse response = servletRequestAttributes.getResponse();

        return result;

    }

}
