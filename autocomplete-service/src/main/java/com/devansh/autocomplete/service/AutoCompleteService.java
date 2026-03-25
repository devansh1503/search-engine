package com.devansh.autocomplete.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoCompleteService {

    private static final String KEY = "autocomplete";
    private static final int LIMIT = 10;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String normalize(String input){
        return input==null ? "" : input.trim().toLowerCase();
    }

    public List<String> getSuggestions(String prefix){
        String normalized = normalize(prefix);
        if(normalized.length() < 2) return new ArrayList<>();

        Range<String> range = Range.closed(normalized, normalized + "\uFFFF");
        Limit limit = Limit.limit().count(LIMIT);

        Set<String> candidates = stringRedisTemplate.opsForZSet().rangeByLex(KEY, range, limit);

        if (candidates==null || candidates.isEmpty()) return new ArrayList<>();

        return candidates.stream()
                .map(s -> new AbstractMap.SimpleEntry<>(s,
                        stringRedisTemplate.opsForZSet().score(KEY, s)))
                .sorted((a,b) -> Double.compare(
                        b.getValue() == null ? 0 : b.getValue(),
                        a.getValue() == null ? 0 : a.getValue()
                ))
                .limit(LIMIT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
