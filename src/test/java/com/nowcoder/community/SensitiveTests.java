package com.nowcoder.community;


import com.nowcoder.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SensitiveTests {

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter(){
        String text = "这里可以赌博，可以嫖娼，可以吸毒，可以开票，哈哈";
        String filter = sensitiveFilter.filter(text);
        System.out.println(filter);

        text = "这里可以赌**博，可以**嫖娼，可以吸毒**，可以开票，哈哈";
        text = sensitiveFilter.filter(text);
        System.out.println(text);
    }


}
