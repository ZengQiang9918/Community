package com.nowcoder.community.service;


import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    //声明一个日期的格式转换器,格式是年月日即可
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");



    /**
     * 将指定的IP计入UV
     * 采用的redis的数据结构是HypeLoglog
     */
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }


    /**
     * 统计指定日期范围内的UV
     */
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 整理该日期范围内的key
        // 计算出这个日期范围内的所有的key，用的是Calender类
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            //加一天
            calendar.add(Calendar.DATE, 1);
        }

        // 合并这些数据
        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());

        // 返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }



    /**
     * 将指定用户计入DAU
     * 使用的redis数据结构是Bitmap
     *
     */
    public void recordDAU(int userId){
        //采用当天的日期作为redisKey
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));

        //好家伙，offset居然就是userId，秒啊
        //这样确实可以统计出DAU了
        redisTemplate.opsForValue().setBit(redisKey,userId,true);
    }

    /**
     * 统计指定日期范围内的DAU
     */
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 整理该日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }

        // 进行OR运算
        // 只要有一天是活跃用户，那么这段时候内该用户就算是活跃用户
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(),
                        keyList.toArray(new byte[0][0]));  //ArrayList的toArray()待考古
                return connection.bitCount(redisKey.getBytes());
            }
        });
    }




}
