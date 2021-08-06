package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

@SpringBootTest
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectUser(){
        User user = userMapper.selectById(101);
        System.out.println(user);
    }

    @Test
    public void testInsertUser(){
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("test@qq.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int count = userMapper.insertUser(user);
        System.out.println(count);
    }

    @Test
    public void testUpdateUser(){
        int rows = userMapper.updateStatus(150, 1);
        System.out.println(rows);
    }


    //---------------------DiscussPostMapper-----------------------------
    @Test
    public void testSelectPosts(){
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, 0, 10,0);
        System.out.println(discussPosts);

        int rows = discussPostMapper.selectDiscussPostRows(0);
        System.out.println(rows);
    }

    //--------------------LoginTicketMapper--------------------------
    @Test
    public void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abc");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+1000*60*10));

        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket(){
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus("abc",1);
        LoginTicket loginTicket1 = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket1);
    }


    //------------------------MessageMapper-----------------------------
    @Test
    public void testSelectLetters(){
        List<Message> messages = messageMapper.selectConversations(111, 0, 20);
        System.out.println(messages);

        int count = messageMapper.selectConversationCount(111);
        System.out.println("=================");
        System.out.println(count);

        List<Message> messages1 = messageMapper.selectLetters("111_112", 0, 10);
        System.out.println("=================");
        System.out.println(messages1);

        int count2 = messageMapper.selectLetterCount("111_112");
        System.out.println("=================");
        System.out.println(count2);

        int count3 = messageMapper.selectLetterUnreadCount(131, "111_131");
        System.out.println(count3);
    }
}
