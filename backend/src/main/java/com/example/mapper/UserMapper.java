package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 继承BaseMapper可以直接使用MyBatis-Plus提供的CRUD方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}