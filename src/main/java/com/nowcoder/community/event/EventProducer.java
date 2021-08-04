package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 处理事件，发送消息
     */
    public void fireEvent(Event event){
        //将事件发布到队列中
        amqpTemplate.convertAndSend("queue", JSONObject.toJSONString(event));
    }

}
