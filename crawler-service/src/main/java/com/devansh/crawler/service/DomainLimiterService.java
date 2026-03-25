package com.devansh.crawler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DomainLimiterService {

    private static final int MAX_URLS_PER_DOMAIN = 100;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public boolean allow(String domain){
        String key = "domain_count:"+domain;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        return count!=null && count<=MAX_URLS_PER_DOMAIN;
    }
}
