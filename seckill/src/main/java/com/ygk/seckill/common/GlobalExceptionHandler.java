package com.ygk.seckill.common;

import com.ygk.seckill.exception.SecKillException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局捕获异常类，只要作用在@RequestMapping上，所有的异常都会被捕获
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    //不加ResponseBody的话会会报错
    @ExceptionHandler(value = SecKillException.class)
    @ResponseBody
    public Message handleSecKillException(SecKillException secKillException){
        log.info(secKillException.getSecKillEnum().getMessage());
        return new Message(secKillException.getSecKillEnum());
    }
}
