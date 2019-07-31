package com.ygk.seckill.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static java.util.UUID.randomUUID;

/**
 * 可靠确认模式
 */
@Slf4j
@Component
public class RabbitMQSender implements RabbitTemplate.ConfirmCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String message){
        rabbitTemplate.setConfirmCallback(this);//指定 ConfirmCallback

        // 自定义消息唯一标识
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        /**
         * 发送消息
         */
        rabbitTemplate.convertAndSend("seckillExchange", "seckillRoutingKey", message, correlationData);

    }

    /**
     * 生产者发送消息后的回调函数
     * @param correlationData
     * @param b
     * @param s
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean b, String s) {
        log.info("callbakck confirm: " + correlationData.getId());
        if(b){
            log.info("插入record成功，更改库存成功");
        }else{
            log.info("cause:"+s);
        }
    }
}
