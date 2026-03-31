package com.devansh.pagerank.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class ESUpdater {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public void update() {
        Set<String> nodes = stringRedisTemplate.opsForSet().members("graph:nodes");
        if(nodes == null) return;

        for(String node : nodes){
            try{
                String scoreStr = stringRedisTemplate.opsForValue().get("pagerank:" + node);
                if(scoreStr == null) continue;

                double score = Double.parseDouble(scoreStr);

                elasticsearchClient.update(u -> u
                        .index("pages")
                        .id(node)
                        .doc(Map.of("pagerank", score)),
                        Map.class
                );
            } catch (Exception ignored){}
        }

        System.out.println("ES for Pagerank Updated");
    }
}
