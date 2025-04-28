package com.anran.highthumb.service;

import com.anran.highthumb.model.dto.DoThumbRequest;
import com.anran.highthumb.model.entity.Thumb;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author macbook
* @description 针对表【thumb】的数据库操作Service
* @createDate 2025-04-27 12:29:00
*/
public interface ThumbService extends IService<Thumb> {

    /**
     * 点赞
     */
    Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

    /**
     * 取消点赞
     */
    Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

    Boolean hasThumb(Long blogId, Long userId);
}
