package com.nowcoder.community.service;


import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant{

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;



    public List<Comment> findCommentsByEntity(int entityType,int entityId,int offset,int limit){

        return commentMapper.selectCommentsByEntity(entityType,entityId,offset,limit);
    }

    public int findCommentCount(int entityType,int entityId){

        return commentMapper.selectCountByEntity(entityType,entityId);
    }

    /**
     * 添加评论的业务处理,事务处理
     * 插入评论
     */
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if(comment==null){
            throw new IllegalArgumentException("参数不能为空！");
        }

        //添加评论
        //先过滤帖子的标签，防止html标签的误解
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        //对敏感词进行过滤
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        //插入评论
        int rows = commentMapper.insertComment(comment);

        //如果当前评论是对帖子的评论，需要更新帖子评论数量
        //主要是为了实时能看到帖子数增加！！！
        if(comment.getEntityType() == ENTITY_TYPE_POST){
            //获取评论数量
            int count = commentMapper.selectCountByEntity(comment.getEntityType(),comment.getEntityId());

            //注意：comment的entity_id对应的字段是discuss_post的id
            discussPostService.updateCommentCount(comment.getEntityId(),count);
        }

        return rows;

    }

    /**
     * 根据id查找Comment对象
     */
    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }

}
