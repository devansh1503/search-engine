package com.devansh.crawler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VisitedUrlService {
    private static final String KEY = "visited_urls";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public boolean isVisited(String url) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(KEY, url));
    }

    public void markVisited(String url) {
        stringRedisTemplate.opsForSet().add(KEY, url);
    }
}
