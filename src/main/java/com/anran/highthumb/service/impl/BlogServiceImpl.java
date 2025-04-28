package com.anran.highthumb.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.anran.highthumb.constant.ThumbConstant;
import com.anran.highthumb.mapper.BlogMapper;
import com.anran.highthumb.model.VO.BlogVO;
import com.anran.highthumb.model.entity.Blog;
import com.anran.highthumb.model.entity.User;
import com.anran.highthumb.service.BlogService;
import com.anran.highthumb.service.ThumbService;
import com.anran.highthumb.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author macbook
* @description 针对表【blog】的数据库操作Service实现
* @createDate 2025-04-27 12:36:29
*/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService {

    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private ThumbService thumbService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public BlogVO getBlogVOById(long blogId, HttpServletRequest request) {
        Blog blog = this.getById(blogId);
        User loginUser = userService.getLoginUser(request);
        return this.getBlogVO(blog, loginUser);
    }

    @Override
    public List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        HashMap<Long, Boolean> blogIdHasThumbMap = new HashMap<>();
        if(ObjUtil.isNotEmpty(loginUser)) {
            List<Object> blogIdList = blogList.stream().map(blog -> blog.getId().toString()).collect(Collectors.toList());

            //List<Thumb> thumbList = thumbService.lambdaQuery()
            //        .eq(Thumb::getUserId, loginUser.getId())
            //        .in(Thumb::getBlogId, blogIdSet)
            //        .list();

            // 获取点赞列表
            List<Object> thumbList = redisTemplate.opsForHash().multiGet(
                    ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(),
                    blogIdList);
            for (int i = 0;i <thumbList.size();i++) {
                if(thumbList.get(i) == null) {
                    continue;
                }
                blogIdHasThumbMap.put(Long.valueOf(blogIdList.get(i).toString()), true);
            }
        }

        return blogList.stream()
            .map(blog -> {
                BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);
                blogVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
                return blogVO;
            })
            .toList();
    }

    private BlogVO getBlogVO(Blog blog, User loginUser) {
        BlogVO blogVO = new BlogVO();
        BeanUtil.copyProperties(blog, blogVO);

        if (loginUser == null) {
            return blogVO;
        }
        //Thumb thumb = thumbService.lambdaQuery()
        //        .eq(Thumb::getUserId, loginUser.getId())
        //        .eq(Thumb::getBlogId, blog.getId())
        //        .one();
        // 获取点赞
        Boolean exist = thumbService.hasThumb(blog.getId(), loginUser.getId());
        blogVO.setHasThumb(exist);

        return blogVO;
    }
}




