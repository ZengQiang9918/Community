package com.nowcoder.community.event;


import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitListener(queues = "esqueue")   //监听队列esqueue
public class ESEventConsumer implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;


    /**
     * 消费发帖事件,同时也处理删帖事件，看event的topic字段
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

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        //如果当前主题是发布帖子，那么saveDiscussPost
        if(event.getTopic().equals(TOPIC_PUBLISH)){
            elasticsearchService.saveDiscussPost(post);
        } else{
            //否则，表示当前主题是删除帖子
            elasticsearchService.deleteDiscussPost(post.getId());
        }

    }
}
