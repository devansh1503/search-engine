package com.devansh.crawler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RateLimiterService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final int MAX_REQUEST = 500;

    public boolean allow(String domain) {
        String key = "rate_limit:" + domain;
        long now = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, now-60000);

        Long count = stringRedisTemplate.opsForZSet().zCard(key);

        if(count!=null && count>=MAX_REQUEST){
            return false;
        }

        stringRedisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);

        return true;
    }
}
