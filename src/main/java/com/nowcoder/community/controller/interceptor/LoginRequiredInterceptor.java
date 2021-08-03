package com.nowcoder.community.controller.interceptor;


import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    /**
     * 对请求进行拦截
     * 如果请求的方法上有@LoginRequired注解,表示该方法必须先登录
     * 根据hostHolder即可知道当前用户是否登录
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //这个handler其实就是HandlerMethod，可以获取到Method
        if(handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getDeclaredAnnotation(LoginRequired.class);
            if(loginRequired!=null && hostHolder.getUser()==null){
                //当前方法需要先登录，于是重定向到登录页面
                response.sendRedirect(request.getContextPath()+"/login");
                return false;
            }
        }

        return true;
    }
}
