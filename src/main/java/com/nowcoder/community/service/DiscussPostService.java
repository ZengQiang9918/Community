package com.nowcoder.community.service;


import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    /**
     * 查询指定分页条件下的帖子
     */
    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit,int orderMode){
        return discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }

    /**
     * 查询得到帖子的总数
     */
    public int findDiscussPostRows(int userId){

        return discussPostMapper.selectDiscussPostRows(userId);
    }


    /**
     * 发布帖子
     * 需要转义html标签，过滤敏感词
     */
    public int addDiscussPost(DiscussPost post){
        if(post==null){
            throw new IllegalArgumentException("参数不能为空");
        }

        //转义HTML标记，防止html的标签对页面影响
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    /**
     * 查询帖子详情
     */
    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    /**
     * 更新帖子的回复数量comment_count
     */
    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    /**
     * 修改帖子的类型：加精，置顶
     */
    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }

    /**
     * 修改帖子的状态：删除帖子
     */
    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }

    /**
     * 修改帖子的分数
     */
    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }

}
