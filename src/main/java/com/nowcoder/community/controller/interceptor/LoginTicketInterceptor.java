package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;


/**
 * 对所有请求进行拦截
 * 判断当前用户的cookie中是否保存有ticket，数据库的登录记录中是否有对应的ticket
 */
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    /**
     * 对请求进行拦截，获取cookie中的凭证ticket
     * 判断ticket是否有效，过期；
     * 如果有效未过期，根据该ticket去数据库中查出登录记录，获取出User对象
     * 存在hostHolder中的ThreadLocal中
     *
     * 很重要的一点：不管用户是否需要自动登录，该拦截器都不能拦截没有登录的"游客"
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取凭证
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            // 查询凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // 检查凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {

                // 根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有用户
                hostHolder.setUser(user);


                //构建用户认证的结果，并存入SecurityContext中，以便于Security进行授权
                //因为我们并没有认证，投机取巧的方式...
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user,user.getPassword(),userService.getAuthorities(user.getId()));
                //将用户认证结果存到SecurityContextHolder中
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));


            }
        }

        return true;
    }

    /**
     * 控制器方法执行之后执行postHandle(),
     * 将user对象存到ModelAndView中!!!
     * (其实我觉得这一步放在preHandle()中也可以)
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {

            //如果用户登录成功，前端页面始终可以获取到这个User对象
            //即loginUser
            modelAndView.addObject("loginUser", user);
        }
    }

    /**
     * 当这个请求执行完毕，清理数据
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
    }

}
