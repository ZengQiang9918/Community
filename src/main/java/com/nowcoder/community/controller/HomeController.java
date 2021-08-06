package com.nowcoder.community.controller;


import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;


    /**
     * 访问首页
     * 需要把帖子分页显示出来，还需要把帖子的用户名，帖子的点赞数量显示出来
     * 前端页面可以根据orderMode选择"最热","最新";如果前端没传的话，@RequestParam设置默认值为0
     */
    @RequestMapping(path = "/index",method = RequestMethod.GET)
    public String getIndexPage(Model model,
                               Page page,
                               @RequestParam(name="orderMode",defaultValue = "0") int orderMode){


        //方法调用前，SpringMVC会自动实例化Model和Page,并将Page注入Model中
        //所以，thymeleaf中可以直接访问Page对象中的数据
        //需要注意的是page是有默认值的
        page.setRows(discussPostService.findDiscussPostRows(0));
        //这个拼串很细节！！！
        page.setPath("/index?orderMode="+orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(),orderMode);
        List<Map<String,Object>> discussPosts= new ArrayList<>();
        for (DiscussPost post : list) {
            HashMap<String, Object> map = new HashMap<>();
            //存放帖子
            map.put("post",post);

            //存放帖子所属的用户
            User user = userService.findUserById(post.getUserId());
            map.put("user",user);

            //存放帖子的点赞数
            long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
            map.put("likeCount",likeCount);

            discussPosts.add(map);
        }

        //查出帖子信息，包括帖子的发布人user对象,放到model中
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("orderMode",orderMode);
        return "index";
    }

    /**
     * 错误处理
     */
    @RequestMapping(value = "/error",method = RequestMethod.GET)
    public String getErrorPage(){
        return "/error/500";
    }

    /**
     * 当权限不足时拒绝请求
     */
    @RequestMapping(value = "/denied",method = RequestMethod.GET)
    public String getDeniedPage(){
        return "error/404";
    }



}
