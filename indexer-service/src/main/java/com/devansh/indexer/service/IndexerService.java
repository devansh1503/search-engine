package com.devansh.indexer.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.devansh.common.model.CrawledPage;
import com.devansh.common.util.JsonUtil;
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

import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class IndexerService {

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGADD =
            () -> "FT.SUGADD".getBytes();

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGGET =
            () -> "FT.SUGGET".getBytes();

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    public void index(String json){
        CrawledPage page = JsonUtil.fromJson(json, CrawledPage.class);

        try{
            String text = page.getTitle() + " " + page.getContent();

            Map response = restTemplate.postForObject(
                    "http://embedding-service:12434/engines/v1/embeddings",
                    Map.of(
                            "model", "ai/qwen3-embedding",
                            "input", text
                    ),
                    Map.class
            );

            Object embedding = response.get("embedding");
            Map<String, Object> doc = Map.of(
                    "url", page.getUrl(),
                    "title", page.getTitle(),
                    "content", page.getContent(),
                    "embedding", embedding
            );
            esClient.index(i -> i
                    .index("pages")
                    .id(page.getUrl())
                    .document(doc)
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

        RedisConnection connection = getConnection();

        ((LettuceConnection) connection).getNativeConnection().dispatch(
                FT_SUGADD,
                new IntegerOutput<>(ByteArrayCodec.INSTANCE),
                new CommandArgs<>(ByteArrayCodec.INSTANCE)
                        .addKey("autocomplete:suggest".getBytes())
                        .addValue(normalized.getBytes())
                        .add(1)
        );
    }

    public RedisConnection getConnection(){
        return stringRedisTemplate.getConnectionFactory().getConnection();
    }
}
