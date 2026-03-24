package com.devansh.common.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CrawledPage {
    private String url;
    private String title;
    private String content;
    private List<String> links;
    private long timestamp;
}
