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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

            List<?> rawVector = (List<?>) res.get("embedding");
            List<Double> queryVector = rawVector.stream()
                    .map(v -> ((Number) v).doubleValue())
                    .toList();


            co.elastic.clients.elasticsearch.core.SearchResponse<Map> response = elasticsearchClient.search(s -> s
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
                                            .inline(in -> in
                                                    .source("""
                                                            double vectorScore = cosineSimilarity(params.query_vector, 'embedding');
                                                            double pagerankScore = doc['pagerank'].size() == 0 ? 1.0 : doc['pagerank'].value;
                                                            return vectorScore +  pagerankScore + 1.0;
                                                            """
                                                    )
                                                    .params(Map.of("query_vector", JsonData.of(queryVector)))
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

                results.add(new SearchResponse(url, title, snippet, content));
            }

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

    public String searchAiSummary(String query) {
        String normalized = normalize(query);
        System.out.println(normalized);
        List<SearchResponse> results = search(normalized);

        String context = results.stream()
                .limit(5)
                .map(r -> {
                    String title = r.getTitle() != null ? r.getTitle() : "";
                    String content = r.getContent() != null ? r.getContent() : "";

                    // Trim content to avoid token explosion
                    content = content.length() > 500 ? content.substring(0, 500) : content;

                    return """
                   Title: %s
                   Content: %s
                   """.formatted(title, content);
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
                You are a search assistant.
                
                Answer the user's query using only the information provided in the context below.
                Do Not Make up information.
                If the answer is not present in the context, say: "I could not find relevant information."
                
                Keep the answer clear and concise.
                
                Context:
                %s
                
                Query:
                %s
                
                Answer:
                """.formatted(context, query);
        System.out.println(prompt);
        Map<String, Object> request = Map.of(
                "model", "llama3",
                "prompt", prompt
        );

        Map response = restTemplate.postForObject(
                "http://embedding-service:11434/api/generate",
                request,
                Map.class
        );

//        String response = chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content()
//                .trim();
        System.out.println(response);
        return (String) response.get("response");
    }
    private RedisConnection getConnection(){
        return redisTemplate.getConnectionFactory().getConnection();
    }

    private String normalize(String input){
        return input==null ? "" : input.trim().toLowerCase();
    }

}
