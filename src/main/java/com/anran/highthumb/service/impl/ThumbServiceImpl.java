package com.anran.highthumb.service.impl;

import com.anran.highthumb.exception.BusinessException;
import com.anran.highthumb.exception.ErrorCode;
import com.anran.highthumb.exception.ThrowUtils;
import com.anran.highthumb.model.dto.DoThumbRequest;
import com.anran.highthumb.model.entity.Blog;
import com.anran.highthumb.model.entity.User;
import com.anran.highthumb.service.BlogService;
import com.anran.highthumb.service.UserService;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anran.highthumb.model.entity.Thumb;
import com.anran.highthumb.service.ThumbService;
import com.anran.highthumb.mapper.ThumbMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.BooleanSupplier;

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
                boolean exist = this.lambdaQuery()
                        .eq(Thumb::getUserId, loginUser.getId())
                        .eq(Thumb::getBlogId, blogId)
                        .exists();

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
                return update && this.save(thumb);
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
                Thumb thumb = this.lambdaQuery()
                        .eq(Thumb::getUserId, loginUser.getId())
                        .eq(Thumb::getBlogId, blogId)
                        .one();

                ThrowUtils.throwIf(thumb == null, ErrorCode.OPERATION_ERROR, "用户未点赞");

                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount - 1")
                        .update();

                return update && this.removeById(thumb.getId());
            });
        }
    }
}




