package com.ygk.seckill.utils;

import com.ygk.seckill.constant.RedisCacheConst;

public class SecKillUtils {
    public static String getRedisHasBoughtSetKey(String productName){
        String hasBySet = "";
        if (productName!=null && !productName.isEmpty()){
            switch (productName){
                case "iphone":
                    hasBySet = RedisCacheConst.IPHONE_HAS_BOUGHT_SET;
                    break;
                case "huawei":
                    hasBySet = RedisCacheConst.HUAWEI_HAS_BOUGHT_SET;
                    break;
                case "samsung":
                    hasBySet = RedisCacheConst.SAMSUNG_HAS_BOUGHT_SET;
                    break;
                case "xiaomi":
                    hasBySet = RedisCacheConst.XIAOMI_HAS_BOUGHT_SET;
                    break;
            }
        }
        return hasBySet;
    }
}
