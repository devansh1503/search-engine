package com.devansh.query.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.devansh.autocomplete.service.AutoCompleteService;
import com.devansh.query.model.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AutoCompleteService autoCompleteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<SearchResponse> search(String query) {
        String cached = redisTemplate.opsForValue().get(query);
        if(cached != null) {
            try {
                return Arrays.asList(
                        objectMapper.readValue(cached, SearchResponse[].class)
                );
            }catch(Exception e) {}
        }

        List<SearchResponse> results = new ArrayList<>();

        try{
            co.elastic.clients.elasticsearch.core.SearchResponse<Map> response = elasticsearchClient.search(s -> s
                    .index("pages")
                    .query(q -> q
                            .multiMatch(m -> m
                                    .query(query)
                                    .fields("title", "content")
                            )
                    ),
                    Map.class
            );

            for(Hit<Map>hit : response.hits().hits()){
                Map source = hit.source();
                String url = (String) source.get("url");
                String title = (String) source.get("title");
                String content = (String) source.get("content");

                String snippet = content != null && content.length() > 150 ? content.substring(0, 150) : content;

                results.add(new SearchResponse(url, title, snippet));
            }

            redisTemplate.opsForValue().set(query, objectMapper.writeValueAsString(results));
        }catch(Exception e) {
            System.out.println("SEARCH FAILED: "+e.getMessage());
        }finally {
            autoCompleteService.incrementQuery(query);
        }

        return results;
    }

}
