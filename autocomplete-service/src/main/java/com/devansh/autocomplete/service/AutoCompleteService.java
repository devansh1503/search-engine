package com.devansh.autocomplete.service;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.ValueListOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoCompleteService {

    private static final String KEY = "autocomplete:suggest";
    private static final int LIMIT = 10;

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGADD =
            () -> "FT.SUGADD".getBytes();

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGGET =
            () -> "FT.SUGGET".getBytes();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String normalize(String input){
        //TODO: Need to check this
        return input==null ? "" : input.trim().toLowerCase();
    }

    public List<String> getSuggestions(String prefix){
        String normalized = normalize(prefix);
        if(normalized.length() < 2) return new ArrayList<>();

        RedisConnection connection = getConnection();
        try {
            List<byte[]> result = ((LettuceConnection) connection)
                    .getNativeConnection()
                    .dispatch(
                            FT_SUGGET,
                            new ValueListOutput<>(ByteArrayCodec.INSTANCE),
                            new CommandArgs<>(ByteArrayCodec.INSTANCE)
                                    .addKey(KEY.getBytes())
                                    .addValue(normalized.getBytes())
                                    .add("MAX").add(LIMIT)
                                    .add("FUZZY")
                    ).get();

            List<String> suggestions = new ArrayList<>();
            for (byte[] b : result) {
                suggestions.add(new String(b));
            }

            return suggestions;
        }catch (Exception e){
            throw new RuntimeException("Autocomplete Failed", e);
        }
    }

    private RedisConnection getConnection(){
        return stringRedisTemplate.getConnectionFactory().getConnection();
    }
}
