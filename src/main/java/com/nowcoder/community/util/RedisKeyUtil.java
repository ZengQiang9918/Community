package com.nowcoder.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";
    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_FOLLOWER = "follower";
    private static final String PREFIX_KAPTCHA = "kaptcha";
    private static final String PREFIX_TICKET = "ticket";
    private static final String PREFIX_USER = "user";


    // 生成某个实体的赞
    // like:entity:entityType:entityId -> set()
    // 比如我们想看到哪些人给我们点赞了，需要用到set这个数据结构来存储
    // 同时，set也是可以统计点赞人数的
    public static String getEntityLikeKey(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    /**
     * 某个用户得到的所有赞
     * like:user:userId        int类型
     * like:user是前缀
     */
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    /**
     * 某个用户关注的实体
     * entityType的意义是 帖子：用户：...
     * 由于关注的目标可以是用户，帖子，题目等；在实现时将这些目标抽象为实体；
     * 所以某个用户关注的实体，这个key应该是由userId:entityType组成
     * zset中的值应该存的是"实体"
     *
     * followee:userId:entityType -> zset(entityId,now)
     */
    public static String getFolloweeKey(int userId,int entityType){
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    /**
     * 某个实体拥有的粉丝
     * key应当是entityType:entityId组成
     * zset中存的应该是userId
     *
     * follower:entityType:entityId  ->zset(userId,now)
     */
    public static String getFollowerKey(int entityType,int entityId){
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    /**
     * 登录验证码
     * 注意此时用户还没有登录
     */
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    /**
     * 登录的凭证
     */
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET + SPLIT + ticket;
    }

    /**
     * 获取用户
     */
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT + userId;
    }

}
