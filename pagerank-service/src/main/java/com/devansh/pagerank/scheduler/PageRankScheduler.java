package com.devansh.pagerank.scheduler;

import com.devansh.pagerank.service.ESUpdater;
import com.devansh.pagerank.service.PageRankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PageRankScheduler {

    @Autowired
    private PageRankService pageRankService;

    @Autowired
    private ESUpdater esUpdater;

    @Scheduled(fixedRate = 30000)
    public void run(){
        System.out.println("PageRank Scheduler Running...");
        pageRankService.compute();
        esUpdater.update();
    }
}
