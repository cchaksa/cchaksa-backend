package com.chukchuk.haksa.global.config;

import io.lettuce.core.ReadFrom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory cf) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(redisHost);
        conf.setPort(redisPort);
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            conf.setPassword(RedisPassword.of(redisPassword));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder().readFrom(ReadFrom.REPLICA_PREFERRED);
        if (redisSslEnabled) builder.useSsl(); // prod=false 여야 함

        var cf = new LettuceConnectionFactory(conf, builder.build());
        return cf;
    }

    // 부팅 시 실제 효과값 로그 + 즉시 ping
    @Bean
    CommandLineRunner redisBootCheck(StringRedisTemplate rt) {
        return args -> {
            log.warn(">>> Redis effective: host={}, port={}, ssl={}",
                    redisHost, redisPort, redisSslEnabled);
            try {
                rt.opsForValue().set("__boot_check__", "ok");
                String v = rt.opsForValue().get("__boot_check__");
                log.warn(">>> Redis ping/set/get OK: {}", v);
            } catch (Exception e) {
                log.error(">>> Redis check FAILED", e);
            }
        };
    }
}
