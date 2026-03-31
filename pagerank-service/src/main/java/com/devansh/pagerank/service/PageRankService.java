package com.devansh.pagerank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PageRankService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${pagerank.damping}")
    private double damping;

    @Value("${pagerank.iterations}")
    private int iterations;

    public void compute(){
        Set<String> nodes = stringRedisTemplate.opsForSet().members("graph:nodes");
        if(nodes == null || nodes.isEmpty()) return;

        int N = nodes.size();
        double init = 1.0/N;

        for(String url : nodes){
            stringRedisTemplate.opsForValue().set("pr:curr:"+url, String.valueOf(init));
        }

        for(int i=0; i<iterations; i++){
            for(String url : nodes){
                Set<String> incoming = stringRedisTemplate.opsForSet().members("graph:in:"+url);
                double sum = 0;

                if(incoming!=null) {
                    for(String url2 : incoming){
                        double pr = Double.parseDouble(
                                stringRedisTemplate.opsForValue().get("pr:curr:"+url2)
                        );

                        Long out = stringRedisTemplate.opsForSet().size("graph:out:"+url2);

                        if(out != null && out > 0){
                            sum += pr / out;
                        }
                    }
                }

                double newPR = (1 - damping) / N + damping * sum;

                stringRedisTemplate.opsForValue().set("pr:next:"+ url, String.valueOf(newPR));
            }

            for(String url : nodes){
                String val = stringRedisTemplate.opsForValue().get("pr:next:"+url);
                stringRedisTemplate.opsForValue().set("pr:curr:"+url, val);
            }
        }

        for(String url : nodes){
            String val = stringRedisTemplate.opsForValue().get("pr:curr:"+url);
            stringRedisTemplate.opsForValue().set("pagerank:"+url, val);
        }
        System.out.println("PageRank Done");

    }
}
