package com.devansh.crawler.util;

import java.net.URI;

public class UrlUtil {

    public static String normalize(String baseUrl, String link){
        try {
            URI base =  new URI(baseUrl);
            URI resolved = base.resolve(link);
            return resolved.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
