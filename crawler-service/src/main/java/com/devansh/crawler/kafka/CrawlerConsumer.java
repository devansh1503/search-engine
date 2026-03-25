package com.devansh.crawler.kafka;

import com.devansh.crawler.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CrawlerConsumer {
    @Autowired
    private CrawlerService crawlerService;

    @KafkaListener(topics = "urls", groupId = "crawler-group")
    public void consume(String message){
        String[]parts = message.split("\\|");
        String url = parts[0];
        int depth = parts.length>1 ? Integer.parseInt(parts[1]) : 0;
        crawlerService.crawlSingle(url, depth);
    }
}
