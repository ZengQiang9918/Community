package com.nowcoder.community.event;


import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RabbitListener(queues = "queue")   //监听队列queue
public class EventConsumer implements CommunityConstant {

    @Autowired
    private MessageService messageService;


    /**
     *
     * @param record 传过来的json字符串
     */
    @RabbitHandler
    public void handleCommentMessage(String record){
        if (record == null || record == null) {
            log.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record, Event.class);
        if (event == null) {
            log.error("消息格式错误!");
            return;
        }

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());


        //消息的content字段需要一些基础的信息
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        //如果event中还有额外的字段信息，也添加到content中
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }

        //message中的content字段存储的是json格式的字符串
        message.setContent(JSONObject.toJSONString(content));

        //将这条消息添加到数据库中
        messageService.addMessage(message);
    }

}
