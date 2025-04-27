package com.anran.highthumb.service;


import com.anran.highthumb.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author macbook
* @description 针对表【user】的数据库操作Service
* @createDate 2025-04-27 12:29:56
*/
public interface UserService extends IService<User> {

    User getLoginUser(HttpServletRequest request);
}
