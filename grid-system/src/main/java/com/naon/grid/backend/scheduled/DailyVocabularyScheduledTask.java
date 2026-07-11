package com.naon.grid.backend.scheduled;

import com.naon.grid.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyVocabularyScheduledTask {

    private final RedisUtils redisUtils;

    private static final String CACHE_KEY_TODAY_MAIN = "daily_vocabulary:today:main";
    private static final String CACHE_KEY_TODAY_BACKUPS = "daily_vocabulary:today:backups";

    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshTodayCache() {
        log.info("每日一词缓存刷新开始");
        redisUtils.del(CACHE_KEY_TODAY_MAIN, CACHE_KEY_TODAY_BACKUPS);
        log.info("每日一词缓存已清除，等待懒加载");
    }
}
