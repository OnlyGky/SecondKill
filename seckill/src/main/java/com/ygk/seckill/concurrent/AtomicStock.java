package com.ygk.seckill.concurrent;

import com.ygk.seckill.entity.Product;
import com.ygk.seckill.mapper.SecKillMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data  //自动填充get set等方法
@Component
public class AtomicStock {
    private AtomicInteger samsungInteger = new AtomicInteger();

    private AtomicInteger huaweiInteger = new AtomicInteger();

    private AtomicInteger xiaomiInteger = new AtomicInteger();

    private AtomicInteger iphoneInteger = new AtomicInteger();

    @Autowired
    private SecKillMapper secKillMapper;

    @PostConstruct //被注解的方法，在对象加载完依赖后执行，只执行一次
    public void initAtomicInteger(){
        List<Product> productList = secKillMapper.getAllProduct();
        for(Product product : productList){
            //将每个商品的库存放到对应的AtomicInteger中
            getAtomicInteger(product.getProductName()).set(product.getStock());
        }
    }

    public AtomicInteger getAtomicInteger(String productName) {
        AtomicInteger ai = null;
        if(productName !=null && !productName.isEmpty()){
            switch (productName){
                case "iphone":
                    ai = iphoneInteger;
                    break;
                case "huawei":
                    ai = huaweiInteger;
                    break;
                case "samsung":
                    ai = samsungInteger;
                    break;
                case "xiaomi":
                    ai = xiaomiInteger;
                    break;
            }
        }
        return ai;
    }
}
