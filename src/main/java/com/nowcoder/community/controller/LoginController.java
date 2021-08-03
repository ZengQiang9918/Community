package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;



    @RequestMapping("/register")
    public String getRegisterPage(){
        return "site/register";
    }

    @RequestMapping("/login")
    public String getLoginPage(){
        return "site/login";
    }


    /**
     * 注册逻辑
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {

            //当map不为空，表示这个注册成功，注册成功会跳到site/operate-result.html页面
            //同时，我们需要往model中放入一些属性
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "site/operate-result";
        } else {

            //注册失败，调回到site/register.html页面
            //往model中填属性，告诉前端页面，哪些环节出现了错误
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "site/register";
        }
    }

    /**
     * 处理激活码
     * 我们希望的处理格式：http://localhost:8080/community/activation/101/code
     * 一般都是从邮件中点击链接跳到这里，激活账号
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model,
                             @PathVariable("userId") int userId,
                             @PathVariable("code") String code) {


        //不管账号是否激活，都将信心存到model中
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }

        //跳转到operate-result页面，告诉用户激活情况
        return "/site/operate-result";
    }


    /**
     * 处理验证码
     * 前端页面的图片<img th:src="@{/kaptcha}"/>
     * 所以，验证码的处理由该控制器方法处理
     * 该请求是一个异步的ajax请求!
     * 在session中存取这个验证码的内容
     */
    @RequestMapping(value = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response){
        //生成验证码
        //生成验证码的文字内容
        String text = kaptchaProducer.createText();
        //根据文字内容生成图片
        BufferedImage image = kaptchaProducer.createImage(text);

        /*//将验证码存入session
        session.setAttribute("kaptcha",text);*/

        // 验证码的归属,临时给客户端发一个凭证
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将验证码存入Redis中,存的是验证码的文字内容text
        // 有效时间是60秒
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);


        //将图片输出给浏览器
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
            log.error("响应验证码失败："+e.getMessage());
        }


    }


    /**
     * 处理登录逻辑
     * 注意，这个只是处理登录请求的，当前端点击登录按钮才会触发完整流程的登录请求
     * 登录成功后，会把登录记录存在数据库中，把ticket登录凭证存在cookie中
     *
     */
    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public String login(String username,String password,String code,boolean rememberme,
                        Model model,HttpSession session,
                        HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner){
        // 1.先检查验证码
        // 验证码是存在了Session中的
        //String kaptcha = (String) session.getAttribute("kaptcha");

        // 验证码是存在了Redis中的，这个key需要从cookie中取
        String kaptcha = null;
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }

        //对验证码的验证
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "site/login";
        }

        // 2.再检查账号,密码
        System.out.println("记住状态："+rememberme);

        //当"记住我"时，登录的超时时间是一个月
        //默认登录状态保存一天
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;

        //登录逻辑，如果登录信息无误，往login_ticket表中插入一条登录记录
        Map<String, Object> map = userService.login(username, password, expiredSeconds);

        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);       //设置cookie的作用范围，默认是整个项目下
            cookie.setMaxAge(expiredSeconds);    //设置cookie的超时时间
            response.addCookie(cookie);
            //登录成功，重定向到index首页
            return "redirect:index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            //登录失败，登录信息有误，返回login登录界面
            return "site/login";
        }

    }


    /**
     * 退出登录处理逻辑
     * 其实就是把数据库中这条ticket对应的记录给删除掉
     * 那么即使前端保留着对应的ticket的cookie，在数据库中查找不到对应ticket的记录，那么登录就不会成功
     * 需要重新登录
     */
    @RequestMapping(value = "/logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }

}
