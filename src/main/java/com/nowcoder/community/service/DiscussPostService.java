package com.nowcoder.community.service;


import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;


    //Caffeine的核心接口：Cache,LoadingCache(常用),AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    //初始化缓存
    @PostConstruct
    public void init() {


        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                //CacheLoader的泛型必须和缓存的key一样
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 在这个地方可以加上二级缓存:
                        // Redis -> mysql
                        // 本地缓存得不到数据，再去二级缓存中去取数据
                        // 最后再是去数据库中去取数据

                        // 访问完数据库会把数据缓存到本地缓存中,自动填充

                        log.debug("从数据库中获取帖子详情");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });

        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        log.debug("从数据库中获取帖子的行数");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }





    /**
     * 查询指定分页条件下的帖子
     * 对热帖运用了本地缓存(用Redis缓存也是可以的)
     */
    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit,int orderMode){
        //只有当userId!=0时，且orderMode为1时才缓存数据
        //因为我们缓存的是首页的热帖，此时传进来的userId=0,并且不对新帖进行缓存(新帖orderMode=0)
        if(userId==0 && orderMode==1){
            return postListCache.get(offset+":"+limit);
        }

        //访问数据库
        log.debug("从数据库中获取帖子详情");
        return discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }

    /**
     * 查询得到帖子的总数
     * 运用到了本地缓存
     */
    public int findDiscussPostRows(int userId){

        //使用缓存
        if(userId==0){
            return postRowsCache.get(userId);
        }

        log.debug("从数据库中获取帖子的行数");
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
