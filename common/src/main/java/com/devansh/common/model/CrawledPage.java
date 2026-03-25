package com.devansh.common.model;

import java.util.List;

public class CrawledPage {

    private String url;
    private String title;
    private String content;
    private List<String> links;
    private long timestamp;
    private int depth;

    public CrawledPage() {}
    public CrawledPage(String url, String title, String content,
                       List<String> links, long timestamp, int depth) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.links = links;
        this.timestamp = timestamp;
        this.depth = depth;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getLinks() { return links; }
    public void setLinks(List<String> links) { this.links = links; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

}