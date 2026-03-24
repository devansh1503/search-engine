package com.devansh.indexer.kafka;

import com.devansh.indexer.service.IndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class IndexerConsumer {

    @Autowired
    private IndexerService indexerService;

    @KafkaListener(topics = "raw-pages", groupId = "indexer-group")
    public void consume(String message){
        indexerService.index(message);
    }
}
