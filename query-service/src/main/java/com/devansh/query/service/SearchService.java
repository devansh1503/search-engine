package com.devansh.query.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.devansh.query.model.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGADD =
            () -> "FT.SUGADD".getBytes();

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGGET =
            () -> "FT.SUGGET".getBytes();

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private StringRedisTemplate redisTemplate;


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
                            .functionScore(fs -> fs
                                    .query(q2 -> q2
                                            .multiMatch(m -> m
                                                    .query(query)
                                                    .fields("title", "content")
                                            )
                                    )
                                    .functions(f -> f
                                            .fieldValueFactor(v -> v
                                                    .field("pagerank")
                                                    .missing(1.0)
                                            )
                                    )
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
            incrementQuery(query);
        }

        return results;
    }

    public void incrementQuery(String query){
        String normalized = normalize(query);
        if(normalized.length() < 2) return;

        RedisConnection connection = getConnection();

        ((LettuceConnection) connection).getNativeConnection().dispatch(
                FT_SUGADD,
                new IntegerOutput<>(ByteArrayCodec.INSTANCE),
                new CommandArgs<>(ByteArrayCodec.INSTANCE)
                        .addKey("autocomplete:suggest".getBytes())
                        .addValue(normalized.getBytes())
                        .add(1)
                        .add("INCR")
        );
    }

    private RedisConnection getConnection(){
        return redisTemplate.getConnectionFactory().getConnection();
    }

    private String normalize(String input){
        return input==null ? "" : input.trim().toLowerCase();
    }

}
