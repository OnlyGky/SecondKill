package com.ygk.seckill.exception;

import com.ygk.seckill.common.SecKillEnum;
import lombok.Data;

@Data
public class SecKillException extends RuntimeException {

    private SecKillEnum secKillEnum;

    public SecKillException(SecKillEnum secKillEnum){
        this.secKillEnum = secKillEnum;
    }
}