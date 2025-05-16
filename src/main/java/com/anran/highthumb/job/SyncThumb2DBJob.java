package com.anran.highthumb.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.anran.highthumb.mapper.BlogMapper;
import com.anran.highthumb.model.entity.Thumb;
import com.anran.highthumb.model.enums.ThumbTypeEnum;
import com.anran.highthumb.service.ThumbService;
import com.anran.highthumb.util.RedisKeyUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时将 Redis 中的临时点赞数据同步到数据库
 *
 */
@Component
@Slf4j
public class SyncThumb2DBJob {

    @Resource
    private ThumbService thumbService;

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 10000)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        log.info("定时任务，将 Redis 中的临时点赞数据同步到数据库");
        DateTime nowDate = DateUtil.date();
        // 如果秒数未0~9 则返回上一分钟的 50 s
        int second = (DateUtil.second(nowDate) / 10 - 1) * 10;
        if(second == -10) {
            second = 50;
            // 返回上一分钟
            nowDate = DateUtil.offsetMinute(nowDate, -1);
        }
        String timeSlice = DateUtil.format(nowDate, "HH:mm:") + second;
        syncThumb2DBByDate(timeSlice);
        log.info("临时数据同步完成,当前时间片：{}", timeSlice);
    }

    public void syncThumb2DBByDate(String date) {
        // 获取到 临时点赞和取消点赞 数据
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(date);
        Map<Object, Object> allTempThumbMap = redisTemplate.opsForHash().entries(tempThumbKey);
        boolean thumbMapEmpty = CollUtil.isEmpty(allTempThumbMap);

        // 同步 点赞 到数据库
        // 构建插入列表并收集 blogId
        HashMap<Long, Long> blogThumbCountMap = new HashMap<>();
        if(thumbMapEmpty) {
            return;
        }
        ArrayList<Thumb> thumbList = new ArrayList<>();
        LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
        boolean needRemove = false;
        for (Object userIdBlogIdObj : allTempThumbMap.keySet()) {
            String userIdBlogId = (String) userIdBlogIdObj;
            String[] userIdAndBlogId = userIdBlogId.split(StrPool.COLON);
            Long userId = Long.valueOf(userIdAndBlogId[0]);
            Long blodId = Long.valueOf(userIdAndBlogId[1]);
            // -1 取消点赞， 1 点赞
            Integer thumbType = Integer.valueOf(allTempThumbMap.get(userIdBlogId).toString());
            if(thumbType == ThumbTypeEnum.INCR.getValue()) {
                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setBlogId(blodId);
                thumbList.add(thumb);
            } else if (thumbType == ThumbTypeEnum.DECR.getValue()) {
                // 拼接查询条件， 批量删除
                needRemove = true;
                wrapper.or()
                        .eq(Thumb::getUserId, userId)
                        .eq(Thumb::getBlogId, blodId);
            } else {
                if(thumbType != ThumbTypeEnum.NON.getValue()) {
                    log.warn("数据异常：{}", userId + "," + blodId + "," + thumbType);
                }
                continue;
            }
            // 计算点赞增量
            blogThumbCountMap.put(blodId, blogThumbCountMap.getOrDefault(blodId, 0L) + thumbType);
        }
        // 批量插入
        thumbService.saveBatch(thumbList);
        // 批量删除
        if(needRemove) {
            thumbService.remove(wrapper);
        }
        // 批量更新博客点赞量
        if(!blogThumbCountMap.isEmpty()) {
            blogMapper.batchUpdateThumbCount(blogThumbCountMap);
        }
        // 异步删除
        Thread.startVirtualThread(() -> {
            redisTemplate.delete(tempThumbKey);
        });
    }
}
