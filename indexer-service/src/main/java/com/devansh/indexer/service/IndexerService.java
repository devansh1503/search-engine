package com.devansh.indexer.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.devansh.common.model.CrawledPage;
import com.devansh.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IndexerService {

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void index(String json){
        CrawledPage page = JsonUtil.fromJson(json, CrawledPage.class);

        try{
            esClient.index(i -> i
                    .index("pages")
                    .id(page.getUrl())
                    .document(page)
            );
            System.out.println("INDEXED: "+ page.getUrl());
        }
        catch (Exception e){
            System.out.println("INDEX FAILED: "+e.getMessage());
        }finally {
            addTitle(page.getTitle());
        }
    }

    private String normalize(String input){
        return input==null ? "" : input.trim().toLowerCase();
    }

    public void addTitle(String title){
        String normalized = normalize(title);
        if(normalized.length() < 2) return;

        stringRedisTemplate.opsForZSet().add("autocomplete", normalized, 1);
    }
}
