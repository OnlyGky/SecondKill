package com.ygk.seckill.controller;


import com.ygk.seckill.common.Message;
import com.ygk.seckill.common.SecKillEnum;
import com.ygk.seckill.service.SecKillService;
import com.ygk.seckill.web.req.SecKillRequest;
import com.ygk.seckill.web.vo.SecKillResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/seckill")
@Controller
public class SecKillController {
    @Autowired
    private SecKillService secKillService;


    /**
     * MySql悲观锁实现
     */

    @RequestMapping(value = "pessLockInMySQL",method = RequestMethod.POST)
    public Message<SecKillResponse> pessLockInMySQL(@RequestBody Message<SecKillRequest> requestMessage){
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId",requestMessage.getBody().getUserId());
        paramMap.put("productId",requestMessage.getBody().getProductId());
        SecKillEnum secKillEnum = secKillService.handleByPessLockInMySQL(paramMap);
        Message<SecKillResponse> responseMessage = new Message<>(secKillEnum,null);
        return responseMessage;
    }

    /**
     * 利用mybstis乐观锁实现
     * @param requestMessage
     * @return
     */
    @RequestMapping(value = "/posiLockInMySQL",method = RequestMethod.POST)
    public Message<SecKillResponse> posiLockInMySQL(@RequestBody Message<SecKillRequest> requestMessage){
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId",requestMessage.getBody().getUserId());
        paramMap.put("productId",requestMessage.getBody().getProductId());
        SecKillEnum secKillEnum = secKillService.handleByPosiLockInMySQL(paramMap);
        Message<SecKillResponse> responseMessage = new Message<>(secKillEnum,null);
        return responseMessage;
    }


    /**
     * 利用redis的watch监控的特性
     */
    @RequestMapping(value = "/baseOnRedisWatch",method = RequestMethod.POST)
    public Message<SecKillResponse> baseOnRedisWatch(@RequestBody Message<SecKillRequest> requestMessage) throws InterruptedException {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId",requestMessage.getBody().getUserId());
        paramMap.put("productId",requestMessage.getBody().getProductId());
        SecKillEnum secKillEnum = secKillService.handleByRedisWatch(paramMap);
        Message<SecKillResponse> responseMessage = new Message<>(secKillEnum,null);
        return responseMessage;
    }

    /**
     * 利用AtomicInteger的CAS机制特性
     * @param requestMessage
     * @return
     */
    public  Message<SecKillResponse> baseOnAtomicInteger(@RequestBody Message<SecKillRequest> requestMessage){
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("userId",requestMessage.getBody().getUserId());
        paramMap.put("productId",requestMessage.getBody().getProductId());
        SecKillEnum secKillEnum = secKillService.handleByAtomicInteger(paramMap);
        Message<SecKillResponse> responseMessage = new Message<>(secKillEnum,null);
        return responseMessage;
    }
}
