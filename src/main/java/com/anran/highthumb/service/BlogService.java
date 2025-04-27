package com.anran.highthumb.service;

import com.anran.highthumb.model.VO.BlogVO;
import com.anran.highthumb.model.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author macbook
* @description 针对表【blog】的数据库操作Service
* @createDate 2025-04-27 12:36:29
*/
public interface BlogService extends IService<Blog> {
    BlogVO getBlogVOById(long blogId, HttpServletRequest request);

    List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request);
}
