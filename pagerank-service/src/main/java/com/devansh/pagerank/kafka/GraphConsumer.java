package com.devansh.pagerank.kafka;

import com.devansh.common.model.CrawledPage;
import com.devansh.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class GraphConsumer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "raw-pages", groupId = "pagerank-group")
    public void consume(String message){
        CrawledPage page = JsonUtil.fromJson(message, CrawledPage.class);

        String url = page.getUrl();
        if(url == null) return;

        redisTemplate.opsForSet().add("graph:nodes", url);

        if(page.getLinks() != null){
            for(String link : page.getLinks()){
                if(link == null || link.isBlank()) continue;

                redisTemplate.opsForSet().add("graph:out:"+url, link);
                redisTemplate.opsForSet().add("graph:in:"+link, url);
                redisTemplate.opsForSet().add("graph:nodes", link);
            }
        }
    }
}
