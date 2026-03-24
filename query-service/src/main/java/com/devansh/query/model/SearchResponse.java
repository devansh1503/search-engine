package com.devansh.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResponse {
    private String url;
    private String title;
    private String snippet;
}
