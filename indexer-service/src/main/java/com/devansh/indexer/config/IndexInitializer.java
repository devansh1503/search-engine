package com.devansh.indexer.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class IndexInitializer {
    @Autowired
    private ElasticsearchClient esClient;
    private final String INDEX_NAME = "pages";

    @PostConstruct
    public void init() {
        try{
            boolean exists = esClient.indices()
                    .exists(e -> e.index(INDEX_NAME))
                    .value();

            if(exists){
                System.out.println("Index already exists");
                return;
            }

            System.out.println("Creating index: "+ INDEX_NAME);

            Map<String, Property> properties = new HashMap<>();

            properties.put("url", Property.of(p -> p.keyword(k -> k)));
            properties.put("title", Property.of(p -> p.text(t -> t)));
            properties.put("content", Property.of(p -> p.text(t -> t)));
            properties.put("pagerank", Property.of(p -> p.float_(f -> f)));

            properties.put("embedding", Property.of(p -> p.denseVector(dv -> dv
                    .dims(768)
                    .index(true)
                    .similarity("cosine")
            )));

            esClient.indices().create(c -> c
                    .index(INDEX_NAME)
                    .mappings(m -> m.properties(properties))
            );
            System.out.println("Index created Successfully");
        }catch(Exception e){
            throw new RuntimeException("Failed to initialize ElasticSearch index", e);
        }
    }
}
