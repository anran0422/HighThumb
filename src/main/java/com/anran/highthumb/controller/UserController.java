package com.anran.highthumb.controller;


import com.anran.highthumb.common.BaseResponse;
import com.anran.highthumb.common.ResultUtils;
import com.anran.highthumb.constant.UserConstant;
import com.anran.highthumb.model.entity.User;
import com.anran.highthumb.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user")
public class UserController {  
    @Resource
    private UserService userService;

    @GetMapping("/login")
    public BaseResponse<User> login(long userId, HttpServletRequest request) {
        User user = userService.getById(userId);
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return ResultUtils.success(user);
    }
    @GetMapping("/get/login")
    public BaseResponse<User> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(loginUser);
    }
}