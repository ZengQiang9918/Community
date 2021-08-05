package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //需要考虑到分页，该帖子的发布者UserId
    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);

    //如果方法只有一个参数，并且在动态sql <if>中使用，必须使用@Param注解
    //注：困惑...
    int selectDiscussPostRows(@Param("userId") int userId);

    //增加帖子的方法
    int insertDiscussPost(DiscussPost discussPost);

    //查询帖子的详情
    DiscussPost selectDiscussPostById(int id);

    //插入评论的时候，需要更新DiscussPost的comment_count字段
    int updateCommentCount(int id,int commentCount);

    //修改帖子的类型，加精，置顶等
    int updateType(int id,int type);

    //修改帖子的状态，如删除帖子
    int updateStatus(int id,int status);



}
