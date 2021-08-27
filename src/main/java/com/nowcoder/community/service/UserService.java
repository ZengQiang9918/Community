package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    //拿到模板引擎Thymeleaf
    @Autowired
    private TemplateEngine templateEngine;

    //Spring的注解，将application.yaml配置文件中的值赋给属性
    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 要运用好缓存，需要做好这几件事
     * 1.优先从缓存中取值，先不去从mysql中取
     * 2.取不到时到数据库中去取，并且初始化缓存数据
     * 3.数据变更时清除缓存数据
     */

    /**
     * 1.优先从缓存中取值
     */
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        User user = (User) redisTemplate.opsForValue().get(redisKey);
        return user;
    }

    /**
     * 2.取不到时从数据库中去取
     */
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String rediskey = RedisKeyUtil.getUserKey(userId);
        //这个数据存1个小时
        redisTemplate.opsForValue().set(rediskey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    /**
     * 3.数据变更时清除缓存中的数据，删除这个key
     */
    private void clearCache(int userId){
        String rediskey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(rediskey);
    }






    /**
     * 根据userId查找User对象
     * 需要先从缓存中获取
     */
    public User findUserById(int id){
        //return userMapper.selectById(id);
        User user = getCache(id);
        if(user==null){
            user = initCache(id);
        }
        return user;
    }


    /**
     *  处理注册逻辑
     *  细节：返回给controller层的是一个Map，这个Map中存了所有的信息
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        // 空值判断处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }


        // 注册用户
        // 生成随机盐
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        // 密码的处理方式，将密码和随机盐加密
        // 数据库中存的密码是加密过后的
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        //注意：插入用户之后，用户的用户名ID就自动生成并回填给user
        userMapper.insertUser(user);



        // 固定写法：给邮箱发送激活码
        // 激活邮件
        // 拿到Thymeleaf中的context,给context设置属性
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // 我们希望的处理格式：http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        //生成html格式的内容,即返回给用户的邮件是html内容
        // 参数1是转发的html模板
        String content = templateEngine.process("mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);


        return map;
    }

    /**
     *  激活用户账号
     *  根据userId和激活码code来判断是否完成激活!
     *  userId：表示当前激活的账号的userId
     *  code：表示当前的激活码
     */
    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){

            //若User.status==1，表示该账号已经激活过了
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){

            //若User.status==0,且激活码对得上，此时激活账号
            //所谓的激活账号就是将status从0修改为1
            userMapper.updateStatus(user.getId(),1);
            //清空redis缓存
            clearCache(userId);

            return ACTIVATION_SUCCESS;
        }else{

            //其他情况，修改失败
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 处理登录逻辑
     * 返回值也用Map，存放一些信息
     * 每次登录都会生成一个登录记录LoginTicket，这个登录记录暂时存在数据库中
     * 每次登录往login_ticket表中插入响应的登录记录
     *
     * 后期会把这个登录凭证存到redis中
     *
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 走到这，说明用户登录是合法的
        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + (long)expiredSeconds * 1000));
        //loginTicketMapper.insertLoginTicket(loginTicket);

        //将凭证存到redis中
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //redis会把这个loginTicket序列化为json字符串保存
        redisTemplate.opsForValue().set(redisKey,loginTicket);

        // 注意：存在map中的ticket并不是LoginTicket对象
        // 而是"登录凭证"，需要存在Cookie中
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    /**
     * 处理退出登录逻辑
     */
    public void logout(String ticket){
        //将凭证状态改为1即可
        //loginTicketMapper.updateStatus(ticket,1);

        //采用redis的解决方案，先从redis中取出来，修改完状态之后再放进去
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);


    }

    /**
     * 查询登录凭证
     */
    public LoginTicket findLoginTicket(String ticket){

        //return loginTicketMapper.selectByTicket(ticket);

        //采用redis的解决方案
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        return loginTicket;
    }

    /**
     * 更新用户的头像
     */
    public int updateHeader(int userId,String headerUrl){
        int count = userMapper.updateHeader(userId, headerUrl);
        //清理缓存
        clearCache(userId);
        return count;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);

    }


    /**
     * 根据userId获取用户的权限
     * 根据User的type字段进行判断
     */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}
