package com.example.service;

import com.example.entity.User;
import java.util.Optional;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    User getUserById(Long id);
    
    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    User getUserByUsername(String username);
    
    /**
     * 根据手机号获取用户信息
     * @param phone 手机号
     * @return 用户对象，如果不存在则返回null
     */
    User getUserByPhone(String phone);
    
    /**
     * 创建新用户
     * @param user 用户对象
     * @return 创建成功的用户对象，包含生成的ID
     */
    User createUser(User user);
    
    /**
     * 更新用户信息
     * @param user 用户对象（必须包含ID）
     * @return 更新后的用户对象
     */
    User updateUser(User user);
    
    /**
     * 删除用户
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long id);
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回用户对象，否则返回null
     */
    User login(String username, String password);
}