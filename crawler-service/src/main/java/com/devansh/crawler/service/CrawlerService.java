package com.devansh.crawler.service;

import com.devansh.common.model.CrawledPage;
import com.devansh.common.util.JsonUtil;
import com.devansh.crawler.kafka.CrawlerProducer;
import com.devansh.crawler.util.UrlUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class CrawlerService {

    private final Set<String> visited = Collections.synchronizedSet(new HashSet<String>());


    @Autowired
    private CrawlerProducer crawlerProducer;

    public void crawlSingle(String url){

        if(url == null || visited.contains(url)) return;

        try{
            System.out.println("CRAWLING: " + url);

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            visited.add(url);

            String title = document.title();
            String text = document.body().text();

            Elements links = document.select("a[href]");
            List<String> extractedLinks = new ArrayList<>();

            links.forEach(link -> {
                String normalized = UrlUtil.normalize(url, link.attr("href"));
                if(normalized!=null && !visited.contains(normalized)){
                    extractedLinks.add(normalized);
                    crawlerProducer.sendUrl(normalized);
                }
            });

            CrawledPage page = CrawledPage.builder()
                    .url(url)
                    .title(title)
                    .content(text)
                    .links(extractedLinks)
                    .timestamp(System.currentTimeMillis())
                    .build();

            crawlerProducer.sendRawPage(JsonUtil.toJson(page));

        }
        catch(Exception e) {
            System.out.println("FAILED: " + url);
            System.out.println(e.getMessage());
        }
    }
}
