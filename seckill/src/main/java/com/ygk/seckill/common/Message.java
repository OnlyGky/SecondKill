package com.ygk.seckill.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 这是一个返回信息的类
 * @param <T>
 */

@Data //使用这个注解，Getter,Setter,equals,canEqual,hasCode,toString等方法会在编译时自动加进去
@NoArgsConstructor //使用后创建一个无参构造函数
@AllArgsConstructor //使用后添加一个构造函数，该构造函数含有所有已声明字段属性参数
public class Message<T> {

    private Head head;

    private T body;

    public Message(SecKillEnum resultEnum, T body){
        this.head = new Head();
        this.head.setStatusCode(resultEnum.getCode());
        this.head.setStatusMessage(resultEnum.getMessage());
        this.body = body;
    }

    public Message(SecKillEnum resultEnum){
        this.head = new Head();
        this.head.setStatusCode(resultEnum.getCode());
        this.head.setStatusMessage(resultEnum.getMessage());
    }

}
