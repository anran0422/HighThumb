package com.anran.highthumb.service.impl;

import com.anran.highthumb.constant.UserConstant;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anran.highthumb.model.entity.User;
import com.anran.highthumb.service.UserService;
import com.anran.highthumb.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
* @author macbook
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-04-27 12:29:56
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
    }
}




