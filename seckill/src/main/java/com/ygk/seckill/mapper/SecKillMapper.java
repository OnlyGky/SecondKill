package com.ygk.seckill.mapper;

import com.ygk.seckill.entity.Product;
import com.ygk.seckill.entity.Record;
import com.ygk.seckill.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
@Component
public interface SecKillMapper {
    List<User> getAllUser();

    User getUserById(Integer id);

    List<Product> getAllProduct();

    Product getProductById(Integer id);

    boolean updatePessLockInMySQL(Product product);

    boolean updatePosiLockInMySQL(Product product);

    boolean insertRecord(Record record);

    boolean updateByAsynPattern(Product product);
}
