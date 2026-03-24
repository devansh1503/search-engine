package com.devansh.crawler.runner;

import com.devansh.crawler.service.CrawlerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrawlerRunner {

    @Bean
    CommandLineRunner run(CrawlerService crawlerService) {
        return args -> crawlerService.start("");
    }

}
