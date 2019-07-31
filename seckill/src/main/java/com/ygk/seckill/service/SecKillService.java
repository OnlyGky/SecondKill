package com.ygk.seckill.service;

import com.alibaba.fastjson.JSON;
import com.ygk.seckill.cache.RedisCacheHandle;
import com.ygk.seckill.common.SecKillEnum;
import com.ygk.seckill.concurrent.AtomicStock;
import com.ygk.seckill.entity.Product;
import com.ygk.seckill.entity.Record;
import com.ygk.seckill.entity.User;
import com.ygk.seckill.exception.SecKillException;
import com.ygk.seckill.mapper.SecKillMapper;
import com.ygk.seckill.mq.RabbitMQSender;
import com.ygk.seckill.utils.SecKillUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.xml.ws.Action;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class SecKillService {

    @Autowired
    private RedisCacheHandle redisCacheHandle;

    @Autowired
    private SecKillMapper secKillMapper;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    @Autowired
    private AtomicStock atomicStock;

    /**
     * 利用MySQL的update行锁实现悲观锁
     *
     * @param paramMap
     * @return propagation= Propagation.NOT_SUPPORTED 取消事务的自动提交
     */
    @Transactional
    public SecKillEnum handleByPessLockInMySQL(Map<String, Object> paramMap) {
        Jedis jedis = redisCacheHandle.getJedis();
        Record record;

        Integer userId = (Integer) paramMap.get("userId");
        Integer productId = (Integer) paramMap.get("productId");

        User user = secKillMapper.getUserById(userId);
        Product product = secKillMapper.getProductById(productId);
        /**
         * 拿到用户所买商品在redis中对应的key
         */
        String hasBoughtSetKey = SecKillUtils.getRedisHasBoughtSetKey(product.getProductName());

        //判断该用户是否重复购买该商品
        boolean isBuy = jedis.sismember(hasBoughtSetKey, user.getId().toString());
        if (isBuy) {
            log.error("用户:" + user.getUsername() + "重复购买商品" + product.getProductName());
            throw new SecKillException(SecKillEnum.REPEAT);
        }

        /**
         * 判断该商品的库存  利用update行实现悲观锁 这里应该取消事务的自动提交功能
         */
        boolean secKillSuccess = secKillMapper.updatePessLockInMySQL(product);
        if (!secKillSuccess) {
            log.error("商品:" + product.getProductName() + "库存不足!");
            throw new SecKillException(SecKillEnum.LOW_STOCKS);
        }

        long result = jedis.sadd(hasBoughtSetKey, user.getId().toString());
        if (result > 0) {
            record = new Record(null, user, product, SecKillEnum.SUCCESS.getCode(), SecKillEnum.SUCCESS.getMessage(), new Date());
            log.info(record.toString());
            boolean insertFlag = secKillMapper.insertRecord(record);
            if (insertFlag) {
                log.info("用户:" + user.getUsername() + "秒杀商品：" + product.getProductName() + "成功!");
                return SecKillEnum.SUCCESS;
            } else {
                log.error("系统错误!");
                throw new SecKillException(SecKillEnum.SYSTEM_EXCEPTION);
            }
        } else {
            log.error("用户:" + user.getUsername() + "重复秒杀商品" + product.getProductName());
            throw new SecKillException(SecKillEnum.REPEAT);
        }
    }

    /**
     * mysql加version来实现乐观锁
     * @param paramMap
     * @return
     */
    @Transactional
    public SecKillEnum handleByPosiLockInMySQL(Map<String, Object> paramMap) {
        Jedis jedis = redisCacheHandle.getJedis();
        Record record = null;

        Integer userId = (Integer) paramMap.get("userId");
        Integer productId = (Integer) paramMap.get("productId");
        User user = secKillMapper.getUserById(userId);
        Product product = secKillMapper.getProductById(productId);

        String hasBoughtSetKey = SecKillUtils.getRedisHasBoughtSetKey(product.getProductName());
        boolean isBuy = jedis.sismember(hasBoughtSetKey, user.getId().toString());
        if (isBuy) {
            log.error("用户:" + user.getUsername() + "重复购买商品" + product.getProductName());
            throw new SecKillException(SecKillEnum.REPEAT);
        }
        //手动库存减一
        int lastStock = product.getStock() - 1;
        if (lastStock >= 0) {
            product.setStock(lastStock);
            /**
             * 修改库存在version相同的情况下
             */
            boolean secKillSuccess = secKillMapper.updatePosiLockInMySQL(product);
            if (!secKillSuccess) {
                log.error("用户:" + user.getUsername() + "秒杀商品" + product.getProductName() + "失败!");
                throw new SecKillException(SecKillEnum.FAIL);
            }} else {
                log.error("商品:" + product.getProductName() + "库存不足!");
                throw new SecKillException(SecKillEnum.LOW_STOCKS);
            }
            long addResult = jedis.sadd(hasBoughtSetKey, user.getId().toString());
            if (addResult > 0) {
                record = new Record(null, user, product, SecKillEnum.SUCCESS.getCode(), SecKillEnum.SUCCESS.getMessage(), new Date());
                log.info(record.toString());
                boolean insertFlag = secKillMapper.insertRecord(record);
                if (insertFlag) {
                    log.info("用户:" + user.getUsername() + "秒杀商品" + product.getProductName() + "成功!");
                    return SecKillEnum.SUCCESS;
                } else {
                    throw new SecKillException(SecKillEnum.SYSTEM_EXCEPTION);
                }
            } else {
                log.error("用户:" + user.getUsername() + "重复秒杀商品:" + product.getProductName());
                throw new SecKillException(SecKillEnum.REPEAT);
            }
        }

    /**
     * redis的watch监控
     * @param paramMap
     * @return
     */
    public SecKillEnum handleByRedisWatch(Map<String, Object> paramMap) {
        Jedis jedis = redisCacheHandle.getJedis();
        Record record;
        Integer userId = (Integer) paramMap.get("userId");
        Integer productId = (Integer)paramMap.get("productId");
        User user = secKillMapper.getUserById(userId);
        Product product = secKillMapper.getProductById(productId);

        /**
         * 获得该产品的键值对
         */
        String productStockCacheKey = product.getProductName()+"_stock";
        /**
         * 拿到用户所买商品在redis中对应的key
         */
        String hasBoughtSetKey = SecKillUtils.getRedisHasBoughtSetKey(product.getProductName());
        /**
         * 开启watch监控
         * 可以决定事务是执行还是回滚
         * 它首先会去比对被 watch 命令所监控的键值对，如果没有发生变化，那么它会执行事务队列中的命令，
         * 提交事务；如果发生变化，那么它不会执行任何事务中的命令，而去事务回滚。无论事务是否回滚，
         * Redis 都会去取消执行事务前的 watch 命令
         */
        jedis.watch(productStockCacheKey);

        boolean isBuy = jedis.sismember(hasBoughtSetKey, user.getId().toString());
        if (isBuy){
            log.error("用户:"+user.getUsername()+"重复购买商品"+product.getProductName());
            throw new SecKillException(SecKillEnum.REPEAT);
        }
        String stock = jedis.get(productStockCacheKey);
        if (Integer.parseInt(stock) <= 0) {
            log.error("商品:"+product.getProductName()+"库存不足!");
            throw new SecKillException(SecKillEnum.LOW_STOCKS);
        }
        //开启redis事务
        Transaction tx = jedis.multi();

        //库存减一
        tx.decrBy(productStockCacheKey,1);
        //执行事务
        List<Object> resultList = tx.exec();

        if(resultList == null || resultList.isEmpty()){
            jedis.unwatch();
            //watch监控被更改过----物品抢购失败;
            log.error("商品:"+product.getProductName()+",watch监控被更改,物品抢购失败");
            throw new SecKillException(SecKillEnum.FAIL);
        }

        //添加到已买队列
        long addResult = jedis.sadd(hasBoughtSetKey,user.getId().toString());
        if(addResult>0){
            //秒杀成功
            record =  new Record(null,user,product,SecKillEnum.SUCCESS.getCode(),SecKillEnum.SUCCESS.getMessage(),new Date());
           //添加record到rabbitmq消息队列
            rabbitMQSender.send(JSON.toJSONString(record));
            return SecKillEnum.SUCCESS;
        }else{
            //重复秒杀
            //这里抛出RuntimeException异常，redis的decr操作并不会回滚，所以需要手动incr回去
            jedis.incrBy(productStockCacheKey,1);
            throw new SecKillException(SecKillEnum.REPEAT);
        }

    }


    /**
     * AtomicInteger的cas机制
     * @param paramMap
     * @return
     */
    @Transactional
    public SecKillEnum handleByAtomicInteger(Map<String, Object> paramMap) {
        Jedis jedis = redisCacheHandle.getJedis();
        Record record;

        Integer userId = (Integer) paramMap.get("userId");
        Integer productId = (Integer)paramMap.get("productId");
        User user = secKillMapper.getUserById(userId);
        Product product = secKillMapper.getProductById(productId);

        String hasBoughtSetKey = SecKillUtils.getRedisHasBoughtSetKey(product.getProductName());
        //判断是否重复购买
        boolean isBuy = jedis.sismember(hasBoughtSetKey, user.getId().toString());
        if (isBuy){
            log.error("用户:"+user.getUsername()+"重复购买商品"+product.getProductName());
            throw new SecKillException(SecKillEnum.REPEAT);
        }
        AtomicInteger atomicInteger = atomicStock.getAtomicInteger(product.getProductName());
        int stock = atomicInteger.decrementAndGet();

        if(stock < 0){
            log.error("商品:"+product.getProductName()+"库存不足, 抢购失败!");
            throw new SecKillException(SecKillEnum.LOW_STOCKS);
        }

        long result = jedis.sadd(hasBoughtSetKey,user.getId().toString());
        if (result > 0){
            record = new Record(null,user,product,SecKillEnum.SUCCESS.getCode(),SecKillEnum.SUCCESS.getMessage(),new Date());
            log.info(record.toString());
            boolean insertFlag =  secKillMapper.insertRecord(record);
            if (insertFlag) {
                //更改物品库存
                secKillMapper.updateByAsynPattern(record.getProduct());
                log.info("用户:"+user.getUsername()+"秒杀商品"+product.getProductName()+"成功!");
                return SecKillEnum.SUCCESS;
            } else {
                log.error("系统错误!");
                throw new SecKillException(SecKillEnum.SYSTEM_EXCEPTION);
            }
        } else {
            log.error("用户:"+user.getUsername()+"重复秒杀商品"+product.getProductName());
            atomicInteger.incrementAndGet();
            throw new SecKillException(SecKillEnum.REPEAT);
        }
    }
}