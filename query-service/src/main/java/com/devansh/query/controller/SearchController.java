package com.devansh.query.controller;

import com.devansh.query.model.SearchResponse;
import com.devansh.query.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public List<SearchResponse> search(@RequestParam String query) {
        return searchService.search(query);
    }

    @PostMapping("/ai-summary")
    public ResponseEntity<String> searchAiSummary(@RequestParam String query) {
        String response = searchService.searchAiSummary(query);
        return ResponseEntity.ok(response);
    }
}
