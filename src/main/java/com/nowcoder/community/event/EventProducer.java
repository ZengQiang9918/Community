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
        //将事件发布到交换机中，指定routingKey
        //交换机是direct类型的，路由模式
        amqpTemplate.convertAndSend("ex","ordinary", JSONObject.toJSONString(event));
    }

    /**
     * 处理时间，发送es处理的消息
     */
    public void fireESEvent(Event event){
        //将事件发布到交换机中，指定routingKey
        amqpTemplate.convertAndSend("ex","es", JSONObject.toJSONString(event));
    }

}
