package com.ygk.seckill.common;

import lombok.Data;

/**
 * 返回给前端页面的头部信息
 * @author twc
 */
@Data
public class Head {

    /**
     * 状态码
     */
    private String statusCode;

    /**
     * 状态信息
     */
    private String statusMessage;

}
