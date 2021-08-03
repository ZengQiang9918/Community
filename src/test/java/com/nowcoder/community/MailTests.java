package com.nowcoder.community;


import com.nowcoder.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Test
    public void testTextMail(){
        for(int i=0;i<10;i++){
            mailClient.sendMail("2468295981@qq.com","TEST","Welcome");
        }

    }

}
