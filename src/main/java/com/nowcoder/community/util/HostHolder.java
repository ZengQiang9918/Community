package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于代替session对象
 */
@Component
public class HostHolder {

    //持有ThreadLocal对象,线程局部变量，用于保存不同线程的登录用户
    private ThreadLocal<User> users = new ThreadLocal<>();


    /**
     * ThreadLocal调用set()时，会先获取到当前线程，获取到当前线程的ThreadLocalMap对象
     * 将threadLocal对象作为key,user对象作为value存到ThreadLocalMap中
     * 所以可以保证线程的局部变量的使用
     */
    public void setUser(User user){
        users.set(user);
    }

    public User getUser(){
        return users.get();
    }

    public void clear(){
        users.remove();
    }
}
