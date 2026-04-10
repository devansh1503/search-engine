package com.devansh.query.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.devansh.query.model.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ChatClient chatClient;


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
            Map res = restTemplate.postForObject(
                    "http://embedding-service:11434/api/embeddings",
                    Map.of(
                            "model", "nomic-embed-text",
                            "prompt", query
                    ),
                    Map.class
            );

            List<?> queryVector = (List<?>) res.get("embedding");

//            Map faissRes = restTemplate.postForObject(
//                    "http://faiss-service:8002/search",
//                    Map.of("embedding", rawVector, "k", 20),
//                    Map.class
//            );
//
//            List<Map<String, Object>> vectorResult =
//                    (List<Map<String, Object>>) faissRes.get("results");
//
//            Map<String, Double> vectorScores = new HashMap<>();
//            for( Map<String, Object> result : vectorResult ) {
//                vectorScores.put(
//                        (String) result.get("url"),
//                        1 / (1 + (Double) result.get("score"))
//                );
//            }

            // BM25 + Pagerank ES Result-

            co.elastic.clients.elasticsearch.core.SearchResponse<Map> esResponse =
                    elasticsearchClient.search(s -> s
                    .index("pages")
                    .query(q -> q
                            .scriptScore(ss -> ss
                                    .query(q2 -> q2
                                            .multiMatch(m -> m
                                                    .query(query)
                                                    .fields("title", "content")
                                            )
                                    )
                                    .script(sc -> sc
                                            .inline(m -> m
                                                    .source("""
                                                            double cosine = cosineSimilarity(params.query_vector, 'embedding');
                                                            double pr = doc['pagerank'].size() == 0 ? 1.0 : doc['pagerank'].value;
                                                            
                                                            return 0.3 * _score + 0.6 * (cosine + 1.0) + 0.1 * Math.log(1 + pr);
                                                            """)
                                                    .params("query_vector", JsonData.of(queryVector))
                                            )
                                    )
                            )

                    ),
                    Map.class
            );


            for(Hit<Map> hit : esResponse.hits().hits()) {
                Map source = hit.source();
                String url = (String) source.get("url");

                double finalScore = hit.score();

                String content = (String) source.get("content");
                String snippet = content != null && content.length() > 150 ? content.substring(0, 150) : content;

                results.add(new SearchResponse(
                        url,
                        (String) source.get("title"),
                        content,
                        snippet,
                        finalScore
                ));
            }

            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));


            redisTemplate.opsForValue().set(query, objectMapper.writeValueAsString(results));
        }catch(Exception e) {
            System.out.println("SEARCH FAILED BECAUSE: "+e.getMessage());
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

    public String searchAiSummary(List<SearchResponse> results, String query) {
        String context = results.stream()
                .limit(5)
                .map(r -> {
                    String title = r.getTitle() != null ? r.getTitle() : "";
                    String content = r.getContent() != null ? r.getContent() : "";
                    return """
                   Title: %s
                   Content: %s
                   """.formatted(title, content);
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
                You are a search assistant.
                
                Answer the question using ONLY the provided context.
                
                Rules:
                - Do NOT make up information
                - If the answer is not present, say: "I don't know"
                - Be concise and clear
                - If possible, combine information from multiple sources
                
                Context:
                %s
                
                Query:
                %s
                
                Answer:
                """.formatted(context, query);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content()
                .trim();
        System.out.println(response);
        return response;
    }
    private RedisConnection getConnection(){
        return redisTemplate.getConnectionFactory().getConnection();
    }

    private String normalize(String input){
        return input==null ? "" : input.trim().toLowerCase();
    }

}
