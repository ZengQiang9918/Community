package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //需要考虑到分页，该帖子的发布者UserId
    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);

    //如果方法只有一个参数，并且在<if>中使用，必须使用@Param注解
    int selectDiscussPostRows(@Param("userId") int userId);
}
