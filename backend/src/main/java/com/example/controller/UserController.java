package com.example.controller;

import com.example.entity.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 创建新用户
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        return ResponseEntity.ok(deleted);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        User user = userService.login(username, password);
        if (user != null) {
            // 登录成功，将用户信息保存到Session中
            session.setAttribute("user", user);
            // 设置Session过期时间为30分钟
            session.setMaxInactiveInterval(30 * 60);
            // 不返回密码信息
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(401).build(); // 未授权
        }
    }
    
    /**
     * 检查Session是否有效
     */
    @GetMapping("/checkSession")
    public ResponseEntity<Boolean> checkSession(HttpSession session) {
        // 检查Session中是否存在用户信息
        boolean isLoggedIn = session.getAttribute("user") != null;
        return isLoggedIn ? ResponseEntity.ok(true) : ResponseEntity.status(401).build();
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Boolean> logout(HttpSession session) {
        // 使当前Session失效
        session.invalidate();
        return ResponseEntity.ok(true);
    }
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public ResponseEntity<User> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            // 不返回密码信息
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(401).build();
        }
    }
}