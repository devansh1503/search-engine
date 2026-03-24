package com.devansh.crawler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public boolean allow(String domain) {
        String key = "rate_limit:" + domain;

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMillis(500));

        return Boolean.TRUE.equals(success);
    }
}
