package com.nowcoder.community.dao;


import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType,int entityId,int offset,int limit);


    /**
     * 根据entityType和entityId来查找Comment对象
     */
    int selectCountByEntity(int entityType,int entityId);

    /**
     * 增加评论
     */
    int insertComment(Comment comment);

    /**
     * 根据id来查找Comment对象
     */
    Comment selectCommentById(int id);
}
