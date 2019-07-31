package com.ygk.seckill.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * redis的连接池和数据源
 */
@Component
public class RedisCacheHandle {

    @Autowired
    private JedisPool jedisPool;

    public Jedis getJedis(){
        /**
         * 返回的也是redis
         */
        return jedisPool.getResource();
    }
}
