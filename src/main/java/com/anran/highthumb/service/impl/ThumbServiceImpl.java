package com.anran.highthumb.service.impl;

import com.anran.highthumb.constant.ThumbConstant;
import com.anran.highthumb.exception.BusinessException;
import com.anran.highthumb.exception.ErrorCode;
import com.anran.highthumb.exception.ThrowUtils;
import com.anran.highthumb.mapper.ThumbMapper;
import com.anran.highthumb.model.dto.DoThumbRequest;
import com.anran.highthumb.model.entity.Blog;
import com.anran.highthumb.model.entity.Thumb;
import com.anran.highthumb.model.entity.User;
import com.anran.highthumb.service.BlogService;
import com.anran.highthumb.service.ThumbService;
import com.anran.highthumb.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
* @author macbook
* @description 针对表【thumb】的数据库操作Service实现
* @createDate 2025-04-27 12:29:00
*/
@Service
@Slf4j
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb>
    implements ThumbService{

    @Resource
    private UserService userService;
    @Resource
    private BlogService blogService;
    @Resource
    private TransactionTemplate transactionTemplate;
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
        // 加锁
        synchronized (loginUser.getId().toString().intern()) {

            // 编程式事务
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();
                boolean exist = this.hasThumb(blogId, loginUser.getId());

                if(exist) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已经点赞");
                }

                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount + 1")
                        .update();
                Thumb thumb = new Thumb();
                thumb.setUserId(loginUser.getId());
                thumb.setBlogId(blogId);
                // 更新成功才执行
                boolean success =  update && this.save(thumb);

                // 点赞存入 Redis
                if(success) {
                    redisTemplate.opsForHash()
                            .put(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(),
                                    blogId.toString(),
                                    thumb.getId());
                }
                return success;
            });
        }
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if(doThumbRequest == null || doThumbRequest.getBlogId() == null ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        User loginUser = userService.getLoginUser(request);

        synchronized (loginUser.getId().toString().intern()) {
            // 编程事务
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();
                //Thumb thumb = this.lambdaQuery()
                //        .eq(Thumb::getUserId, loginUser.getId())
                //        .eq(Thumb::getBlogId, blogId)
                //        .one();

                Object thumbIdObj = redisTemplate.opsForHash().get(
                        ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(),
                        blogId.toString());
                ThrowUtils.throwIf(thumbIdObj == null, ErrorCode.OPERATION_ERROR, "用户未点赞");

                Long thumbId = Long.valueOf(thumbIdObj.toString());

                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount - 1")
                        .update();
                boolean success =  update && this.removeById(thumbId);

                // 点赞记录从 Redis 删除
                if(success) {
                    redisTemplate.opsForHash().delete(

                            ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(),
                            blogId.toString());
                }
                return success;
            });
        }
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash()
                .hasKey(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
    }
}




