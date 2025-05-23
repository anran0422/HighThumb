package com.anran.highthumb.mapper;

import com.anran.highthumb.model.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
* @author macbook
* @description 针对表【blog】的数据库操作Mapper
* @createDate 2025-04-27 12:36:29
* @Entity com.anran.highthumb.model.entity.Blog
*/
public interface BlogMapper extends BaseMapper<Blog> {
    void batchUpdateThumbCount(@Param("countMap")Map<Long, Long> countMap);
}




