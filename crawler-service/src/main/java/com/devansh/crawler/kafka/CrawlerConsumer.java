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
    public void consume(String url){
        crawlerService.crawlSingle(url);
    }
}
