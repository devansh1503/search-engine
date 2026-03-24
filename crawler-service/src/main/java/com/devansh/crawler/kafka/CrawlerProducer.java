package com.devansh.crawler.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CrawlerProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendRawPage(String message) {
        kafkaTemplate.send("raw-pages", message);
    }

    public void sendUrl(String url) {
        kafkaTemplate.send("urls", url);
    }
}
