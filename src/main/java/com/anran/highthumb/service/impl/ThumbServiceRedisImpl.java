package com.anran.highthumb.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.anran.highthumb.constant.RedisLuaScriptConstant;
import com.anran.highthumb.constant.ThumbConstant;
import com.anran.highthumb.exception.BusinessException;
import com.anran.highthumb.exception.ErrorCode;
import com.anran.highthumb.mapper.ThumbMapper;
import com.anran.highthumb.model.dto.DoThumbRequest;
import com.anran.highthumb.model.entity.Thumb;
import com.anran.highthumb.model.entity.User;
import com.anran.highthumb.model.enums.LuaStatusEnum;
import com.anran.highthumb.service.ThumbService;
import com.anran.highthumb.service.UserService;
import com.anran.highthumb.util.RedisKeyUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service("thumbServiceRedis")
@Slf4j
public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb>
    implements ThumbService {

    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 点赞流程
     */
    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if(doThumbRequest == null || doThumbRequest.getBlogId() == null ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();

        String timeSlice = getTimeSlice();

        // Redis Key
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());

        // Lua 脚本
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已经点赞");
        }

        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if(doThumbRequest == null || doThumbRequest.getBlogId() == null ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();
        // 计算时间片
        String timeSlice = getTimeSlice();
        // Redis Key
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);

        // 执行 Lua 脚本
        Long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );
        // 根据返回值处理结果
        if(result == LuaStatusEnum.FAIL.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未点赞");
        }

        return LuaStatusEnum.SUCCESS.getValue() == result;
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数秒，比如当前 11:20:23 ，获取到 11:20:20
        return DateUtil.format(nowDate, "HH:MM:") + (DateUtil.second(nowDate) / 10) * 10;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash()
                .hasKey(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
    }
}




