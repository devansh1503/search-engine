package com.devansh.autocomplete.migration;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AutocompleteMigration implements CommandLineRunner {

    private static final String OLD_KEY = "autocomplete";
    private static final String NEW_KEY = "autocomplete:suggest";

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGADD =
            () -> "FT.SUGADD".getBytes();

    private static final io.lettuce.core.protocol.ProtocolKeyword FT_SUGGET =
            () -> "FT.SUGGET".getBytes();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(String... args) throws Exception {
        RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();
        LettuceConnection lettuce = (LettuceConnection) connection;

        Set<String> all = stringRedisTemplate.opsForZSet().range(OLD_KEY, 0, -1);

        if (all == null || all.isEmpty()) {
            System.out.println("NO DATA TO MIGRATE");
            return;
        }

        System.out.println("Migrating "+ all.size() +" items");
        for (String key : all) {
            Double score = stringRedisTemplate.opsForZSet().score(OLD_KEY, key);
            if (score == null) { score = 1.0;}

            lettuce.getNativeConnection().dispatch(
                    FT_SUGADD,
                    new IntegerOutput<>(ByteArrayCodec.INSTANCE),
                    new CommandArgs<>(ByteArrayCodec.INSTANCE)
                            .addKey(NEW_KEY.getBytes())
                            .addValue(key.getBytes())
                            .add(score)
            );
        }

        System.out.println("Migratioin Complete");
    }

}
