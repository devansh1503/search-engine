package com.devansh.autocomplete.controller;

import com.devansh.autocomplete.service.AutoCompleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/autocomplete")
@CrossOrigin(origins = "*")
public class AutocompleteController {

    @Autowired
    private AutoCompleteService autoCompleteService;

    @GetMapping
    public List<String> autocomplete(@RequestParam String query) {
        return autoCompleteService.getSuggestions(query);
    }
}
