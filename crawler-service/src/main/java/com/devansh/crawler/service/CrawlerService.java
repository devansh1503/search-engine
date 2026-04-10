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

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class CrawlerService {

    private static final int MAX_DEPTH = 10;
    private static final int MAX_URLS = 100000;

    @Autowired
    private CrawlerProducer crawlerProducer;

    @Autowired
    private VisitedUrlService visitedUrlService;

    @Autowired
    private RateLimiterService rateLimiterService;

    public void crawlSingle(String url, int depth){

        if (url == null) return;
        if (visitedUrlService.isVisited(url)){
            System.out.println("Skipping- Already Visited url: " + url);
            return;
        }
        if (depth > MAX_DEPTH){
            System.out.println("Skipping- Depth exceeded " + depth+" Max Depth: " + MAX_DEPTH);
            return;
        }
        if (visitedUrlService.getSize() > MAX_URLS){
            System.out.println("Skipping- Visited URL full: " + visitedUrlService.getSize() + " Max URLS: " + MAX_URLS);
            return;
        }
//        if(!rateLimiterService.allow(getDomain(url))){
//            System.out.println("Skipping- Rate Limiting: " + url);
//            return;
//        }

        try{
            System.out.println("CRAWLING: " + url + " DEPTH: " + depth);

            String domain = getDomain(url);

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            visitedUrlService.markVisited(url);

            String title = document.title();
            String text = document.body().text();

            Elements links = document.select("a[href]");
            List<String> extractedLinks = new ArrayList<>();

            links.forEach(link -> {
                String normalized = UrlUtil.normalize(url, link.attr("href"));
                String childDomain = getDomain(normalized);
                System.out.println("############################################");
                System.out.println("PARENT DOMAIN: " + domain);
                System.out.println("CHILD DOMAIN: " + childDomain);
                if(normalized!=null && !visitedUrlService.isVisited(normalized) && childDomain.equals(domain)){
                    System.out.println("Adding to crawler: "+normalized);
                    extractedLinks.add(normalized);
                    crawlerProducer.sendUrl(normalized, depth+1);
                }
            });

            CrawledPage page = new CrawledPage(
                    url,
                    title,
                    text,
                    extractedLinks,
                    System.currentTimeMillis(),
                    depth
            );

            crawlerProducer.sendRawPage(JsonUtil.toJson(page));

        }
        catch(Exception e) {
            System.out.println("FAILED: " + url);
            System.out.println(e.getMessage());
        }
    }

    private String getDomain(String url) {
        try {
            return new URI(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
